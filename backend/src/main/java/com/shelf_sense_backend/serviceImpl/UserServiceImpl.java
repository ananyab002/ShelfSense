package com.shelf_sense_backend.serviceImpl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.shelf_sense_backend.model.UserEntity;
import com.shelf_sense_backend.repo.UserRepository;
import com.shelf_sense_backend.service.UserService;

import jakarta.transaction.Transactional;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    
    @Override
    @Transactional
    public UserEntity registerUser(UserEntity user) {
        // Check if the user already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return null;
        }

        // Encode the password and save the user
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserEntity savedUser=userRepository.save(user);
        // try {
        	
        // 	//userSearchService.indexUser(savedUser.getUserId(), savedUser.getEmail(), savedUser.getName(), savedUser.getPhoneNumber(), savedUser.getCountry(),savedUser.getImage());
        // 	/*UserSearchEntity userSearch = new UserSearchEntity();
        //     userSearch.setEmail(savedUser.getEmail());
        //     userSearch.setName(savedUser.getName());
        //     userSearch.setPhoneNumber(savedUser.getPhoneNumber());*/
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to index user in Elasticsearch", e);
        // }
        return savedUser ;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch user details from the database using the email as username
        Optional<UserEntity> userDetail = userRepository.findByEmail(username);

        UserEntity user = userDetail.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(user.getEmail())  
                .password(user.getPassword())  
               // .roles(user.getRoles().toArray(new String[0])) 
                .build();
    }
    

	/* @Override
	    public void deleteUserById(Long userId) {
	        // You can delete the user by ID like this:
	        userSearchRepository.deleteById(userId);  // Deletes from Elasticsearch
	        userRepository.deleteById(userId);  // Deletes from your relational database
	    }*/
}

