package com.blueapron.marinator;

import java.util.HashMap;

/**
 * Centralized repository for all injectors. This enables us to swap out implementations of
 * injectors trivially during testing.
 */
public final class Marinator {

    private static final HashMap<Class, Injector> sInjectors = new HashMap<>();

    // Hide to avoid construction.
    private Marinator() {
    }

    public static void registerInjector(Class clazz, Injector injector) {
        synchronized (sInjectors) {
            if (sInjectors.containsKey(clazz)) {
                throw new IllegalArgumentException("Cannot register multiple injectors for class!");
            }
            sInjectors.put(clazz, injector);
        }
    }

    public static void unregisterInjector(Class clazz) {
        synchronized (sInjectors) {
            sInjectors.remove(clazz);
        }
    }

    public static void clear() {
        synchronized (sInjectors) {
            sInjectors.clear();
        }
    }

    public static void inject(Object obj) {
        Class clazz = obj.getClass();
        Injector injector = getInjector(clazz);
        if (injector == null) {
            throw new IllegalStateException("No injector for type " + clazz.getSimpleName());
        }
        injector.inject(obj);
    }

    private static Injector getInjector(Class clazz) {
        synchronized (sInjectors) {
            return sInjectors.get(clazz);
        }
    }

    public interface Injector {
        void inject(Object obj);
    }
}
