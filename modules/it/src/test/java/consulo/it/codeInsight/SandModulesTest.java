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
package consulo.it.codeInsight;

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessModules;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The module layout a test builds with {@link HeadlessModules} lands in the project model as written: roots carry
 * the kind they were given, a nested module owns the files under its own content root, and a dependency is recorded.
 * <p>
 * Sand looks classes up project-wide, so the cross-module case shows that the second module's sources were indexed -
 * not that the dependency scopes resolution, which Sand does not do.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandModulesTest {
    @Test
    public void sourceAndTestRootsKeepTheirKind(CodeInsightTestFixture fixture) throws Exception {
        Module module = fixture.getModule();

        VirtualFile testRoot = WriteAction.compute(() -> VirtualFileUtil.createDirectoryIfMissing(fixture.getProjectRoot(), "test"));
        HeadlessModules.addTestSourceRoot(module, testRoot);
        HeadlessProjects.awaitIdle(fixture.getProject());

        ModuleRootManager roots = ModuleRootManager.getInstance(module);
        VirtualFile sourceRoot = fixture.getProjectRoot().findChild(CodeInsightTestFixture.SOURCE_ROOT);

        assertThat(ReadAction.compute(() -> roots.getContentFolderFiles(LanguageContentFolderScopes.production())))
            .as("production sources are src only")
            .containsExactly(sourceRoot);
        assertThat(ReadAction.compute(() -> roots.getContentFolderFiles(LanguageContentFolderScopes.productionAndTest())))
            .as("with tests included the test root joins it")
            .containsExactlyInAnyOrder(sourceRoot, testRoot);
    }

    @Test
    public void aSecondModuleIsIndexedOwnsItsFilesAndIsDependedOn(CodeInsightTestFixture fixture) throws Exception {
        Project project = fixture.getProject();

        VirtualFile libRoot = WriteAction.compute(() -> VirtualFileUtil.createDirectoryIfMissing(fixture.getProjectRoot(), "lib"));
        VirtualFile libSources = WriteAction.compute(() -> VirtualFileUtil.createDirectoryIfMissing(libRoot, "src"));
        Module lib = HeadlessModules.createModule(project, "lib", libRoot);
        HeadlessModules.addSourceRoot(lib, libSources);
        HeadlessModules.addDependency(fixture.getModule(), lib);

        PsiFile item = fixture.addFileToProject("lib/src/item.sand", "class Item { \"body\" }\n");
        fixture.configureByText("main.sand", "class User : <caret>Item {}\n");

        assertThat(ModuleRootManager.getInstance(fixture.getModule()).getDependencies())
            .as("main depends on lib")
            .containsExactly(lib);
        assertThat(ReadAction.compute(() -> ModuleUtilCore.findModuleForFile(item.getVirtualFile(), project)))
            .as("a file under the nested content root belongs to the nested module")
            .isEqualTo(lib);

        PsiElement resolved = ReadAction.compute(fixture.getReferenceAtCaretPositionWithAssertion()::resolve);
        assertThat(resolved).isInstanceOf(SandClass.class);
        assertThat(ReadAction.compute(() -> resolved.getContainingFile().getVirtualFile())).isEqualTo(item.getVirtualFile());
    }
}
