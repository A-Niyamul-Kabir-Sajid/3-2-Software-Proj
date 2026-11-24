package edu.kuet.classsync.common.exception;

/** 404: the requested entity does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " " + id + " not found");
    }
}
