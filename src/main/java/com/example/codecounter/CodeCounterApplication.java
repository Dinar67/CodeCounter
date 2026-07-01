package com.example.codecounter;

import Services.*;
import javafx.application.Application;
import javafx.stage.Stage;

public class CodeCounterApplication extends Application {
    public static ServiceManager SERVICE_MANAGER = new ServiceManager();

    @Override
    public void start(Stage primaryStage) throws Exception {
        NavigationService navService = SERVICE_MANAGER.setService(NavigationService.class, new NavigationService(primaryStage));
        FileManager fileManager = SERVICE_MANAGER.setService(FileManager.class, new FileManager(primaryStage));
        ExcelExproterAllLexems excelExproterAllLexems = SERVICE_MANAGER.setService(ExcelExproterAllLexems.class, new ExcelExproterAllLexems());
        SettingsService settingsService = SERVICE_MANAGER.setService(SettingsService.class, new SettingsService());
        navService.nextScene(MainPageController.class);

        primaryStage.show();
    }
}
