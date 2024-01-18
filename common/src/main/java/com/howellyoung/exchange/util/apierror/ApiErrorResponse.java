package com.howellyoung.exchange.util.apierror;

public record ApiErrorResponse(ApiError error, String data, String message) {

}
