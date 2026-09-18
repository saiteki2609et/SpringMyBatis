package com.example.scm.exception;

/** 業務エラー(在庫不足など)。画面にメッセージとして出す想定。 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
