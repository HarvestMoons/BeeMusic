package com.example.musicplayer.controller;

import com.example.musicplayer.model.User;
import com.example.musicplayer.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/unlock-hidden-playlist")
    public ResponseEntity<String> unlockHiddenPlaylist(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        userService.unlockHiddenPlaylist(user.getId());
        return ResponseEntity.ok("Hidden playlist unlocked");
    }
}
