# JavaFX for Mobile — without a Gluon license

A documentation-first repo about building and shipping **Android apps written in plain JavaFX**, and being precise about what actually requires a Gluon license and what doesn't. It exists because that distinction is widely misunderstood: most people assume "mobile JavaFX" = "pay Gluon", and that's not quite true.

## TL;DR

- **You do not need to pay Gluon anything to compile, sign, and publish a JavaFX app to Android.**
- The tool that compiles your JavaFX/Java code into a native Android library (`gluonfx-maven-plugin`) is free, open source ([BSD-3-Clause](https://github.com/gluonhq/gluonfx-maven-plugin)), and works standalone with no license key.
- Some pieces of **Gluon Mobile** (the `com.gluonhq.attach.*` and `com.gluonhq:charm-glisten` libraries, and the GraalVM-with-Gluon native-image build itself) show a **non-commercial-use popup at runtime** unless a `gluonmobile.license` key is present. That popup is cosmetic, not a legal gate — Gluon explicitly says you're allowed to ship to the Play Store with or without one, and open-source projects can request a **free** one-year license.
- If you want zero Gluon involvement at all — not even the free build plugin — your options today are early-stage ([OpenJDK Mobile](https://openjdk-mobile.github.io/), no production Android support yet) or architecturally different ([WebFX](https://docs.webfx.dev/), which transpiles JavaFX to JS/Wasm instead of compiling native code).

Read [`docs/licensing.md`](docs/licensing.md) first — it's the part most tutorials get wrong or skip.

## What's in this repo

| Path | What it is |
|---|---|
| [`docs/licensing.md`](docs/licensing.md) | Exactly what needs a Gluon license, what doesn't, and why. Start here. |
| [`docs/tutorial-android.md`](docs/tutorial-android.md) | Step-by-step: set up your environment, build, run on a device, sign, and publish an APK/AAB. |
| [`docs/alternatives.md`](docs/alternatives.md) | OpenJDK Mobile and WebFX — for when you want to avoid the Gluon toolchain entirely, and what you give up by doing so. |
| [`examples/hello-mobile/`](examples/hello-mobile/) | A minimal, buildable JavaFX app targeting Android. No Gluon Mobile framework code, no Attach dependencies — just `javafx.application.Application` plus the build plugin. |

## The short version of how this works

JavaFX-on-Android is **not** running a JVM on the phone. `gluonfx-maven-plugin` drives GraalVM's `native-image` (a Gluon-maintained build of GraalVM) to ahead-of-time compile your Java + JavaFX code into a native ARM library. Android loads that library through a thin JNI shim and treats it exactly like a C/C++ `.so` — you write 100% Java, the toolchain produces a native binary.

```
Your Java/JavaFX code  →  GraalVM native-image (via gluonfx-maven-plugin)  →  libyourapp.so
                                                                              ↓
                                                          Android APK/AAB wraps it + JNI glue
```

Nothing in that pipeline requires a paid license. What triggers Gluon's non-commercial popup is *using Gluon Mobile's runtime libraries* (Attach APIs, Charm/Glisten UI) without a license key registered — see [`docs/licensing.md`](docs/licensing.md) for the precise mechanism and sources.

## Credit and sources

This repo is a synthesis of, and commentary on, existing public work — it is not a rewrite of any single source. Primary references:

- [michiel-jfx/iceconverter](https://github.com/michiel-jfx/iceconverter) — a real shipped Play Store app built with plain `org.openjfx` (no Charm/Glisten), used here as the model for "minimal Gluon" native builds.
- [michiel-jfx/nop](https://github.com/michiel-jfx/nop) — a minimal mobile app skeleton; its README is the clearest public statement of the actual Gluon licensing mechanics.
- [Gluon: Bringing OpenJDK to Mobile — a community effort](https://gluonhq.com/news/bringing-openjdk-to-mobile-a-community-effort/) and [openjdk-mobile.github.io](https://openjdk-mobile.github.io/) — Gluon's own upstream effort to make mobile support part of OpenJDK, removing the need for their proprietary toolchain long-term.
- [WebFX docs](https://docs.webfx.dev/#_introduction) — a JavaFX-to-web/native transpiler that never touches Gluon's native-image toolchain.

## License

Documentation and example code in this repo are released under the [Apache License 2.0](LICENSE), matching the upstream projects it draws from.
