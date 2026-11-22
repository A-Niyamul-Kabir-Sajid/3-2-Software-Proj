package edu.kuet.classsync.common.exception;

/** One invalid request field and the reason. */
public record FieldErrorItem(String field, String message) { }
