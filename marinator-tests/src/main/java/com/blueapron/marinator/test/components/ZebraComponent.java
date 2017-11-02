package com.blueapron.marinator.test.components;

import com.blueapron.marinator.Injector;
import com.blueapron.marinator.test.models.ZebraObject;

/**
 * Component to test injection.
 */
public class ZebraComponent {
    @Injector
    public void inject(ZebraObject obj) {
        obj.injected = true;
    }
}
