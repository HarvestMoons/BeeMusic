package com.example.musicplayer.exception;

public class ElementExistedException extends RuntimeException {
    public ElementExistedException(String message) {
        super(message);
    }
}