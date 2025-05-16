package com.shelf_sense_backend.service;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.shelf_sense_backend.model.UserEntity;



public interface UserService extends UserDetailsService{

	public UserEntity registerUser(UserEntity userData);

	//public void deleteUserById(Long id);
}
