# Tutorial: compiling a JavaFX app to Android, without Gluon Mobile

This walks through building the [`examples/hello-mobile`](../examples/hello-mobile) app from source to an installed APK on a real device, then to a signed release bundle. It follows Option B from [`licensing.md`](licensing.md): plain `org.openjfx` code, no Gluon Mobile runtime, using only the free `gluonfx-maven-plugin` for the native build.

Read [`licensing.md`](licensing.md) first if you haven't — it explains why this approach needs no Gluon license.

## 1. Host OS: Windows needs WSL2

Gluon's own docs are explicit that the `android` target only builds from a Linux host — every IDE/build workflow section for Android in `docs.gluonhq.com` repeats the line *"This part is only applicable for Linux."* iOS is macOS-only. Only the plain desktop target builds natively on Windows. There is no native-Windows path to `gluonfx:build -Pandroid` — the reference Ubuntu setup this tutorial links to in [section 2](#2-prerequisites) exists for exactly this reason.

If you're on Windows with Android Studio already installed, the practical fix is **WSL2 running Ubuntu**, not a dual-boot or a separate physical/VM machine:

- WSL2 is a real Linux kernel, not an emulation layer, so GraalVM's `native-image` and the Android NDK's cross-linker run inside it completely unmodified — this is not a Gluon-specific accommodation, it's just Linux.
- IntelliJ IDEA on Windows has first-class WSL support: it can open a project that physically lives inside the WSL filesystem, run its embedded terminal attached to the WSL distro, and use a JDK/Maven installed inside WSL as the project's toolchain — so day-to-day editing, refactoring, and debugging still feel like a normal Windows IntelliJ session.
- **For an AI coding agent working on this repo (Claude Code or another instance):** a Bash/PowerShell tool running on the Windows side cannot execute Linux ELF binaries — `native-image` and the NDK linker will simply fail to run. Either invoke commands via `wsl.exe -d Ubuntu -- bash -lc "..."` from the Windows-side session for each build step, or — cleaner — have the agent's session run from *inside* a WSL terminal in the first place, so its shell tool is a native WSL bash and every path it touches is already a Linux path. The latter avoids constant Windows-path/WSL-path translation and is the recommended setup if you're delegating the actual `gluonfx:build`/`:package` steps to an agent.

Practical setup notes:

