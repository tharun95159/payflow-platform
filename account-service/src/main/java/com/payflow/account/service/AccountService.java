package com.payflow.account.service;

import com.payflow.account.dto.AccountRequest;
import com.payflow.account.dto.AccountResponse;
import com.payflow.account.entity.Account;
import com.payflow.account.entity.AccountStatus;
import com.payflow.account.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.payflow.account.exception.AccountNotFoundException;
import com.payflow.account.exception.AccountAlreadyExistsException;

import java.time.LocalDateTime;

@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository){
        this.accountRepository=accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request){
        if(accountRepository.existsById(request.getAccountId())){
            throw new AccountAlreadyExistsException("Account already exists" + request.getAccountId());
        }

        LocalDateTime now=LocalDateTime.now();

        Account account= Account.builder()
                .accountId(request.getAccountId())
                .customerId(request.getCustomerId())
                .accountType(request.getAccountType())
                .balance(request.getBalance())
                .currency(request.getCurrency().toUpperCase())
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Account savedAccount=accountRepository.save(account);

        return AccountResponse.builder()
                .accountId(savedAccount.getAccountId())
                .customerId(savedAccount.getCustomerId())
                .accountType(savedAccount.getAccountType())
                .balance(savedAccount.getBalance())
                .currency(savedAccount.getCurrency())
                .status(savedAccount.getStatus())
                .createdAt(savedAccount.getCreatedAt())
                .updatedAt(savedAccount.getUpdatedAt())
                .build();

    }


    public AccountResponse getAccountById(Long accountId) {

        // Step 1: Find the account in PostgreSQL
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId
                ));

        // Step 2: Convert the entity into a response DTO
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .customerId(account.getCustomerId())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

}
