package com.blueapron.marinator.test.components;

import com.blueapron.marinator.Injector;
import com.blueapron.marinator.test.models.BananaObject;

/**
 * Component to test injection.
 */
public class BananaComponent {
    @Injector
    public void inject(BananaObject obj) {
        obj.injected = true;
    }
}
