# Running an existing application

Flagon runs existing desktop Java/LWJGL applications in the browser by compiling them
together with the compatibility layer.

Unlike a desktop JVM, the browser cannot load arbitrary jars at runtime. Every application is
compiled ahead of time into its own browser bundle.

This document shows how to create that bundle.

## How it works

A downstream project normally looks like this:

```
your-app/
├── app/                 your existing application
└── bundle-browser/      browser entry point
```

`app` is your application exactly as it exists today. It continues to compile against the real
LWJGL artifacts.

`bundle-browser` depends on your application together with Flagon's runtime and compatibility
layer, then compiles everything into JavaScript with TeaVM.

The `examples/app` and `examples/bundle` modules in this repository demonstrate this layout.

## 1. Create a bundle module

Your bundle depends on:

```xml
<dependencies>
    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>runtime</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>platform-browser</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.flagon</groupId>
        <artifactId>lwjgl-compat</artifactId>
    </dependency>

    <dependency>
        <groupId>com.example</groupId>
        <artifactId>your-app</artifactId>
    </dependency>
</dependencies>
```

`lwjgl-compat` provides the `org.lwjgl.*` classes your application was compiled against.

Add `socket-classlib` too if your application opens a `java.net.Socket`; without it the class is
absent from the classlib and the bundle will not link.

Add `reflect-policy` if your application reflects over the fields, methods or constructors of its
own classes. Without it those answer empty; with it, the classes you name keep their members. See
[Reflection](COMPATIBILITY.md#reflection-reaches-types-not-members) for how to name them.

Your application's normal LWJGL dependency **must not** appear in the bundle module. Mark it as
`provided` or exclude it entirely. Otherwise the real LWJGL classes will shadow the compatibility
layer and the build will fail when native methods are linked.

## 2. Register your application

Every bundle has a small browser entry point.

```java
public static void main(String[] arguments) {
    ProgramRegistry registry = new ProgramRegistry()
            .register(new MainProgram(
                    "app",
                    "Your Application",
                    YourApplication::main));

    FlagonRuntime.boot(
            BrowserPlatform.create(arguments),
            registry);
}
```

Programs are registered explicitly because there is no runtime class loading. A method reference
keeps the application's entry point reachable during ahead-of-time compilation.

Multiple applications can be registered in the same bundle.

## 3. Configure the build

If your project inherits `flagon-parent`, only three properties are required:

```xml
<properties>
    <flagon.bundle.mainClass>com.example.BrowserMain</flagon.bundle.mainClass>
    <flagon.bundle.name>app</flagon.bundle.name>
    <flagon.bundleOutput>...</flagon.bundleOutput>
</properties>
```

If you use your own parent POM, copy the TeaVM plugin configuration from this repository's root
`pom.xml`.

A shell can only offer a bundle's programs before it has booted if the build writes them down.
Expose the registry from a static method, point `flagon.bundle.catalogClass` at a class whose
`main` hands it to `ProgramCatalog.writeTo(registry, args[0])`, and the build emits
`app.catalog.json` beside `app.wasm`. Without it the picker fills in on the first run instead.

Leave `flagon.strict` enabled. Without it, Java null dereferences become JavaScript
`TypeError`s instead of catchable `NullPointerException`s.

## 4. Build

```
./mvnw package
```

TeaVM compiles the application, Flagon, and your bundle entry point into a single WebAssembly GC
module, `app.wasm`, beside the loader it is started through.

There is no runtime class loading. Every reachable class is included during compilation.

Adding `-Pjs` compiles the same sources to JavaScript instead, emitting `app.js`. Ship both if you
serve browsers that predate WebAssembly GC; the shell picks between them by feature detection.

## Publishing

Released versions are published to GitHub Packages.

Configure authentication in `~/.m2/settings.xml`:

```xml
<server>
    <id>flagon</id>
    <username>your-github-username</username>
    <password>github-token-with-read:packages</password>
</server>
```

Then add the repository:

```xml
<repository>
    <id>flagon</id>
    <url>https://maven.pkg.github.com/KingJayan/flagon</url>
</repository>
```

For development against Flagon itself:

```
./mvnw install
```

and depend on the snapshot version installed into your local Maven repository.

## Stable API

The following surfaces are intended for downstream use.

| Surface | Stability |
|---------|-----------|
| `org.lwjgl.*` | Stable |
| `FlagonRuntime.boot` | Stable |
| `Program`, `ProgramRegistry`, `MainProgram` | Stable |
| `BrowserPlatform.create` | Stable |
| `window.__flagonHost` | Stable |
| `flagon.bundle.*` build properties | Stable |
| `@Reflectable`, `flagon.reflect.classes` | Stable |

Everything else should be considered an implementation detail unless documented otherwise.

## Missing functionality

Unsupported features throw `UnsupportedOperationException` naming the missing entry point.

If your application encounters one, check the
[Compatibility Matrix](COMPATIBILITY.md). If the feature is not listed there,
please open an issue with:

- the exception
- the application
- reproduction steps