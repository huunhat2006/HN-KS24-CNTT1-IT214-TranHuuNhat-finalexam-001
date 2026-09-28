package org.example.orderservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.clients.ProductClient;
import org.example.orderservice.models.dto.responses.ProductResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductGatewayService {

    private final ProductClient productClient;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "productService", fallbackMethod = "getProductByIdFallback")
    public ProductResponse getProductById(Long productId) {
        return productClient.getProductById(productId);
    }

    public ProductResponse getProductByIdFallback(Long productId, Throwable t) {
        if (t instanceof feign.FeignException.NotFound) {
            throw new org.example.orderservice.exceptions.ProductNotFoundException(productId);
        }
        throw new org.example.orderservice.exceptions.ProductServiceException("Product service is unavailable", t);
    }
}
