package com.howellyoung.exchange.bean;

import com.howellyoung.exchange.enums.OrderDirectionEnum;
import com.howellyoung.exchange.util.apierror.ApiError;
import com.howellyoung.exchange.util.apierror.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OrderRequestBean implements ValidatableBean {

    public OrderDirectionEnum direction;

    public BigDecimal price;

    public BigDecimal quantity;

    @Override
    public void validate() {
        if (this.direction == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "direction", "direction is required.");
        }
        // price:
        if (this.price == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "price", "price is required.");
        }
        this.price = this.price.setScale(2, RoundingMode.DOWN);
        if (this.price.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "price", "price must be positive.");
        }
        // quantity:
        if (this.quantity == null) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "quantity", "quantity is required.");
        }
        this.quantity = this.quantity.setScale(2, RoundingMode.DOWN);
        if (this.quantity.signum() <= 0) {
            throw new ApiException(ApiError.PARAMETER_INVALID, "quantity", "quantity must be positive.");
        }
    }
}