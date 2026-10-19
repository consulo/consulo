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
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.stub.IndexOption;
import consulo.language.psi.stub.IndexOptionSelector;
import consulo.language.psi.stub.ModuleAwareIndexOptionProvider;
import consulo.language.psi.stub.ModuleAwareIndexOptions;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.sandboxPlugin.lang.moduleAware.SandModuleAwareIndexOptionProvider;
import consulo.sandboxPlugin.lang.moduleAware.SandOptions;
import consulo.sandboxPlugin.lang.moduleAware.SandOptionsExternalizer;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static consulo.it.index.ScanningTestSupport.addContentRoot;
import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.findFile;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A file of a library belongs to no module: it is compiled as part of every module reaching it through an order entry.
 * It is indexed under each distinct options those modules give it - modules configured alike share one variant - the
 * first module by name gives its primary variant, a reader in another module reads that module's variant, and the
 * file can be viewed as any of them compiles it.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SandLibraryContextsTest {
    @Test
    public void libraryFileIsIndexedOncePerDistinctModuleSetup(HeadlessProjects projects) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-library-contexts");
        Path alphaDirectory = Files.createDirectories(directory.resolve("alpha"));
        Path betaDirectory = Files.createDirectories(directory.resolve("beta"));
        Path gammaDirectory = Files.createDirectories(directory.resolve("gamma"));
        Files.writeString(alphaDirectory.resolve("Alpha.sand"), "class LibraryContextsAlphaOwn {}");
        Files.writeString(betaDirectory.resolve("Beta.sand"), "class LibraryContextsBetaOwn {}");
        Files.writeString(gammaDirectory.resolve("Gamma.sand"), "class LibraryContextsGammaOwn {}");

        Path libraryDirectory = Files.createTempDirectory("consulo-it-sand-library-contexts-lib");
        Files.writeString(libraryDirectory.resolve("Shared.sand"), """
            #if A
            class SharedLibraryWithA {}
            #else
            class SharedLibraryWithoutA {}
            #end
            """);

        Project project = projects.open(directory);
        Module alpha = addContentRoot(project, "alpha", alphaDirectory);
        Module beta = addContentRoot(project, "beta", betaDirectory);
        Module gamma = addContentRoot(project, "gamma", gammaDirectory);

        VirtualFile libraryRoot = findFile(libraryDirectory);
        useLibrary(alpha, libraryRoot, Set.of("A"));
        useLibrary(beta, libraryRoot, Set.of());
        useLibrary(gamma, libraryRoot, Set.of("A"));

        awaitIdle(project);

        VirtualFile shared = findFile(libraryDirectory.resolve("Shared.sand"));

        // the primary context is the first module by name - alpha, which defines A
        waitFor("the library file must be parsed under its first module's flags", () -> Set.of("A").equals(symbolsOf(project, shared)));
        waitFor("the library file must be viewed as alpha compiles it", () -> classesIn(project, shared).equals(List.of("SharedLibraryWithA")));

        // alpha and gamma compile it alike, beta differently: two variants, not three
        waitFor("each distinct setup must be indexed once", () ->
            classCount(project, IndexOptionSelector.ALL_VARIANTS, "SharedLibraryWithA") == 1
                && classCount(project, IndexOptionSelector.ALL_VARIANTS, "SharedLibraryWithoutA") == 1);

        // a reader in beta reads the variant beta compiles
        SandModuleAwareIndexOptionProvider provider = project.getApplication()
            .getExtensionPoint(ModuleAwareIndexOptionProvider.class)
            .findExtensionOrFail(SandModuleAwareIndexOptionProvider.class);
        IndexOption betaOptions = ReadAction.compute(() -> provider.getOptions(beta, shared));
        IndexOptionSelector fromBeta = (providerId, file) -> SandModuleAwareIndexOptionProvider.ID.equals(providerId) ? betaOptions : null;
        assertThat(classCount(project, fromBeta, "SharedLibraryWithoutA")).isEqualTo(1);
        assertThat(classCount(project, fromBeta, "SharedLibraryWithA")).isEqualTo(0);

        // and the file can be viewed as beta compiles it - the variant exists, so the switch is accepted
        boolean switched = ReadAction.compute(() ->
            ModuleAwareIndexOptions.setViewOptions(project, shared, Map.of(SandModuleAwareIndexOptionProvider.ID, betaOptions)));
        assertThat(switched).as("beta's variant of the library file must be stored").isTrue();
        waitFor("the viewed library file must follow beta's flags", () -> classesIn(project, shared).equals(List.of("SharedLibraryWithoutA")));
    }

    private static void useLibrary(Module module, VirtualFile libraryRoot, Set<String> flags) {
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            Library library = rootModel.getModuleLibraryTable().createLibrary("shared-sand");
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

    private static Set<String> symbolsOf(Project project, VirtualFile file) {
        return ReadAction.compute(() -> {
            SandOptions options = ModuleAwareIndexOptions.getOptions(project, file, SandModuleAwareIndexOptionProvider.ID, SandOptionsExternalizer.INSTANCE);
            return options == null ? Set.of() : options.symbols();
        });
    }

    private static int classCount(Project project, IndexOptionSelector selector, String name) {
        return ModuleAwareIndexOptions.withSelector(selector, () -> findClasses(project, name)).size();
    }

    /**
     * @return the names of the classes the file's tree holds, which is how its current variant is observed - the
     * disabled branch is not parsed at all
     */
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
