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

import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessProjects;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaRecorder;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexMetaStorage;
import consulo.language.index.impl.internal.moduleAware.ModuleAwareIndexVariants;
import consulo.language.index.impl.internal.stub.SerializedStubTree;
import consulo.language.index.impl.internal.stub.StubUpdatingIndex;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexOptionSelector;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The index of a file is one for the whole application, but the variants a library file is indexed under come from the
 * project indexing it. Two projects reaching the same library file - a system header, in C/C++ - with different
 * options must both keep their variant: neither may find the file "drifted" because the other indexed it, which would
 * reindex it on every switch, and each reads its own variant by default.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SharedLibraryAcrossProjectsTest {
    private static final String SHARED = """
        #if A
        class SharedAcrossWithA {}
        #else
        class SharedAcrossWithoutA {}
        #end
        """;

    @Test
    public void eachProjectKeepsItsVariantOfASharedLibraryFile(HeadlessProjects projects) throws Exception {
        Path libraryDirectory = Files.createTempDirectory("consulo-it-shared-across-lib");
        Files.writeString(libraryDirectory.resolve("Shared.sand"), SHARED);

        Project alpha = openProjectUsingLibrary(projects, "alpha", libraryDirectory, Set.of("A"));
        VirtualFile shared = findFile(libraryDirectory.resolve("Shared.sand"));
        waitFor("alpha must read the file as it compiles it", () -> classesIn(alpha, shared).equals(List.of("SharedAcrossWithA")));

        Project beta = openProjectUsingLibrary(projects, "beta", libraryDirectory, Set.of());
        waitFor("beta must read the file as it compiles it", () -> classesIn(beta, shared).equals(List.of("SharedAcrossWithoutA")));
        awaitIdle(alpha);

        // indexing the file for beta kept alpha's variant: nothing is drifted for either project
        assertThat(isStale(alpha, shared)).as("alpha's variant must survive beta indexing the shared file").isFalse();
        assertThat(isStale(beta, shared)).as("beta's variant must be recorded").isFalse();
        assertThat(classCount(alpha, IndexOptionSelector.ALL_VARIANTS, "SharedAcrossWithA")).isEqualTo(1);
        assertThat(classCount(alpha, IndexOptionSelector.ALL_VARIANTS, "SharedAcrossWithoutA")).isEqualTo(1);

        // and each project reads its own variant without asking for it; findClasses answers nothing while a project is
        // dumb, so the positive answers are waited for before the negative ones count
        waitFor("alpha reads its variant", () -> findClasses(alpha, "SharedAcrossWithA").size() == 1);
        waitFor("beta reads its variant", () -> findClasses(beta, "SharedAcrossWithoutA").size() == 1);
        assertThat(findClasses(alpha, "SharedAcrossWithoutA")).as("alpha does not read beta's variant").isEmpty();
        assertThat(findClasses(beta, "SharedAcrossWithA")).as("beta does not read alpha's variant").isEmpty();
    }

    /**
     * Closing the second project and opening the first again - a switch - must find the first project's variant still
     * recorded, and its tree must follow its own flags.
     */
    @Test
    public void switchingBackFindsNothingDrifted(HeadlessProjects projects) throws Exception {
        Path libraryDirectory = Files.createTempDirectory("consulo-it-shared-switch-lib");
        Files.writeString(libraryDirectory.resolve("Shared.sand"), SHARED);

        Project alpha = openProjectUsingLibrary(projects, "alpha", libraryDirectory, Set.of("A"));
        VirtualFile shared = findFile(libraryDirectory.resolve("Shared.sand"));
        waitFor("alpha must read the file as it compiles it", () -> classesIn(alpha, shared).equals(List.of("SharedAcrossWithA")));
        projects.close(alpha);

        Project beta = openProjectUsingLibrary(projects, "beta", libraryDirectory, Set.of());
        waitFor("beta must read the file as it compiles it", () -> classesIn(beta, shared).equals(List.of("SharedAcrossWithoutA")));
        projects.close(beta);

        Project alphaAgain = openProjectUsingLibrary(projects, "alpha-again", libraryDirectory, Set.of("A"));
        assertThat(isStale(alphaAgain, shared)).as("alpha's variant must still be recorded after beta indexed the file").isFalse();
        waitFor("alpha must read its variant again", () -> classesIn(alphaAgain, shared).equals(List.of("SharedAcrossWithA")));
        // findClasses answers nothing while the project is dumb, which a late rescan of the reopened project can make it
        waitFor("alpha must find its variant through the index",
            () -> findClasses(alphaAgain, "SharedAcrossWithA").size() == 1,
            () -> indexState(alphaAgain, shared));
    }

    private static Project openProjectUsingLibrary(HeadlessProjects projects, String name, Path libraryDirectory, Set<String> flags)
        throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-shared-across-" + name);
        Path sources = Files.createDirectories(directory.resolve("src"));
        Files.writeString(sources.resolve("Own.sand"), "class SharedAcrossOwn" + name.replace("-", "") + " {}");

        Project project = projects.open(directory);
        Module module = addContentRoot(project, name, sources);
        useLibrary(module, findFile(libraryDirectory), flags);
        awaitIdle(project);
        return project;
    }

    private static void useLibrary(Module module, VirtualFile libraryRoot, Set<String> flags) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Library library = rootModel.getModuleLibraryTable().createLibrary("shared-across");
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            libraryModel.addRoot(libraryRoot, BinariesOrderRootType.ID);
            libraryModel.commit();

            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(true);
            extension.setFlags(flags);
            rootModel.commit();
        });
    }

    /**
     * What the index holds for {@code file} and which of it {@code project} reads - reported when a wait times out.
     */
    private static String indexState(Project project, VirtualFile file) {
        return ReadAction.compute(() -> {
            StringBuilder state = new StringBuilder();
            state.append("dumb=").append(DumbService.isDumb(project));
            state.append(", stale=").append(ModuleAwareIndexMetaRecorder.isStale(StubUpdatingIndex.INDEX_ID, file, project));
            state.append(", meta=").append(ModuleAwareIndexMetaStorage.getInstance().get(StubUpdatingIndex.INDEX_ID, ((VirtualFileWithId) file).getId()));
            state.append(", remembered=").append(ModuleAwareIndexVariants.remembered(((VirtualFileWithId) file).getId()));
            state.append(", own=").append(ModuleAwareIndexMetaRecorder.ownPrimaryState(project, file));
            state.append(", allVariants(WithA)=").append(classCount(project, IndexOptionSelector.ALL_VARIANTS, "SharedAcrossWithA"));
            state.append(", allVariants(WithoutA)=").append(classCount(project, IndexOptionSelector.ALL_VARIANTS, "SharedAcrossWithoutA"));
            for (SerializedStubTree tree : FileBasedIndex.getInstance().getFileData(StubUpdatingIndex.INDEX_ID, file, project).values()) {
                state.append(", current=").append(ModuleAwareIndexVariants.currentVariant(tree, file, project));
                for (int i = 0; i < tree.getVariantCount(); i++) {
                    state.append(", variant").append(i).append('=').append(tree.getDescriptor(i));
                }
            }
            return state.toString();
        });
    }

    private static boolean isStale(Project project, VirtualFile file) {
        return ReadAction.compute(() -> ModuleAwareIndexMetaRecorder.isStale(StubUpdatingIndex.INDEX_ID, file, project));
    }

    private static int classCount(Project project, IndexOptionSelector selector, String name) {
        return ModuleAwareIndexOptions.withSelector(selector, () -> findClasses(project, name)).size();
    }

    private static List<String> classesIn(Project project, VirtualFile file) {
        return ReadAction.compute(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            List<String> names = new ArrayList<>();
            if (psiFile != null) {
                for (SandClass sandClass : PsiTreeUtil.findChildrenOfType(psiFile, SandClass.class)) {
                    names.add(sandClass.getName());
                }
            }
            return names;
        });
    }
}
