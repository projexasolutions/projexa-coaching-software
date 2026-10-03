package com.projexa.coaching.common.exceptions;
import com.projexa.coaching.common.responses.ApiResponse; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.security.access.AccessDeniedException; import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice public class GlobalExceptionHandler {
 @ExceptionHandler(ApiException.class) ResponseEntity<ApiResponse<Void>> handle(ApiException e){HttpStatus s=switch(e.getCode()){case "INVALID_CREDENTIALS","INVALID_REFRESH_TOKEN","TENANT_INACTIVE"->HttpStatus.UNAUTHORIZED;case "FORBIDDEN"->HttpStatus.FORBIDDEN;case "NOT_FOUND","USER_NOT_FOUND"->HttpStatus.NOT_FOUND;default->HttpStatus.BAD_REQUEST;};return ResponseEntity.status(s).body(ApiResponse.error(e.getCode(),e.getMessage()));}
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("FORBIDDEN",e.getMessage()));}
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ApiResponse<Void>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(ApiResponse.error("HTTP_ERROR",e.getReason()));}
 @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ApiResponse<Void>> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST",e.getMessage()));}
 @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> generic(Exception e){return ResponseEntity.status(500).body(ApiResponse.error("INTERNAL_ERROR","An unexpected error occurred"));}
}
