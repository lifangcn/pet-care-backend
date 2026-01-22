package pvt.mktech.petcare.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;

import java.sql.SQLException;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.error(ex.getCode(), ex.getMessage());
    }

    // 处理系统异常
    @ExceptionHandler(SystemException.class)
    public Result<String> handleSystemException(SystemException ex) {
        log.error("System exception: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return Result.error(ex.getCode(), ex.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<String> handleValidationException(Exception ex) {
        BindingResult bindingResult = null;
        if (ex instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
        } else if (ex instanceof BindException) {
            bindingResult = ((BindException) ex).getBindingResult();
        }

        if (bindingResult != null && bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            StringBuilder sb = new StringBuilder();
            for (FieldError error : fieldErrors) {
                sb.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            }
            return Result.error(ResultCode.VALIDATE_FAILED, sb.toString());
        }
        return Result.error(ResultCode.VALIDATE_FAILED, "参数校验失败");
    }

    // 处理参数异常
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return Result.error(ResultCode.VALIDATE_FAILED, ex.getMessage());
    }

    // 处理数据库异常
    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public Result<String> handleDataAccessException(Exception ex) {
        log.error("Database exception occurred", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    // 处理空指针异常
    @ExceptionHandler(NullPointerException.class)
    public Result<String> handleNullPointerException(NullPointerException ex) {
        log.error("Null pointer exception occurred", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    // 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }

    // 处理所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception ex) {
        // SSE 客户端断开时会产生 IOException: Broken pipe，这是正常现象，不需要处理
        if (ex instanceof java.io.IOException && "Broken pipe".equals(ex.getMessage())) {
            log.debug("SSE 客户端断开连接: {}", ex.getMessage());
            return null;
        }
        log.error("Unexpected exception occurred", ex);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}