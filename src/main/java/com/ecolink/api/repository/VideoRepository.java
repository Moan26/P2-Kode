package com.ecolink.api.repository;

import com.ecolink.api.model.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {

    // Hent alle videoer filtreret på om de er udgivet eller ej
    Page<Video> findByIsPublished(boolean isPublished, Pageable pageable);

    // Hent alle videoer oprettet af en bestemt bruger
    Page<Video> findByCreatedBy(String createdBy, Pageable pageable);

    // Hent alle videoer filtreret på både udgivetstatus og opretter
    Page<Video> findByIsPublishedAndCreatedBy(boolean isPublished, String createdBy, Pageable pageable);
}