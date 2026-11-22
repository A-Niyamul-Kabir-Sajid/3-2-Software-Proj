package edu.kuet.classsync.common.exception;

/** 400: the request is syntactically valid but breaks a business rule. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) { super(message); }
}
