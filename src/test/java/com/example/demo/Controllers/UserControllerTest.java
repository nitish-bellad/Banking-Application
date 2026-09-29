package com.example.demo.Controllers;

import com.example.demo.Entity.User;
import com.example.demo.Exception.GlobalExceptionHandler;
import com.example.demo.Exception.UserNotFoundException;
import com.example.demo.Repository.UserRepository;
import com.example.demo.kafka.KafkaProducerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1, user2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler()) // Add Global Exception Handling
                .build();
        objectMapper = new ObjectMapper();

        user1 = new User(1L, "John", "Doe", "john@example.com", "password", "USER", null);
        user2 = new User(2L, "Jane", "Smith", "jane@example.com", "password", "ADMIN", null);
    }

    @Test
    void testShowAllUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        mockMvc.perform(get("/user/fetch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2));

        verify(userRepository, times(1)).findAll();
        verify(kafkaProducerService, times(1)).sendMessage("Fetched all users: 2");
    }

    @Test
    void testShowUserById_Found() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        mockMvc.perform(get("/user/fetch/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(userRepository, times(1)).findById(1L);
        verify(kafkaProducerService, times(1)).sendMessage("Fetched user with ID: 1");
    }

    @Test
    void testShowUserById_NotFound() throws Exception {
        when(userRepository.findById(70L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/user/fetch/70"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UserNotFoundException))
                .andExpect(result -> assertEquals("User not found with ID: 70", result.getResolvedException().getMessage()));

        verify(userRepository, times(1)).findById(70L);
        verifyNoInteractions(kafkaProducerService); // Ensure Kafka is NOT called
    }

    @Test
    void testAddUser() throws Exception {
        when(userRepository.save(any(User.class))).thenReturn(user1);

        mockMvc.perform(post("/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isCreated())
                .andExpect(content().string("User added successfully"));

        verify(userRepository, times(1)).save(any(User.class));
        verify(kafkaProducerService, times(1)).sendMessage("User added: 1");
    }

    @Test
    void testUpdateUser_Success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenReturn(user1);

        mockMvc.perform(put("/user/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated successfully"));

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
        verify(kafkaProducerService, times(1)).sendMessage("User updated: 1");
    }

    @Test
    void testUpdateUser_NotFound() throws Exception {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/user/update/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UserNotFoundException));

        verify(userRepository, times(1)).findById(3L);
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        doNothing().when(userRepository).deleteById(1L);

        mockMvc.perform(delete("/user/delete/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted successfully"));

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).deleteById(1L);
        verify(kafkaProducerService, times(1)).sendMessage("User deleted: 1");
    }

    @Test
    void testDeleteUser_NotFound() throws Exception {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/user/delete/3"))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof UserNotFoundException));

        verify(userRepository, times(1)).findById(3L);
    }
}
