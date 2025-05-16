package com.framework.tools.gus.soap;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class GusException extends RuntimeException {
    private final String errorCode;
    private final String error;

    public GusException(String errorCode, String error){
        super("Gus error [" + errorCode + ": " + error+ "]");
        this.errorCode = errorCode;
        this.error = error;
    }
}