- **Keep the project inside the WSL filesystem** (e.g. `~/dev/javafxmobile` under `/home/<you>/...`), not under `/mnt/c/...`. A Linux process reading/writing across the 9P bridge into NTFS is dramatically slower than native ext4 I/O, and `native-image` compiles are already I/O- and memory-heavy — this is the single biggest performance mistake people make with WSL2 JavaFX-mobile builds. IntelliJ can still open that same project from Windows via the `\\wsl$\Ubuntu\home\<you>\dev\javafxmobile` (or `\\wsl.localhost\Ubuntu\...` on newer Windows builds) UNC path, or via IntelliJ's native "open a WSL folder" support.
- **Install a separate Android SDK/NDK inside WSL2** (via the command-line `sdkmanager`, not by pointing at your Windows-side Android Studio SDK) — the two live on different filesystems with different path syntax, and `gluonfx-maven-plugin` expects `ANDROID_SDK`/`ANDROID_NDK` to resolve as native Linux paths. Android Studio on Windows is still useful for its SDK Manager UI and for editing, just not for the compile step itself.
- **USB device access is the one real friction point.** WSL2 has no native USB passthrough, so `adb` inside WSL2 won't see a device plugged into the Windows host by default. Two fixes:
  1. [`usbipd-win`](https://github.com/dorssel/usbipd-win) — forwards the USB device into the WSL2 distro so `adb devices` sees it directly.
  2. Or skip USB passthrough entirely: run `adb.exe tcpip 5555` from the Windows side (using Android Studio's platform-tools, after authorizing the device once over USB there), then `adb connect <windows-host-ip>:5555` from inside WSL2. Simpler to set up than `usbipd-win` if you already have Android Studio's `adb` working on Windows.

## 2. Prerequisites

| Tool | Version used here | Notes |
|---|---|---|
| JDK | 17 | Used to run Maven and compile your Java sources |
| GraalVM with Gluon (native-image) | 22.1.0.1 | Does the actual AOT compile to native ARM code |
| Maven | any recent 3.x | Build driver |
| Android SDK + NDK | compileSdk 35–36, build-tools 35.0.0 | Needed by `gluonfx-maven-plugin` to package the `.so` into an APK/AAB |
| A physical Android device | USB debugging enabled | GraalVM native-image cross-compiles for `aarch64`; there is no emulator target |

Set `GRAALVM_HOME` to your GraalVM-with-Gluon install and `ANDROID_SDK`/`ANDROID_NDK` to your SDK/NDK paths — `gluonfx-maven-plugin` reads these from the environment. If you're setting up a Linux (Ubuntu) box from scratch, see the [environment setup walkthrough](https://www.dotjava.nl/2025/04/20/ubuntu-for-mobile-android-java-development/) referenced by the `iceconverter` project.

## 3. Project layout

```
hello-mobile/
├── pom.xml
└── src/
    ├── main/java/
    │   ├── module-info.java
    │   └── dev/javafxmobile/hello/HelloMobileApp.java
    └── android/
        └── AndroidManifest.xml
```

Two things are non-obvious and worth calling out:
- `src/android/AndroidManifest.xml` is **your own** manifest, referenced from `pom.xml` via `<manifestPath>`. `gluonfx-maven-plugin` merges it into the generated Android project rather than fully generating one for you.
- The manifest's launcher `<activity>` points at `com.gluonhq.helloandroid.MainActivity` — that's the plugin's generic native-activity bootstrap class (JNI glue to start your native library), not part of Gluon Mobile's licensed runtime.

## 4. The POM, annotated

The full file is at [`examples/hello-mobile/pom.xml`](../examples/hello-mobile/pom.xml). The parts that matter:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.17</version>
</dependency>
```
Plain JavaFX. No `com.gluonhq.attach.*`, no `charm-glisten`.

```xml
<profile>
    <id>android</id>
    <properties>
        <gluonfx.target>android</gluonfx.target>
        <manifestPath>${project.basedir}/src/android/AndroidManifest.xml</manifestPath>
    </properties>
</profile>
```
Activated with `-Pandroid`; tells the plugin which manifest to merge and which target to cross-compile for.

```xml
<plugin>
    <groupId>com.gluonhq</groupId>
    <artifactId>gluonfx-maven-plugin</artifactId>
    <version>1.0.28</version>
    <configuration>
        <target>${gluonfx.target}</target>
        <mainClass>dev.javafxmobile.hello.HelloMobileApp</mainClass>
        <linkerArgs>
            <arg>-Wl,-z,max-page-size=16384</arg>
        </linkerArgs>
        <nativeImageArgs>
            <arg>-H:PageSize=16384</arg>
            <arg>-H:NativeLinkerOption=-Wl,-z,max-page-size=16384</arg>
        </nativeImageArgs>
    </configuration>
</plugin>
```
The `linkerArgs`/`nativeImageArgs` block exists because **since November 1, 2025, Google Play requires 16 KB memory page size support** for new/updated apps. Without these flags the native library is built with the older 4 KB page alignment and Play Console will reject the upload.

## 5. Build and run on a connected device

```bash
mvn clean
mvn -Pandroid gluonfx:build gluonfx:package
mvn -Pandroid gluonfx:install
mvn -Pandroid gluonfx:nativerun
```

- `gluonfx:build` — runs the GraalVM native-image AOT compile. This is the slow step (minutes, not seconds).
- `gluonfx:package` — wraps the resulting `libHelloMobile.so` into an Android project and builds an APK/AAB via Gradle under the hood.
- `gluonfx:install` — pushes the APK to your connected device via `adb`.
- `gluonfx:nativerun` — launches it.

Outputs land under `target/gluonfx/aarch64-android/gvm/`: `HelloMobile.apk`, `HelloMobile.aab`, and the native `libHelloMobile.so`.

If a build is slow, parallelize the Maven reactor: `mvn -T 2C clean gluonfx:build gluonfx:package -Pandroid`.

**This genuinely requires a physical arm64 device — an emulator will not substitute.** It's tempting to create an `arm64-v8a` AVD to sidestep needing a physical device, since the Android SDK happily lets you download that system image. Don't bother: modern Android Emulator builds (tested here on 36.6.11) refuse to boot it outright on an x86_64 host —

```
FATAL | Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host.
      | System image must match the host architecture.
```

Google dropped software-translated arm64 emulation from current emulator releases; arm64 system images now only boot with hardware acceleration on Apple Silicon Mac hosts. On x86_64 Windows/Linux there's no fallback — not even a slow one. A same-arch (x86_64) emulator can't run our build either, since it's an aarch64 `.so` and `gluonfx-maven-plugin`/this GraalVM+substrate combination only targets `aarch64-linux-android` for Android (every reference project here does the same — there's no evidence an `x86_64-linux-android` target is supported). A real device over USB is the only way to see this build run.

## 6. Run on desktop for fast iteration

Native builds are slow, so develop against the plain desktop JVM with the `javafx-maven-plugin` and only cross-compile for Android once the UI/logic works:

```bash
mvn gluonfx:run
```

(this actually invokes `org.openjfx:javafx-maven-plugin`'s `run` goal wired up in the POM — see the example POM for the exact binding).

## 7. Verify the 16 KB page alignment

Google provides `check_elf_alignment` to confirm your APK/library actually meets the November 2025 requirement before you upload anywhere:

```bash
check_elf_alignment target/gluonfx/aarch64-android/gvm/HelloMobile.apk
check_elf_alignment target/gluonfx/aarch64-android/libHelloMobile.so
```

## 8. Signing for the Play Store

`gluonfx:package` produces a **debug-signed** `.aab`, which Play Console rejects outright ("contains debug information"). Getting a real release build takes a few extra steps because the plugin's own signing configuration doesn't reliably flow through to the generated Gradle project. The condensed, working recipe (full detail and troubleshooting notes in the [`iceconverter` signing README](https://github.com/michiel-jfx/iceconverter/blob/main/README-signing.md)):

**a. Create a keystore once, per app:**
```bash
mkdir -p ~/signing/hello-mobile && cd ~/signing/hello-mobile
keytool -genkey -v -keystore hello-mobile-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias hello-mobile-alias
```

**b. Build, then edit the generated Gradle project directly** (the plugin regenerates this project fresh each `gluonfx:package`, so these edits don't persist across builds — expect to redo them per release):

```bash
mvn -Pandroid clean gluonfx:build gluonfx:package
```

In `target/gluonfx/aarch64-android/gvm/android_project/`:
- `gradle.properties` and `project.properties` → add `android.debuggable=false`
- `app/build.gradle` → set `compileSdkVersion 35` / `targetSdkVersion 35` (or whatever Play currently requires — this moves; check the [Play Console requirements](https://support.google.com/googleplay/android-developer/answer/11926878) before you upload)
- `app/keystore.properties` → point it at your real keystore:
  ```properties
  storeFile=/home/you/signing/hello-mobile/hello-mobile-release-key.jks
  storePassword=<your password>
  keyAlias=hello-mobile-alias
  keyPassword=<your password>
  ```

**c. Build the release bundle directly with Gradle** (not the debug variants the plugin defaults to):
```bash
cd target/gluonfx/aarch64-android/gvm/android_project
./gradlew assembleRelease
./gradlew bundleRelease
cp app/build/outputs/bundle/release/app-release.aab ~/Downloads/HelloMobile.aab
```

That `.aab` is what Play Console will actually accept.

## 9. What none of the above needed

At no point did this require a Gluon account, a Gluon license key, or a `gluonmobile.license` file. Everything used is the free `gluonfx-maven-plugin`, the free GraalVM-with-Gluon build, standard Android SDK tooling, and plain `org.openjfx` code. See [`licensing.md`](licensing.md) if you *do* want to add Gluon Mobile's Attach APIs or Charm/Glisten UI toolkit later — that's the point where the licensing conversation becomes relevant.

## 10. Troubleshooting: WSL2-specific failures

This project builds successfully under WSL2, but two WSL2-specific issues showed up running it there that have nothing to do with Gluon licensing and are easy to mistake for something else. Both are one-time environment fixes, not code changes.

**`Fatal error: java.lang.NullPointerException: Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null`**

This can show up twice, in two unrelated JVMs: once during the `native-image` compile (`gluonfx:build`), and again inside the Gradle daemon during packaging (`gluonfx:package`). Both crash for the same reason: the JVM's cgroup v2 controller detection — used internally by `ManagementFactory`/`JvmWideVariable` to build resource-usage MBeans — assumes a full systemd-managed cgroup v2 hierarchy. WSL2's cgroupfs doesn't populate `cgroup.controllers` the same way, so the lookup NPEs. This is a JDK bug (this exact GraalVM build bundles JDK 17.0.3), not anything specific to `gluonfx-maven-plugin`.

Since it hits two independent JVM subprocesses, fix it once at the environment level rather than patching each Maven goal separately:

```bash
export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"
```

Every JVM this build spawns (`native-image`, the Gradle wrapper, the Gradle daemon) reads this standard env var automatically and prints `Picked up JAVA_TOOL_OPTIONS: ...` when it does — that line in the log confirms it took effect.

**`SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable...`**

`gluonfx-maven-plugin` downloads and manages its own Android SDK/NDK (under `~/.gluon/substrate/Android` by default) the first time it needs one, but it doesn't automatically export `ANDROID_HOME` for the Gradle subprocess it shells out to during `gluonfx:package`. Set both explicitly before building:

```bash
export ANDROID_HOME="$HOME/.gluon/substrate/Android"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

**Put it all together** — this is the full environment a build needs on a fresh WSL2 shell:

```bash
export GRAALVM_HOME="$HOME/graalvm-svm-java17-linux-gluon-22.1.0.1-Final"
export JAVA_HOME="$GRAALVM_HOME"
export PATH="$GRAALVM_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/.gluon/substrate/Android"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_TOOL_OPTIONS="-XX:-UseContainerSupport"

mvn -Pandroid clean gluonfx:build gluonfx:package
```

**If you're scripting this (including handing it to an AI coding agent):** WSL's `.bashrc` starts with a guard —

```bash
case $- in
    *i*) ;;
      *) return;;
esac
```

— that makes it return immediately for any non-interactive shell. `wsl.exe -d <distro> -- bash -lc "..."` (a login shell, but non-interactive) hits that guard and skips the rest of `.bashrc` entirely, so any `GRAALVM_HOME`/`JAVA_HOME`/SDKMAN setup that normally lives there silently never runs — env vars that look correct in an interactive terminal come back empty. Export everything explicitly in the script you hand to `bash`, as above, rather than relying on `.bashrc` to have already set it up.

## 11. FAQ

**The app doesn't fill the screen after the first launch — it's only correct on a fresh install/first open, and resuming from the background shows it smaller or off to one side.**

This is caused by giving the `Scene` an explicit fixed size at construction time, e.g. `new Scene(root, 360, 640)`. On a true cold start that happens to still render correctly, because the native Android window is created fresh and JavaFX's Android Glass backend applies the real device bounds over it. But our `AndroidManifest.xml` sets `android:configChanges="orientation|keyboardHidden"`, which tells Android to keep the existing Activity (and the same native/JavaFX process) alive across a pause/resume instead of recreating it — home-button-and-back, an incoming call, a rotation, etc. On that kind of resume, the native surface gets reattached at its actual current size, but the `Scene`'s explicit width/height from the constructor doesn't get re-synced to it, so the app renders at the stale cached dimensions instead of filling the (possibly different) real window. Fully killing the app (swipe away from recents, not just backgrounding it) and relaunching forces `start()` to run again and construct a brand new `Scene`, which is why that "fixes" it temporarily.

The fix: never give the `Scene` itself an explicit width/height on a build that targets Android — always construct it as `new Scene(root)` and let it defer to the native window's actual bounds every time, cold start or resume alike. If you want a specific window size for desktop development (`mvn gluonfx:run`), set it on the root node instead — `root.setPrefSize(360, 640)` — since that's just an initial layout hint for the root `Region`, not a cached size the `Scene` treats as authoritative; the `Scene` still resizes the root to fill whatever space it actually has, on every platform. `examples/hello-mobile` does this from the start; if you're adapting a project of your own, look for `new Scene(root, <width>, <height>)` and drop the width/height arguments.

This diagnosis is based on the code and on how JavaFX's Android backend handles resize-on-resume — it wasn't reproduced against a physical device in the writing of this tutorial (see §5's note on why an emulator can't substitute for one here). If you hit this and the fix above doesn't fully resolve it, please compare notes against your specific device/Android version rather than assuming this is the complete story.
