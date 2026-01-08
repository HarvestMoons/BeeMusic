package com.example.musicplayer.repository;

import com.example.musicplayer.model.Meme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemeRepository extends JpaRepository<Meme, Long> {

    // Efficient random selection: 1. Count, 2. Page(random_index)
    long countByIsDeletedFalse();

    Page<Meme> findByIsDeletedFalse(Pageable pageable);

    boolean existsByKey(String key);

    @org.springframework.data.jpa.repository.Query("SELECT m.key FROM Meme m")
    java.util.List<String> findAllKeys();
}
