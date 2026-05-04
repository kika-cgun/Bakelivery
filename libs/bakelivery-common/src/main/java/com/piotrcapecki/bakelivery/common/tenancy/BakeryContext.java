package com.piotrcapecki.bakelivery.common.tenancy;

import java.util.UUID;

public final class BakeryContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private BakeryContext() {}

    public static void set(UUID bakeryId) {
        CURRENT.set(bakeryId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) throw new IllegalStateException("BakeryContext not set");
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
