package com.example.demo.Controllers;

import com.example.demo.DTO.TransactionRequest;
import com.example.demo.Entity.Account;
import com.example.demo.Entity.Transaction;
import com.example.demo.Exception.TransactionNotFoundException;
import com.example.demo.Repository.AccountRepository;
import com.example.demo.Repository.TransactionRepository;
import com.example.demo.kafka.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private TransactionController transactionController;

    private Transaction transaction;
    private Account account;
    private TransactionRequest transactionRequest;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);

        transaction = new Transaction(1L, 100.0, "Deposit", account);

        transactionRequest = new TransactionRequest();
        transactionRequest.setAmount(100.0);
        transactionRequest.setTransactionType("Deposit");
        transactionRequest.setAccountId(1L);
    }

    @Test
    void addTransaction_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        ResponseEntity<String> response = transactionController.addTransaction(transactionRequest);

        assertEquals(CREATED, response.getStatusCode());
        assertEquals("Transaction added successfully", response.getBody());
        verify(kafkaProducerService, times(1)).sendMessage(anyString());
    }

    @Test
    void addTransaction_AccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<String> response = transactionController.addTransaction(transactionRequest);

        assertEquals(NOT_FOUND, response.getStatusCode());
        assertEquals("Account not found", response.getBody());
    }

    @Test
    void showAllTransactions_Success() {
        when(transactionRepository.findAll()).thenReturn(List.of(transaction));

        ResponseEntity<List<Transaction>> response = transactionController.showAllTransactions();

        assertEquals(OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
    }

    @Test
    void showTransactionById_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        ResponseEntity<Transaction> response = transactionController.showTransactionById(1L);

        assertEquals(OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void showTransactionById_NotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(TransactionNotFoundException.class, () ->
                transactionController.showTransactionById(1L));

        assertEquals("Transaction not found with ID: 1", exception.getMessage());
    }

    @Test
    void updateTransaction_Success() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        ResponseEntity<String> response = transactionController.updateTransaction(1L, transactionRequest);

        assertEquals(OK, response.getStatusCode());
        assertEquals("Transaction updated successfully", response.getBody());
        verify(kafkaProducerService, times(1)).sendMessage(anyString());
    }

    @Test
    void updateTransaction_NotFound() {
        when(transactionRepository.findById(1L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(TransactionNotFoundException.class, () ->
                transactionController.updateTransaction(1L, transactionRequest));

        assertEquals("Transaction not found with ID: 1", exception.getMessage());
    }

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.existsById(1L)).thenReturn(true);
        doNothing().when(transactionRepository).deleteById(1L);

        ResponseEntity<String> response = transactionController.deleteTransaction(1L);

        assertEquals(OK, response.getStatusCode());
        assertEquals("Transaction deleted successfully", response.getBody());
        verify(kafkaProducerService, times(1)).sendMessage(anyString());
    }

    @Test
    void deleteTransaction_NotFound() {
        when(transactionRepository.existsById(1L)).thenReturn(false);

        Exception exception = assertThrows(TransactionNotFoundException.class, () ->
                transactionController.deleteTransaction(1L));

        assertEquals("Transaction not found with ID: 1", exception.getMessage());
    }
}
