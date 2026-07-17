# Alternatives: avoiding the Gluon toolchain entirely

[`tutorial-android.md`](tutorial-android.md) still relies on `gluonfx-maven-plugin` for the actual native compile — it's free and BSD-licensed, but it's Gluon-authored tooling. If your requirement is stricter than "no license fee" and means "no Gluon code in the pipeline at all," here are the two real options as of this writing, and what each one costs you.

## OpenJDK Mobile — the long-term fix, not ready yet

[OpenJDK Mobile](https://openjdk-mobile.github.io/) is Gluon's own answer to "stop maintaining a proprietary mobile patch set forever": instead of a separate commercial toolchain, upstream the minimal patches needed for JVM and library support on mobile directly into OpenJDK itself.

From Gluon's announcement, [*Bringing OpenJDK to Mobile — a Community Effort*](https://gluonhq.com/news/bringing-openjdk-to-mobile-a-community-effort/):

> "Gluon spent nearly a decade offering a custom toolchain enabling Java and JavaFX on iOS and Android. However, this approach proved unsustainable — each OS update and JDK release required extensive maintenance work... Java was designed to be portable, and it should run on mobile as a first-class citizen."

What it actually is, per the [project site](https://openjdk-mobile.github.io/):
- An official downstream repository of `openjdk/jdk` with "minimal patches required for JVM and library support on mobile devices," developed in the open under the `openjdk-mobile` GitHub organization, with the explicit long-term goal of upstreaming into mainline OpenJDK.
- Built via GitHub Actions nightly CI, to catch platform breakage early rather than accumulate a maintenance backlog the way Gluon's old proprietary toolchain did.
- **Current state:** a HelloWorld app runs on iOS. Simulator support, Android compatibility, JavaFX restoration, and native library bindings are listed as planned/in-progress work, not shipped. The project site says plainly: **"This is not (yet) a tutorial for building mobile apps in Java."**

**Verdict:** watch this project — it's the eventual answer to "JavaFX on mobile with no Gluon-specific tooling anywhere" — but as of this writing it is not a viable path to ship an Android app. There's no Android build target you can point Maven at yet, and no JavaFX support on top of it. Revisit this page as the project matures.

## WebFX — works today, different architecture

[WebFX](https://docs.webfx.dev/#_introduction) takes a completely different approach: instead of compiling your JavaFX code to native ARM machine code, it **transpiles** it to JavaScript (via GWT) or WebAssembly (via TeaVM), replacing JavaFX's graphics pipeline with a scene-graph-to-DOM mapper that the browser renders directly.

> "WebFX is a JavaFX application transpiler powered by GWT or TeaVM" — it patches OpenJFX's higher layers for compatibility while swapping out the rendering pipeline entirely.

What that buys you:
- **No GraalVM, no native-image, no Gluon build plugin anywhere in the pipeline.** The output is a web bundle (WebFX reports 97KB–296KB compressed for real apps), which you can host as a plain website, wrap in a WebView-based Android/iOS shell, or run in a desktop browser.
- Runs on: web browsers, desktop (Windows/macOS/Linux), Android, and iOS — from one JavaFX codebase.
- You keep writing familiar JavaFX API calls; no new UI framework to learn.

What it costs you:
- **Feature coverage is intentionally partial.** Per the docs, unsupported today: CSS styling, FXML, 3D graphics, and complex controls like `ComboBox` and `TableView` (simpler alternatives are on the roadmap, but check current status before committing a nontrivial UI to this path).
- Getting started requires structuring your project into platform-specific modules (base app, GWT, TeaVM, OpenJFX, Gluon) via the WebFX CLI — a different project shape than a normal single-module JavaFX app.
- Interestingly, WebFX's own docs mention an optional **"Gluon toolchain"** path for producing native desktop/mobile executables — so even here, if you want a truly native (not WebView-wrapped) mobile binary, Gluon's tooling can re-enter the picture. The Gluon-free path is specifically the web/WebView route, not every deployment target WebFX supports.

**Verdict:** the real "zero Gluon" option available *today*, at the cost of a reduced JavaFX feature surface and a different (transpile + WebView) architecture rather than a true native compile. Worth it if your UI fits within WebFX's supported control/CSS subset and you don't need `gluonfx-maven-plugin`'s native `.so` output specifically.

## Decision guide

| You want... | Use |
|---|---|
| A real native Android binary, shipped today, no license fee | `gluonfx-maven-plugin` + plain `org.openjfx` code — see [`tutorial-android.md`](tutorial-android.md) |
| Gluon Mobile's extra runtime APIs (storage, lifecycle, Charm UI) and don't mind the free license request | Same as above + the [free open-source Gluon license](licensing.md#two-ways-to-avoid-the-popup) |
| Zero Gluon-authored code anywhere, and can live with FXML/CSS/complex-control gaps | [WebFX](https://docs.webfx.dev/#_introduction) |
| Zero Gluon-authored code, full native JavaFX, and can wait | [OpenJDK Mobile](https://openjdk-mobile.github.io/) — not shippable for Android yet |
