package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    private UserRepository userRepository;  // Wire in the UserRepository

    @PutMapping("/user")
    public ResponseEntity<?> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response

        // Check if the email or username already exists in the database
        if (userRepository.existsByEmail(payload.getEmail()) || userRepository.existsByName(payload.getName())) {
            return ResponseEntity.badRequest().body("Email or Username already in use");
        }

        // Create a user entity from the payload
        User newUser = new User();
        newUser.setName(payload.getName());
        newUser.setEmail(payload.getEmail());  // Assuming email is part of the payload
        newUser = userRepository.save(newUser);  // Save the new user to the database

        // Return the ID of the new user
        return ResponseEntity.ok(newUser.getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        // Check if a user exists with the given ID
        return userRepository.findById(userId).map(user -> {
            userRepository.delete(user);  // Delete the user if found
            return ResponseEntity.ok("User deleted successfully");
        }).orElseGet(() -> ResponseEntity.badRequest().body("No user found with ID: " + userId));
    }
}
