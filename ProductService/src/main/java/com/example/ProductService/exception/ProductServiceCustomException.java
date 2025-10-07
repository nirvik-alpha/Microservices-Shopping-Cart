package com.example.ProductService.exception;

import lombok.Data;

@Data
public class ProductServiceCustomException extends RuntimeException{

    private String errorCode;

    public ProductServiceCustomException(String message, String errorCode){

      super(message);  // message field is already part of RuntimeException
      this.errorCode = errorCode;

    }

}
