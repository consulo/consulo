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
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
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
import consulo.sandboxPlugin.ide.module.extension.SandMutableModuleExtension;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.sandboxPlugin.lang.psi.stub.SandClassSearch;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The disabled-block contract, C#-preprocessor style: a branch whose guard fails the file's
 * seeded flag environment is <b>not parsed</b> — its tokens are inert (comment/whitespace
 * category), it produces no PSI declarations, no stubs, no index entries and no error
 * elements even when it contains garbage. Exactly one variant of a declaration exists at a
 * time; changing the environment (module flags, includer directives) re-seeds the file and
 * flips which branch exists via reparse + reindex.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandDisabledBlockTest {
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
    public void disabledBlockIsNotParsed(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-disabled");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("some.sand"), """
            #if A
            class Hidden { "h" } %%%
            #else
            class Visible { "v" }
            #end
            """);

        Project project = openProjectWithModule(application, projectManager, directory);

        // default environment has no A: the #if branch must not exist - no PSI class, no
        // index entry, and its garbage must produce no error elements
        waitFor(() -> onlyClassIs(project, "Visible", "Hidden"), () -> describeOnlyClassIs(project, "Visible", "Hidden"));

        Module module = ModuleManager.getInstance(project).findModuleByName("main");
        assertThat(module).isNotNull();
        WriteAction.run(() -> {
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            SandMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(SandMutableModuleExtension.class);
            assertThat(extension).isNotNull();
            extension.setEnabled(true);
            extension.setFlags(Set.of("A"));
            rootModel.commit();
        });

        // flag flips the seed: the file re-parses and re-indexes; now only the #if branch
        // exists and the enabled-side garbage still degrades without error elements
        waitFor(() -> onlyClassIs(project, "Hidden", "Visible"), () -> describeOnlyClassIs(project, "Hidden", "Visible"));
    }

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
    public void includerSeedsEnabledBranch(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-disabled-inc");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("main.sand"), """
            #flag A
            #include "some.sand"
            class User : Item {}
            """);
        Files.writeString(src.resolve("some.sand"), """
            #if A
            class Item { "variant a" }
            #else
            class Item { "variant b" }
            #end
            """);

        Project project = openProjectWithModule(application, projectManager, directory);

        // the includer defines A: the included file is seeded with it, so only the first
        // (physically earlier) variant exists
        waitFor(() -> singleItemAt(project, true));

        // dropping the includer's #flag re-seeds the included file: reparse + reindex flip
        // the existing variant to the #else one - the second declaration in the text
        Files.writeString(src.resolve("main.sand"), """
            #include "some.sand"
            class User : Item {}
            """);
        VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(src);
        assertThat(srcFile).isNotNull();
        srcFile.refresh(false, true);

        waitFor(() -> singleItemAt(project, false));
    }

    /**
     * One read action asserts the whole disabled-block contract for a two-branch file:
     * exactly one class named {@code expected} exists in PSI and index, {@code absent} exists
     * in neither, the absent name's text offset is not covered by any {@link SandClass}, and
     * the file carries no {@link PsiErrorElement}.
     */
    private static boolean onlyClassIs(Project project, String expected, String absent) {
        return describeOnlyClassIs(project, expected, absent) == null;
    }

    private static @Nullable String describeOnlyClassIs(Project project, String expected, String absent) {
        return ReadAction.compute(() -> {
            try {
                int expectedCount = SandClassSearch.allVariants(project, expected).size();
                if (expectedCount != 1) {
                    return "indexed " + expected + " count=" + expectedCount;
                }
                int absentCount = SandClassSearch.allVariants(project, absent).size();
                if (absentCount != 0) {
                    return "indexed " + absent + " count=" + absentCount;
                }

                PsiFile psiFile = findSomeFile(project, expected);
                if (psiFile == null) {
                    return "no psi file for " + expected;
                }

                Collection<SandClass> classes = PsiTreeUtil.findChildrenOfType(psiFile, SandClass.class);
                if (classes.size() != 1 || !expected.equals(classes.iterator().next().getName())) {
                    return "psi classes=" + classes.size();
                }

                int absentOffset = psiFile.getText().indexOf(absent);
                if (absentOffset < 0) {
                    return "text lacks " + absent;
                }
                PsiElement atAbsent = psiFile.findElementAt(absentOffset);
                if (atAbsent != null && PsiTreeUtil.getParentOfType(atAbsent, SandClass.class) != null) {
                    return "absent name covered by SandClass";
                }

                return PsiTreeUtil.findChildOfType(psiFile, PsiErrorElement.class) == null ? null : "error elements present";
            }
            catch (IndexNotReadyException e) {
                return "index not ready";
            }
        });
    }

    /**
     * Exactly one {@code Item} exists, and its name identifier sits at the first textual
     * {@code class Item} declaration when {@code first} is true, at the second otherwise.
     */
    private static boolean singleItemAt(Project project, boolean first) {
        return ReadAction.compute(() -> {
            try {
                Collection<SandClass> items = SandClassSearch.allVariants(project, "Item");
                if (items.size() != 1) {
                    return false;
                }
                SandClass item = items.iterator().next();
                PsiFile psiFile = item.getContainingFile();
                PsiElement nameIdentifier = item.getNameIdentifier();
                if (psiFile == null || nameIdentifier == null) {
                    return false;
                }

                String text = psiFile.getText();
                int expectedOffset = first ? text.indexOf("Item") : text.lastIndexOf("Item");
                return nameIdentifier.getTextOffset() == expectedOffset;
            }
            catch (IndexNotReadyException e) {
                return false;
            }
        });
    }

    private static @Nullable PsiFile findSomeFile(Project project, String className) {
        Collection<SandClass> classes = SandClassSearch.allVariants(project, className);
        if (classes.isEmpty()) {
            return null;
        }
        return classes.iterator().next().getContainingFile();
    }

    private static Project openProjectWithModule(Application application, ProjectManager projectManager, Path directory) throws Exception {
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
        CountDownLatch smart = new CountDownLatch(1);
        dumbService.runWhenSmart(smart::countDown);
        assertThat(smart.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("project must reach smart mode").isTrue();

        return project;
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        waitFor(condition, () -> "condition false");
    }

    private static void waitFor(BooleanSupplier condition, java.util.function.Supplier<String> describe) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).as("timed out: " + describe.get()).isTrue();
    }
}
