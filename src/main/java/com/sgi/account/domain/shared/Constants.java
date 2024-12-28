package com.sgi.account.domain.shared;

import java.util.Random;

/**
 * Utility class for defining constants and helper methods used throughout the application.
 * This class includes static constants and a method to generate unique account numbers.
 */
public class Constants {

    public static final String EXTERNAL_REQUEST_SUCCESS_FORMAT = "Request to {} succeeded: {}";
    public static final String EXTERNAL_REQUEST_ERROR_FORMAT = "Error during request to {}";

    public static String generateAccountNumber() {
        return String.format("%04d00%012d", new Random().nextInt(10000), new Random().nextLong(1000000000000L));
    }
}
