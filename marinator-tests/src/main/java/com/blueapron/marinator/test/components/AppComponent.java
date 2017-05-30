package com.blueapron.marinator.test.components;

import com.blueapron.marinator.Injector;
import com.blueapron.marinator.test.models.AppObject1;
import com.blueapron.marinator.test.models.AppObject2;

/**
 * Component to test injection.
 */
public class AppComponent {
    @Injector
    public void inject(AppObject1 obj) {
        obj.injected = true;
    }

    @Injector
    public void provide(AppObject2 obj) {
        obj.injected = true;
    }
}
