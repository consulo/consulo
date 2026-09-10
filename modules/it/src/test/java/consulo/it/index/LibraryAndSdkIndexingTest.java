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
package consulo.it.index;

import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.content.library.Library;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessModuleExtensionProvider;
import consulo.it.internal.HeadlessMutableModuleExtension;
import consulo.it.internal.HeadlessSdkType;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.LibraryOrigin;
import consulo.language.index.impl.internal.roots.kind.SdkOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.extension.ModuleInheritableNamedPointerImpl;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roots that reach the project through an order entry rather than through module content: a module library and an SDK
 * attached through a module extension. Both are seeded by their own iterators, so a change to how module content is
 * seeded must leave them alone.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class LibraryAndSdkIndexingTest {
    @Test
    public void classesOfAModuleLibraryAreIndexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-library");
        Files.writeString(directory.resolve("Content.sand"), "class ContentClassNextToLibrary {}");

        Path libraryClasses = Files.createTempDirectory("consulo-it-library-classes");
        Files.writeString(libraryClasses.resolve("Lib.sand"), "class ClassFromLibrary {}");

        Project project = openProject(application, projectManager, directory);
        Module module = addContentRoot(project, "main", directory);

        VirtualFile libraryRoot = findFile(libraryClasses);
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Library library = rootModel.getModuleLibraryTable().createLibrary("test-library");
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(libraryRoot, BinariesOrderRootType.ID);
            libraryModel.commit();
            rootModel.commit();
        });

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        assertThat(collectRoots(project, LibraryOrigin.class))
            .as("the library must be offered as a root of its own")
            .contains(libraryRoot);

        waitFor("the module content must be indexed", () -> !findClasses(project, "ContentClassNextToLibrary").isEmpty());
        waitFor("a class from a module library must be indexed", () -> !findClasses(project, "ClassFromLibrary").isEmpty());
    }

    @Test
    public void classesOfAnSdkAreIndexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sdk");
        Files.writeString(directory.resolve("Content.sand"), "class ContentClassNextToSdk {}");

        Path sdkHome = Files.createTempDirectory("consulo-it-sdk-home");
        Path sdkClasses = sdkHome.resolve("classes");
        Path sdkSources = sdkHome.resolve("sources");
        Files.createDirectories(sdkClasses);
        Files.createDirectories(sdkSources);
        Files.writeString(sdkClasses.resolve("Sdk.sand"), "class ClassFromSdk {}");
        Files.writeString(sdkSources.resolve("SdkSource.sand"), "class ClassFromSdkSources {}");

        Project project = openProject(application, projectManager, directory);
        Module module = addContentRoot(project, "main", directory);

        VirtualFile sdkRoot = findFile(sdkClasses);
        VirtualFile sdkSourceRoot = findFile(sdkSources);
        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("headless-sdk", new HeadlessSdkType());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(sdkHome.toString());
            modificator.addRoot(sdkRoot, BinariesOrderRootType.ID);
            modificator.addRoot(sdkSourceRoot, SourcesOrderRootType.ID);
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });

        try {
            attachSdk(module, sdk);

            awaitSmart(DumbService.getInstance(project));
            awaitIdle(project);

            assertThat(collectRoots(project, SdkOrigin.class))
                .as("both SDK roots must be offered, one iterator each")
                .contains(sdkRoot, sdkSourceRoot);

            waitFor("the module content must be indexed", () -> !findClasses(project, "ContentClassNextToSdk").isEmpty());
            waitFor("a class from the SDK binaries must be indexed", () -> !findClasses(project, "ClassFromSdk").isEmpty());
            waitFor("a class from the SDK sources must be indexed", () -> !findClasses(project, "ClassFromSdkSources").isEmpty());
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }
    }

    /**
     * The realistic shape of an SDK: its classes live in an archive, so the root handed to the iterator belongs to the
     * archive file system rather than to the local one.
     */
    @Test
    public void classesInAnSdkArchiveAreIndexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sdk-archive");
        Files.writeString(directory.resolve("Content.sand"), "class ContentClassNextToSdkArchive {}");

        Path sdkHome = Files.createTempDirectory("consulo-it-sdk-archive-home");
        Path archive = sdkHome.resolve("sdk-classes.jar");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            out.putNextEntry(new ZipEntry("Archived.sand"));
            out.write("class ClassFromSdkArchive {}".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        Project project = openProject(application, projectManager, directory);
        Module module = addContentRoot(project, "main", directory);

        VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(findFile(archive));
        assertThat(archiveRoot).as("the archive must be mounted by the archive file system").isNotNull();

        SdkTable sdkTable = SdkTable.getInstance();
        Sdk sdk = WriteAction.compute(() -> {
            Sdk created = sdkTable.createSdk("headless-archive-sdk", new HeadlessSdkType());
            SdkModificator modificator = created.getSdkModificator();
            modificator.setHomePath(sdkHome.toString());
            modificator.addRoot(archiveRoot, BinariesOrderRootType.ID);
            modificator.commitChanges();
            sdkTable.addSdk(created);
            return created;
        });

        try {
            attachSdk(module, sdk);

            awaitSmart(DumbService.getInstance(project));
            awaitIdle(project);

            assertThat(collectRoots(project, SdkOrigin.class))
                .as("the archive root must be offered as an SDK root")
                .contains(archiveRoot);

            waitFor("the module content must be indexed", () -> !findClasses(project, "ContentClassNextToSdkArchive").isEmpty());
            waitFor("a class inside the SDK archive must be indexed", () -> !findClasses(project, "ClassFromSdkArchive").isEmpty());
        }
        finally {
            WriteAction.run(() -> sdkTable.removeSdk(sdk));
        }
    }

    private static void attachSdk(Module module, Sdk sdk) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            HeadlessMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(HeadlessModuleExtensionProvider.ID);
            assertThat(extension).as("the headless module extension must be registered").isNotNull();
            extension.setEnabled(true);
            ((ModuleInheritableNamedPointerImpl<Sdk>) extension.getInheritableSdk()).set(null, sdk.getName());
            ((ModifiableModuleRootLayer) extension.getModuleRootLayer()).addModuleExtensionSdkEntry(extension);
            rootModel.commit();
        });
    }

    private static List<VirtualFile> collectRoots(Project project, Class<?> originType) {
        FileBasedIndexImpl index = (FileBasedIndexImpl) FileBasedIndex.getInstance();
        List<VirtualFile> roots = new ArrayList<>();
        for (IndexableFilesIterator iterator : index.getOrderedIndexableFilesProviders(project)) {
            Object origin = iterator.getOrigin();
            if (originType.isInstance(origin)) {
                if (origin instanceof LibraryOrigin libraryOrigin) {
                    roots.addAll(libraryOrigin.getClassRoots());
                }
                else if (origin instanceof SdkOrigin sdkOrigin) {
                    roots.addAll(sdkOrigin.getRootsToIndex());
                }
            }
        }
        return roots;
    }
}
