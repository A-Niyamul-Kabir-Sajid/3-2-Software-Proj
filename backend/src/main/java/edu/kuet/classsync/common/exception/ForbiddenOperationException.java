package edu.kuet.classsync.common.exception;

/** 403: the user is authenticated but may not perform this operation (e.g. not the owner). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) { super(message); }
}
