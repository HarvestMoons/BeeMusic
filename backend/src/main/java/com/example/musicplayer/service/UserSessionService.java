package com.example.musicplayer.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

@Service
public class UserSessionService {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    public UserSessionService(FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public void invalidateOtherSessions(String principalName, HttpServletRequest request) {
        HttpSession currentSession = request.getSession(false);
        String currentSessionId = currentSession == null ? null : currentSession.getId();
        sessionRepository.findByPrincipalName(principalName).keySet()
                .forEach(sessionId -> {
                    if (!sessionId.equals(currentSessionId)) {
                        sessionRepository.deleteById(sessionId);
                    }
                });
    }
}
