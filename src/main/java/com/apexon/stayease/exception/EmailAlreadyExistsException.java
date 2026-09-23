package com.apexon.stayease.exception;

/**
 * Email Already Exists Exception
 * Thrown when attempting to register with an email that already exists.
 *
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
