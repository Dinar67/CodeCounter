package Services;

import Classes.AnalysisSettings;
import Interfaces.IService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;

public class SettingsService implements IService {

    private static final String SETTINGS_FILE = "settings.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private AnalysisSettings current;

    public SettingsService() {
        current = load();
    }

    public AnalysisSettings getSettings(int filesCount) {
        return filesCount <= 100
                ? current
                : new AnalysisSettings(false, current.showTokenList, current.selectedTokenTypes);
    }

    public void saveSettings(AnalysisSettings settings) {
        this.current = settings;
        try (Writer writer = new FileWriter(SETTINGS_FILE)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            System.err.println("Не удалось сохранить настройки: " + e.getMessage());
        }
    }

    private AnalysisSettings load() {
        Path path = Path.of(SETTINGS_FILE);
        if (!Files.exists(path)) {
            return new AnalysisSettings(); // дефолтные настройки
        }
        try (Reader reader = new FileReader(SETTINGS_FILE)) {
            AnalysisSettings settings = gson.fromJson(reader, AnalysisSettings.class);
            return settings != null ? settings : new AnalysisSettings();
        } catch (IOException e) {
            System.err.println("Не удалось загрузить настройки: " + e.getMessage());
            return new AnalysisSettings();
        }
    }
}