package Classes;

import Interfaces.ICodeSource;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class FileItem {
    private final ICodeSource source;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public FileItem(ICodeSource source, boolean value) {
        this.source = source;
        selected.set(value);
    }

    public ICodeSource getSource() { return source; }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
}