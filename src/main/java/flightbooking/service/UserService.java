package com.flightbooking.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.flightbooking.model.User;

public interface UserService extends UserDetailsService {

	public User findByUserName(String userName);

}
