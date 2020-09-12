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
import java.util.stream.Stream;

public class Controller {

    private static final Path MUSIC = Paths.get("/mnt/Data/Music2").toAbsolutePath().normalize();
    public static final String PLAYLIST_NAME = "test.m3u";

    public static final Path MUSIC_HISTORY = Paths.get("mdirhistory.cfg");
    public static final Path PLAYLIST_HISTORY = Paths.get("pdirhistory.cfg");

    private final List<Path> selected = new ArrayList<>();

    @FXML
    private TreeView<Path> dirTree;

    @FXML
    private TextField playlistName;

    @FXML
    private ComboBox<Path> playlistDir;

    @FXML
    private ComboBox<Path> musicDir;

    @FXML
    private void initialize() {
        loadMusicDirHistory();
        musicDir.getSelectionModel().selectFirst();
        musicDir.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> displayTreeView(newValue));

        loadPlaylistDirHistory();
        playlistDir.getSelectionModel().selectFirst();

        playlistName.setText(PLAYLIST_NAME);

        displayTreeView(MUSIC);
        dirTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadPlaylistDirHistory() {
        try (Stream<String> lines = Files.lines(PLAYLIST_HISTORY)) {
            lines.forEach((line) -> playlistDir.getItems().add(Paths.get(line)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMusicDirHistory() {
        try (Stream<String> lines = Files.lines(MUSIC_HISTORY)) {
            lines.forEach((line) -> musicDir.getItems().add(Paths.get(line)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openMusicDir() {
        DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle("Select music directory");
        dialog.setInitialDirectory(musicDir.getSelectionModel().getSelectedItem().toFile());
        File result = dialog.showDialog(null);
        if (result != null) {
            musicDir.getItems().add(0, result.toPath());
            musicDir.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void openPlaylistDir() {
        DirectoryChooser dialog = new DirectoryChooser();
        dialog.setTitle("Select playlist directory");
        dialog.setInitialDirectory(musicDir.getSelectionModel().getSelectedItem().toFile());
        File result = dialog.showDialog(null);
        if (result != null){
            playlistDir.getItems().add(0, result.toPath());
            playlistDir.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void createPlaylist() throws IOException {
        Path playlist = playlistDir.getValue().resolve(Path.of(playlistName.getText()));
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
            Path filename = playlistDir.getSelectionModel().getSelectedItem().relativize(track);
            writer.println(filename.toString());
            writer.println();

        } catch (CannotReadException e) {
            System.out.println("File read error");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Input/Output error");
            e.printStackTrace();
        } catch (TagException e) {
            System.out.println("Tag read error");
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            System.out.println("File is read only");
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            System.out.println("Invalid audio frame");
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
                if (item.getValue().toString().endsWith(".flac"))
                    selected.add(item.getValue());
            } else {
                selected.remove(item.getValue());
            }
        };
    }

    public void displayTreeView(Path rootPath) {

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

    public void saveHistory() {
        try (PrintWriter mWriter = new PrintWriter(MUSIC_HISTORY.toString());
        PrintWriter pWriter = new PrintWriter(PLAYLIST_HISTORY.toString())) {
            musicDir.getItems().forEach(mWriter::println);
            playlistDir.getItems().forEach(pWriter::println);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
