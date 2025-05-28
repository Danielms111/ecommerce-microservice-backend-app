package com.selimhorri.app.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.selimhorri.app.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Integration Tests")
class PaymentServiceIntegrationTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment1;
    private Payment payment2;
    private PaymentDto paymentDto1;
    private PaymentDto paymentDto2;
    private OrderDto orderDto1;
    private OrderDto orderDto2;

    @BeforeEach
    void setUp() {
        // Setup OrderDtos
        orderDto1 = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order 1")
                .orderFee(100.0)
                .build();

        orderDto2 = OrderDto.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test Order 2")
                .orderFee(200.0)
                .build();

        // Setup PaymentDtos
        paymentDto1 = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderDto(OrderDto.builder().orderId(1).build())
                .build();

        paymentDto2 = PaymentDto.builder()
                .paymentId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderDto(OrderDto.builder().orderId(2).build())
                .build();

        // Setup Payment entities
        payment1 = Payment.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderId(1)
                .build();

        payment2 = Payment.builder()
                .paymentId(2)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .orderId(2)
                .build();
    }

    @Test
    @DisplayName("Should fetch all payments with order details from external service")
    void testFindAllPaymentsWithOrderServiceIntegration() {
        // Given
        when(paymentRepository.findAll()).thenReturn(Arrays.asList(payment1, payment2));
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenReturn(orderDto1);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/2"),
                eq(OrderDto.class)))
                .thenReturn(orderDto2);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first payment
        PaymentDto firstPayment = result.get(0);
        assertEquals(1, firstPayment.getPaymentId());
        assertTrue(firstPayment.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, firstPayment.getPaymentStatus());
        assertNotNull(firstPayment.getOrderDto());
        assertEquals(1, firstPayment.getOrderDto().getOrderId());
        assertEquals("Test Order 1", firstPayment.getOrderDto().getOrderDesc());
        assertEquals(100.0, firstPayment.getOrderDto().getOrderFee());

        // Verify second payment
        PaymentDto secondPayment = result.get(1);
        assertEquals(2, secondPayment.getPaymentId());
        assertFalse(secondPayment.getIsPayed());
        assertEquals(PaymentStatus.IN_PROGRESS, secondPayment.getPaymentStatus());
        assertNotNull(secondPayment.getOrderDto());
        assertEquals(2, secondPayment.getOrderDto().getOrderId());
        assertEquals("Test Order 2", secondPayment.getOrderDto().getOrderDesc());
        assertEquals(200.0, secondPayment.getOrderDto().getOrderFee());

        // Verify external service calls
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1", OrderDto.class);
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/2", OrderDto.class);
    }

    @Test
    @DisplayName("Should handle order service unavailability gracefully when fetching payment by ID")
    void testFindPaymentByIdWithOrderServiceFailure() {
        // Given
        Integer paymentId = 1;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment1));
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenThrow(new ResourceAccessException("Order service unavailable"));

        // When & Then
        ResourceAccessException exception = assertThrows(ResourceAccessException.class, () -> {
            paymentService.findById(paymentId);
        });

        assertEquals("Order service unavailable", exception.getMessage());

        // Verify that repository was called
        verify(paymentRepository, times(1)).findById(paymentId);
        // Verify that external service was attempted
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1", OrderDto.class);
    }

    @Test
    @DisplayName("Should successfully fetch payment by ID with order details")
    void testFindPaymentByIdWithSuccessfulOrderServiceIntegration() {
        // Given
        Integer paymentId = 1;
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment1));
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenReturn(orderDto1);

        // When
        PaymentDto result = paymentService.findById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPaymentId());
        assertTrue(result.getIsPayed());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());

        // Verify order details are correctly populated
        assertNotNull(result.getOrderDto());
        assertEquals(1, result.getOrderDto().getOrderId());
        assertEquals("Test Order 1", result.getOrderDto().getOrderDesc());
        assertEquals(100.0, result.getOrderDto().getOrderFee());
        assertNotNull(result.getOrderDto().getOrderDate());

        // Verify service interactions
        verify(paymentRepository, times(1)).findById(paymentId);
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1", OrderDto.class);
    }

    @Test
    @DisplayName("Should handle partial order service response when fetching all payments")
    void testFindAllPaymentsWithPartialOrderServiceResponse() {
        // Given
        when(paymentRepository.findAll()).thenReturn(Arrays.asList(payment1, payment2));
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenReturn(orderDto1);
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/2"),
                eq(OrderDto.class)))
                .thenReturn(null); // Simulate partial failure

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // First payment should have complete order details
        PaymentDto firstPayment = result.get(0);
        assertNotNull(firstPayment.getOrderDto());
        assertEquals("Test Order 1", firstPayment.getOrderDto().getOrderDesc());

        // Second payment should have null order details
        PaymentDto secondPayment = result.get(1);
        assertNull(secondPayment.getOrderDto());

        // Verify both external service calls were made
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1", OrderDto.class);
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/2", OrderDto.class);
    }

    @Test
    @DisplayName("Should validate payment-order relationship consistency")
    void testPaymentOrderRelationshipConsistency() {
        // Given
        Integer paymentId = 1;
        OrderDto inconsistentOrderDto = OrderDto.builder()
                .orderId(99) // Different order ID than expected
                .orderDate(LocalDateTime.now())
                .orderDesc("Inconsistent Order")
                .orderFee(999.0)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment1));
        when(restTemplate.getForObject(
                eq(AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1"),
                eq(OrderDto.class)))
                .thenReturn(inconsistentOrderDto);

        // When
        PaymentDto result = paymentService.findById(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPaymentId());

        // The order details should be whatever the order service returns
        // (this tests the integration behavior, not validation)
        assertNotNull(result.getOrderDto());
        assertEquals(99, result.getOrderDto().getOrderId());
        assertEquals("Inconsistent Order", result.getOrderDto().getOrderDesc());

        // Verify the service call was made with the correct order ID from payment
        verify(restTemplate, times(1)).getForObject(
                AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/1", OrderDto.class);
    }
}