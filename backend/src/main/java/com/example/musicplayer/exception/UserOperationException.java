package com.example.musicplayer.exception;

/**
 * 用户自助操作（修改密码、修改用户名等）失败，通常是请求参数不合法或校验不通过。
 */
public class UserOperationException extends RuntimeException {

    public UserOperationException(String message) {
        super(message);
    }
}
