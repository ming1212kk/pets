package com.pet.social.repository;

import com.pet.social.entity.PetProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetProfileRepository extends JpaRepository<PetProfileEntity, Long> {
    List<PetProfileEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByOwnerIdAndDefaultPetTrue(Long ownerId);
}
