package org.example.authchecking.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProfileController {

    @GetMapping("/profile")
    public String profile(Authentication authentication) {
        return "Hello " + authentication.getName();
    }

    @PostMapping("/profile/update")
    public String updateProfile() {
        return "Profile updated";
    }
}
