package com.smartspend.copilot.exception;

public class ClarificationRequiredException extends AppException {
    private final String item;
    private final double val1;
    private final double val2;

    public ClarificationRequiredException(String item, double val1, double val2) {
        super(ErrorCode.CLARIFICATION_REQUIRED, "Clarification required for ambiguous amounts");
        this.item = item;
        this.val1 = val1;
        this.val2 = val2;
    }

    public String getItem() {
        return item;
    }

    public double getVal1() {
        return val1;
    }

    public double getVal2() {
        return val2;
    }
}
