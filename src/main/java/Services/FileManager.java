package Services;

import Interfaces.IService;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;

public class FileManager implements IService {
    public Stage _mainStage;

    public FileManager(Stage stage){
        _mainStage = stage;
    }

    public File chooseDirectory(){
        var chooser = new DirectoryChooser();
        var dir = chooser.showDialog(_mainStage);

        return dir;
    }

    public List<File> selectFilesFromDir(String extensionFormat, File directory) throws InvalidPathException {
        checkFolder(directory);

        var list = new ArrayList<File>();
        selectFiles(directory, list);

        list.removeIf(file -> !file.getName().endsWith("." + extensionFormat));
        return list;
    }

    public File saveFile(String fileName){
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Сохранить анализ в Excel");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Excel файлы (*.xlsx)", "*.xlsx")
        );
        fileChooser.setInitialFileName(fileName);

        String homeDir = System.getProperty("user.home");
        File initialDir = new File(homeDir + "/Desktop");
        if (initialDir.exists()) { fileChooser.setInitialDirectory(initialDir); }

        var file = fileChooser.showSaveDialog(_mainStage);
        return file;
    }

    private void selectFiles(File directory, List<File> files) throws InvalidPathException {
        checkFolder(directory);

        var tempfiles = toList(directory.listFiles());
        for (File file : tempfiles){
            if(file.isFile()) files.add(file);
            else if (file.isDirectory()) selectFiles(file, files);
        }
    }

    private <T> List<T> toList(T[] array){
        List<T> list = new ArrayList<>();
        for(T a : array) list.add(a);
        return list;
    }

    private void checkFolder(File directory) throws InvalidPathException{
        if(!directory.isDirectory()) throw new InvalidPathException("Неверный путь", "Вы выбрали не папку!");
    }
}
