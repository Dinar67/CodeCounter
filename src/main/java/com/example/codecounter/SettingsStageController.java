package com.example.codecounter;

import Classes.CompactAnalyzeOutputStrategy;
import Classes.DetailedAnalyzeOutputStrategy;
import Services.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;

public class SettingsStageController {
    @FXML protected RadioButton fullRb;
    @FXML protected RadioButton shortRb;

    private SettingsService settingsService;

    @FXML
    protected void initialize(){
        settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
        fullRb.fire();

        fullRb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) settingsService.setOutputStrategy(new DetailedAnalyzeOutputStrategy());
        });
        shortRb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) settingsService.setOutputStrategy(new CompactAnalyzeOutputStrategy());
        });
    }


}
