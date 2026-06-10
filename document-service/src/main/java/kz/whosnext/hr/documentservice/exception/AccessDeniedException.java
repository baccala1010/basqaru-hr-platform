package kz.whosnext.hr.documentservice.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) { super(message); }
}

