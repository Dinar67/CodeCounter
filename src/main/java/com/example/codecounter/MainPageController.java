package com.example.codecounter;

import Classes.DetailedAnalyzeOutputStrategy;
import Interfaces.IAnalyzeOutputStrategy;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import Services.ExcelExproterAllLexems;
import Services.FileManager;
import Services.NavigationService;
import Services.SettingsService;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MainPageController {

    @FXML protected Label resultLb;
    @FXML  protected ListView<FileItem> fileListView;
    @FXML protected TextArea outputArea;
    @FXML protected Button analyzeBtn;
    @FXML protected Button settingsBtn;
    @FXML protected Button exportBtn;
    @FXML protected Button exportSortedBtn;
    @FXML protected Button selectAllBtn;
    @FXML protected Button deselectAllBtn;

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private Map<File, List<Token>> fileTokensMap = new HashMap<>();
    private Map<File, String> fileCodeMap = new HashMap<>();

    @FXML
    protected void initialize(){
        var image = new Image(getClass().getResourceAsStream("/images/settings_icon.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        imageView.setPreserveRatio(true);
        settingsBtn.setGraphic(imageView);
    }

    @FXML
    protected void openSettings() throws IOException {
        var navService = CodeCounterApplication.SERVICE_MANAGER.getService(NavigationService.class);
        navService.openModalWindow(SettingsStageController.class);
    }

    @FXML
    protected void chooseDir() throws IOException {
        FileManager fileManager = CodeCounterApplication.SERVICE_MANAGER.getService(FileManager.class);
        var dir = fileManager.chooseDirectory();

        if (dir == null) {
            outputArea.setText("Папка не выбрана!");
            return;
        }

        //Получаем все Java файлы из папки
        List<File> javaFiles = fileManager.selectFilesFromDir("java", dir);

        if (javaFiles.isEmpty()) {
            outputArea.setText("В выбранной папке не найдено Java файлов!");
            fileItems.clear();
            return;
        }

        //Создаем список с галочками
        fileItems.clear();
        for (File file : javaFiles) {
            fileItems.add(new FileItem(file, true)); // По умолчанию все выбраны
        }

        //Настраиваем ListView с CheckBox
        setupFileListView();

        outputArea.setText("Найдено файлов: " + fileItems.size() +
                "\nВыберите файлы для анализа и нажмите 'Анализировать выбранные файлы'");
        resultLb.setText("Найдено файлов: " + fileItems.size() + " (выбрано: " + getSelectedFiles().size() + ")");

        //Сбрасываем результаты анализа
        fileTokensMap.clear();
        fileCodeMap.clear();
        exportBtn.setDisable(true);
        exportSortedBtn.setDisable(true);
    }

    private void setupFileListView() {
        fileListView.setItems(fileItems);
        fileListView.setCellFactory(new Callback<ListView<FileItem>, ListCell<FileItem>>() {
            @Override
            public ListCell<FileItem> call(ListView<FileItem> listView) {
                return new CheckBoxListCell<>(new Callback<FileItem, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(FileItem item) {
                        return item.selectedProperty();
                    }
                }) {
                    @Override
                    public void updateItem(FileItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getFile().getName());
                        }
                    }
                };
            }
        });
    }

    @FXML
    protected void selectAllFiles() {
        for (FileItem item : fileItems) {
            item.setSelected(true);
        }
        updateResultLabel();
    }

    @FXML
    protected void deselectAllFiles() {
        for (FileItem item : fileItems) {
            item.setSelected(false);
        }
        updateResultLabel();
    }

    private List<File> getSelectedFiles() {
        List<File> selected = new ArrayList<>();
        for (FileItem item : fileItems) {
            if (item.isSelected()) {
                selected.add(item.getFile());
            }
        }
        return selected;
    }

    private void updateResultLabel() {
        int total = fileItems.size();
        int selected = getSelectedFiles().size();
        resultLb.setText("Найдено файлов: " + total + " (выбрано: " + selected + ")");
    }

    @FXML
    protected void analyzeFiles() throws IOException {
        List<File> selectedFiles = getSelectedFiles();
        var settings = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
        if (selectedFiles.isEmpty()) {
            outputArea.setText("Выберите хотя бы один файл для анализа!");
            return;
        }

        fileTokensMap.clear();
        fileCodeMap.clear();

        RegexLexer lexer = new RegexLexer(new JavaLanguage());

        for (File file : selectedFiles) {
            String code = Files.readString(file.toPath());
            fileCodeMap.put(file, code);
            fileTokensMap.put(file, lexer.tokenize(code));
        }


        outputArea.setText(settings.getOutputStrategy().makeAnalyzeOutput(new HashMap<>(fileTokensMap)));

        exportBtn.setDisable(false);
        exportSortedBtn.setDisable(false);
    }




    @FXML
    protected void exportToExcel() {
        ExcelExproterAllLexems excelExproterAllLexems = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproterAllLexems.class);

        try {
            var file = saveFile("tokens_analysis.xlsx");
            if (file != null) {
                excelExproterAllLexems.exportWorkbook(file.getAbsolutePath(), false, fileTokensMap);
                outputArea.appendText("\n\nФайл экспортирован в:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(); // полный стектрейс в консоль
        }
    }
    @FXML
    protected void exportSortedToExcel() {
        ExcelExproterAllLexems excelExproterAllLexems = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproterAllLexems.class);

        try {
            var file = saveFile("sorted_tokens_analysis.xlsx");
            if (file != null) {
                excelExproterAllLexems.exportWorkbook(file.getAbsolutePath(), true, fileTokensMap);
                outputArea.appendText("\n\nФайл экспортирован (по типам) в:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    private File saveFile(String fileName){
        if (fileTokensMap.isEmpty()){
            outputArea.setText("Сначала выполните анализ файлов!");
            return null;
        }

        FileManager fileManager = CodeCounterApplication.SERVICE_MANAGER.getService(FileManager.class);

        if(fileManager == null) {
            outputArea.setText("Сервис \"Файловый менеджер\" не зарегистрирован!");
            return null;
        }

        return fileManager.saveFile(fileName);
    }




    // Вспомогательный класс для хранения файла с состоянием выбора
    public static class FileItem {
        private final File file;
        private final javafx.beans.property.BooleanProperty selected;

        public FileItem(File file, boolean selected) {
            this.file = file;
            this.selected = new javafx.beans.property.SimpleBooleanProperty(selected);
        }

        public File getFile() {
            return file;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        public javafx.beans.property.BooleanProperty selectedProperty() {
            return selected;
        }
    }

    // Вспомогательный класс для хранения токена с именем файла
    private static class TokenWithFile {
        String fileName;
        Token token;

        TokenWithFile(String fileName, Token token) {
            this.fileName = fileName;
            this.token = token;
        }
    }
}
