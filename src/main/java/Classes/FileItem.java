package Classes;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.util.function.Consumer;

public class FileItem {
    private final File file;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public FileItem(File file, boolean value) {
        this.file = file;
        selected.set(value);
    }

    public File getFile() { return file; }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
}
