package com.example.codecounter;

import Classes.AnalysisSettings;
import LanguageLexer.LanguageToken.TokenType;
import Services.NavigationService;
import Services.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

public class SettingsStageController {

    @FXML private VBox tokenTypesBox;
    @FXML private CheckBox showFileDetailsCheck;

    private final ToggleGroup modeGroup = new ToggleGroup();
    private final Map<TokenType, CheckBox> typeCheckBoxes = new LinkedHashMap<>();
    private SettingsService settingsService;

    @FXML
    public void initialize() {
        // Динамически создаём чекбоксы для каждого типа токена
        for (TokenType type : TokenType.values()) {
            if (type == TokenType.WHITESPACE) continue;
            CheckBox cb = new CheckBox(type.toString());
            typeCheckBoxes.put(type, cb);
            tokenTypesBox.getChildren().add(cb);
        }

        settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
        if(settingsService == null) return;
        var settings = settingsService.getSettings(0);
        initSettings(settings.showFileDetails, settings.selectedTokenTypes);
    }

    public void initSettings(boolean showFileDetails, Set<TokenType> selectedTypes) {
        showFileDetailsCheck.setSelected(showFileDetails);

        for (Map.Entry<TokenType, CheckBox> entry : typeCheckBoxes.entrySet()) {
            entry.getValue().setSelected(selectedTypes.contains(entry.getKey()));
        }
    }

    @FXML
    private void onSave() {
        var newSettings = new AnalysisSettings();
        newSettings.showFileDetails = showFileDetailsCheck.isSelected();

        for (Map.Entry<TokenType, CheckBox> entry : typeCheckBoxes.entrySet())
            if (entry.getValue().isSelected()) newSettings.selectedTokenTypes.add(entry.getKey());

        settingsService.saveSettings(newSettings);

        close();
    }

    @FXML
    private void onDefault() {
        showFileDetailsCheck.setSelected(true);

        for (Map.Entry<TokenType, CheckBox> entry : typeCheckBoxes.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == TokenType.KEYWORD);
            entry.getValue().setDisable(false);
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        var navService = CodeCounterApplication.SERVICE_MANAGER.getService(NavigationService.class);
        Stage stage = navService.currentStage();
        if(stage != null) stage.close();
    }
}