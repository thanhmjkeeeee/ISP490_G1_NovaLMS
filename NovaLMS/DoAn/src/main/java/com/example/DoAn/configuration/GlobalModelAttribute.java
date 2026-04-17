package com.example.DoAn.configuration;

import com.example.DoAn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private final UserRepository userRepository;

    @ModelAttribute
    public void addUserProfile(Model model, Principal principal) {
        if (principal != null) {
            userRepository.findByEmailWithRole(principal.getName()).ifPresent(user -> {
                model.addAttribute("userProfile", user);
            });
        }
    }
}
