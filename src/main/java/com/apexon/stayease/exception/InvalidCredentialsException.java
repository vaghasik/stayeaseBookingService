package com.apexon.stayease.exception;

/**
 * Invalid Credentials Exception
 * Thrown when login credentials are invalid.
 *
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
