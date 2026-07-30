# BYO application

Flagon compiles ahead of time, so an application jar is a build input.
Running one means adding a module that puts the jar on a classpath alongside the compatibility
layer and compiles the result to JavaScript. `bundle-example/` in this repository is that module,
and `example-app/` is a jar built against the real `org.lwjgl.lwjgl:lwjgl` artifact that knows
nothing about Flagon — together they are the template to copy.

## The bundle module

```xml
<dependencies>
  <dependency><groupId>dev.flagon</groupId><artifactId>runtime</artifactId></dependency>
  <dependency><groupId>dev.flagon</groupId><artifactId>platform-browser</artifactId></dependency>
  <!-- supplies org.lwjgl.* in place of the artifact the application was built against -->
  <dependency><groupId>dev.flagon</groupId><artifactId>lwjgl-compat</artifactId></dependency>

  <dependency><groupId>com.example</groupId><artifactId>your-app</artifactId></dependency>
</dependencies>
```

The application's own LWJGL dependency must be `provided` or excluded. If the real `org.lwjgl`
classes reach the classpath they will shadow the compatibility layer, and the bundle will fail to
link against native methods that cannot exist in a browser.

The entry point names the host and the application's `main`:

```java
public static void main(String[] arguments) {
    ProgramRegistry registry = new ProgramRegistry()
            .register(new MainProgram("app", "your application", YourApp::main));
    FlagonRuntime.boot(BrowserPlatform.create(arguments), registry);
}
```

A method reference, not a class name: there is no class loading at runtime, so a name the
compiler never saw would not be in the bundle at all.

## Building

Inheriting `dev.flagon:flagon-parent` gives the TeaVM configuration; a bundle sets only where it
starts and what it emits.

```xml
<properties>
  <flagon.bundle.mainClass>com.example.BrowserMain</flagon.bundle.mainClass>
  <flagon.bundle.fileName>app.js</flagon.bundle.fileName>
  <flagon.bundleOutput>${project.basedir}/../web/public/flagon</flagon.bundleOutput>
</properties>
```

Outside this repository, or with a parent of your own, copy the `teavm-maven-plugin` execution
from the root `pom.xml`. `strict` is the one setting not to change: without it a null dereference
becomes a JavaScript `TypeError` that `catch (NullPointerException)` cannot catch.

## Depending on a version

Releases are published to GitHub Packages, which requires authentication even for public
packages. In `~/.m2/settings.xml`:

```xml
<server>
  <id>flagon</id>
  <username>your-github-username</username>
  <password>a-token-with-read:packages</password>
</server>
```

```xml
<repository>
  <id>flagon</id>
  <url>https://maven.pkg.github.com/KingJayan/flagon</url>
</repository>
```

To work against an unreleased Flagon instead, run `./mvnw install` here and depend on the
snapshot version in the root `pom.xml`.

## What a new Flagon version can change

Pin a version, and treat an upgrade as something to run your own tests against. Under semantic
versioning before 1.0, a minor bump may break the surfaces below that are not stable.

| Surface | Stability |
|---------|-----------|
| `org.lwjgl.*` | stable; signatures match LWJGL 2.9.3 |
| `dev.flagon.runtime` — `Program`, `ProgramRegistry`, `MainProgram`, `FlagonRuntime.boot` | stable |
| `dev.flagon.platform.browser.BrowserPlatform.create` | stable |
| `window.__flagonHost` — `log`, `status`, `ready`, `exited`, `fatal` | stable |
| Bundle build properties (`flagon.bundle.*`, `flagon.strict`, `flagon.target`) | stable |
| `dev.flagon.platform.*` beyond the above, including `GraphicsDevice` | changes with the layer below it |
| Anything in a package named `internal` | no guarantees |

New capabilities land as new entry points, so an upgrade adds calls that used to throw rather
than changing what existing ones do. [The compatibility matrix](COMPATIBILITY.md) is the record
of what works at a given version; each release's notes say what moved.

## Reporting a gap

An unimplemented entry point throws `UnsupportedOperationException` naming itself, so the message
is enough to open an issue with. That is the intended way to find what is missing: run the
application, read what it asked for, and [open an issue](https://github.com/KingJayan/flagon/issues/new) in GitHub with details.
