package com.example.demo.Controllers;

import com.example.demo.DTO.AuthRequest;
import com.example.demo.DTO.AuthResponse;
import com.example.demo.Entity.User;
import com.example.demo.Repository.UserRepository;
import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthController authController;

    private User user;
    private AuthRequest authRequest;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("ROLE_USER");

        authRequest = new AuthRequest("test@example.com", "rawPassword");

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("encodedPassword")
                .roles("USER")
                .build();
    }

    @Test
    void testRegisterUser_Success() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        ResponseEntity<String> response = authController.registerUser(user);

        assertEquals("User registered successfully!", response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        ResponseEntity<String> response = authController.registerUser(user);

        assertEquals("Email already in use!", response.getBody());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testLogin_Success() {
        when(userDetailsService.loadUserByUsername(authRequest.getEmail())).thenReturn(userDetails);
        when(passwordEncoder.matches(authRequest.getPassword(), userDetails.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mockedToken");

        ResponseEntity<AuthResponse> response = authController.login(authRequest);

        assertNotNull(response.getBody());
        assertEquals("mockedToken", response.getBody().getToken());
    }

    @Test
    void testLogin_InvalidCredentials() {
        when(userDetailsService.loadUserByUsername(authRequest.getEmail())).thenReturn(userDetails);
        when(passwordEncoder.matches(authRequest.getPassword(), userDetails.getPassword())).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> authController.login(authRequest));
        assertEquals("Invalid email or password!", exception.getMessage());
    }
}
