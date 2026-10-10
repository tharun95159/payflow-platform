
package com.payflow.payment.client;

import com.payflow.payment.dto.AccountResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class AccountClient {

    private final RestClient accountRestClient;

    public AccountClient(RestClient accountRestClient) {
        this.accountRestClient = accountRestClient;
    }


    public AccountResponse getAccountById(Long accountId) {

        try {
            return accountRestClient.get()
                    .uri("/api/v1/accounts/{accountId}", accountId)
                    .retrieve()
                    .body(AccountResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found with ID: " + accountId
            );
        }
    }

}
