package com.example.codecounter;

import Services.ExcelExproter;
import Services.FileManager;
import Services.NavigationService;
import Services.ServiceManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class CodeCounterApplication extends Application {
    public static ServiceManager SERVICE_MANAGER = new ServiceManager();

    @Override
    public void start(Stage primaryStage) throws Exception {
        NavigationService navService = SERVICE_MANAGER.setService(NavigationService.class, new NavigationService(primaryStage));
        FileManager fileManager = SERVICE_MANAGER.setService(FileManager.class, new FileManager(primaryStage));
        ExcelExproter excelExproter = SERVICE_MANAGER.setService(ExcelExproter.class, new ExcelExproter());
        navService.nextScene(MainPageController.class);

        primaryStage.show();
    }
}
