package Services;

import Classes.DetailedAnalyzeOutputStrategy;
import Interfaces.IAnalyzeOutputStrategy;
import Interfaces.IService;

public class SettingsService implements IService {
    IAnalyzeOutputStrategy outputStrategy = new DetailedAnalyzeOutputStrategy();

    public SettingsService(){
        loadSettings();
    }

    private void loadSettings() { }

    private void saveSettings() { }

    public IAnalyzeOutputStrategy getOutputStrategy() { return outputStrategy; }
    public IAnalyzeOutputStrategy setOutputStrategy(IAnalyzeOutputStrategy strategy) { outputStrategy = strategy; return outputStrategy; }
}
