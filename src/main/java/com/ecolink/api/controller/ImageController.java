package com.ecolink.api.controller;

import com.ecolink.api.dto.UpdateImageRequest;
import com.ecolink.api.model.Image;
import com.ecolink.api.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
@Tag(
        name = "Images",
        description = "Endpoints for uploading, retrieving, updating and deleting images"
)
@RestController
@RequestMapping("/api")
public class ImageController {


    //Using constructor injection
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    //Image upload endpoints
   @Operation(
            summary = "Upload image",
            description = "Uploads a new image with metadata and stores it in the system"
    )
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Image> uploadImage(
            @RequestPart("imageFile") MultipartFile imageFile,
            @RequestPart("title") String title,
            @RequestPart("alt_text") String altText,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "caption", required = false) String caption,
            Authentication authentication
    ) throws IOException {

        Image created = imageService.uploadImage(imageFile, title, altText, description, caption, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /*	GET /api/images returns paginated list of all images (default 20 per page)
        GET /api/images supports filters: isPublished=true, createdBy=userId  */
    @Operation(
            summary = "Get images",
            description = "Returns a paginated list of images with optional filtering by published status or creator"
    )
    @GetMapping("/images")
    public ResponseEntity<?> getImageList(
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

        //Input validation for page and limit
        if (page < 1 || limit < 1 || limit > 100) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid pagination parameters"));
        }

        //building the response for the page response
        Page<Image> images = imageService.getImagesInList(isPublished, createdBy, page, limit);

        //what we will include in our response.
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", images.getContent(),
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", images.getTotalElements(),
                        "totalPages", images.getTotalPages())));
    }
    @Operation(
            summary = "Get image by ID",
            description = "Retrieves a single image by its unique ID"
    )
    @GetMapping("/images/{id}")
    public Image getImageById(@PathVariable String id){
        return imageService.findById(id);
    }

    @Operation(
            summary = "Update image",
            description = "Updates image metadata and optionally replaces the image file. Image processing runs asynchronously if a new file is provided"
    )
    @PatchMapping(value = "/images/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Image> updateImage(
            @PathVariable String id,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestPart(value = "title", required = false) String title,
            @RequestPart(value = "alt_text", required = false) String altText,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "caption", required = false) String caption,
            @RequestPart(value = "isPublished", required = false) Boolean isPublished,
            Authentication authentication
    ) throws IOException {

        UpdateImageRequest request = new UpdateImageRequest();
        request.setTitle(title);
        request.setAltText(altText);
        request.setDescription(description);
        request.setCaption(caption);
        request.setIsPublished(isPublished);

        Image updatedImage = imageService.updateImage(id, request, imageFile, authentication);
        return ResponseEntity.ok(updatedImage);
    }

    // DELETE /api/images/{id}
    // Deletes a specific image (only owner or admin allowed)
    @Operation(
            summary = "Delete image",
            description = "Deletes an image. Only the owner or an admin can perform this action"
    )
    @DeleteMapping("/images/{id}")
    public ResponseEntity<?> deleteImage(
            @PathVariable String id,
            Authentication authentication
    ){
        imageService.deleteImage(id, authentication);

        // 204 No Content = standard for successful delete
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/images/stream/{fileId}") // Gør det muligt at stream billeder
    public ResponseEntity<InputStreamResource> streamImage(
            @PathVariable String fileId) throws IOException {
        GridFsResource resource = imageService.getImageStream(fileId); //henter fil fra GridFS ved hjælp af ImageService.
        return ResponseEntity.ok() //Returnere korrekt filtype som størrelse og format (JPEG osv.)
                .contentType(MediaType.parseMediaType(resource.getContentType()))
                .contentLength(resource.contentLength())
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
