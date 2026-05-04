package com.piotrcapecki.bakelivery.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFound_usesFallbackWhenMessageIsNull() {
        var response = handler.notFound(new NotFoundException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Resource not found");
    }

    @Test
    void forbidden_usesFallbackWhenMessageIsNull() {
        var response = handler.forbidden(new ForbiddenException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("error", "Access denied");
    }

    @Test
    void conflict_usesFallbackWhenMessageIsNull() {
        var response = handler.conflict(new ConflictException(null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "Conflict");
    }

    @Test
    void bad_usesFallbackWhenMessageIsNull() {
        var response = handler.bad(new IllegalArgumentException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid request");
    }

    @Test
    void validation_usesFallbackWhenThereAreNoValidationErrors() throws NoSuchMethodException {
        BindingResult emptyBindingResult = new BeanPropertyBindingResult(new TestPayload("value"), "testPayload");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter(),
                emptyBindingResult
        );

        var response = handler.validation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Validation failed");
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validatedEndpoint", TestPayload.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void validatedEndpoint(TestPayload payload) {
    }

    private record TestPayload(String value) {
    }
}
