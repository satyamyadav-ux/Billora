package com.billora.modules.auth.controller;

import com.billora.common.exception.BusinessException;
import com.billora.modules.auth.dto.RegisterRequestDto;
import com.billora.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {

        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email address or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully logged out.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Account created successfully! You can now sign in with your credentials.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterRequestDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegistration(
            @Valid @ModelAttribute("registerDto") RegisterRequestDto registerDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }

        // Email uniqueness validation
        if (registerDto.getEmail() != null && authService.isEmailRegistered(registerDto.getEmail())) {
            bindingResult.rejectValue("email", "error.registerDto", "This email is already registered. Please sign in or use another email.");
        }

        // Password confirmation match validation
        if (registerDto.getPassword() != null && !registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.registerDto", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerCustomer(registerDto);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please sign in.");
            return "redirect:/login?registered=true";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("pageTitle", "Access Denied");
        return "auth/access-denied";
    }
}
