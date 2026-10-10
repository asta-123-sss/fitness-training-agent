package com.fitness.agent.exception;

/**
 * 业务异常，携带面向用户的友好信息。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
