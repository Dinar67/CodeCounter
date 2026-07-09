package Interfaces;

import Classes.AnalysisResultData;
import Classes.FileAnalysisData;

public interface IAnalyzeOutputStrategy {

    default void onFileAnalyzed(FileAnalysisData fileData) {}
    String finalizeOutput(AnalysisResultData aggregate);
}