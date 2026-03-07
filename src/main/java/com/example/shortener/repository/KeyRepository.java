package com.example.shortener.repository;

import com.example.shortener.entity.Key;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeyRepository extends JpaRepository<Key, Long> {
    
    Optional<Key> findByKeyValue(String keyValue);
    
    boolean existsByKeyValue(String keyValue);
    
    Optional<Key> findFirstByIsUsedFalseOrderByCreatedAtAsc();
    
    @Query("SELECT COUNT(k) FROM Key k WHERE k.isUsed = false")
    long countUnusedKeys();
    
    @Modifying
    @Query("UPDATE Key k SET k.isUsed = true WHERE k.keyValue = ?1")
    int markAsUsed(String keyValue);
}
