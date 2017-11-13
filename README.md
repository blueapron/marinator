<p align="center" >
  <img src="https://user-images.githubusercontent.com/16063079/29837717-f3a09954-8cc6-11e7-8b3b-c89719d5ac40.png" width=256px height=256 alt="Marinator" title="Marinator">
</p>

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
  implementation 'com.github.blueapron.marinator:marinator:1.0.4'
  annotationProcessor 'com.github.blueapron.marinator:marinator-processor:1.0.4'

  // If using Kotlin:
  kapt 'com.github.blueapron.marinator:marinator-processor:1.0.4'
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

Strict vs Loose Injection
------

Marinator supports two different injection modes, which we call "strict" and "loose". By default, all injectors are strict - the Injector registered for a class will match that class name only. This is the safest default behavior, since it prevents unintended consequences. But sometimes, it's useful to be slightly looser and let injection be provided by a super class. Notably, this might be the case if you are extending a production class with a testing class - all of the injected dependencies can be satisfied by the production injection, so loose injection is safe.

Consider the following example:
```java
public class Fruit {
  @Inject Context mContext;

  public Fruit() {
    Marinator.inject(this);
  }
}

public class Apple extends Fruit {
}

public class Banana extends Fruit {
}

public class Cherry extends Fruit {
}
```

We might register our Injector for this scenario as follows:

```java
@Singleton
@Component(modules = FruitModule.class)
public interface FruitComponent {
  @Injector
  void inject(Apple apple);
  @Injector
  void inject(Banana banana);
  @Injector
  void inject(Cherry cherry);
}
```

This is using the default of strict injection. This is safe, but somewhat annoying if someone adds a new Fruit - they have to remember to add a new injector for their new type. Using loose injection, we could write the following:

```java
@Singleton
@Component(modules = FruitModule.class)
public interface FruitComponent {
  @Injector(strict = false)
  void inject(Fruit fruit);
}
```

That's it! All new Fruit subclasses will "just work". The downside to this approach is that if someone changes a Fruit subclass to require specific injection (ie, by adding injected members), the code may produce unexpected results since the expected Injector will not be run.

The implication of this is that *you should not mix strict and loose for objects in the same class hierarchy.* This behavior is important to remember if you choose to use loose injection. Since Marinator will generate its injection cascade based on a non-determinstic order of traversal, you cannot guarantee that superclass evaluation will be checked before subclass - leading to unexpected results. So for all objects within a given class hierarchy, use *either* strict injection *or* loose injection, but not both.

To demonstrate this, consider the following component:

```java
@Singleton
@Component(modules = FruitModule.class)
public interface FruitComponent {
  @Injector(strict = false)
  void inject(Fruit fruit);
  @Injector
  void inject(Apple apple);
}
```

What will happen when an Apple is constructed? It depends on exactly which code Marinator happens to generate. The generated code may read something like this:

```java
@Override
public void inject(Object obj) {
  if (obj instanceof Fruit) {
    mFruitComponent.inject((Fruit) obj);
  } else if (obj instanceof Apple) {
    mFruitComponent.inject((Apple) obj);
  }
}
```

This would result in undefined behavior where `Apple` objects would not have their members injected correctly. (If mixing strict and loose injection is important for your use cases, please file an issue so we can track it and consider the best way to support it in a future release.)

Testing
------

To run the Marinator tests, use the command line. (Android Studio doesn't play nicely with annotation processing in unit tests)

From the root directory of the project, run `./gradlew clean test` to run the unit tests.

License
-------

Marinator is licensed under the MIT license. See [the license](LICENSE) for details.

Third-Party Licenses
-------

#### Guava

Copyright 2011, Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

#### JavaPoet

Copyright 2015 Square, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.