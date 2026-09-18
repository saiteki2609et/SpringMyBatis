package com.example.scm.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.scm.exception.BusinessException;

/**
 * 画面(Thymeleaf)側の業務例外ハンドラ。
 * REST(/api)側は {@link com.example.scm.web.api.ApiExceptionHandler} が JSON で返す。
 */
@ControllerAdvice(assignableTypes = {DashboardController.class, ItemController.class,
        StockController.class, ShipmentController.class})
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        return "error-page";
    }
}
