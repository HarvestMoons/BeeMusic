package com.example.musicplayer.exception;

public class UnexpectedNullElementException extends Exception {

    public UnexpectedNullElementException() {
        super("意外的错误(找不到指定的目标)，请稍后重试");
    }

    public UnexpectedNullElementException(String message) {
        super(message);
    }
}
