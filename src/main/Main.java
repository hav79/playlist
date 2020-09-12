package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("main.fxml"));
        AnchorPane root = loader.load();
        primaryStage.setTitle("Playlist creator");
        primaryStage.setScene(new Scene(root, 800, 600));

        primaryStage.setOnCloseRequest(event -> {
            Controller controller = loader.getController();
            controller.saveHistory();
        });
        primaryStage.show();
    }
}
