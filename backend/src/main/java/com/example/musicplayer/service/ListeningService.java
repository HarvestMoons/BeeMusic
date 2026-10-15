package com.example.musicplayer.service;

import com.example.musicplayer.dto.ListeningReport;
import com.example.musicplayer.dto.ListeningStatsResponse;
import com.example.musicplayer.exception.UserOperationException;
import com.example.musicplayer.model.UserListeningSession;
import com.example.musicplayer.model.UserSongListeningStats;
import com.example.musicplayer.repository.ListeningSessionRepository;
import com.example.musicplayer.repository.SongRepository;
import com.example.musicplayer.repository.UserRepository;
import com.example.musicplayer.repository.UserSongListeningStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListeningService {
    private final ListeningSessionRepository sessionRepository;
    private final UserSongListeningStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    @Transactional
    public boolean start(Long userId, String playbackId, Long songId) {
        validateId(playbackId);
        lockUser(userId);
        var existing = sessionRepository.findLockedById(playbackId);
        if (existing.isPresent()) {
            checkOwner(existing.get(), userId);
            if (!Objects.equals(existing.get().getSongId(), songId)) throw new UserOperationException("播放记录与歌曲不一致");
            return !existing.get().isClosed();
        }
        if (songId == null || songId <= 0) throw new UserOperationException("歌曲 ID 不正确");
        var song = songRepository.findLockedById(songId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲不存在"));
        if (song.getIsDeleted() != 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "歌曲已删除");
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.closeOpenSessions(userId);
        UserListeningSession playback = new UserListeningSession();
        playback.setId(playbackId);
        playback.setUserId(userId);
        playback.setSongId(songId);
        playback.setSongName(song.getName());
        playback.setStartedAt(now);
        playback.setLastReportedAt(now);
        sessionRepository.save(playback);
        return true;
    }

    @Transactional
    public boolean report(Long userId, String playbackId, ListeningReport report) {
        validateId(playbackId);
        lockUser(userId);
        var playback = sessionRepository.findLockedById(playbackId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "播放记录不存在"));
        checkOwner(playback, userId);
        if (report.sequence() == null || report.sequence() <= 0 || report.listenedMs() == null ||
                report.listenedMs() < 0 || report.listenedMs() > 604_800_000L) {
            throw new UserOperationException("听歌记录参数不正确");
        }
        if (playback.isClosed()) return false;
        if (report.sequence() <= playback.getLastSequence()) return true;
        if (report.listenedMs() < playback.getClientListenedMs()) throw new UserOperationException("听歌时长不能回退");
        LocalDateTime now = LocalDateTime.now();
        // 会话创建请求与音频开始可能有少量网络延迟，保留 5 秒启动宽限。
        long budget = Math.max(0, Duration.between(playback.getStartedAt(), now).toMillis() + 5000 - playback.getListenedMs());
        long delta = Math.min(report.listenedMs() - playback.getClientListenedMs(), budget);
        playback.setListenedMs(Math.addExact(playback.getListenedMs(), delta));
        playback.setClientListenedMs(report.listenedMs());
        playback.setLastSequence(report.sequence());
        playback.setClosed(report.closed());
        playback.setLastReportedAt(now);
        if (delta > 0) addStats(playback, delta, now);
        return true;
    }

    @Transactional(readOnly = true)
    public ListeningStatsResponse summary(Long userId) {
        long total = statsRepository.totalListenedMs(userId);
        var favorites = statsRepository.findFavorites(userId, PageRequest.of(0, 1));
        return new ListeningStatsResponse(total, favorites.isEmpty() ? null : favorites.getFirst().getSongName());
    }

    private void lockUser(Long userId) {
        if (userRepository.findActiveLockedById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "用户不可用");
        }
    }

    private void checkOwner(UserListeningSession playback, Long userId) {
        if (!Objects.equals(playback.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改播放记录");
    }

    /** 用户行锁覆盖整个事务，首次创建统计和累计更新都不会与同一用户的其他请求并发。 */
    private void addStats(UserListeningSession playback, long delta, LocalDateTime now) {
        UserSongListeningStats stats = statsRepository.findByUserIdAndSongId(playback.getUserId(), playback.getSongId())
                .orElseGet(() -> {
                    UserSongListeningStats created = new UserSongListeningStats();
                    created.setUserId(playback.getUserId());
                    created.setSongId(playback.getSongId());
                    created.setFirstListenedAt(now);
                    return created;
                });
        stats.setSongName(playback.getSongName());
        stats.setListenedMs(Math.addExact(stats.getListenedMs(), delta));
        stats.setLastListenedAt(now);
        // 已查询到的实体由 JPA 在事务提交时同步，仅新统计需要显式保存。
        if (stats.getId() == null) statsRepository.save(stats);
    }

    private void validateId(String id) {
        try {
            if (id == null || !UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException error) {
            throw new UserOperationException("播放记录 ID 不正确");
        }
    }
}
