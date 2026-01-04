package com.example.musicplayer.repository;

import com.example.musicplayer.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Query("SELECT s FROM Song s WHERE s.key LIKE :prefix% AND s.isDeleted = :isDeleted")
    List<Song> findByKeyStartingWithAndIsDeleted(@Param("prefix") String prefix,
                                                 @Param("isDeleted") int isDeleted);

    @Modifying
    @Transactional
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :id")
    void incrementPlayCount(@Param("id") Long id);

    @Query("SELECT s FROM Song s WHERE s.key LIKE :prefix%")
    List<Song> findByKeyStartingWith(@Param("prefix") String prefix);

    @Query("SELECT COUNT(s) FROM Song s WHERE s.isDeleted = 0 AND s.key LIKE CONCAT(:prefix, '%')")
    long countActiveSongsByPrefix(@Param("prefix") String prefix);
}

