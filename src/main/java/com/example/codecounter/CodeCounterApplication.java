package com.example.codecounter;

import Services.*;
import javafx.application.Application;
import javafx.stage.Stage;

public class CodeCounterApplication extends Application {
    public static ServiceManager SERVICE_MANAGER = new ServiceManager();

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        NavigationService navService = SERVICE_MANAGER.setService(NavigationService.class, new NavigationService(primaryStage));
        FileManager fileManager = SERVICE_MANAGER.setService(FileManager.class, new FileManager(primaryStage));
        ExcelAnalysisExporter excelExproterAllLexems = SERVICE_MANAGER.setService(ExcelAnalysisExporter.class, new ExcelAnalysisExporter());
        SettingsService settingsService = SERVICE_MANAGER.setService(SettingsService.class, new SettingsService());
        ExcelAnalysisExporter excelAnalysisExporter = SERVICE_MANAGER.setService(ExcelAnalysisExporter.class, new ExcelAnalysisExporter());
        navService.nextScene(MainPageController.class);

        primaryStage.show();
    }
}
