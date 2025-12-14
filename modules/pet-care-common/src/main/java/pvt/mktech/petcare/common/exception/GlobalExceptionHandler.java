package pvt.mktech.petcare.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pvt.mktech.petcare.common.dto.response.Result;
import pvt.mktech.petcare.common.dto.response.ResultCode;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception ex) {
        log.error("Exception occurred: ", ex);
        return Result.error(ResultCode.FAILED, ex.getMessage());
    }

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

    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }
}