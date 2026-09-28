package org.example.orderservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.models.dto.requests.CreateOrderRequest;
import org.example.orderservice.models.dto.responses.OrderResponse;
import org.example.orderservice.models.repositories.OrderDetailRepository;
import org.example.orderservice.models.repositories.OrderRepository;
import org.example.orderservice.models.services.OrderService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final OrderDetailRepository orderDetailRepository;
        private final ProductGatewayService productGatewayService;
        private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

        @org.springframework.transaction.annotation.Transactional
        @Override
        public OrderResponse createOrder(CreateOrderRequest request) {
                org.example.orderservice.models.entities.Order order = org.example.orderservice.models.entities.Order.builder()
                        .customerName(request.customerName())
                        .status(org.example.orderservice.models.constants.OrderStatus.PENDING)
                        .total(0.0)
                        .build();

                order = orderRepository.save(order);

                double total = 0.0;
                java.util.List<org.example.orderservice.models.dto.responses.OrderDetailResponse> detailResponses = new java.util.ArrayList<>();

                for (org.example.orderservice.models.dto.requests.CreateOrderDetailRequest item : request.items()) {
                        org.example.orderservice.models.dto.responses.ProductResponse product = productGatewayService.getProductById(item.productId());
                        
                        double unitPrice = product.price();
                        double subtotal = unitPrice * item.quantity();
                        total += subtotal;

                        org.example.orderservice.models.entities.OrderDetail detail = org.example.orderservice.models.entities.OrderDetail.builder()
                                .order(order)
                                .productId(item.productId())
                                .quantity(item.quantity())
                                .unitPrice(unitPrice)
                                .build();
                        
                        detail = orderDetailRepository.save(detail);

                        detailResponses.add(new org.example.orderservice.models.dto.responses.OrderDetailResponse(
                                detail.getId(),
                                detail.getProductId(),
                                product.name(),
                                detail.getQuantity(),
                                detail.getUnitPrice(),
                                subtotal
                        ));
                }

                order.setTotal(total);
                orderRepository.save(order);

                kafkaTemplate.send("order-created", request.customerEmail());

                return new OrderResponse(
                        order.getId(),
                        order.getCustomerName(),
                        order.getTotal(),
                        order.getStatus(),
                        detailResponses
                );
        }
}
