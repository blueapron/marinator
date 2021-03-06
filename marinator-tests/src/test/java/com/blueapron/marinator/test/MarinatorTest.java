package com.blueapron.marinator.test;

import com.blueapron.marinator.Marinator;
import com.blueapron.marinator.generated.MarinadeHelper;
import com.blueapron.marinator.test.components.AppComponent;
import com.blueapron.marinator.test.components.BananaComponent;
import com.blueapron.marinator.test.components.NetComponent;
import com.blueapron.marinator.test.components.ZebraComponent;
import com.blueapron.marinator.test.models.AppObject1;
import com.blueapron.marinator.test.models.AppObject2;
import com.blueapron.marinator.test.models.BananaObject;
import com.blueapron.marinator.test.models.CavendishObject;
import com.blueapron.marinator.test.models.NetObject1;
import com.blueapron.marinator.test.models.NetObject2;
import com.blueapron.marinator.test.models.NonInjectedObject;
import com.blueapron.marinator.test.models.OkapiObject;
import com.blueapron.marinator.test.models.OnagerObject;
import com.blueapron.marinator.test.models.ZebraObject;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

/**
 * Tests for the Marinator class.
 */
public class MarinatorTest {

    @Test
    public void testInjectors() {
        // Create the components and prepare the Marinade.
        AppComponent appComponent = new AppComponent();
        BananaComponent bananaComponent = new BananaComponent();
        NetComponent netComponent = new NetComponent();
        ZebraComponent zebraComponent = new ZebraComponent();
        MarinadeHelper.prepare(appComponent, bananaComponent, netComponent, zebraComponent);

        // Now create some objects and inject them.
        AppObject1 app1 = new AppObject1();
        assertThat(app1.injected).isFalse();
        Marinator.inject(app1);
        assertThat(app1.injected).isTrue();

        AppObject2 app2 = new AppObject2();
        assertThat(app2.injected).isFalse();
        Marinator.inject(app2);
        assertThat(app2.injected).isTrue();

        BananaObject banana = new BananaObject();
        assertThat(banana.injected).isFalse();
        Marinator.inject(banana);
        assertThat(banana.injected).isTrue();

        NetObject1 net1 = new NetObject1();
        assertThat(net1.injected).isFalse();
        Marinator.inject(net1);
        assertThat(net1.injected).isTrue();

        NetObject2 net2 = new NetObject2();
        assertThat(net2.injected).isFalse();
        Marinator.inject(net2);
        assertThat(net2.injected).isTrue();

        ZebraObject zebra = new ZebraObject();
        assertThat(zebra.injected).isFalse();
        Marinator.inject(zebra);
        assertThat(zebra.injected).isTrue();

        NonInjectedObject nonInjected = new NonInjectedObject();
        assertThat(nonInjected.injected).isFalse();
        try {
            Marinator.inject(nonInjected);
            fail("Non registered injector should fail");
        } catch (IllegalStateException ise) {
            // Expected to fail - move on
        }

        // Banana is a strict injector - so we shouldn't be able to inject a Cavendish without
        // registering for it directly.
        CavendishObject cavendish = new CavendishObject();
        assertThat(cavendish.injected).isFalse();
        try {
            Marinator.inject(cavendish);
            fail("Injecting a strict class with no explicit injector should fail");
        } catch (IllegalStateException ise) {
            // Expected - this injection should fail.
        }

        // ZebraComponent allows loose injection - it lets us inject things that are kind of
        // zebras, but not quite. Note that the overridden "injected" field is NOT mutated here.
        OkapiObject okapi = new OkapiObject();
        assertThat(okapi.injected).isFalse();
        Marinator.inject(okapi);
        assertThat(okapi.injected).isFalse();
    }
}
