package com.example.musicplayer.repository;

import com.example.musicplayer.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Query("SELECT s FROM Song s WHERE s.key LIKE :prefix% AND s.isDeleted = :isDeleted")
    List<Song> findByKeyStartingWithAndIsDeleted(@Param("prefix") String prefix,
                                                 @Param("isDeleted") int isDeleted);
}

