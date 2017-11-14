package com.blueapron.marinator.test.components;

import com.blueapron.marinator.Injector;
import com.blueapron.marinator.test.models.ZebraObject;

/**
 * Component to test injection.
 */
public class ZebraComponent {
    @Injector(strict = false)
    public void inject(ZebraObject obj) {
        obj.injected = true;
    }

    // This is unsupported - do not mix strict and loose injection within the same hierarchy!
    //    @Injector
    //    public void inject(OnagerObject obj) {
    //        obj.injected = true;
    //    }
}
