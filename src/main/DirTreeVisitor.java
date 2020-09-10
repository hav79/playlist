package main;

import javafx.scene.control.CheckBoxTreeItem;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class DirTreeVisitor implements FileVisitor<Path> {

    private CheckBoxTreeItem<String> rootItem;
    private CheckBoxTreeItem<String> parent;
    private CheckBoxTreeItem<String> currentItem;

    public DirTreeVisitor(CheckBoxTreeItem<String> parent) {
        this.rootItem = parent;
        this.parent = parent;
        this.currentItem = parent;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        currentItem = new CheckBoxTreeItem<String>(dir.getFileName().toString());
        parent.getChildren().add(currentItem);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

        return null;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        parent = null;
        return FileVisitResult.CONTINUE;
    }
}
