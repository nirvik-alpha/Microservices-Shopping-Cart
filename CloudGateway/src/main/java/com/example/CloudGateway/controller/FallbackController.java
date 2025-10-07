package com.example.CloudGateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/orderServiceFallBack")
    public String orderServiceFallback(){
        return "Order service is down";
    }

    @GetMapping("/paymentServiceFallBack")
    public String paymentServiceFallback(){
        return "Payment service is down";
    }

    @GetMapping("/productServiceFallback")
    public String productServiceFallback(){
        return "Product service is down";
    }

}
