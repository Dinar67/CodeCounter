package Classes;

import java.io.File;

public class FileItem {
    private final File file;
    private boolean selected = true;

    public FileItem(File file, boolean selected) {
        this.file = file;
        this.selected = selected;
    }

    public File getFile() {
        return file;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}