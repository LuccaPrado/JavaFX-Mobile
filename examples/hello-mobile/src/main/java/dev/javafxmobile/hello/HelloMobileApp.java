package dev.javafxmobile.hello;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Plain javafx.application.Application - no Gluon Mobile runtime (no Attach APIs,
 * no Charm/Glisten) anywhere in this class. See ../../../../../../docs/licensing.md
 * for why that matters: it's what keeps this app free of any Gluon license check.
 */
public class HelloMobileApp extends Application {

    private int tapCount = 0;

    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, mobile JavaFX!");
        Button button = new Button("Tap me");
        button.setOnAction(e -> {
            tapCount++;
            label.setText("Tapped " + tapCount + " time" + (tapCount == 1 ? "" : "s"));
        });

        VBox root = new VBox(16, label, button);
        root.setAlignment(Pos.CENTER);
        // Preferred size only, for a sane desktop dev window (mvn gluonfx:run). Never
        // give the Scene itself an explicit width/height here - see docs/tutorial-android.md
        // FAQ "App doesn't fill the screen after the first launch" for why that breaks
        // resume-without-recreate on Android.
        root.setPrefSize(360, 640);

        Scene scene = new Scene(root);
        stage.setTitle("HelloMobile");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
