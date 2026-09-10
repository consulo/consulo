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
import consulo.application.dumb.IndexNotReadyException;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessCountedFileType;
import consulo.it.internal.HeadlessCountingIndex;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.createSandFiles;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scanner no longer offers every file to every candidate index: {@code RequiredIndexesEvaluator} decides which
 * indexes a file needs, and an index whose input filter is a file type hint is decided once per file type instead of
 * once per file.
 * <p>
 * {@link HeadlessCountingIndex} accepts only {@link HeadlessCountedFileType} and answers through a file type hint, so a
 * scan of a project full of {@code .sand} files must never hand it a single {@code .sand} file, while the {@code .counted}
 * files it does accept still end up in its index.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class ScanningRequiredIndexesTest {
    private static final int SAND_FILES = 40;
    private static final int COUNTED_FILES = 3;

    @Test
    public void fileTypeHintDecidesWithoutBeingHandedEveryFile(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-required-indexes");
        Path src = createSandFiles(directory, SAND_FILES, "Required");
        for (int i = 0; i < COUNTED_FILES; i++) {
            Files.writeString(src.resolve(countedFileName(i)), "counted" + i);
        }

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        HeadlessCountingIndex.reset();

        addContentRoot(project, directory);
        awaitIdle(project);

        Set<String> sandFilesHandedToTheFilter = new TreeSet<>(HeadlessCountingIndex.perFileNames());
        sandFilesHandedToTheFilter.retainAll(sandFileNames());

        assertThat(sandFilesHandedToTheFilter)
            .as("a file type hint must reject the .sand file type without the framework handing it a single .sand file")
            .isEmpty();

        assertThat(HeadlessCountingIndex.perFileCalls())
            .as("the filter must be consulted far fewer times than once per scanned file")
            .isLessThan(SAND_FILES);

        assertThat(HeadlessCountingIndex.fileTypeCalls(SandFileType.INSTANCE))
            .as("deciding the .sand file type must cost at most a handful of hint calls")
            .isLessThan(SAND_FILES);

        for (int i = 0; i < COUNTED_FILES; i++) {
            String name = countedFileName(i);
            waitFor(name + " must be indexed by the accepting index", () -> !containingFiles(project, name).isEmpty());
            assertThat(containingFiles(project, name))
                .as("%s must be the only file indexed under its own name", name)
                .extracting(VirtualFile::getName)
                .containsExactly(name);
        }
    }

    private static String countedFileName(int index) {
        return "counted" + index + "." + HeadlessCountedFileType.EXTENSION;
    }

    private static Set<String> sandFileNames() {
        Set<String> names = new TreeSet<>();
        for (int i = 0; i < SAND_FILES; i++) {
            names.add("file" + i + ".sand");
        }
        return names;
    }

    private static Collection<VirtualFile> containingFiles(Project project, String key) {
        return ReadAction.compute(() -> {
            try {
                return FileBasedIndex.getInstance()
                    .getContainingFiles(HeadlessCountingIndex.NAME, key, GlobalSearchScope.allScope(project));
            }
            catch (IndexNotReadyException e) {
                return List.of();
            }
        });
    }
}
