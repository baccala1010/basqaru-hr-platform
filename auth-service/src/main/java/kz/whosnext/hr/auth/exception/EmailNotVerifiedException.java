package kz.whosnext.hr.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Email не подтверждён. Проверьте почту для подтверждения");
    }
}

