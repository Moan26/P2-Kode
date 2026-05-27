package com.ecolink.api.service;

import com.ecolink.api.dto.UpdateImageRequest;
import com.ecolink.api.model.Dimensions;
import com.ecolink.api.model.Image;
import com.ecolink.api.model.ImageMetaData;
import com.ecolink.api.model.ImageResolutions;
import com.ecolink.api.repository.ImageRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.bson.Document;
import com.ecolink.api.service.ImageProcessingService;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final GridFsTemplate gridFsTemplate;
    private final GridFSBucket imagesGridFsBucket;
    private final GridFSBucket thumbnailsGridFsBucket;
    private final GridFSBucket mediumGridFsBucket;
    private final GridFSBucket largeGridFsBucket;
    private final ImageProcessingService imageProcessingService;

    public ImageService(ImageRepository imageRepository,
                        GridFsTemplate gridFsTemplate,
                        GridFSBucket imagesGridFsBucket,
                        GridFSBucket thumbnailsGridFsBucket,
                        GridFSBucket mediumGridFsBucket,
                        GridFSBucket largeGridFsBucket,
                        ImageProcessingService imageProcessingService) {
        this.imageRepository = imageRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.imagesGridFsBucket = imagesGridFsBucket;
        this.thumbnailsGridFsBucket = thumbnailsGridFsBucket;
        this.mediumGridFsBucket = mediumGridFsBucket;
        this.largeGridFsBucket = largeGridFsBucket;
        this.imageProcessingService = imageProcessingService;
    }


    public Image uploadImage(MultipartFile imageFile,
                             String title,
                             String altText,
                             String description,
                             String caption,
                             Authentication authentication) throws IOException {

        if (imageFile == null || imageFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        if (altText == null || altText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alt_text is required");
        }

        validateImageFile(imageFile);
        String resolvedMimeType = resolveMimeType(imageFile);
        String filename = imageFile.getOriginalFilename() != null
                ? imageFile.getOriginalFilename().toLowerCase()
                : "";

        BufferedImage bufferedImage = ImageIO.read(imageFile.getInputStream());
        if (bufferedImage == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        ObjectId fileId;
        try (InputStream inputStream = imageFile.getInputStream()) {
            fileId = gridFsTemplate.store(
                    inputStream,
                    imageFile.getOriginalFilename(),
                    resolvedMimeType
            );
        }

        String formatName = detectFormatName(filename);
        String resizedMimeType = formatNameToMimeType(formatName);

        ImageMetaData metaData = ImageMetaData.builder()
                .fileSize(imageFile.getSize())
                .format(resolvedMimeType)
                .dimensions(new Dimensions(width, height))
                .build();

        String currentUsername = authentication != null ? authentication.getName() : null;

        Image image = Image.builder()
                .imageUrl(fileId.toHexString())
                .title(title)
                .alt_text(altText)
                .description(description)
                .caption(caption)
                .uploadedAt(Instant.now())
                .updatedAt(Instant.now())
                .createdBy(currentUsername)
                .isPublished(false)
                .imageMetaData(metaData)
                .build();

        Image savedImage = imageRepository.save(image);

        imageProcessingService.processImageResolutionsAsync(
                savedImage,
                bufferedImage,
                imageFile.getOriginalFilename(),
                formatName,
                resizedMimeType
        );

        return savedImage;

    }


    //used by /images/{id}
    // Optional<Image> is used because findById may or may not return a result.
    // It lets us handle the "not found" case explicitly without working directly with null.
    public Image findById(String id) {
        Optional<Image> e = imageRepository.findById(id);
            if (e.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Image with id %s not found", id));
            }
            return e.get();
    }

    //used by /images for getting paginated list
        public Page<Image> getImagesInList(Boolean isPublished, String createdBy,int page, int limit){


            PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by("updatedAt").descending());

            if (isPublished != null && createdBy != null) {
                return imageRepository.findByIsPublishedAndCreatedBy(isPublished, createdBy, pageable);
            } else if (isPublished != null) {
                return imageRepository.findByIsPublished(isPublished, pageable);
            } else if (createdBy != null) {
                return imageRepository.findByCreatedBy(createdBy, pageable);
            } else {
                return imageRepository.findAll(pageable);
            }
        }

    // Used by PATCH /api/images/{id}
    // Updates only fields sent in the request
    public Image updateImage(String id,
                             UpdateImageRequest request,
                             MultipartFile imageFile,
                             Authentication authentication) throws IOException {

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication is required");
        }

        Image image = findById(id);

        String currentUsername = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = image.getCreatedBy() != null
                && image.getCreatedBy().equals(currentUsername);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to modify this image");
        }

        if (request != null) {
            if (request.getTitle() != null) {
                if (request.getTitle().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be blank");
                }
                image.setTitle(request.getTitle());
            }

            if (request.getAltText() != null) {
                if (request.getAltText().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alt_text cannot be blank");
                }
                image.setAlt_text(request.getAltText());
            }

            if (request.getDescription() != null) {
                image.setDescription(request.getDescription());
            }

            if (request.getCaption() != null) {
                image.setCaption(request.getCaption());
            }

            if (request.getIsPublished() != null) {
                image.setIsPublished(request.getIsPublished());
            }
        }

        BufferedImage bufferedImage = null;
        String originalFilename = null;
        String formatName = null;
        String resizedMimeType = null;

        if (imageFile != null && !imageFile.isEmpty()) {
            validateImageFile(imageFile);

            bufferedImage = ImageIO.read(imageFile.getInputStream());
            if (bufferedImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
            }

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            String resolvedMimeType = resolveMimeType(imageFile);

            originalFilename = imageFile.getOriginalFilename() != null
                    ? imageFile.getOriginalFilename()
                    : "image";

            String lowerFilename = originalFilename.toLowerCase();
            formatName = detectFormatName(lowerFilename);
            resizedMimeType = formatNameToMimeType(formatName);

            ObjectId newOriginalFileId;
            try (InputStream inputStream = imageFile.getInputStream()) {
                newOriginalFileId = gridFsTemplate.store(
                        inputStream,
                        originalFilename,
                        resolvedMimeType
                );
            }

            deleteImageFiles(image);

            image.setImageUrl(newOriginalFileId.toHexString());
            image.setResolutions(null);

            image.setImageMetaData(ImageMetaData.builder()
                    .fileSize(imageFile.getSize())
                    .format(resolvedMimeType)
                    .dimensions(new Dimensions(width, height))
                    .build());
        }

        image.setUpdatedAt(Instant.now());

        Image savedImage = imageRepository.save(image);

        if (imageFile != null && !imageFile.isEmpty()) {
            imageProcessingService.processImageResolutionsAsync(
                    savedImage,
                    bufferedImage,
                    originalFilename,
                    formatName,
                    resizedMimeType
            );
        }

        return savedImage;
    }

    // Used by DELETE /api/images/{id}
    // Deletes image if user is owner or admin
    public void deleteImage(String id, Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication is required");
        }

        Image image = findById(id);

        String currentUsername = authentication.getName();

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = image.getCreatedBy() != null &&
                image.getCreatedBy().equals(currentUsername);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to delete this image");
        }
        deleteImageFiles(image);
        imageRepository.deleteById(id);
    }


    private String detectFormatName(String filename) {
        String lower = filename.toLowerCase();

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "jpg";
        } else if (lower.endsWith(".png")) {
            return "png";
        } else if (lower.endsWith(".gif")) {
            return "gif";
        } else if (lower.endsWith(".webp")) {
            return "png";
        }

        return "png";
    }
    private String resolveMimeType(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        String filename = imageFile.getOriginalFilename() != null
                ? imageFile.getOriginalFilename().toLowerCase()
                : "";

        if (contentType != null
                && !contentType.isBlank()
                && !contentType.equals("application/octet-stream")) {
            return contentType;
        }

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".webp")) return "image/webp";

        return "image/png";
    }
    private String formatNameToMimeType(String formatName) {
        return switch (formatName.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }
    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        String contentType = imageFile.getContentType();
        String filename = imageFile.getOriginalFilename() != null
                ? imageFile.getOriginalFilename().toLowerCase()
                : "";

        boolean validContentType = List.of("image/jpeg", "image/png", "image/webp", "image/gif")
                .contains(contentType);

        boolean validExtension = filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp")
                || filename.endsWith(".gif");

        if (!validContentType && !validExtension) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format");
        }

        long maxSize = 10L * 1024 * 1024;
        if (imageFile.getSize() > maxSize) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File too large");
        }
    }
    private void deleteImageFiles(Image image) {
        if (image.getImageUrl() != null && !image.getImageUrl().isBlank()) {
            imagesGridFsBucket.delete(new ObjectId(image.getImageUrl()));
        }

        if (image.getResolutions() != null) {
            if (image.getResolutions().getThumbnail() != null
                    && !image.getResolutions().getThumbnail().isBlank()) {
                thumbnailsGridFsBucket.delete(new ObjectId(image.getResolutions().getThumbnail()));
            }

            if (image.getResolutions().getMedium() != null
                    && !image.getResolutions().getMedium().isBlank()) {
                mediumGridFsBucket.delete(new ObjectId(image.getResolutions().getMedium()));
            }

            if (image.getResolutions().getLarge() != null
                    && !image.getResolutions().getLarge().isBlank()) {
                largeGridFsBucket.delete(new ObjectId(image.getResolutions().getLarge()));
            }
        }
    }
    public GridFsResource getImageStream(String fileId) { //Henter image file i GridFS ved ID og kaldes af StreamImage i ImageController.
        GridFSFile file = gridFsTemplate.findOne( //Finder fil i GridFS med ID og konvertere til noget MongoDB kan aflæse.
                new Query(Criteria.where("_id").is(new ObjectId(fileId)))
        );
        if (file == null) throw new //Kaster 404 hvis filen ikke eksistere (Hvis id'et man brugte ikke findes)
                ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        return gridFsTemplate.getResource(file); //Returnere filen som streambar ressource så Controlleren kan bruge det.
    }
}
