package ru.rkjrth.adboard.entity;

/**
 * Состояние refresh-сессии в БД (задание 5: ротация и повторное использование старого refresh).
 */
public enum SessionStatus {
    /** Текущая активная пара токенов. */
    ACTIVE,
    /** Сессия заменена при успешном refresh (старый refresh недействителен). */
    REPLACED,
    /** Явно отозвана (выход и т.п.). */
    REVOKED
}
