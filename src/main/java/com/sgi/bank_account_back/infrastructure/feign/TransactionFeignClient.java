package com.sgi.bank_account_back.infrastructure.feign;

import com.sgi.bank_account_back.infrastructure.dto.TransactionRequest;
import com.sgi.bank_account_back.infrastructure.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@FeignClient(
        name = "Transaction-service",
        url = "${feign.client.config.transaction-service.url}")
public interface TransactionFeignClient {

    @PostMapping("/v1/transaction")
    Mono<TransactionResponse> saveTransaction(@RequestBody Mono<TransactionRequest> transactionRequest);
}
