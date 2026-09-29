package com.example.demo.Controllers;

import com.example.demo.Entity.User;
import com.example.demo.Exception.UserNotFoundException;
import com.example.demo.kafka.KafkaProducerService;
import com.example.demo.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @GetMapping("/fetch")
    public ResponseEntity<List<User>> showAllUsers() {
        log.info("Request received to show all users");
        List<User> users = userRepository.findAll();
        log.info("Returning {} users", users.size());
        kafkaProducerService.sendMessage("Fetched all users: " + users.size());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/fetch/{id}")
    public ResponseEntity<User> showUserById(@PathVariable Long id) {
        log.info("Request received to show user with ID: {}", id);
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            log.info("User found with ID: {}", id);
            kafkaProducerService.sendMessage("Fetched user with ID: " + id);
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            log.warn("User not found with ID: {}", id);
            throw new UserNotFoundException("User not found with ID: " + id);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<String> addUser(@RequestBody User newUser) {
        log.info("Request received to add a new user: {}", newUser);
        userRepository.save(newUser);
        log.info("User added successfully with ID: {}", newUser.getId());
        kafkaProducerService.sendMessage("User added: " + newUser.getId());
        return new ResponseEntity<>("User added successfully", HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateUser(@RequestBody User newUser, @PathVariable Long id) {
        log.info("Request received to update user with ID: {}", id);
        Optional<User> userOld = userRepository.findById(id);
        if (userOld.isPresent()) {
            User user = userOld.get();
            user.setFirstName(newUser.getFirstName());
            user.setLastName(newUser.getLastName());
            if (newUser.getEmail() != null && !newUser.getEmail().isEmpty()) {
                user.setEmail(newUser.getEmail());
            }
            if (newUser.getPassword() != null && !newUser.getPassword().isEmpty()) {
                user.setPassword(newUser.getPassword());
            }
            if (newUser.getRole() != null && !newUser.getRole().isEmpty()) {
                user.setRole(newUser.getRole());
            }
            user.setAccounts(newUser.getAccounts());
            userRepository.save(user);
            log.info("User updated successfully with ID: {}", id);
            kafkaProducerService.sendMessage("User updated: " + id);
            return new ResponseEntity<>("User updated successfully", HttpStatus.OK);
        } else {
            log.warn("User not found with ID: {}", id);
            throw new UserNotFoundException("User not found with ID: " + id);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        log.info("Request received to delete user with ID: {}", id);
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            userRepository.deleteById(id);
            log.info("User deleted successfully with ID: {}", id);
            kafkaProducerService.sendMessage("User deleted: " + id);
            return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
        } else {
            log.warn("User not found with ID: {}", id);
            throw new UserNotFoundException("User not found with ID: " + id);
        }
    }
}
