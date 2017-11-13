package com.blueapron.marinator;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized repository for all injectors. This enables us to swap out implementations of
 * injectors trivially during testing.
 */
public final class Marinator {

    private static final Map<Class, Injector> STRICT_INJECTORS = new HashMap<>();
    private static final Map<Class, Injector> LOOSE_INJECTORS = new HashMap<>();
    private static boolean sStrictMode = true;

    // Hide to avoid construction.
    private Marinator() {
    }

    public static void registerInjector(Class clazz, Injector injector, boolean strict) {
        synchronized (STRICT_INJECTORS) {
            if (STRICT_INJECTORS.containsKey(clazz)) {
                throw new IllegalArgumentException("Cannot register multiple injectors for class!");
            }
            STRICT_INJECTORS.put(clazz, injector);
            if (!strict) {
                LOOSE_INJECTORS.put(clazz, injector);
            }
        }
    }

    public static void unregisterInjector(Class clazz) {
        synchronized (STRICT_INJECTORS) {
            STRICT_INJECTORS.remove(clazz);
        }
    }

    public static void clear() {
        synchronized (STRICT_INJECTORS) {
            STRICT_INJECTORS.clear();
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
        synchronized (STRICT_INJECTORS) {
            // Look for the direct injector to use. If we find one, we're done!
            Injector injector = STRICT_INJECTORS.get(clazz);
            if (injector != null) {
                return injector;
            }

            // If loose injection is allowed for this class, check to see if we can inject via
            // parent class.
            for (Class parent : LOOSE_INJECTORS.keySet()) {
                if (parent.isAssignableFrom(clazz)) {
                    return STRICT_INJECTORS.get(parent);
                }
            }

            // No injector found - ah well, we did our best.
            return null;
        }
    }

    public interface Injector {
        void inject(Object obj);
    }
}
