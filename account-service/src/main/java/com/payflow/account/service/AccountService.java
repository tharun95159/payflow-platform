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
import java.math.BigDecimal;
import com.payflow.account.repository.TransferRecordRepository;
import com.payflow.account.entity.TransferRecord;

import java.time.LocalDateTime;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final TransferRecordRepository transferRecordRepository;


    public AccountService(
            AccountRepository accountRepository,
            TransferRecordRepository transferRecordRepository) {

        this.accountRepository = accountRepository;
        this.transferRecordRepository = transferRecordRepository;
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



    @Transactional
    public void transferFunds(
            String transferReference,
            Long fromAccountId,
            Long toAccountId,
            BigDecimal amount) {


// Step 0: Validate transfer reference
        if (transferReference == null ||
                transferReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Transfer reference is required"
            );
        }


        // Step 1: Check whether this transfer was already recorded
        if (transferRecordRepository.existsById(transferReference)) {
            throw new IllegalArgumentException(
                    "Transfer already exists with reference: " + transferReference
            );
        }



        // Step 1: Prevent transferring to the same account
        if (fromAccountId == null || toAccountId == null ||
                fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException(
                    "Source and destination must be different valid accounts"
            );
        }

        // Step 2: Lock accounts in a consistent order
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account firstAccount = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + firstId
                ));

        Account secondAccount = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + secondId
                ));

        // Step 3: Identify source and destination
        Account sourceAccount = fromAccountId.equals(firstId)
                ? firstAccount : secondAccount;

        Account destinationAccount = toAccountId.equals(firstId)
                ? firstAccount : secondAccount;


        // Step 4: Validate transfer amount
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0 ||
                amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Transfer amount must be positive with at most 2 decimal places"
            );
        }

        // Step 5: Check both accounts are active
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE ||
                destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Both accounts must be ACTIVE"
            );
        }

        // Step 6: Check currencies match
        if (sourceAccount.getCurrency() == null ||
                !sourceAccount.getCurrency().equalsIgnoreCase(
                        destinationAccount.getCurrency())) {
            throw new IllegalArgumentException(
                    "Source and destination currencies must match"
            );
        }

        // Step 7: Check sufficient balance
        if (sourceAccount.getBalance() == null ||
                sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance in source account"
            );
        }


        // Step 8: Prepare a record for this transfer
        TransferRecord transferRecord = new TransferRecord(
                transferReference,
                fromAccountId,
                toAccountId,
                amount,
                LocalDateTime.now()
        );


        // Step 9: Calculate the new account balances
        BigDecimal newSourceBalance =
                sourceAccount.getBalance().subtract(amount);

        BigDecimal newDestinationBalance =
                destinationAccount.getBalance().add(amount);

        // Step 10: Update both account entities
        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestinationBalance);

        LocalDateTime now = LocalDateTime.now();

        sourceAccount.setUpdatedAt(now);
        destinationAccount.setUpdatedAt(now);

        // Step 11: Save both accounts and the transfer record
        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        transferRecordRepository.saveAndFlush(transferRecord);

    }


}
