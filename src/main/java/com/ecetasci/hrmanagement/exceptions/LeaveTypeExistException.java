package com.ecetasci.hrmanagement.exceptions;

public class LeaveTypeExistException extends RuntimeException {
    public LeaveTypeExistException(String message) {
        super(message);
    }
}
