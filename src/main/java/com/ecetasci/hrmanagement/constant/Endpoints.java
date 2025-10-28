package com.ecetasci.hrmanagement.constant;

public class Endpoints {
    // API kök yolu — controller'lar proje içinde "/api/..." kullandığı için BASE olarak versiyon/çevre bilgisi ile tanımlandı
    public static final String API = "/api";
    public static final String VERSION = "/v1";
    public static final String DEV = "/dev";
    public static final String BASE = API + VERSION + DEV;


    // Controller bazlı ana yollar
    public static final String AUTH = BASE + "/auth";
    public static final String USER = BASE + "/user";
    public static final String ADMIN = BASE + "/admin";
    public static final String EMPLOYEE = BASE + "/employee";
    public static final String MANAGER = BASE + "/manager";
    public static final String COMPANY = BASE + "/company";
    public static final String DASHBOARD = BASE + "/dashboard";
       public static final String EXPENSES = BASE + "/expenses";
    public static final String REVIEWS = BASE + "/reviews";
    // company shifts path
    public static final String COMPANY_SHIFTS = COMPANY + "/shifts";
    // genel shifts path (ShiftController) — eklendi
    public static final String SHIFT = BASE + "/shifts";


    // Yaygın işlem yolları (gerekirse controller'larda kullanılmak üzere)
    public static final String FIND_ALL = "/find-all";
    public static final String FIND_BY_ID = "/find-by-id";
    public static final String SAVE = "/save";

    // Auth ile ilgili yollar
    public static final String REGISTER = "/register";
    public static final String LOGIN = "/login";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String VERIFY = "/verify";
    public static final String LOGOUT = "/logout";

    // Admin/definitions yardımcı yolları
    public static final String CREATE_SUBSCRIPTION = "/create-subscription";
    public static final String LIST_COMPANY = "/list-company";
    public static final String DEFINITIONS = "/definitions";
    public static final String LEAVE_TYPES = "/definitions/leave-types";
    public static final String CREATE_LEAVE_TYPES = "/definitions/create-leave-types";
    public static final String DEPARTMENTS = "/definitions/departments";

    private Endpoints() { /* prevent instantiation */ }
}