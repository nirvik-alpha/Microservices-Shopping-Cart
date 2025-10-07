package com.example.OrderService.service;

import com.example.OrderService.entity.Order;
import com.example.OrderService.exception.CustomException;
import com.example.OrderService.external.client.PaymentService;
import com.example.OrderService.external.client.ProductService;
import com.example.OrderService.external.request.PaymentRequest;
import com.example.OrderService.external.response.PaymentResponse;
import com.example.OrderService.model.OrderRequest;
import com.example.OrderService.model.OrderResponse;
import com.example.OrderService.model.ProductResponse;
import com.example.OrderService.repository.OrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@Log4j2
public class OrderServiceImpl implements OrderService{

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public long placeOrder(OrderRequest orderRequest) {

        // order entity --> save the data with status order created
        // product service - block products ( reduce the quantity )
        // payment service - > payment -> success -> complete else cancelled

        log.info("placing order request: {} ", orderRequest);

        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity() );
        log.info("Creating Order with Status CREATED");

        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .orderDate(Instant.now())
                .quantity(orderRequest.getQuantity())
                .build();

        order  = orderRepository.save(order);

        log.info("Calling payment service to complete the payment ");

        PaymentRequest paymentRequest =
                PaymentRequest.builder()
                        .orderId(order.getId())
                        .paymentMode(orderRequest.getPaymentMode())
                        .amount(orderRequest.getTotalAmount())
                        .build();

        String orderStatus= null;
        try{
            paymentService.doPayment(paymentRequest);
            log.info("Payment done. Changed order status to PLACED");
            orderStatus = "PLACED";
        }catch (Exception e){
            log.error("Error occured in payment. Changing order status");
            orderStatus = "PAYMENT_FAILED";
        }

        order.setOrderStatus(orderStatus);
        orderRepository.save(order);

    log.info("Order placed successfully with order id: {}", order.getId());

        return order.getId();
    }

    @Override
    public OrderResponse getOrderDetails(long orderId) {

        log.info("Get order details for order id :{}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new CustomException("Order not found for the order id" + orderId,
                        "NOT_FOUND", 404));

        log.info("Invoking Product service to fetch the product for id: {}" , order.getProductId());

       ProductResponse productResponse = restTemplate.getForObject("http://PRODUCT-SERVICE/product/" +order.getProductId(),
               ProductResponse.class);

       log.info("Getting information from the payment service");

        PaymentResponse paymentResponse = restTemplate.getForObject(
                "http://PAYMENT-SERVICE/payment/order/"+order.getId(),
                PaymentResponse.class
        );

       OrderResponse.ProductDetails productDetails =
               OrderResponse.ProductDetails.builder()
                       .productName(productResponse.getProductName())
                       .productId(productResponse.getProductId())
                       .build();

       OrderResponse.PaymentDetails paymentDetails =
               OrderResponse.PaymentDetails.builder()
                       .paymentId(paymentResponse.getPaymentId())
                       .paymentStatus(paymentResponse.getStatus())
                       .paymentDate(paymentResponse.getPaymentDate())
                       .paymentMode(paymentResponse.getPaymentMode())
                       .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(order.getId())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .amount(order.getAmount())
                .productDetails(productDetails)
                .paymentDetails(paymentDetails)
                .build();

        return orderResponse;
    }
}
