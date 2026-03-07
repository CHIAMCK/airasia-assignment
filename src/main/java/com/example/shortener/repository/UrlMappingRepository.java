package com.example.shortener.repository;

import com.example.shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {
    // No need to redeclare findById - it's inherited from JpaRepository
}
