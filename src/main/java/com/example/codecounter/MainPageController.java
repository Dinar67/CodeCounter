package com.example.codecounter;

import Classes.*;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import Services.ExcelExproterAllLexems;
import Services.FileManager;
import Services.NavigationService;
import Services.SettingsService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainPageController {

    @FXML protected Label resultLb;
    @FXML  protected ListView<FileItem> fileListView;
    @FXML protected TextArea outputArea;
    @FXML protected Button analyzeBtn;
    @FXML protected Button exportBtn;
    @FXML protected Button exportSortedBtn;
    @FXML protected Button selectAllBtn;
    @FXML protected Button deselectAllBtn;
    @FXML protected Button settingsBtn;

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private HashMap<TokenType, Integer> typeCount = new HashMap<>();


    @FXML
    protected void initialize(){
        var image = new Image(CodeCounterApplication.class.getResourceAsStream("/images/settings_icon.png"));
        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(20);
        imageView.setFitHeight(20);

        settingsBtn.setGraphic(imageView);
    }


    @FXML
    protected void chooseDir() {
        FileManager fileManager = CodeCounterApplication.SERVICE_MANAGER.getService(FileManager.class);
        try{ choose(fileManager); }
        catch (Exception e){ outputArea.setText(e.getMessage()); }
    }
    private void choose(FileManager fileManager) throws Exception {
        var dir = fileManager.chooseDirectory();
        if (dir == null) throw new Exception("Папка не выбрана!");

        List<File> javaFiles = fileManager.selectFilesFromDir("java", dir);
        fileItems.clear();

        if (javaFiles.isEmpty()) throw new Exception("В выбранной папке не найдено Java файлов!");

        for (File file : javaFiles) fileItems.add(new FileItem(file, true)); // По умолчанию все выбраны

        setupFileListView();

        outputArea.setText("Найдено файлов: " + fileItems.size() +
                "\nВыберите файлы для анализа и нажмите 'Анализировать выбранные файлы'");
        resultLb.setText("Найдено файлов: " + fileItems.size() + " (выбрано: " + getSelectedFiles().size() + ")");

        exportBtn.setDisable(true);
        exportSortedBtn.setDisable(true);
    }


    private void setupFileListView() {
        fileListView.setItems(fileItems);
        fileListView.setCellFactory(param -> new FileItemCell(this::selectChanged));
    }
    private void selectChanged(FileItem fileItem) { }

    @FXML
    protected void openSettings() throws IOException {
        var navManager = CodeCounterApplication.SERVICE_MANAGER.getService(NavigationService.class);
        if(navManager != null) navManager.openModalWindow(SettingsStageController.class);
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
        for (FileItem item : fileItems)
            if (item.isSelected()) selected.add(item.getFile());
        return selected;
    }

    private void updateResultLabel() {
        int total = fileItems.size();
        int selected = getSelectedFiles().size();
        resultLb.setText("Найдено файлов: " + total + " (выбрано: " + selected + ")");
    }



    @FXML
    protected void analyzeFiles() {
        try{
            analyze();
        }
        catch (Exception e){
            outputArea.setText(e.getMessage());
        }
    }

    private void analyze() throws Exception{
        var selected = getSelectedFiles();
        if (selected.isEmpty()) throw  new Exception("Выберите хотя бы один файл для анализа!");
        var settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
        if(settingsService == null) throw new Exception("Сервис настроек отсутствует!");
        var settings = settingsService.getSettings();

        var result = analyzeHandle(selected, settings);

        var outputStrategy = settings.showFileDetails? new DetailedAnalyzeOutputStrategy(settings) : new CompactAnalyzeOutputStrategy(settings);
        //outputArea.setText(outputStrategy.makeAnalyzeOutput());
        primitiveOutput(result);

        exportBtn.setDisable(false);
        exportSortedBtn.setDisable(false);
    }
    private AnalysisResultData analyzeHandle(List<File> selected, AnalysisSettings settings) throws Exception {
        var lexer = new RegexLexer(new JavaLanguage());
        var result = new AnalysisResultData();
        result.setFiles(selected);
        Set<TokenType> selectedTypes = Set.copyOf(settings.selectedTokenTypes);

        for(File file : selected) {
            var code = Files.readString(file.toPath());
            var tokens = lexer.tokenize(code);



            for (Token token : tokens)
                result.getTokenTypeCount().merge(token.getType(), 1, Integer::sum);

            List<Token> allowedTokens = tokens.stream()
                    .filter(token -> selectedTypes.contains(token.getType()))
                    .toList();

            for (Token token : allowedTokens) {
                HashMap<String, Integer> innerMap = result.getTokenCount().computeIfAbsent(
                        token.getType(),
                        k -> new HashMap<String, Integer>()
                );

                innerMap.merge(token.getValue(), 1, Integer::sum);
            }

            result.setLineCount(result.getLineCount() + code.lines().count());
            result.setNonEmptyLineCount(result.getNonEmptyLineCount() + code.lines()
                    .filter(line -> !line.strip().isEmpty())
                    .count());
        }
        return result;
    }
    private void primitiveOutput(AnalysisResultData result){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("кол-во файлов: " + result.getFiles().size() + "\n");
        stringBuilder.append("кол-во строк: " + result.getLineCount() + " (не пустых строк: " + result.getNonEmptyLineCount() + ")\n");

        for (Map.Entry<TokenType, Integer> entry : result.getTokenTypeCount().entrySet()){
            TokenType type = entry.getKey();
            int count = entry.getValue();

            stringBuilder.append(type.toString() + ": " + count + "\n");
        }

        stringBuilder.append("\n");
        stringBuilder.append("Подсчет выбранных лексем:\n");
        for (var entry : result.getTokenCount().entrySet()){
            var type = entry.getKey();
            var tokens = entry.getValue();

            stringBuilder.append(type.toString() + ":\n");
            for(var tokenEntry : tokens.entrySet()){
                var token = tokenEntry.getKey();
                var count = tokenEntry.getValue();

                stringBuilder.append(token + ": " + count + "\n");
            }
            stringBuilder.append("\n");
        }

        outputArea.setText(stringBuilder.toString());
    }




    @FXML
    protected void exportToExcel() {
        ExcelExproterAllLexems excelExproter = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproterAllLexems.class);

        try {
            var file = saveFile("tokens_analysis.xlsx");
            if (file != null) {
                //excelExproter.exportWorkbook(file.getAbsolutePath(), false, fileTokensMap);
                outputArea.appendText("\n\nФайл экспортирован в:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(); // полный стектрейс в консоль
        }
    }
    @FXML
    protected void exportSortedToExcel() {
        ExcelExproterAllLexems excelExproter = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproterAllLexems.class);

        try {
            var file = saveFile("sorted_tokens_analysis.xlsx");
            if (file != null) {
                //excelExproter.exportWorkbook(file.getAbsolutePath(), true, fileTokensMap);
                outputArea.appendText("\n\nФайл экспортирован (по типам) в:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    private File saveFile(String fileName){
//        if (fileTokensMap.isEmpty()){
//            outputArea.setText("Сначала выполните анализ файлов!");
//            return null;
//        }

        FileManager fileManager = CodeCounterApplication.SERVICE_MANAGER.getService(FileManager.class);

        if(fileManager == null) {
            outputArea.setText("Сервис \"Файловый менеджер\" не зарегистрирован!");
            return null;
        }

        return fileManager.saveFile(fileName);
    }
}
