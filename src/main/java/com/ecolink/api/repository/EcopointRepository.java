package com.ecolink.api.repository;

import com.ecolink.api.model.Ecopoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EcopointRepository extends MongoRepository<Ecopoint, String> {

	// Midlertidigt beholdt helt simpelt, så repository matcher den nuværende Ecopoint-model
	// uden felter som isActive, name eller address, som ikke findes i modellen endnu.

}