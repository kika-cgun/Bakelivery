package com.piotrcapecki.bakelivery.auth.model;

public enum Role {
    SUPER_ADMIN,    // operator platformy (poza tenantami)
    BAKERY_ADMIN,   // właściciel piekarni
    DISPATCHER,     // dyspozytor piekarni
    DRIVER,         // kierowca piekarni
    CUSTOMER        // klient (osoba prywatna lub firma)
}
