Marinator
============

[![CircleCI](https://circleci.com/gh/blueapron/marinator/tree/master.svg?style=shield&circle-token=3abd00c5089a936b41f8fc49bd5fffe0694ee8c6)](https://circleci.com/gh/blueapron/marinator/tree/master)
[![Release](https://jitpack.io/v/blueapron/marinator.svg)](https://jitpack.io/#blueapron/marinator)

Dependency Injection made delicious.

When using dependency injection, getting access to the classes which perform the injection is a common problem. On Android, the most common solution to this tends to be to store the components in the Application object. But this then requires the developer to reach into their Application object in multiple places throughout their code. This creates several challenges - in addition to just looking ugly, it can make it harder to write pure JUnit tests.

Marinator helps solve this problem by wrapping your components with a simple static class. Instead of calling code like `MyApplication.get().getComponent().inject(this)`, you can simply call `Marinator.inject(this)`. Marinator relies on an annotation processor to generate a helper that registers your injectors - as a developer, all you have to do is annotate your injector methods and provide the component to the MarinadeHelper.

Marinator was created with Dagger2 style dependency injection in mind. But there's no requirement that you use Dagger2 or even any sort of framework for DI to use Marinator. As long as your injector class has a method annotated with `@Injector`, Marinator will recognize this and add it to the `prepare` method.

Setup
------

Marinator is distributed via Jitpack. To use Marinator in your project, add the following lines to your `build.gradle` file:

```groovy
// Root build.gradle file:
allprojects {
  repositories {
    maven { url "https://jitpack.io" }
  }
}

// App-level build.gradle file:

// If using Kotlin:
apply plugin: 'kotlin-kapt'

dependencies {
  compile 'com.github.blueapron.marinator:marinator:1.0.2'
  annotationProcessor 'com.github.blueapron.marinator:marinator-processor:1.0.2'

  // If using Kotlin:
  kapt 'com.github.blueapron.marinator:marinator-processor:1.0.2'
}
```

When you define your components, declare the injector methods and annotate them with `@Injector`. So, as an example:

```java
@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {
  @Injector
  void inject(Recipe recipe);

  // Note that the injector method can be named
  // whatever you want - it doesn't have to be
  // called "inject".
  @Injector
  void provide(Wine wine);

  // Other Dagger component dependencies declared here.
}
```

The next time you compile after adding this annotation, the annotation processor will generate the `MarinadeHelper` class for you. Wherever you create your components, you can use the `MarinadeHelper` to register them as injectors:

```java
// In your application class:
public class MyApplication extends Application {

  // ...

  @Override
  public void onCreate() {
    super.onCreate();

    // Create our components and register as an injector. Note that we can
    // use multiple components here as needed.
    mApplicationComponent = createApplicationComponent();
    mNetworkComponent = createNetworkComponent(mApplicationComponent);
    MarinadeHelper.prepare(mApplicationComponent, mNetworkComponent);
  }
}

// In unit tests:
public abstract class BaseUnitTest extends TestCase {

  // ...

  @Before
  public void init() {
      mApplicationComponent = DaggerMockApplicationComponent.create();
      mNetworkComponent = DaggerMockNetworkComponent.builder()
        .mockApplicationComponent(mApplicationComponent)
        .build();
      MarinadeHelper.prepare(mApplicationComponent, mNetworkComponent);
  }
}
```

You can also register/unregister injectors dynamincally using Marinator. This helps if you need to register an injector for less than the entire lifecycle of the application.

Finally, in your classes, use Marinator to inject the necessary dependencies. The code doesn't care whether the components were provided by the application, by a unit test, or by something else altogether:

```java
public final class Recipe {
  @Inject Context mContext;

  public Recipe() {
    Marinator.inject(this);
  }
}

public final class Wine {
  @Inject Context mContext;

  public Wine() {
    Marinator.inject(this);
  }
}
```

Testing
------

To run the Marinator tests, use the command line. (Android Studio doesn't play nicely with annotation processing in unit tests)

From the root directory of the project, run `./gradlew clean test` to run the unit tests.

License
-------

Marinator is licensed under the MIT license. See [the license](LICENSE) for details.
