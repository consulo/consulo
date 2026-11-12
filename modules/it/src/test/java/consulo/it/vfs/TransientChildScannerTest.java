/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.it.vfs;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.content.library.Library;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.it.internal.HeadlessFileTypeManager;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.ChildInfo;
import consulo.virtualFileSystem.event.VFileCreateEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.impl.internal.refresh.RefreshWorker;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.createModule;
import static consulo.it.index.ScanningTestSupport.findFile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A directory tree created outside the IDE is reported by the refresh as one create event per new top directory. The event
 * carries the pre-scanned children, so the indexer does not list every new directory again, but only for the part of the tree
 * which an open project indexes: content roots and library roots, minus their exclusions, minus ignored names, down to an
 * indexable root nested under an exclusion.
 * <p>
 * The exclusions and roots are configured by URL before the directories exist, which is the usual case for an output folder.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@ExtendWith(HeadlessProjectExtension.class)
public class TransientChildScannerTest {
    /**
     * The creations any refresh published - a refresh queued by the roots change may pick up a new directory before the
     * scan of the test does, with the same worker and the same child scanner.
     */
    private final List<VFileCreateEvent> myPublishedCreations = new CopyOnWriteArrayList<>();
    private @Nullable MessageBusConnection myConnection;

    @BeforeEach
    public void setUp() {
        myConnection = Application.get().getMessageBus().connect();
        myConnection.subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileCreateEvent create) {
                        myPublishedCreations.add(create);
                    }
                }
            }
        });
    }

    @AfterEach
    public void tearDown() {
        MessageBusConnection connection = myConnection;
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Test
    public void treeUnderContentRootIsPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-content");
        Path root = Files.createDirectories(directory.resolve("root"));
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(), List.of());

        VirtualFile rootFile = loadedDirectory(root);
        Files.createDirectories(root.resolve("src/a"));
        Files.writeString(root.resolve("src/a/A.sand"), "class A {}");
        Files.writeString(root.resolve("src/B.sand"), "class B {}");

        VFileCreateEvent event = scanCreation(rootFile, "src");

        assertThat(event.getChildren()).as("the new directory under a content root must be pre-scanned").isNotNull();
        assertThat(event.isAllChildren()).isTrue();
        assertThat(names(event.getChildren())).containsExactlyInAnyOrder("a", "B.sand");
        ChildInfo a = child(event.getChildren(), "a");
        assertThat(a.isAllChildren()).isTrue();
        assertThat(names(a.getChildren())).containsExactly("A.sand");
    }

    @Test
    public void excludedFolderCreatedLaterIsNotPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-excluded");
        Path root = Files.createDirectories(directory.resolve("root"));
        Path target = root.resolve("target");
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(target), List.of());

        VirtualFile rootFile = loadedDirectory(root);
        Files.createDirectories(target.resolve("classes"));
        Files.writeString(target.resolve("classes/A.class"), "binary");

        VFileCreateEvent event = scanCreation(rootFile, "target");

        assertThat(event.getChildren())
            .as("an excluded folder configured before it existed must not be pre-scanned")
            .isNull();
    }

    @Test
    public void contentRootUnderExcludedFolderIsPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-nested-content");
        Path root = Files.createDirectories(directory.resolve("root"));
        Path target = root.resolve("target");
        Path generated = target.resolve("generated");
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(target), List.of());
        WriteAction.run(() -> {
            Module nested = createModule(project, "nested", generated);
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(nested).getModifiableModel();
            rootModel.addContentEntry(url(generated));
            rootModel.commit();
        });
        awaitIdle(project);

        VirtualFile rootFile = loadedDirectory(root);
        Files.createDirectories(generated.resolve("pkg"));
        Files.writeString(generated.resolve("pkg/G.sand"), "class G {}");
        Files.createDirectories(target.resolve("classes"));
        Files.writeString(target.resolve("classes/A.class"), "binary");

        VFileCreateEvent event = scanCreation(rootFile, "target");

        assertThat(event.getChildren()).as("the excluded folder must be scanned down to the nested content root").isNotNull();
        assertThat(event.isAllChildren()).as("only the path to the nested content root is scanned").isFalse();
        assertThat(names(event.getChildren())).containsExactly("generated");
        ChildInfo generatedInfo = child(event.getChildren(), "generated");
        assertThat(generatedInfo.isAllChildren()).isTrue();
        assertThat(names(generatedInfo.getChildren())).containsExactly("pkg");
        assertThat(names(child(generatedInfo.getChildren(), "pkg").getChildren())).containsExactly("G.sand");
    }

    @Test
    public void libraryRootUnderExcludedFolderIsPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-nested-library");
        Path root = Files.createDirectories(directory.resolve("root"));
        Path target = root.resolve("target");
        Path libraryRoot = target.resolve("lib");
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(target), List.of(libraryRoot));

        VirtualFile rootFile = loadedDirectory(root);
        Files.createDirectories(libraryRoot.resolve("pkg"));
        Files.writeString(libraryRoot.resolve("pkg/L.sand"), "class L {}");
        Files.createDirectories(target.resolve("classes"));

        VFileCreateEvent event = scanCreation(rootFile, "target");

        assertThat(event.getChildren())
            .as("an excluded content folder must not exclude a library root nested in it")
            .isNotNull();
        assertThat(event.isAllChildren()).isFalse();
        assertThat(names(event.getChildren())).containsExactly("lib");
        ChildInfo lib = child(event.getChildren(), "lib");
        assertThat(lib.isAllChildren()).isTrue();
        assertThat(names(child(lib.getChildren(), "pkg").getChildren())).containsExactly("L.sand");
    }

    @Test
    public void treeOutsideOfProjectsIsNotPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-project");
        Path root = Files.createDirectories(directory.resolve("root"));
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(), List.of());

        Path outside = Files.createTempDirectory("consulo-it-scanner-outside");
        VirtualFile outsideFile = loadedDirectory(outside);
        Files.createDirectories(outside.resolve("tree/a"));
        Files.writeString(outside.resolve("tree/a/A.sand"), "class A {}");

        VFileCreateEvent event = scanCreation(outsideFile, "tree");

        assertThat(event.getChildren()).as("a directory outside of every project must not be pre-scanned").isNull();
    }

    @Test
    public void ignoredDirectoryIsNotPreScanned(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-scanner-ignored");
        Path root = Files.createDirectories(directory.resolve("root"));
        Project project = projects.open(directory);
        configure(project, directory, root, List.of(), List.of());

        Disposable ignore = Disposable.newDisposable();
        try {
            HeadlessFileTypeManager.ignoreFileName(".git", ignore);

            VirtualFile rootFile = loadedDirectory(root);
            Files.createDirectories(root.resolve(".git/objects"));
            Files.writeString(root.resolve(".git/HEAD"), "ref: refs/heads/master");

            VFileCreateEvent event = scanCreation(rootFile, ".git");

            assertThat(event.getChildren()).as("an ignored directory must not be pre-scanned").isNull();
        }
        finally {
            Disposer.dispose(ignore);
        }
    }

    private static void configure(Project project, Path directory, Path root, List<Path> excluded, List<Path> libraryRoots)
        throws Exception {
        VirtualFile rootFile = findFile(root);
        WriteAction.run(() -> {
            Module module = createModule(project, "main", directory);
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            ContentEntry contentEntry = rootModel.addContentEntry(rootFile);
            for (Path path : excluded) {
                contentEntry.addFolder(url(path), ExcludedContentFolderTypeProvider.getInstance());
            }
            if (!libraryRoots.isEmpty()) {
                Library library = rootModel.getModuleLibraryTable().createLibrary("test-library");
                Library.ModifiableModel libraryModel = library.getModifiableModel();
                for (Path libraryRoot : libraryRoots) {
                    libraryModel.addRoot(url(libraryRoot), BinariesOrderRootType.ID);
                }
                libraryModel.commit();
            }
            rootModel.commit();
        });
        awaitIdle(project);
    }

    private static String url(Path path) {
        return VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(path.toString()));
    }

    private static VirtualFile loadedDirectory(Path path) {
        VirtualFile file = findFile(path);
        assertThat(file.getChildren()).isNotNull();
        return file;
    }

    private VFileCreateEvent scanCreation(VirtualFile parent, String childName) {
        ((NewVirtualFile) parent).markDirty();
        List<VFileEvent> events = new RefreshWorker(List.of((NewVirtualFile) parent), false).scan();
        List<VFileCreateEvent> creations = events.stream()
            .filter(event -> isCreationOf(event, parent, childName))
            .map(event -> (VFileCreateEvent) event)
            .toList();
        if (creations.isEmpty()) {
            creations = myPublishedCreations.stream().filter(event -> isCreationOf(event, parent, childName)).toList();
        }
        assertThat(creations).as("the creation of %s, events: %s, published: %s", childName, events, myPublishedCreations).hasSize(1);
        return creations.getFirst();
    }

    private static boolean isCreationOf(VFileEvent event, VirtualFile parent, String childName) {
        return event instanceof VFileCreateEvent create && create.getParent().equals(parent) && create.getChildName().equals(childName);
    }

    private static List<String> names(ChildInfo @Nullable [] children) {
        assertThat(children).isNotNull();
        return Arrays.stream(children).map(child -> child.getName().toString()).collect(Collectors.toList());
    }

    private static ChildInfo child(ChildInfo @Nullable [] children, String name) {
        assertThat(children).isNotNull();
        return Arrays.stream(children)
            .filter(child -> child.getName().toString().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError(name + " not found among " + names(children)));
    }
}
