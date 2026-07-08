package edu.eci.uniplay.room.infrastructure.web.error;

import edu.eci.uniplay.room.application.exception.RoomCodeGenerationException;
import edu.eci.uniplay.room.application.exception.RoomNotFoundException;
import edu.eci.uniplay.room.domain.model.DuplicatePlayerException;
import edu.eci.uniplay.room.domain.model.RoomFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationError(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "request body contains invalid room data"
        );
        problemDetail.setTitle("Invalid room request");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid room request");
        return problemDetail;
    }

    @ExceptionHandler(RoomCodeGenerationException.class)
    ProblemDetail handleRoomCodeGeneration(RoomCodeGenerationException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problemDetail.setTitle("Room code unavailable");
        return problemDetail;
    }

    @ExceptionHandler(RoomNotFoundException.class)
    ProblemDetail handleRoomNotFound(RoomNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Room not found");
        return problemDetail;
    }

    @ExceptionHandler({DuplicatePlayerException.class, RoomFullException.class})
    ProblemDetail handleRoomConflict(RuntimeException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Room join conflict");
        return problemDetail;
    }
}
