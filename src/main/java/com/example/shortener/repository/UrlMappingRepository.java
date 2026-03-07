package com.example.shortener.repository;

import com.example.shortener.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, String> {
    // Explicitly declare findById to help IDE resolution
    Optional<UrlMapping> findById(String id);
}
