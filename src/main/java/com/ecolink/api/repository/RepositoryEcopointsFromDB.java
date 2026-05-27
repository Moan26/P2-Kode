package com.ecolink.api.repository;

import com.ecolink.api.model.Ecopoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryEcopointsFromDB
        extends MongoRepository<Ecopoint, String>, RepositoryEcopointsFromDBCustom {
}