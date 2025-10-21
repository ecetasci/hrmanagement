package com.ecetasci.hrmanagement.enums;

import java.util.HashMap;
import java.util.Map;

public enum ResponseMessageEnum {
    COMPANY_APPLICATION_APPROVED(202,"Company Application Approved"),
    EMPLOYEE_REGISTER_APPROVED(202,"Employe Approved");

    private final int code;
    private final String desc;

    private static final Map<Integer,ResponseMessageEnum> map = new HashMap<>();

    static {
        for (ResponseMessageEnum value : values()) {
            map.put(value.code,value);
        }
    }

    ResponseMessageEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ResponseMessageEnum getValue(int code) {
        return map.get(code);
    }
}
