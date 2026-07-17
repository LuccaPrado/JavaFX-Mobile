# What actually needs a Gluon license (and what doesn't)

This is the part most JavaFX-mobile tutorials skip, and the reason people wrongly assume you need to pay Gluon to ship an Android app. It doesn't have to be a mystery — the facts are published, just scattered. This page collects them in one place.

## The three separate things people conflate

"Gluon" isn't one thing. For mobile JavaFX there are three genuinely separate pieces, and only one of them ever asks for a license key:

| Piece | What it does | License |
|---|---|---|
| **`gluonfx-maven-plugin`** | Maven plugin that drives the native build (`gluonfx:build`, `:package`, `:install`, `:run`) | [BSD-3-Clause](https://github.com/gluonhq/gluonfx-maven-plugin), fully open source, no key needed |
| **GraalVM with Gluon** (native-image) | Gluon's maintained build of GraalVM's ahead-of-time compiler, used to compile your Java/JavaFX into a native ARM library | Free to download and use for the compile step itself |
| **Gluon Mobile runtime** (`com.gluonhq.attach.*`, `com.gluonhq:charm-glisten`) | Optional runtime libraries for mobile-specific APIs (storage, lifecycle, status bar, the Charm/Glisten UI widget set) | Free for non-commercial use; shows a **runtime popup** unless a license key is present |

The build tooling (first two rows) is what actually turns your code into an installable app. It has no license gate. The **only** thing that gates on a license is *using Gluon Mobile's own runtime libraries* — and even that gate is a popup, not a compile-time or store-submission block.

## What the license key actually does

From Gluon's own docs (`docs.gluonhq.com`):

> "GluonFX Maven plugin is open sourced, and licensed under the BSD-3 license... Some Gluon components require a license in certain circumstances... Licenses are validated online once per application install. If for some reason the license service can't be contacted, your end-users won't be annoyed by the popup, but the license check will be retried each time the application starts until successful."

The mechanism, concretely:
1. If your app depends on `com.gluonhq.attach.*` or `com.gluonhq:charm-glisten`, the built app checks online, once per install, for a `gluonmobile.license` file on your classpath.
2. No valid license → the app shows a small "non-commercial version of Gluon Mobile" popup at startup.
3. That's the entire consequence. It is not a build failure, not a Play Store rejection, not a legal restriction on distribution.

From the [`nop`](https://github.com/michiel-jfx/nop) project README, which ships on the Play Store using Gluon Mobile's Charm/Glisten framework:

> "It uses the GraalVM with Gluon included. This means, there will be a popup to show the non-commercial version of Gluon Mobile is used... If you start building your own opensource mobile application, you can request a one-year license [free, via Gluon's open-source license program]. **With or without license, it is allowed to deploy your app in for example the Google Play Store.**"

That last sentence is the one to remember.

## Two ways to avoid the popup

**Option A — get the free license.** Gluon runs an [open-source license program](https://gluonhq.com/programs/free-gluon-licenses/open-source-license-request/): request a one-year license for a public open-source project, drop the key into `gluonmobile.license` on your classpath, done. Zero cost, the popup goes away, you're still using the same free build plugin.

**Option B — don't use Gluon Mobile's runtime at all.** Skip `com.gluonhq:charm-glisten` and the `com.gluonhq.attach.*` modules entirely and write against plain `javafx.application.Application` / `javafx.scene.*` / FXML. You still use `gluonfx-maven-plugin` to do the native Android build (there's currently no mature open alternative for that specific AOT-compile-to-native-library step — see [`alternatives.md`](alternatives.md)), but since nothing in your dependency tree is Gluon Mobile runtime code, there is nothing to license-check, and no popup.

This repo's [`examples/hello-mobile`](../examples/hello-mobile) follows Option B: it depends on `org.openjfx:javafx-controls` and the `gluonfx-maven-plugin` build tool only. No `com.gluonhq.attach.*`, no Charm/Glisten. The real-world proof this works and ships is [`michiel-jfx/iceconverter`](https://github.com/michiel-jfx/iceconverter), a published Play Store app built the same way (it uses exactly one Attach module, `storage`, for a custom font — see its README: *"I've decided to use the com.gluonhq packages as less as possible, so it stays an org.openjfx tutorial project"*).

## If you want *zero* Gluon anywhere in the toolchain

Options A and B above both still use `gluonfx-maven-plugin` to do the actual native compile — it's free and open source, but it's still a Gluon-maintained tool. If your goal is to have no Gluon-authored code anywhere in the pipeline (not even the free build plugin), see [`alternatives.md`](alternatives.md) for the two real candidates:

- **OpenJDK Mobile** — the long-term fix, upstreaming mobile support directly into OpenJDK so the Gluon plugin becomes unnecessary. Not yet production-ready for Android as of this writing.
- **WebFX** — sidesteps native compilation entirely by transpiling your JavaFX code to JavaScript/WebAssembly and wrapping it as a web/hybrid app. Works today, but is architecturally different (no native `.so`, and a reduced JavaFX API surface).

## Bottom line

For **building and publishing a native JavaFX Android app today**, `gluonfx-maven-plugin` is the practical, working tool, it's free and BSD-licensed, and it does not require anyone to pay Gluon anything. Keep your own code off `com.gluonhq.attach.*`/`charm-glisten` (or grab the free open-source license if you do use them) and you're done — there is no Gluon subscription standing between you and the Play Store.
