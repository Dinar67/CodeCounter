package Classes;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import java.util.function.Consumer;

public class FileItemCell extends ListCell<FileItem> {

    private final Consumer<FileItem> onSelected;
    private final HBox hbox;
    private final CheckBox selectCb;
    private final Label pathLb;
    private FileItem currentItem; // Локальная копия текущего элемента

    public FileItemCell(Consumer<FileItem> select){
        this.onSelected = select;
        this.hbox = new HBox();

        this.selectCb = new CheckBox();
        this.pathLb = new Label();

        this.hbox.getChildren().addAll(selectCb, pathLb);

        HBox.setMargin(selectCb, new Insets(0, 10, 0, 0));
        HBox.setMargin(pathLb, new Insets(0, 20, 0, 0));

        // Слушатель реагирует ТОЛЬКО на клики пользователя
        selectCb.setOnAction(event -> {
            if (currentItem != null) {
                currentItem.setSelected(selectCb.isSelected());
                onSelected.accept(currentItem);
            }
        });
    }

    @Override
    protected void updateItem(FileItem item, boolean empty) {
        super.updateItem(item, empty); // ОБЯЗАТЕЛЬНО вызываем super! Без него скролл будет ломаться.

        if (empty || item == null) {
            currentItem = null;
            setGraphic(null);
        } else {
            currentItem = item;
            selectCb.setSelected(item.isSelected());
            pathLb.setText(item.getFile().getName());
            setGraphic(hbox);
        }
    }
}
