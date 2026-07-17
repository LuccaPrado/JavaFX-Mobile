# hello-mobile

The minimal example referenced by [`docs/tutorial-android.md`](../../docs/tutorial-android.md). A single JavaFX screen with a label and a button — no FXML, no Gluon Mobile runtime dependency, just `javafx.application.Application` and the free `gluonfx-maven-plugin` build tool.

## Run on desktop

```bash
mvn gluonfx:run
```

## Build and run on a connected Android device

```bash
mvn clean
mvn -Pandroid gluonfx:build gluonfx:package
mvn -Pandroid gluonfx:install
mvn -Pandroid gluonfx:nativerun
```

See [`docs/tutorial-android.md`](../../docs/tutorial-android.md) for prerequisites, an explanation of every POM section, the 16 KB page-size requirement, and how to produce a signed release bundle for the Play Store.
