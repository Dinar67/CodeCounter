package Services;

import Interfaces.IService;
import com.example.codecounter.CodeCounterApplication;
import com.example.codecounter.MainPageController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;

public class NavigationService implements IService {
    private Stage _mainStage;
    private HashMap<Class, String> _pageResource = new HashMap<>(){{
       put(MainPageController.class, "MainPage.fxml");
    }};

    public NavigationService(Stage stage){
        _mainStage = stage;
    }

    public void nextScene(Class<?> pageController) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CodeCounterApplication.class.getResource(_pageResource.get(pageController)));
        Scene scene = new Scene(fxmlLoader.load());
        _mainStage.setScene(scene);
    }
}
