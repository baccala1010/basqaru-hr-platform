package kz.whosnext.hr.auth.exception;

public class EmailAlreadyExistsException extends BadRequestException {
    public EmailAlreadyExistsException(String email) {
        super("Пользователь с email " + email + " уже существует");
    }
}

