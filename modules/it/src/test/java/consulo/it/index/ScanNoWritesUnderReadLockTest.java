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
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
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
import static consulo.it.index.ScanningTestSupport.awaitScanningFinished;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.fullScan;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contentless index values are now prepared under the read lock and written outside it by
 * {@code SingleIndexValueApplier}. A scan of a project whose files only contentless indexes accept must therefore
 * still index them, and the applied values must be readable once the scan is over - and stay readable across a
 * following full rescan, which prepares and applies them again.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class ScanNoWritesUnderReadLockTest {
    private static final int FILES = 12;

    @Test
    public void contentlessIndexValuesAreAppliedAndReadable(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-contentless-apply");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        for (int i = 0; i < FILES; i++) {
            Files.writeString(src.resolve(fileName(i)), "applied" + i);
        }

        Project project = openProject(application, projectManager, directory);
        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        addContentRoot(project, directory);
        awaitIdle(project);

        assertAllFilesIndexed(project);

        fullScan(project, "contentless apply rescan").queue();
        awaitScanningFinished(project);
        awaitIdle(project);

        assertAllFilesIndexed(project);
    }

    private static void assertAllFilesIndexed(Project project) throws Exception {
        for (int i = 0; i < FILES; i++) {
            String name = fileName(i);
            waitFor(name + " must be indexed by the contentless index", () -> !containingFiles(project, name).isEmpty());
            assertThat(containingFiles(project, name))
                .as("%s must be the only file indexed under its own name", name)
                .extracting(VirtualFile::getName)
                .containsExactly(name);
        }

        waitFor("the file type index must know every .counted file", () -> filesOfCountedType(project).size() == FILES);
        assertThat(filesOfCountedType(project))
            .as("the file type index must answer with exactly the .counted files of this project")
            .containsExactlyInAnyOrderElementsOf(expectedFileNames());
    }

    private static String fileName(int index) {
        return "applied" + index + "." + HeadlessCountedFileType.EXTENSION;
    }

    private static Set<String> expectedFileNames() {
        Set<String> names = new TreeSet<>();
        for (int i = 0; i < FILES; i++) {
            names.add(fileName(i));
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

    private static Set<String> filesOfCountedType(Project project) {
        return ReadAction.compute(() -> {
            try {
                Set<String> names = new TreeSet<>();
                for (VirtualFile file : FileTypeIndex.getFiles(HeadlessCountedFileType.INSTANCE, GlobalSearchScope.allScope(project))) {
                    names.add(file.getName());
                }
                return names;
            }
            catch (IndexNotReadyException e) {
                return Set.of();
            }
        });
    }
}
