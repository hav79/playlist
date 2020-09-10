package main;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.FlacInfoReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.flac.FlacTag;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Controller {

    private static final Path MUSIC = Paths.get("/mnt/Data/Music2").toAbsolutePath().normalize();
    public static final Path PLAYLIST = Paths.get("/mnt/Data/Music2/playlists").toAbsolutePath().normalize();
    public static final String PLAYLIST_NAME = "test.m3u";

//    private Set<Path> selected = new HashSet<>();
    private final List<Path> selected = new ArrayList<>();

    @FXML
    private TreeView<Path> dirTree;

    @FXML
    private TextField playlistName;

    @FXML
    private TextField playlistDir;

//    @FXML
//    private Button musicDirButton;

    @FXML
    private ComboBox<Path> musicDir;

//    @FXML
//    private Button playlistDirButton;

    @FXML
    private void initialize() {
//        displayTreeView(Paths.get(".").toAbsolutePath().normalize().toString());
        displayTreeView(MUSIC);
        dirTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        playlistDir.setText(PLAYLIST.toString());
        playlistName.setText(PLAYLIST_NAME);

        musicDir.getItems().add(PLAYLIST);
        musicDir.getSelectionModel().selectFirst();
        musicDir.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> displayTreeView(newValue));
    }

    @FXML
    private void openMusicDir() {
        DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle("Select music directory");
        dialog.setInitialDirectory(musicDir.getSelectionModel().getSelectedItem().toFile());
        File result = dialog.showDialog(null);
        if (result != null) {
            musicDir.getItems().add(result.toPath());
            musicDir.getSelectionModel().select(result.toPath());
        }
    }

    @FXML
    private void openPlaylistDir() {
        DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle("Select playlist directory");
        dialog.setInitialDirectory(musicDir.getSelectionModel().getSelectedItem().toFile());
        File result = dialog.showDialog(null);
        if (result != null) {
            playlistDir.setText(result.getAbsolutePath());
        }
    }

//    @FXML
//    private void printSelected() {
//        System.out.println("------Selected:");
//        selected.forEach(System.out::println);
////        System.out.println("----------");
////        selected.forEach(
////                (p -> System.out.println(PLAYLIST.relativize(p)))
////        );
//        System.out.println("----------");
//    }

    @FXML
    private void createPlaylist() throws IOException {
        Path playlist = Paths.get(playlistDir.getText(), playlistName.getText());
        try (PrintWriter writer = new PrintWriter(playlist.toString())) {
            selected.forEach(path -> printTrackInfo(writer, path));
        }
    }


    private void printTrackInfo(PrintWriter writer, Path track) {
        try {
            FlacInfoReader.logger.setLevel(Level.WARNING);
            AudioFile file = AudioFileIO.read(track.toFile());
            FlacTag tag = (FlacTag) file.getTag();
            String pattern = "#EXTINF: %d, %s - %s.%s";
            writer.println(String.format(pattern,
                    file.getAudioHeader().getTrackLength(),
                    tag.getFirst(FieldKey.TRACK),
                    tag.getFirst(FieldKey.TITLE),
                    file.getAudioHeader().getFormat()));
            writer.println(Paths.get(playlistDir.getText()).relativize(track).toString());
            writer.println();

        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }
    }

    public CheckBoxTreeItem<Path> createTree(Path root) {
        CheckBoxTreeItem<Path> rootItem = new CheckBoxTreeItem<>(root);
        rootItem.selectedProperty().addListener(getBooleanChangeListener(rootItem));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path item : stream) {
                if (Files.isDirectory(item)) {
                    rootItem.getChildren().add(createTree(item));
                } else if (item.getFileName().toString().endsWith(".flac") /*|| item.endsWith(".mp3")*/) {
                    CheckBoxTreeItem<Path> treeItem = new CheckBoxTreeItem<>(item);
                    treeItem.selectedProperty().addListener(getBooleanChangeListener(treeItem));
                    rootItem.getChildren().add(treeItem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rootItem;
    }

    private ChangeListener<Boolean> getBooleanChangeListener(CheckBoxTreeItem<Path> item) {
        return (observable, oldValue, newValue) -> {
            if (newValue) {
//                System.out.println(item.getValue() + " selected");
                if (item.getValue().toString().endsWith(".flac"))
                    selected.add(item.getValue());
            } else {
//                System.out.println(item.getValue() + " unselected");
                selected.remove(item.getValue());
            }
        };
    }

    public void displayTreeView(Path rootPath) {

//        Path rootPath = Paths.get(startDirectory);
        dirTree.setShowRoot(true);

        dirTree.setCellFactory(CheckBoxTreeCell.forTreeView(TreeItem::expandedProperty, new StringConverter<TreeItem<Path>>() {
            @Override
            public String toString(TreeItem<Path> object) {
                return object.getValue().getFileName().toString();
            }

            @Override
            public TreeItem<Path> fromString(String string) {
                return new TreeItem<>(Paths.get(string));
            }
        }));
        dirTree.setRoot(createTree(rootPath));
    }
}
