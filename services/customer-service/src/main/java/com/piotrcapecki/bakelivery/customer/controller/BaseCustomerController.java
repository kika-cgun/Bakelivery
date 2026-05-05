package com.piotrcapecki.bakelivery.customer.controller;

import com.piotrcapecki.bakelivery.common.exception.ForbiddenException;
import com.piotrcapecki.bakelivery.customer.security.CustomerPrincipal;

abstract class BaseCustomerController {

    protected void requireBakery(CustomerPrincipal actor) {
        if (actor == null || actor.bakeryId() == null) {
            throw new ForbiddenException("This endpoint requires a bakery-scoped account");
        }
    }
}
