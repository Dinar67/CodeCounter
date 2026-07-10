package Classes;

import java.io.File;
import java.nio.file.Files;
import Interfaces.ICodeSource;

public class LocalFileSource implements ICodeSource {
    private final File file;

    public LocalFileSource(File file) { this.file = file; }

    public File getFile() { return file; }

    @Override public String getDisplayName() { return file.getName(); }
    @Override public String readText() throws Exception { return Files.readString(file.toPath()); }
}