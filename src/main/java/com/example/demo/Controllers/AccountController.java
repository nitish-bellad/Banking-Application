package com.example.demo.Controllers;

import com.example.demo.Entity.Account;
import com.example.demo.Exception.AccountNotFoundException;
import com.example.demo.Repository.AccountRepository;
import com.example.demo.kafka.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/account")
@CrossOrigin(origins = "*")
@Slf4j
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    // Show all accounts
    @GetMapping("/fetch")
    public ResponseEntity<List<Account>> showAllAccounts() {
        log.info("Request received to show all accounts");
        List<Account> accounts = accountRepository.findAll();
        log.info("Returning {} accounts", accounts.size());
        kafkaProducerService.sendMessage("Fetched all accounts: " + accounts.size());
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    // Show account by ID
    @GetMapping("/fetch/{id}")
    public ResponseEntity<Account> showAccountById(@PathVariable Long id) {
        log.info("Request received to show account with ID: {}", id);
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            log.info("Account found with ID: {}", id);
            kafkaProducerService.sendMessage("Fetched account with ID: " + id);
            return new ResponseEntity<>(account.get(), HttpStatus.OK);
        } else {
            log.warn("Account not found with ID: {}", id);
            throw new AccountNotFoundException("Account not found with ID: " + id);
        }
    }

    // Add a new account
    @PostMapping("/add")
    public ResponseEntity<String> addAccount(@RequestBody Account newAccount) {
        log.info("Request received to add a new account with account number: {}", newAccount.getAccountNumber());
        accountRepository.save(newAccount);
        log.info("Account added successfully with account number: {}", newAccount.getAccountNumber());
        kafkaProducerService.sendMessage("Account added: " + newAccount.getAccountNumber());
        return new ResponseEntity<>("Account added successfully", HttpStatus.CREATED);
    }

    // Update an existing account
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateAccount(@RequestBody Account newAccount, @PathVariable Long id) {
        log.info("Request received to update account with ID: {}", id);
        Optional<Account> accountOld = accountRepository.findById(id);
        if (accountOld.isPresent()) {
            Account account = accountOld.get();
            account.setAccountNumber(newAccount.getAccountNumber());
            account.setBalance(newAccount.getBalance());
            account.setUser(newAccount.getUser());
            account.setTransactions(newAccount.getTransactions());
            accountRepository.save(account);
            log.info("Account updated successfully with ID: {}", id);
            kafkaProducerService.sendMessage("Account updated: " + id);
            return new ResponseEntity<>("Account updated successfully", HttpStatus.OK);
        } else {
            log.warn("Account not found with ID: {}", id);
            throw new AccountNotFoundException("Account not found with ID: " + id);
        }
    }

    // Delete an account
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        log.info("Request received to delete account with ID: {}", id);
        Optional<Account> account = accountRepository.findById(id);
        if (account.isPresent()) {
            accountRepository.deleteById(id);
            log.info("Account deleted successfully with ID: {}", id);
            kafkaProducerService.sendMessage("Account deleted: " + id);
            return new ResponseEntity<>("Account deleted successfully", HttpStatus.OK);
        } else {
            log.warn("Account not found with ID: {}", id);
            throw new AccountNotFoundException("Account not found with ID: " + id);
        }
    }

    // Get accounts by user ID
    @GetMapping("/fetch/user/{userId}")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId) {
        log.info("Request received to fetch accounts for user with ID: {}", userId);
        List<Account> accounts = accountRepository.findByUserId(userId);
        if (accounts.isEmpty()) {
            log.warn("No accounts found for user with ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No accounts found for user with ID: " + userId);
        } else {
            log.info("Found {} accounts for user with ID: {}", accounts.size(), userId);
            kafkaProducerService.sendMessage("Fetched accounts for user with ID: " + userId);
            return new ResponseEntity<>(accounts, HttpStatus.OK);
        }
    }
}
