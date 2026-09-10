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
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.index.impl.internal.FileBasedIndexImpl;
import consulo.language.index.impl.internal.roots.IndexableFilesIterator;
import consulo.language.index.impl.internal.roots.kind.ModuleRootOrigin;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.module.Module;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.awaitSmart;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.openProject;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A module whose content root lives inside another module's content root. Each module walks its own content, so the
 * outer walk stops at the inner module's directory and the inner module has to be offered as a root of its own.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
@AllowLogError({
    "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
    "consulo.application.impl.internal.BaseApplication",
    "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
})
public class NestedModuleContentRootTest {
    @Test
    public void moduleNestedInAnotherModuleIsIndexed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-nested-module");
        Path inner = directory.resolve("inner");
        Files.createDirectories(inner);
        Files.writeString(directory.resolve("Outer.sand"), "class OuterModuleClass {}");
        Files.writeString(inner.resolve("Inner.sand"), "class InnerModuleClass {}");

        Project project = openProject(application, projectManager, directory);
        addContentRoot(project, "outer", directory);
        Module innerModule = addContentRoot(project, "inner", inner);

        awaitSmart(DumbService.getInstance(project));
        awaitIdle(project);

        VirtualFile innerFile = findFile(inner);
        FileBasedIndexImpl index = (FileBasedIndexImpl) FileBasedIndex.getInstance();

        List<VirtualFile> innerModuleRoots = new ArrayList<>();
        for (IndexableFilesIterator iterator : index.getOrderedIndexableFilesProviders(project)) {
            if (iterator.getOrigin() instanceof ModuleRootOrigin origin && innerModule.equals(origin.getModule())) {
                innerModuleRoots.addAll(origin.getRoots());
            }
        }

        assertThat(innerModuleRoots)
            .as("the nested module must be offered as a root of its own, the outer module's walk cannot reach it")
            .contains(innerFile);

        waitFor("the outer module must be indexed", () -> !findClasses(project, "OuterModuleClass").isEmpty());
        waitFor("the module nested in another module's content must be indexed", () -> !findClasses(project, "InnerModuleClass").isEmpty());
    }
}
