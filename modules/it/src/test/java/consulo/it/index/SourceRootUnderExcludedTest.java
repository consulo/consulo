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
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createModule;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.indexingDebug;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consulo allows a source folder to live under an excluded folder, which IntelliJ does not: RootIndexImpl.findModuleRootInfo
 * resolves a directory to its nearest content root as soon as any ancestor is a registered content folder, so the nested
 * source root and its files stay in the project even though the folder above them is excluded.
 * <p>
 * Scanning must honour that. The file index is asserted first, so a failure of the indexing assertions alone pins the
 * defect on the scanner's root selection rather than on the roots model.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class SourceRootUnderExcludedTest {
    @Test
    public void sourceRootUnderExcludedFolderIsIndexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-source-under-excluded");

        Path plain = directory.resolve("plain");
        Files.createDirectories(plain);
        Files.writeString(plain.resolve("Plain.sand"), "class PlainInContent {}");

        Path excluded = directory.resolve("excluded");
        Path nestedSource = excluded.resolve("src");
        Files.createDirectories(nestedSource);
        Files.writeString(excluded.resolve("Hidden.sand"), "class HiddenInExcluded {}");
        Files.writeString(nestedSource.resolve("Nested.sand"), "class NestedUnderExcluded {}");

        Project project = openProject(application, projectManager, directory);

        VirtualFile directoryFile = findFile(directory);
        VirtualFile excludedFile = findFile(excluded);
        VirtualFile nestedSourceFile = findFile(nestedSource);

        WriteAction.run(() -> {
            Module module = createModule(project, "main", directory);
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            ContentEntry contentEntry = rootModel.addContentEntry(directoryFile);
            contentEntry.addFolder(excludedFile, ExcludedContentFolderTypeProvider.getInstance());
            contentEntry.addFolder(nestedSourceFile, ProductionContentFolderTypeProvider.getInstance());
            rootModel.commit();
        });

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        assertThat(ReadAction.compute(() -> fileIndex.isExcluded(excludedFile)))
            .as("the excluded folder itself must be excluded")
            .isTrue();
        assertThat(ReadAction.compute(() -> fileIndex.isExcluded(nestedSourceFile)))
            .as("a source folder under an excluded folder is still project content in Consulo")
            .isFalse();
        assertThat(ReadAction.compute(() -> fileIndex.isInContent(nestedSourceFile)))
            .as("a source folder under an excluded folder is still project content in Consulo")
            .isTrue();

        waitFor("the file in the plain content folder must be indexed", () -> !findClasses(project, "PlainInContent").isEmpty());

        assertThat(findClasses(project, "HiddenInExcluded"))
            .as("a file directly in the excluded folder must not be indexed")
            .isEmpty();

        waitFor(
            "a source root nested under an excluded folder must be indexed",
            () -> !findClasses(project, "NestedUnderExcluded").isEmpty()
        );
    }

    @Test
    public void moduleFileIndexIteratesIntoTheNestedSourceRoot(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-source-under-excluded-iterate");
        Path excluded = directory.resolve("excluded");
        Path nestedSource = excluded.resolve("src");
        Files.createDirectories(nestedSource);
        Files.writeString(excluded.resolve("Hidden.sand"), "class HiddenForIteration {}");
        Files.writeString(nestedSource.resolve("Nested.sand"), "class NestedForIteration {}");

        Project project = openProject(application, projectManager, directory);
        Module module = declareNestedSourceRoot(project, directory, excluded, nestedSource);

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        List<String> iterated = new ArrayList<>();
        ReadAction.run(() -> ModuleRootManager.getInstance(module).getFileIndex().iterateContent(file -> {
            if (!file.isDirectory()) {
                iterated.add(file.getName());
            }
            return true;
        }));

        assertThat(iterated)
            .as("the module content walk must reach the source root nested under the excluded folder")
            .contains("Nested.sand");
        assertThat(iterated)
            .as("a file directly in the excluded folder is not module content")
            .doesNotContain("Hidden.sand");
    }

    @Test
    public void scanningProducesAnOriginForTheNestedSourceRoot(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-source-under-excluded-origin");
        Path excluded = directory.resolve("excluded");
        Path nestedSource = excluded.resolve("src");
        Files.createDirectories(nestedSource);
        Files.writeString(nestedSource.resolve("Nested.sand"), "class NestedForOrigin {}");

        Project project = openProject(application, projectManager, directory);
        Module module = declareNestedSourceRoot(project, directory, excluded, nestedSource);

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        VirtualFile nestedSourceFile = findFile(nestedSource);
        FileBasedIndexImpl index = (FileBasedIndexImpl) FileBasedIndex.getInstance();

        List<VirtualFile> moduleRoots = new ArrayList<>();
        for (IndexableFilesIterator iterator : index.getOrderedIndexableFilesProviders(project)) {
            if (iterator.getOrigin() instanceof ModuleRootOrigin origin && module.equals(origin.getModule())) {
                moduleRoots.addAll(origin.getRoots());
            }
        }

        assertThat(moduleRoots)
            .as("the nested source root must be offered as a root of its own, since the walk cannot reach it from above")
            .contains(nestedSourceFile);
    }

    /**
     * The excluded folder keeps its type, so exclusion stays dynamic: a file created under it later is excluded without
     * anything recomputing the folder, while a file created later under the nested source root is still indexed. A
     * representation that instead expanded the excluded folder into per-child exclusions would cover only the children
     * that existed when it was expanded.
     */
    @Test
    public void filesCreatedLaterFollowTheFolderTypes(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-source-under-excluded-later");
        Path excluded = directory.resolve("excluded");
        Path nestedSource = excluded.resolve("src");
        Files.createDirectories(nestedSource);
        Files.writeString(nestedSource.resolve("Nested.sand"), "class NestedBeforeTheChange {}");

        Project project = openProject(application, projectManager, directory);
        declareNestedSourceRoot(project, directory, excluded, nestedSource);

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);
        waitFor("the initial scan must index the nested source root", () -> !findClasses(project, "NestedBeforeTheChange").isEmpty());

        indexingDebug(true);
        Files.writeString(excluded.resolve("LateHidden.sand"), "class LateHiddenInExcluded {}");
        Files.createDirectories(excluded.resolve("late"));
        Files.writeString(excluded.resolve("late").resolve("LateHidden2.sand"), "class LateHiddenInNewExcludedChild {}");
        Files.writeString(nestedSource.resolve("LateNested.sand"), "class LateNestedUnderExcluded {}");

        VirtualFileUtil.markDirtyAndRefresh(false, true, true, findFile(directory));
        awaitIdle(project);

        waitFor(
            "a file created later under the nested source root must be indexed",
            () -> !findClasses(project, "LateNestedUnderExcluded").isEmpty()
        );

        assertThat(findClasses(project, "LateHiddenInExcluded"))
            .as("a file created later directly in the excluded folder must stay unindexed")
            .isEmpty();
        assertThat(findClasses(project, "LateHiddenInNewExcludedChild"))
            .as("a directory created later under the excluded folder must be excluded too")
            .isEmpty();

        indexingDebug(false);
    }

    private static Module declareNestedSourceRoot(Project project, Path directory, Path excluded, Path nestedSource) throws Exception {
        VirtualFile directoryFile = findFile(directory);
        VirtualFile excludedFile = findFile(excluded);
        VirtualFile nestedSourceFile = findFile(nestedSource);

        return WriteAction.compute(() -> {
            Module module = createModule(project, "main", directory);
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            ContentEntry contentEntry = rootModel.addContentEntry(directoryFile);
            contentEntry.addFolder(excludedFile, ExcludedContentFolderTypeProvider.getInstance());
            contentEntry.addFolder(nestedSourceFile, ProductionContentFolderTypeProvider.getInstance());
            rootModel.commit();
            return module;
        });
    }
}
