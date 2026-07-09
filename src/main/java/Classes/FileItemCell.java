package Classes;

import com.example.codecounter.CodeCounterApplication;
import com.example.codecounter.FileAnalysisStageController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FileItemCell extends ListCell<FileItem> {

    private final Consumer<FileItem> onSelected;
    private final HBox hbox;
    private final CheckBox selectCb;
    private final Label pathLb;
    private final Button analysisBtn;

    // Общие на все ячейки одного ListView - передаются извне (из MainPageController),
    // чтобы разные ячейки (которые JavaFX постоянно переиспользует под разные FileItem
    // при виртуализации списка) могли обмениваться состоянием между собой
    private final boolean[] suppressPropagation; // защита от зацикливания при массовом обновлении
    private final int[] anchorIndex;              // опорный индекс для shift-выделения

    private FileItem currentItem;

    public FileItemCell(Consumer<FileItem> select, boolean[] suppressPropagation, int[] anchorIndex) {
        this.onSelected = select;
        this.suppressPropagation = suppressPropagation;
        this.anchorIndex = anchorIndex;

        this.hbox = new HBox();
        this.selectCb = new CheckBox();
        this.pathLb = new Label();
        this.analysisBtn = new Button();

        this.hbox.getChildren().addAll(selectCb, pathLb, analysisBtn);

        HBox.setMargin(selectCb, new Insets(0, 10, 0, 0));
        HBox.setMargin(pathLb, new Insets(0, 10, 0, 0));

        pathLb.setPrefWidth(180);
        hbox.setAlignment(Pos.CENTER);

        selectCb.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (currentItem == null || oldValue == newValue) return;
            propagateIfNeeded(newValue);
            onSelected.accept(currentItem);
        });

        // Клик по названию файла управляет подсветкой строки (выделением), а не чекбоксом
        pathLb.setOnMouseClicked(this::handleRowClick);

        setupBtn();
    }

    // Если чекбокс переключили у файла, входящего в текущее многострочное выделение
    // (подсвеченное через shift/ctrl клик по названию) - применяем то же значение
    // ко всем файлам этого выделения, а не только к одному
    private void propagateIfNeeded(boolean newValue) {
        if (suppressPropagation[0]) return; // мы сами сейчас внутри такого массового обновления - не зацикливаемся

        ListView<FileItem> lv = getListView();
        if (lv == null) return;

        List<FileItem> highlighted = new ArrayList<>(lv.getSelectionModel().getSelectedItems());
        if (highlighted.size() <= 1 || !highlighted.contains(currentItem)) return;

        suppressPropagation[0] = true;
        try {
            for (FileItem item : highlighted) {
                if (item != currentItem) item.setSelected(newValue);
            }
        } finally {
            suppressPropagation[0] = false;
        }
    }

    private void handleRowClick(MouseEvent event) {
        int index = getIndex();
        if (index < 0) return;

        ListView<FileItem> lv = getListView();
        if (lv == null) return;

        if (event.isShiftDown() && anchorIndex[0] >= 0) {
            int start = Math.min(anchorIndex[0], index);
            int end = Math.max(anchorIndex[0], index);
            lv.getSelectionModel().clearSelection();
            for (int i = start; i <= end; i++) {
                lv.getSelectionModel().select(i);
            }
        } else if (event.isControlDown()) {
            if (lv.getSelectionModel().isSelected(index)) {
                lv.getSelectionModel().clearSelection(index);
            } else {
                lv.getSelectionModel().select(index);
            }
            anchorIndex[0] = index;
        } else {
            lv.getSelectionModel().clearSelection();
            lv.getSelectionModel().select(index);
            anchorIndex[0] = index;
        }
    }

    private void setupBtn() {
        var image = new Image(CodeCounterApplication.class.getResourceAsStream("/images/analysis_icon.png"));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);
        analysisBtn.setGraphic(imageView);
        analysisBtn.setOnAction(e -> {
            try {
                analysisClick();
            }
            catch (Exception ex){
                System.out.println(ex.getMessage());
            }
        });
    }

    @Override
    protected void updateItem(FileItem item, boolean empty) {
        super.updateItem(item, empty);

        if (currentItem != null) {
            selectCb.selectedProperty().unbindBidirectional(currentItem.selectedProperty());
        }

        if (empty || item == null) {
            currentItem = null;
            setGraphic(null);
            pathLb.setText(null);
        } else {
            currentItem = item;
            selectCb.selectedProperty().bindBidirectional(item.selectedProperty());
            pathLb.setText(item.getFile().getName());
            setGraphic(hbox);
        }
    }

    private void analysisClick() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CodeCounterApplication.class.getResource("FileAnalysisStage.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        Stage stage = new Stage();
        FileAnalysisStageController controller = fxmlLoader.getController();
        controller.transferData(currentItem.getFile());
        stage.setScene(scene);
        stage.show();
    }
}