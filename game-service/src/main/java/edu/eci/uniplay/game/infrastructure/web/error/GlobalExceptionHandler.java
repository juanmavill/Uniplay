package edu.eci.uniplay.game.infrastructure.web.error;

import edu.eci.uniplay.game.domain.model.DuplicateVoteException;
import edu.eci.uniplay.game.domain.model.RoundAlreadyActiveException;
import edu.eci.uniplay.game.domain.model.RoundExpiredException;
import edu.eci.uniplay.game.domain.model.RoundNotExpiredException;
import edu.eci.uniplay.game.domain.model.RoundNotActiveException;
import edu.eci.uniplay.game.domain.model.SelfVoteException;
import edu.eci.uniplay.game.domain.model.VotingNotEnabledException;
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
                "request body contains invalid game data"
        );
        problemDetail.setTitle("Invalid game request");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Invalid game request");
        return problemDetail;
    }

    @ExceptionHandler({
            RoundAlreadyActiveException.class,
            RoundExpiredException.class,
            RoundNotActiveException.class,
            RoundNotExpiredException.class,
            DuplicateVoteException.class,
            SelfVoteException.class,
            VotingNotEnabledException.class
    })
    ProblemDetail handleRoundConflict(RuntimeException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problemDetail.setTitle("Round conflict");
        return problemDetail;
    }
}
