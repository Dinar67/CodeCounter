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
        SettingsService settingsService = SERVICE_MANAGER.setService(SettingsService.class, new SettingsService());
        ExcelAnalysisExporter excelAnalysisExporter = SERVICE_MANAGER.setService(ExcelAnalysisExporter.class, new ExcelAnalysisExporter());
        GithubService githubService = SERVICE_MANAGER.setService(GithubService.class, new GithubService());
        navService.nextScene(MainPageController.class);

        primaryStage.show();
    }
}
