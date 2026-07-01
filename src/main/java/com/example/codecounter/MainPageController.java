package com.example.codecounter;

import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import Services.ExcelExproter;
import Services.FileManager;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    @FXML protected Button exportBtn;
    @FXML protected Button exportSortedBtn;
    @FXML protected Button selectAllBtn;
    @FXML protected Button deselectAllBtn;

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private Map<File, List<Token>> fileTokensMap = new HashMap<>();
    private Map<File, String> fileCodeMap = new HashMap<>();

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

        if (selectedFiles.isEmpty()) {
            outputArea.setText("Выберите хотя бы один файл для анализа!");
            return;
        }

        fileTokensMap.clear();
        fileCodeMap.clear();

        RegexLexer lexer = new RegexLexer(new JavaLanguage());
        StringBuilder fullResult = new StringBuilder();

        //Общая статистика по всем файлам
        Map<TokenType, Integer> totalCounter = new HashMap<>();

        fullResult.append("=== АНАЛИЗ ВЫБРАННЫХ ФАЙЛОВ ===\n");
        fullResult.append("Всего файлов: ").append(selectedFiles.size()).append("\n");
        fullResult.append("=".repeat(40)).append("\n\n");

        //Анализируем каждый выбранный файл
        for (File file : selectedFiles) {
            try {
                String code = Files.readString(file.toPath());
                fileCodeMap.put(file, code);

                var tokens = lexer.tokenize(code);
                fileTokensMap.put(file, tokens);

                //Статистика для текущего файла
                Map<TokenType, Integer> fileCounter = new HashMap<>();
                tokens.forEach(token -> {
                    fileCounter.merge(token.getType(), 1, Integer::sum);
                    totalCounter.merge(token.getType(), 1, Integer::sum);
                });

                fullResult.append("========================================\n");
                fullResult.append("ФАЙЛ: ").append(file.getName()).append("\n");
                fullResult.append("----------------------------------------\n");
                fullResult.append("Всего токенов: ").append(tokens.size()).append("\n");
                fullResult.append("Статистика:\n");
                for (TokenType type : TokenType.values()) {
                    if (type != TokenType.WHITESPACE) {
                        int count = fileCounter.getOrDefault(type, 0);
                        if (count > 0) {
                            fullResult.append("  ").append(type).append(": ").append(count).append("\n");
                        }
                    }
                }

                // ВЫВОД ВСЕХ ЛЕКСЕМ
                fullResult.append("----------------------------------------\n");
                fullResult.append("ВСЕ ЛЕКСЕМЫ:\n");
                fullResult.append("----------------------------------------\n");

                int tokenNumber = 1;
                for (Token token : tokens) {
                    if (token.getType() != TokenType.WHITESPACE) {
                        String value = token.getValue()
                                .replace("\n", "\\n")
                                .replace("\r", "\\r");

                        // Обрезаем слишком длинные значения для читаемости
                        if (value.length() > 50) {
                            value = value.substring(0, 47) + "...";
                        }

                        fullResult.append(String.format("%4d. %-12s  %s\n",
                                tokenNumber++,
                                token.getType().toString(),
                                value
                        ));
                    }
                }
                fullResult.append("\n");

            } catch (IOException e) {
                fullResult.append("ОШИБКА при чтении файла ").append(file.getName())
                        .append(": ").append(e.getMessage()).append("\n\n");
            }
        }

        // Общая статистика
        fullResult.append("========================================\n");

        int totalTokens = totalCounter.values().stream().mapToInt(Integer::intValue).sum();

        String generalResult = "";

        for (TokenType type : TokenType.values()) {
            if (type != TokenType.WHITESPACE) {
                int count = totalCounter.getOrDefault(type, 0);
                generalResult += "\t" + type.toString() + ": " + count + "\n";
            }
        }
        fullResult.insert(0, "\n\n");
        fullResult.insert(0, generalResult);
        fullResult.insert(0, "Всего токенов: " + totalTokens + "\n");
        fullResult.insert(0, "=== ОБЩАЯ СТАТИСТИКА ===\n");

        outputArea.setText(fullResult.toString());

        //Активируем кнопки экспорта
        exportBtn.setDisable(false);
        exportSortedBtn.setDisable(false);
    }




    @FXML
    protected void exportToExcel() {
        ExcelExproter excelExproter = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproter.class);

        try {
            var file = saveFile("tokens_analysis.xlsx");
            if (file != null) {
                excelExproter.exportWorkbook(file.getAbsolutePath(), false, fileTokensMap);
                outputArea.appendText("\n\nФайл экспортирован в:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(); // полный стектрейс в консоль
        }
    }
    @FXML
    protected void exportSortedToExcel() {
        ExcelExproter excelExproter = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelExproter.class);

        try {
            var file = saveFile("sorted_tokens_analysis.xlsx");
            if (file != null) {
                excelExproter.exportWorkbook(file.getAbsolutePath(), true, fileTokensMap);
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
