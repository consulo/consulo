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
import consulo.application.dumb.IndexNotReadyException;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.SandExtendsRef;
import consulo.sandboxPlugin.lang.psi.SandIncludeDirective;
import consulo.sandboxPlugin.lang.psi.stub.SandClassSearch;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolution under the preprocessor model: an included file is seeded with the union of its
 * includers' entry environments, so exactly one {@code Item} variant exists at a time and
 * every {@code : Item} reference resolves to it. Dropping the defining includer's
 * {@code #flag} re-seeds the included file — reparse + reindex flip which variant the
 * references see.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandContextResolveTest {
    private static final long TIMEOUT_SECONDS = 60;

    /**
     * See {@code SandStubIndexTest} — headless UI-thread VFS listeners and sand's action
     * registrations produce known unrelated errors; anything else still fails the test.
     */
    @AllowLogError({
        "consulo.virtualFileSystem.internal.BaseVirtualFileManager",
        "consulo.application.impl.internal.BaseApplication",
        "consulo.ui.ex.impl.internal.action.ActionManagerImpl"
    })
    @Test
    public void referenceResolvesToSeededVariant(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-resolve");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("some.sand"), """
            #if A
            class Item { "variant a" }
            #else
            class Item { "variant b" }
            #end
            """);
        Files.writeString(src.resolve("main1.sand"), """
            #flag A
            #include "some.sand"
            class UserA : Item {}
            """);
        Files.writeString(src.resolve("main2.sand"), """
            #include "some.sand"
            class UserB : Item {}
            """);

        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(project).isNotNull();

        VirtualFile directoryFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(directory);
        assertThat(directoryFile).isNotNull();

        ModuleManager moduleManager = ModuleManager.getInstance(project);
        WriteAction.run(() -> {
            ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
            Module module = moduleModel.newModule("main", directory.toString());
            moduleModel.commit();

            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            rootModel.addContentEntry(directoryFile);
            rootModel.commit();
        });

        DumbService dumbService = DumbService.getInstance(project);
        awaitSmart(dumbService);

        // main1 defines A: the union seed enables the first variant, and both users resolve to it
        waitFor(() -> resolvesToVariant(project, "UserA", true));
        waitFor(() -> resolvesToVariant(project, "UserB", true));

        // the include directive's file name is a reference to the included file
        waitFor(() -> ReadAction.compute(() -> {
            try {
                Collection<SandClass> users = SandClassSearch.active(project, "UserA");
                if (users.isEmpty()) {
                    return false;
                }
                PsiFile mainFile = users.iterator().next().getContainingFile();
                SandIncludeDirective include = PsiTreeUtil.findChildOfType(mainFile, SandIncludeDirective.class);
                if (include == null) {
                    return false;
                }
                PsiReference reference = include.getReference();
                PsiElement resolved = reference == null ? null : reference.resolve();
                return resolved instanceof PsiFile resolvedFile && "some.sand".equals(resolvedFile.getName());
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        }));

        // dropping the defining includer's #flag re-seeds the included file: the #else variant
        // takes over and the same references now resolve to it
        Files.writeString(src.resolve("main1.sand"), """
            #include "some.sand"
            class UserA : Item {}
            """);
        VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(src);
        assertThat(srcFile).isNotNull();
        srcFile.refresh(false, true);

        waitFor(() -> resolvesToVariant(project, "UserA", false));
        waitFor(() -> resolvesToVariant(project, "UserB", false));
    }

    /**
     * The named user's {@code : Item} reference resolves to the single existing variant, whose
     * name identifier sits at the first textual {@code Item} declaration when {@code first} is
     * true, at the second otherwise.
     */
    private static boolean resolvesToVariant(Project project, String userClass, boolean first) {
        return ReadAction.compute(() -> {
            try {
                Collection<SandClass> users = SandClassSearch.active(project, userClass);
                if (users.isEmpty()) {
                    return false;
                }
                SandClass user = users.iterator().next();
                SandExtendsRef ref = PsiTreeUtil.findChildOfType(user, SandExtendsRef.class);
                if (ref == null) {
                    return false;
                }
                PsiReference reference = ref.getReference();
                PsiElement resolved = reference == null ? null : reference.resolve();
                if (!(resolved instanceof SandClass resolvedClass)) {
                    return false;
                }
                PsiElement nameIdentifier = resolvedClass.getNameIdentifier();
                PsiFile someFile = resolvedClass.getContainingFile();
                if (nameIdentifier == null || someFile == null) {
                    return false;
                }
                String text = someFile.getText();
                int expectedOffset = first ? text.indexOf("Item") : text.lastIndexOf("Item");
                return nameIdentifier.getTextOffset() == expectedOffset;
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        });
    }

    private static void awaitSmart(DumbService dumbService) throws InterruptedException {
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).as("timed out waiting for condition").isTrue();
    }
}
