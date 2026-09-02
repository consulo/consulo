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
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.application.internal.TransferredWriteActionService;
import consulo.undoRedo.CommandProcessor;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
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
 * Simulates the editor path headlessly under the preprocessor model: a loaded document over
 * an indexed file forces the stub-to-AST reconciliation exactly like an opened editor does
 * (only the enabled variant exists; the disabled one is inert non-tokens), then unsaved edits
 * go through the in-memory document indexing path.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SandEditorPsiTest {
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
    public void documentBackedPsiBindsVariantsCorrectly(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-sand-editor");
        Path src = directory.resolve("src");
        Files.createDirectories(src);
        Files.writeString(src.resolve("some.sand"), """
            #if A
            class Item { "variant a" }
            #else
            class Item { "variant b" }
            #end
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
        waitFor(() -> variantCount(project, "Item") == 1);

        VirtualFile file = directoryFile.findFileByRelativePath("src/some.sand");
        assertThat(file).isNotNull();
        Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(file));
        assertThat(document).isNotNull();

        // document loaded = the editor situation; only the enabled variant exists and the
        // stub-based index element must reconcile onto that AST declaration
        assertThat(ReadAction.compute(() -> checkBinding(project, file))).as("variant binding with loaded document").isTrue();

        // unsaved edit goes through the in-memory document indexing path. Production
        // discipline: write authority acquired off the UI thread, the mutation itself
        // transferred onto the UI thread (document listeners assert it). The change is
        // committed by the asynchronous commit pipeline alone, exactly like an editor
        // keystroke - an explicit commitDocument from the test thread would race the
        // async finish (production serializes both on the UI thread)
        WriteAction.run(() -> application.getInstance(TransferredWriteActionService.class).runOnEdtWithTransferredWriteActionAndWait(
            () -> CommandProcessor.getInstance().runUndoTransparentAction(
                () -> document.insertString(document.getTextLength(), "\nclass Extra {}\n"))));
        waitFor(() -> ReadAction.compute(() -> PsiDocumentManager.getInstance(project).isCommitted(document)));

        waitFor(() -> variantCount(project, "Extra") == 1);
        assertThat(ReadAction.compute(() -> checkBinding(project, file))).as("variant binding after unsaved edit").isTrue();

        WriteAction.run(() -> application.getInstance(TransferredWriteActionService.class).runOnEdtWithTransferredWriteActionAndWait(
            () -> FileDocumentManager.getInstance().saveDocument(document)));

        waitFor(() -> variantCount(project, "Extra") == 1);
        assertThat(ReadAction.compute(() -> checkBinding(project, file))).as("variant binding after save").isTrue();
    }

    private static boolean checkBinding(Project project, VirtualFile file) {
        try {
            Collection<SandClass> items = SandClassSearch.allVariants(project, "Item");
            if (items.size() != 1) {
                return false;
            }
            SandClass item = items.iterator().next();
            PsiElement name = item.getNameIdentifier();
            if (name == null) {
                return false;
            }

            PsiFile psiFile = item.getContainingFile();
            String text = psiFile.getText();
            if (name.getTextOffset() != text.lastIndexOf("Item")) {
                return false;
            }
            PsiElement atName = psiFile.findElementAt(name.getTextOffset());
            if (atName == null || !item.equals(atName.getParent())) {
                return false;
            }

            int hiddenOffset = text.indexOf("Item");
            PsiElement atHidden = psiFile.findElementAt(hiddenOffset);
            return atHidden == null || PsiTreeUtil.getParentOfType(atHidden, SandClass.class) == null;
        }
        catch (IndexNotReadyException e) {
            return false;
        }
    }

    private static int variantCount(Project project, String name) {
        return ReadAction.compute(() -> {
            try {
                return SandClassSearch.allVariants(project, name).size();
            }
            catch (IndexNotReadyException e) {
                return -1;
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
