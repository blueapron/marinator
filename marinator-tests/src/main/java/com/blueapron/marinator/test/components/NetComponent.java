package com.blueapron.marinator.test.components;

import com.blueapron.marinator.Injector;
import com.blueapron.marinator.test.models.NetObject1;
import com.blueapron.marinator.test.models.NetObject2;

/**
 * Component to test injection.
 */
public class NetComponent {
    @Injector
    public void inject(NetObject1 obj) {
        obj.injected = true;
    }

    @Injector
    public void provide(NetObject2 obj) {
        obj.injected = true;
    }
}
