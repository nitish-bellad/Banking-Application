package com.example.demo.Controllers;

import com.example.demo.Entity.Account;
import com.example.demo.Exception.AccountNotFoundException;
import com.example.demo.Exception.GlobalExceptionHandler;
import com.example.demo.Repository.AccountRepository;
import com.example.demo.kafka.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler()) // Handles exceptions globally
                .build();
    }

    // ✅ Test fetching all accounts
    @Test
    void testShowAllAccounts() throws Exception {
        List<Account> accounts = Arrays.asList(new Account(1L, "12345", 1000.0, null, null));
        when(accountRepository.findAll()).thenReturn(accounts);

        mockMvc.perform(get("/account/fetch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));

        verify(kafkaProducerService).sendMessage("Fetched all accounts: 1");
    }

    // ✅ Test fetching account by ID (Success)
    @Test
    void testShowAccountById_Success() throws Exception {
        Account account = new Account(1L, "12345", 1000.0, null, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/account/fetch/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("12345"));

        verify(kafkaProducerService).sendMessage("Fetched account with ID: 1");
    }

    // ❌ Test fetching account by ID (Failure - Not Found)
    @Test
    void testShowAccountById_NotFound() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/account/fetch/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found with ID: 1"));
    }

    // ✅ Test adding an account
    @Test
    void testAddAccount() throws Exception {
        Account account = new Account(1L, "12345", 1000.0, null, null);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        mockMvc.perform(post("/account/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"12345\", \"balance\":1000.0}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Account added successfully"));

        verify(kafkaProducerService).sendMessage("Account added: 12345");
    }

    // ✅ Test updating an account (Success)
    @Test
    void testUpdateAccount_Success() throws Exception {
        Account existingAccount = new Account(1L, "12345", 1000.0, null, null);
        Account updatedAccount = new Account(1L, "67890", 2000.0, null, null);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        mockMvc.perform(put("/account/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"67890\", \"balance\":2000.0}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account updated successfully"));

        verify(kafkaProducerService).sendMessage("Account updated: 1");
    }

    // ❌ Test updating an account (Failure - Not Found)
    @Test
    void testUpdateAccount_NotFound() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/account/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountNumber\":\"67890\", \"balance\":2000.0}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found with ID: 1"));
    }

    // ✅ Test deleting an account (Success)
    @Test
    void testDeleteAccount_Success() throws Exception {
        Account account = new Account(1L, "12345", 1000.0, null, null);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        mockMvc.perform(delete("/account/delete/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted successfully"));

        verify(accountRepository).deleteById(1L);
        verify(kafkaProducerService).sendMessage("Account deleted: 1");
    }

    // ❌ Test deleting an account (Failure - Not Found)
    @Test
    void testDeleteAccount_NotFound() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/account/delete/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Account not found with ID: 1"));
    }

    // ✅ Test fetching accounts by user ID
    @Test
    void testGetAccountsByUserId() throws Exception {
        List<Account> accounts = Arrays.asList(new Account(1L, "12345", 1000.0, null, null));
        when(accountRepository.findByUserId(1L)).thenReturn(accounts);

        mockMvc.perform(get("/account/fetch/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));

        verify(kafkaProducerService).sendMessage("Fetched accounts for user with ID: 1");
    }

    // ❌ Test fetching accounts by user ID (No Accounts Found)
    @Test
    void testGetAccountsByUserId_NotFound() throws Exception {
        when(accountRepository.findByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/account/fetch/user/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No accounts found for user with ID: 1"));
    }
}
