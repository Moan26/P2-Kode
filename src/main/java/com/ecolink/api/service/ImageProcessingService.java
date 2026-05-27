package com.ecolink.api.service;

import com.ecolink.api.model.ImageResolutions;
import com.ecolink.api.repository.ImageRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ecolink.api.model.Image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import javax.imageio.ImageIO;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.imgscalr.Scalr;

@Service
public class ImageProcessingService {

    private final ImageRepository imageRepository;
    private final GridFSBucket thumbnailsGridFsBucket;
    private final GridFSBucket mediumGridFsBucket;
    private final GridFSBucket largeGridFsBucket;

    public ImageProcessingService(ImageRepository imageRepository,
                                  GridFSBucket thumbnailsGridFsBucket,
                                  GridFSBucket mediumGridFsBucket,
                                  GridFSBucket largeGridFsBucket) {
        this.imageRepository = imageRepository;
        this.thumbnailsGridFsBucket = thumbnailsGridFsBucket;
        this.mediumGridFsBucket = mediumGridFsBucket;
        this.largeGridFsBucket = largeGridFsBucket;
    }

    @Async
    public void processImageResolutionsAsync(Image image,
                                             BufferedImage bufferedImage,
                                             String originalFilename,
                                             String formatName,
                                             String resizedMimeType) {
        try {
            BufferedImage thumbnailImage = Scalr.resize(
                    bufferedImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.FIT_TO_WIDTH,
                    200
            );

            BufferedImage mediumImage = Scalr.resize(
                    bufferedImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.FIT_TO_WIDTH,
                    600
            );

            BufferedImage largeImage = Scalr.resize(
                    bufferedImage,
                    Scalr.Method.QUALITY,
                    Scalr.Mode.FIT_TO_WIDTH,
                    1200
            );

            String thumbnailFileId = storeResizedImage(
                    thumbnailImage,
                    "thumb_" + originalFilename,
                    formatName,
                    resizedMimeType,
                    thumbnailsGridFsBucket
            );

            String mediumFileId = storeResizedImage(
                    mediumImage,
                    "medium_" + originalFilename,
                    formatName,
                    resizedMimeType,
                    mediumGridFsBucket
            );

            String largeFileId = storeResizedImage(
                    largeImage,
                    "large_" + originalFilename,
                    formatName,
                    resizedMimeType,
                    largeGridFsBucket
            );

            image.setResolutions(ImageResolutions.builder()
                    .thumbnail(thumbnailFileId)
                    .medium(mediumFileId)
                    .large(largeFileId)
                    .build());

            image.setUpdatedAt(Instant.now());
            imageRepository.save(image);

        } catch (IOException e) {
            throw new RuntimeException("Failed to process image resolutions", e);
        }
    }

    private String storeResizedImage(BufferedImage image,
                                     String originalFilename,
                                     String formatName,
                                     String mimeType,
                                     GridFSBucket bucket) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, outputStream);

        Document metadata = new Document("contentType", mimeType);
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(metadata);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
            ObjectId fileId = bucket.uploadFromStream(originalFilename, inputStream, options);
            return fileId.toHexString();
        }
    }
}