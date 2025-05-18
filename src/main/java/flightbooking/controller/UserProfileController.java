package com.flightbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.flightbooking.model.User;
import com.flightbooking.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserProfileController {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder pass;

    @GetMapping("user/customer/profile")
    public String viewProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        User savedUser = userRepository.findByUsername(user.getUsername());
        model.addAttribute("user", savedUser);
        model.addAttribute("updateSuccess", false);
        return "view_profile";
    }

    @PostMapping("user/customer/save_profile")
    public String saveProfile(@ModelAttribute("user") User user, Model model) {
        User savedUser = userRepository.findByUsername(user.getUsername());
        savedUser.setPassword(pass.encode(user.getPassword()));
        savedUser.setFirstName(user.getFirstName());
        savedUser.setLastName(user.getLastName());
        savedUser.setEmail(user.getEmail());
        savedUser.setDob(user.getDob());
        savedUser.setPhone(user.getPhone());
        savedUser.setAddress(user.getAddress());
        
        userRepository.save(savedUser);
        
        model.addAttribute("user", user);
        model.addAttribute("updateSuccess", true);
        return "view_profile";
    }
}
