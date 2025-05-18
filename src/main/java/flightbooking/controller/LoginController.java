package com.flightbooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.flightbooking.model.User;
import com.flightbooking.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder pass;

    @GetMapping("/")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/register/new_admin")
    public String registerAdmin(@ModelAttribute("user") User user, Model model) {
        model.addAttribute("userExist", false);
        model.addAttribute("user", user);
        return "register_admin";
    }

    @GetMapping("/register/new_customer")
    public String registerCustomer(@ModelAttribute("user") User user, Model model) {
        model.addAttribute("userExist", false);
        model.addAttribute("user", user);
        return "register_customer";
    }

    @PostMapping("/register/save_admin")
    public String saveAdminRegistration(@ModelAttribute("user") User user, Model model) {
        model.addAttribute("userExist", false);
        model.addAttribute("registerSuccess", false);

        String username = user.getUsername();
        if (!userRepository.existsByUsername(username)) {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(pass.encode(user.getPassword()));
            newUser.setRole("admin");

            userRepository.save(newUser);
            model.addAttribute("registerSuccess", true);
        } else {
            model.addAttribute("userExist", true);
        }

        model.addAttribute("user", user);
        return "register_admin";
    }

    @PostMapping("/register/save_customer")
    public String saveCustomerRegistration(@ModelAttribute("user") User user, Model model) {
        model.addAttribute("userExist", false);
        model.addAttribute("registerSuccess", false);

        String username = user.getUsername();
        if (!userRepository.existsByUsername(username)) {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(pass.encode(user.getPassword()));
            newUser.setFirstName(user.getFirstName());
            newUser.setLastName(user.getLastName());
            newUser.setEmail(user.getEmail());
            newUser.setDob(user.getDob());
            newUser.setPhone(user.getPhone());
            newUser.setAddress(user.getAddress());
            newUser.setRole("user");

            userRepository.save(newUser);
            model.addAttribute("registerSuccess", true);
        } else {
            model.addAttribute("userExist", true);
        }

        model.addAttribute("user", user);
        return "register_customer";
    }

    @GetMapping("/login_page")
    public String loginPage(@ModelAttribute("user") User user, Model model) {
        model.addAttribute("LoginFailed", false);
        model.addAttribute("user", user);
        return "login_page";
    }

    @PostMapping("/post_login")
    public String login(@ModelAttribute("user") User user, Model model, HttpSession session) {
        model.addAttribute("LoginFailed", false);
        model.addAttribute("user", user);

        User savedUser = userRepository.findByUsername(user.getUsername());
        if (savedUser != null && pass.matches(user.getPassword(), savedUser.getPassword())) {
            session.setAttribute("user", savedUser);
            return savedUser.getRole().equalsIgnoreCase("admin") ?
                   "redirect:/admin/dashboard_admin" :
                   "redirect:/user/dashboard_customer";
        } else {
            model.addAttribute("LoginFailed", true);
            return "login_page";
        }
    }

    @GetMapping("/log_out")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/";
    }

    @GetMapping("/access_denied")
    public String accessDenied() {
        return "access_denied";
    }
}
