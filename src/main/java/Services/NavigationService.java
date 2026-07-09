package Services;

import Interfaces.IService;
import com.example.codecounter.CodeCounterApplication;
import com.example.codecounter.MainPageController;
import com.example.codecounter.SettingsStageController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;

public class NavigationService implements IService {
    private Stage _mainStage;
    private Scene _currentScene;
    private Stage _currentStage;
    private HashMap<Class, String> _pageResource = new HashMap<>(){{
       put(MainPageController.class, "MainPage.fxml");
       put(SettingsStageController.class, "SettingsStage.fxml");
    }};

    public NavigationService(Stage stage){
        _mainStage = stage;
    }

    public void nextScene(Class<?> pageController) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CodeCounterApplication.class.getResource(_pageResource.get(pageController)));
        Scene scene = new Scene(fxmlLoader.load());
        _mainStage.setScene(scene);
        _currentScene = scene;
    }

    public void openModalWindow(Class<?> pageController) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CodeCounterApplication.class.getResource(_pageResource.get(pageController)));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(_mainStage);
        _currentStage = stage;

        stage.showAndWait();
    }

    public Scene currentScene() { return _currentScene; }
    public Stage currentStage() { return _currentStage; }
}
