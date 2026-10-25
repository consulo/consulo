package consulo.virtualFileSystem.impl.internal.archive;

import consulo.util.io.FileAttributes;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.archive.ArchivePath;
import consulo.virtualFileSystem.impl.internal.ZipFileSystemImpl;
import consulo.virtualFileSystem.internal.FileSystemUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArchiveNioFileSystemTest {
    @TempDir
    Path myTempDir;

    private Path myArchive;
    private ArchiveNioFileSystem myFileSystem;

    @BeforeEach
    public void setUp() throws IOException {
        myArchive = myTempDir.resolve("lib.zip");
        try (OutputStream out = Files.newOutputStream(myArchive); ZipOutputStream zip = new ZipOutputStream(out)) {
            // no explicit entry for "org/" - directories are implied by the entries below them
            zip.putNextEntry(new ZipEntry("org/a/A.txt"));
            zip.write("hello".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("org/b/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("root.txt"));
            zip.write("root".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        FileAttributes attributes = FileSystemUtil.getAttributes(myArchive.toString());
        assertThat(attributes).isNotNull();
        myFileSystem = new ArchiveNioFileSystem(new ArchiveNioFileSystemProvider(), new ZipFileSystemImpl(), myArchive, attributes);
    }

    @AfterEach
    public void tearDown() {
        myFileSystem.close();
    }

    @Test
    public void listsEntries() throws IOException {
        ArchivePathImpl root = myFileSystem.getRoot();

        assertThat(names(root)).containsExactly("org", "root.txt");
        assertThat(names(root.resolve("org"))).containsExactly("a", "b");
        assertThat(names(root.resolve("org/a"))).containsExactly("A.txt");
        assertThat(names(root.resolve("org/b"))).isEmpty();
    }

    @Test
    public void readsAttributes() throws IOException {
        ArchivePathImpl root = myFileSystem.getRoot();

        assertThat(Files.isDirectory(root)).isTrue();
        assertThat(Files.isDirectory(root.resolve("org"))).isTrue();
        assertThat(Files.isRegularFile(root.resolve("org/a/A.txt"))).isTrue();
        assertThat(Files.exists(root.resolve("missing"))).isFalse();
        assertThat(Files.isWritable(root.resolve("root.txt"))).isFalse();

        BasicFileAttributes attributes = Files.readAttributes(root.resolve("org/a/A.txt"), BasicFileAttributes.class);
        assertThat(attributes.size()).isEqualTo(5);
        assertThat(Files.readAttributes(root.resolve("root.txt"), "basic:size,isDirectory"))
            .containsEntry("size", 4L)
            .containsEntry("isDirectory", false);
    }

    @Test
    public void readsContent() throws IOException {
        ArchivePathImpl root = myFileSystem.getRoot();

        assertThat(Files.readString(root.resolve("org/a/A.txt"))).isEqualTo("hello");
        assertThat(Files.readAllBytes(root.resolve("root.txt"))).isEqualTo("root".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> Files.readString(root.resolve("missing"))).isInstanceOf(NoSuchFileException.class);
    }

    @Test
    public void isReadOnly() {
        ArchivePathImpl root = myFileSystem.getRoot();

        assertThatThrownBy(() -> Files.writeString(root.resolve("new.txt"), "x")).isInstanceOf(ReadOnlyFileSystemException.class);
        assertThatThrownBy(() -> Files.delete(root.resolve("root.txt"))).isInstanceOf(ReadOnlyFileSystemException.class);
        assertThatThrownBy(() -> Files.createDirectory(root.resolve("dir"))).isInstanceOf(ReadOnlyFileSystemException.class);
    }

    @Test
    public void pathOperations() {
        ArchivePathImpl root = myFileSystem.getRoot();
        Path file = root.resolve("org/a/A.txt");

        assertThat(file.toString()).isEqualTo("/org/a/A.txt");
        assertThat(file.isAbsolute()).isTrue();
        assertThat(file.getNameCount()).isEqualTo(3);
        assertThat(file.getFileName().toString()).isEqualTo("A.txt");
        assertThat(file.getParent()).isEqualTo(root.resolve("org/a"));
        assertThat(root.resolve("org").getParent()).isEqualTo(root);
        assertThat(root.getParent()).isNull();
        assertThat(root.getFileName()).isNull();
        assertThat(file.startsWith(root.resolve("org"))).isTrue();
        assertThat(file.endsWith("a/A.txt")).isTrue();
        assertThat(root.resolve("org").relativize(file).toString()).isEqualTo("a/A.txt");
        assertThat(root.resolve("org/a/../b/./x").normalize()).isEqualTo(root.resolve("org/b/x"));
        assertThat(file.subpath(1, 3).toString()).isEqualTo("a/A.txt");
        assertThat(myFileSystem.getPath("org", "a").toAbsolutePath()).isEqualTo(root.resolve("org/a"));
    }

    @Test
    public void answersVirtualFileUrl() {
        ArchivePath file = (ArchivePath) myFileSystem.getRoot().resolve("org/a/A.txt");
        String localPath = FileUtil.toSystemIndependentName(myArchive.toString());

        assertThat(file.getArchiveFile()).isEqualTo(myArchive);
        assertThat(file.getEntryPath()).isEqualTo("org/a/A.txt");
        assertThat(file.getVirtualFileUrl()).isEqualTo("zip://" + localPath + "!/org/a/A.txt");
        assertThat(myFileSystem.getRoot().getVirtualFileUrl()).isEqualTo("zip://" + localPath + "!/");
    }

    @Test
    public void tracksArchiveChanges() throws IOException {
        FileAttributes attributes = FileSystemUtil.getAttributes(myArchive.toString());
        assertThat(attributes).isNotNull();
        assertThat(myFileSystem.isUpToDate(attributes)).isTrue();

        Files.writeString(myArchive, "changed");
        FileAttributes changed = FileSystemUtil.getAttributes(myArchive.toString());
        assertThat(changed).isNotNull();
        assertThat(myFileSystem.isUpToDate(changed)).isFalse();
    }

    private static List<String> names(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.map(path -> path.getFileName().toString()).toList();
        }
    }
}
