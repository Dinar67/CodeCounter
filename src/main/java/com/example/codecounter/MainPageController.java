package com.example.codecounter;

import Classes.*;
import Interfaces.IAnalyzeOutputStrategy;
import Interfaces.ICodeSource;
import LanguageLexer.LanguageToken.Token;
import LanguageLexer.LanguageToken.TokenType;
import LanguageLexer.Languages.JavaLanguage.JavaLanguage;
import LanguageLexer.Lexer.RegexLexer;
import Services.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainPageController {

    @FXML protected Label resultLb;
    @FXML protected ListView<FileItem> fileListView;
    @FXML protected TextArea outputArea;
    @FXML protected Button analyzeBtn;
    @FXML protected Button exportBtn;
    @FXML protected Button selectAllBtn;
    @FXML protected Button deselectAllBtn;
    @FXML protected Button settingsBtn;
    @FXML protected ProgressBar analysisProgressBar;
    @FXML protected Label progressLb;
    @FXML protected javafx.scene.control.TextField searchField;
    @FXML protected Button searchBtn;
    @FXML protected javafx.scene.control.TextField githubUrlField;
    @FXML protected Button githubLoadBtn;

    private javafx.collections.transformation.FilteredList<FileItem> filteredFileItems;
    private final boolean[] selectionSuppressFlag = new boolean[]{false};
    private final int[] selectionAnchorIndex = new int[]{-1};

    private ObservableList<FileItem> fileItems = FXCollections.observableArrayList();

    // Счётчик завершённых файлов - общий для всех потоков пула
    private final AtomicInteger completedCount = new AtomicInteger(0);

    // Раз в сколько файлов обновляем UI (пересчитывается под конкретный total)
    private volatile int progressUpdateStep = 1;

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
        searchField.clear();

        List<File> javaFiles = fileManager.selectFilesFromDir("java", dir);
        fileItems.clear();

        if (javaFiles.isEmpty()) throw new Exception("В выбранной папке не найдено Java файлов!");

        for (File file : javaFiles) fileItems.add(new FileItem(new LocalFileSource(file), true));

        setupFileListView();

        outputArea.setText("Найдено файлов: " + fileItems.size() +
                "\nВыберите файлы для анализа и нажмите 'Анализировать выбранные файлы'");
        resultLb.setText("Найдено файлов: " + fileItems.size() + " (выбрано: " + getSelectedSource().size() + ")");

        exportBtn.setDisable(true);
    }

    private void setupFileListView() {
        filteredFileItems = new javafx.collections.transformation.FilteredList<>(fileItems, item -> true);
        fileListView.setItems(filteredFileItems);
        fileListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        selectionAnchorIndex[0] = -1;
        fileListView.setCellFactory(param -> new FileItemCell(this::selectFile, selectionSuppressFlag, selectionAnchorIndex));
    }
    @FXML
    protected void searchFiles() {
        if (filteredFileItems == null) return;

        // Меняем состав видимых строк - старая опорная точка и подсветка больше не валидны
        selectionAnchorIndex[0] = -1;
        fileListView.getSelectionModel().clearSelection();

        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            filteredFileItems.setPredicate(item -> true);
        } else {
            String lower = query.toLowerCase();
            filteredFileItems.setPredicate(item -> item.getSource().getDisplayName().toLowerCase().contains(lower));
        }
    }

    @FXML
    protected void openSettings() throws IOException {
        var navManager = CodeCounterApplication.SERVICE_MANAGER.getService(NavigationService.class);
        if(navManager != null) navManager.openModalWindow(SettingsStageController.class);
    }

    private void selectFile(FileItem item){ updateResultLabel(); }

    @FXML
    protected void selectAllFiles() {
        for (FileItem item : fileItems) item.setSelected(true);
        updateResultLabel();
    }

    @FXML
    protected void deselectAllFiles() {
        for (FileItem item : fileItems) item.setSelected(false);
        updateResultLabel();
    }

    private List<ICodeSource> getSelectedSource() {
        List<ICodeSource> selected = new ArrayList<>();
        for (FileItem item : fileItems)
            if (item.isSelected()) selected.add(item.getSource());
        return selected;
    }

    private void updateResultLabel() {
        int total = fileItems.size();
        int selected = getSelectedSource().size();
        resultLb.setText("Найдено файлов: " + total + " (выбрано: " + selected + ")");
    }

    // ==== Запуск анализа ====

    @FXML
    protected void analyzeFiles() {
        try {
            var selected = getSelectedSource();
            if (selected.isEmpty()) throw new Exception("Выберите хотя бы один файл для анализа!");
            var settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
            if (settingsService == null) throw new Exception("Сервис настроек отсутствует!");
            var settings = settingsService.getSettings(selected.size());

            IAnalyzeOutputStrategy strategy = settings.showFileDetails
                    ? new DetailedAnalyzeOutputStrategy(settings)
                    : new CompactAnalyzeOutputStrategy(settings);

            startAnalysis(selected, settings, strategy);
        } catch (Exception e) {
            outputArea.setText(e.getMessage());
        }
    }

    // Готовит UI и запускает фоновый поток - сам метод выполняется в FX-потоке,
    // но быстро (только подготовка), поэтому UI не блокируется
    private void startAnalysis(List<ICodeSource> selected, AnalysisSettings settings, IAnalyzeOutputStrategy strategy) {
        int total = selected.size();
        completedCount.set(0);
        // Ограничиваем количество обновлений UI примерно 200 штуками за весь анализ,
        // вне зависимости от total - иначе при 9124 файлах Platform.runLater будет
        // вызываться слишком часто и сам станет узким местом
        progressUpdateStep = Math.max(1, total / 200);

        analyzeBtn.setDisable(true);
        exportBtn.setDisable(true);
        analysisProgressBar.setVisible(true);
        analysisProgressBar.setProgress(0);
        progressLb.setVisible(true);
        progressLb.setText("Проанализировано 0 из " + total + " файлов");
        outputArea.setText("Анализ запущен, подождите...");

        Thread worker = new Thread(() -> {
            try {
                var result = analyzeHandle(selected, settings, strategy, total);
                String output = strategy.finalizeOutput(result);
                Platform.runLater(() -> onAnalysisFinished(output, null, total));
            } catch (Exception e) {
                Platform.runLater(() -> onAnalysisFinished(null, e, total));
            }
        }, "analysis-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // Вызывается уже в FX-потоке через Platform.runLater
    private void onAnalysisFinished(String output, Exception error, int total) {
        analyzeBtn.setDisable(false);
        analysisProgressBar.setVisible(false);

        if (error != null) {
            outputArea.setText("Ошибка анализа: " + error.getMessage());
            progressLb.setText("Анализ прерван из-за ошибки");
            return;
        }

        outputArea.setText(output);
        progressLb.setText("Готово: проанализировано " + total + " из " + total + " файлов");
        exportBtn.setDisable(false);
    }

    // Выполняется в отдельном (не FX) потоке "analysis-worker"
    private AnalysisResultData analyzeHandle(List<ICodeSource> selected, AnalysisSettings settings,
                                             IAnalyzeOutputStrategy strategy, int total) throws Exception {
        var result = new AnalysisResultData();
        result.setSources(selected);
        Set<TokenType> selectedTypes = Set.copyOf(settings.selectedTokenTypes);

        boolean isRemote = !selected.isEmpty() && selected.get(0) instanceof GithubFileSource;
        int threads = isRemote ? 20 : Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (ICodeSource source : selected) {
            futures.add(executor.submit(() -> {
                try {
                    analyzeSingleFile(source, settings, selectedTypes, result, strategy);
                } catch (Exception e) {
                    Platform.runLater(() ->
                            outputArea.appendText("\nОшибка анализа " + source.getDisplayName() + ": " + e.getMessage()));
                } finally {
                    reportProgress(total);
                }
            }));
        }

        executor.shutdown();
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException ignored) {}
        }

        return result;
    }

    // Вызывается из потоков пула executor'а - потокобезопасно за счёт AtomicInteger
    private void reportProgress(int total) {
        int done = completedCount.incrementAndGet();
        // Throttling: обновляем UI не на каждый файл, а раз в progressUpdateStep файлов,
        // плюс обязательно на последнем файле, чтобы прогресс дошёл ровно до конца
        if (done == total || done % progressUpdateStep == 0) {
            Platform.runLater(() -> {
                analysisProgressBar.setProgress((double) done / total);
                progressLb.setText("Проанализировано " + done + " из " + total + " файлов");
            });
        }
    }

    private void analyzeSingleFile(ICodeSource source, AnalysisSettings settings, Set<TokenType> selectedTypes,
                                   AnalysisResultData result, IAnalyzeOutputStrategy strategy) throws Exception {
        var lexer = new RegexLexer(new JavaLanguage());
        var code = source.readText();
        var tokens = lexer.tokenize(code);

        var fileData = new FileAnalysisData(source);
        fileData.lineCount = (int) code.lines().count();
        fileData.nonEmptyLineCount = (int) code.lines().filter(line -> !line.strip().isEmpty()).count();

        for (Token token : tokens) {
            if(settings.mergeBracketВelimiters && checkCloseBracket(token)) continue;
            result.addTokenType(token.getType());
            fileData.tokenTypeCount.merge(token.getType(), 1, Integer::sum);
        }

        for (Token token : tokens) {
            if (!selectedTypes.contains(token.getType()) || (settings.mergeBracketВelimiters && checkCloseBracket(token))) continue;
            result.addLexeme(token.getType(), token.getValue());

            fileData.tokenCount
                    .computeIfAbsent(token.getType(), k -> new HashMap<>())
                    .merge(mergeBracketDelimetrs(token, settings), 1, Integer::sum);
        }

        result.addLines(fileData.lineCount, fileData.nonEmptyLineCount);
        strategy.onFileAnalyzed(fileData);
    }
    private boolean checkCloseBracket(Token token) {
        return token.getValue().equals(")") || token.getValue().equals("]") || token.getValue().equals("}");
    }

    private String mergeBracketDelimetrs(Token token, AnalysisSettings settings){
        HashMap<String, String> brackets = new HashMap<>() {{
            put("(", ")");
            put("[", "]");
            put("{", "}");
        }};

        if(settings.mergeBracketВelimiters && brackets.containsKey(token.getValue())){
            var part = brackets.get(token.getValue());
            return token.getValue() + part;
        }
        return token.getValue();
    }

    @FXML
    protected void loadFromGithub() {
        String url = githubUrlField.getText();
        if (url == null || url.isBlank()) {
            outputArea.setText("Введите ссылку на GitHub-репозиторий!");
            return;
        }

        var githubService = CodeCounterApplication.SERVICE_MANAGER.getService(GithubService.class);
        if (githubService == null) { outputArea.setText("Сервис GitHub отсутствует!"); return; }

        githubLoadBtn.setDisable(true);
        searchField.clear();
        outputArea.setText("Получение списка файлов из репозитория...");

        // Списковые запросы к API быстрые (2 запроса), но всё равно сеть - лучше не блокировать FX-поток
        Thread worker = new Thread(() -> {
            try {
                var ref = githubService.parseUrl(url);
                String branch = githubService.fetchDefaultBranch(ref);
                var remoteFiles = githubService.listJavaFiles(ref, branch);

                List<FileItem> items = new ArrayList<>();
                for (var rf : remoteFiles) {
                    var source = new GithubFileSource(ref.owner(), ref.repo(), branch, rf.path(), githubService);
                    items.add(new FileItem(source, true));
                }

                Platform.runLater(() -> onGithubFilesLoaded(items, ref, branch));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    githubLoadBtn.setDisable(false);
                    outputArea.setText("Ошибка: " + e.getMessage());
                });
            }
        }, "github-list-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private void onGithubFilesLoaded(List<FileItem> items, GithubService.RepoRef ref, String branch) {
        githubLoadBtn.setDisable(false);
        fileItems.setAll(items);

        if (items.isEmpty()) {
            outputArea.setText("В репозитории " + ref.owner() + "/" + ref.repo() + " не найдено .java файлов");
            return;
        }

        setupFileListView();
        outputArea.setText("Найдено файлов: " + items.size()
                + " (репозиторий " + ref.owner() + "/" + ref.repo() + ", ветка " + branch + ")"
                + "\nВыберите файлы для анализа и нажмите 'Анализировать выбранные файлы'");
        resultLb.setText("Найдено файлов: " + items.size() + " (выбрано: " + getSelectedSource().size() + ")");
        exportBtn.setDisable(true);
    }

    @FXML
    protected void exportToExcel() { runExport(); }

    private void runExport() {
        try {
            var selected = getSelectedSource();
            if (selected.isEmpty()) throw new Exception("Нет выбранных файлов для экспорта!");

            var settingsService = CodeCounterApplication.SERVICE_MANAGER.getService(SettingsService.class);
            if (settingsService == null) throw new Exception("Сервис настроек отсутствует!");
            var settings = settingsService.getSettings(selected.size());

            var excelExporter = CodeCounterApplication.SERVICE_MANAGER.getService(ExcelAnalysisExporter.class);
            if (excelExporter == null) throw new Exception("Сервис экспорта отсутствует!");

            File target = saveFile("tokens_analysis.xlsx");
            if (target == null) return; // пользователь отменил диалог сохранения

            startExport(selected, settings, target, excelExporter);
        } catch (Exception e) {
            outputArea.appendText("\n\nОШИБКА при экспорте:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void startExport(List<ICodeSource> selected, AnalysisSettings settings, File target,
                             ExcelAnalysisExporter exporter) {
        int total = selected.size();
        int step = Math.max(1, total / 200);

        analyzeBtn.setDisable(true);
        exportBtn.setDisable(true);
        analysisProgressBar.setVisible(true);
        analysisProgressBar.setProgress(0);
        progressLb.setVisible(true);
        progressLb.setText("Экспортировано 0 из " + total + " файлов");

        exporter.exportAsync(selected, settings, target,
                done -> {
                    if (done == total || done % step == 0) {
                        Platform.runLater(() -> {
                            analysisProgressBar.setProgress((double) done / total);
                            progressLb.setText("Экспортировано " + done + " из " + total + " файлов");
                        });
                    }
                },
                error -> Platform.runLater(() -> {
                    analyzeBtn.setDisable(false);
                    exportBtn.setDisable(false);
                    analysisProgressBar.setVisible(false);

                    if (error != null) {
                        outputArea.appendText("\n\nОШИБКА при экспорте:\n" + error.getMessage());
                        progressLb.setText("Экспорт прерван из-за ошибки");
                    } else {
                        outputArea.appendText("\n\nФайл экспортирован в:\n" + target.getAbsolutePath());
                        progressLb.setText("Готово: экспортировано " + total + " файлов");
                    }
                }));
    }

    private File saveFile(String fileName){
        FileManager fileManager = CodeCounterApplication.SERVICE_MANAGER.getService(FileManager.class);
        if(fileManager == null) {
            outputArea.setText("Сервис \"Файловый менеджер\" не зарегистрирован!");
            return null;
        }

        return fileManager.saveFile(fileName);
    }
}