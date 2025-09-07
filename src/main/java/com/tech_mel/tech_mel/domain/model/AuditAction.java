package com.tech_mel.tech_mel.domain.model;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    PASSWORD_RESET,
    PASSWORD_CHANGE,
    ACTIVATE,
    DEACTIVATE,
    ROLE_CHANGE,
    EMAIL_VERIFICATION,
    FAILED_LOGIN,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    BULK_UPDATE,
    EXPORT_DATA,
    IMPORT_DATA
}
