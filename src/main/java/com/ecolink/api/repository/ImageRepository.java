package com.ecolink.api.repository;

import com.ecolink.api.model.Image;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends MongoRepository<Image, String> {

    Page<Image> findByIsPublished(Boolean isPublished, Pageable pageable);

    Page<Image> findByCreatedBy(String createdBy, Pageable pageable);

    Page<Image> findByIsPublishedAndCreatedBy(Boolean isPublished, String createdBy, Pageable pageable);
}