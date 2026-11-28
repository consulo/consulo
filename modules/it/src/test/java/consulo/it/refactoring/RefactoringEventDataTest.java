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
package consulo.it.refactoring;

import consulo.application.ReadAction;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessAnswers;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.refactoring.event.RefactoringEventData;
import consulo.language.editor.refactoring.event.RefactoringEventListener;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesProcessor;
import consulo.language.editor.refactoring.rename.PsiElementRenameHandler;
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.language.editor.ui.EditorBox;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The data of {@link RefactoringEventListener#refactoringStarted}: the planned renames of a rename, the target of a move,
 * and the event fired outside the write action.
 *
 * @author VISTALL
 * @since 2026-11-15
 */
@ExtendWith(HeadlessProjectExtension.class)
public class RefactoringEventDataTest {
    private Disposable myScope;
    private final AtomicReference<@Nullable RefactoringEventData> myStartedData = new AtomicReference<>();
    private final AtomicReference<@Nullable String> myStartedId = new AtomicReference<>();
    private final AtomicBoolean myStartedInWriteAction = new AtomicBoolean();

    @BeforeEach
    public void setUp() {
        myScope = Disposable.newDisposable();
    }

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myScope);
    }

    @Test
    public void renameReportsThePlannedRenames(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/event-item.sand", "class Item {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        Project project = fixture.getProject();
        listen(project);
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(RenameDialog.class, dialog -> {
            dialog.find("newName", EditorBox.class).setValue("event-thing.sand");
            dialog.pressOk();
        });

        awaitIdle(project);
        project.getUIAccess().giveAndWaitIfNeed(() -> {
            PsiElementRenameHandler.invoke(file, project, file, null);
            return null;
        });

        waitFor("the file must be renamed", () -> "event-thing.sand".equals(virtualFile.getName()));
        answers.awaitAllAnswered();

        assertThat(myStartedId.get()).isEqualTo("refactoring.rename");
        assertThat(myStartedInWriteAction.get()).isFalse();
        RefactoringEventData data = myStartedData.get();
        assertThat(data).isNotNull();
        Map<PsiElement, String> renames = data.getUserData(RefactoringEventData.RENAMES_KEY);
        assertThat(renames).containsEntry(file, "event-thing.sand");
        assertThat(renames.keySet().iterator().next()).isSameAs(file);
    }

    @Test
    public void moveReportsTheTargetDirectory(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/event-moved.sand", "class Moved {}\n");
        fixture.addFileToProject("src/target/keep.sand", "class Keep {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        Project project = fixture.getProject();
        listen(project);
        PsiDirectory target = ReadAction.compute(() -> PsiManager.getInstance(project).findDirectory(fixture.getProjectRoot().findFileByRelativePath("src/target")));

        awaitIdle(project);
        project.getUIAccess().giveAndWaitIfNeed(() -> {
            new MoveFilesOrDirectoriesProcessor(project, new PsiElement[]{file}, target, false, false, false, null, null).run();
            return null;
        });

        waitFor("the file must be moved", () -> "target".equals(virtualFile.getParent().getName()));

        assertThat(myStartedId.get()).isEqualTo("refactoring.move");
        assertThat(myStartedInWriteAction.get()).isFalse();
        RefactoringEventData data = myStartedData.get();
        assertThat(data).isNotNull();
        assertThat(data.getUserData(RefactoringEventData.TARGET_ELEMENT_KEY)).isSameAs(target);
        assertThat(data.getUserData(RefactoringEventData.PSI_ELEMENT_ARRAY_KEY)).containsExactly(file);
    }

    private void listen(Project project) {
        project.getMessageBus().connect(myScope).subscribe(RefactoringEventListener.class, new RefactoringEventListener() {
            @Override
            public void refactoringStarted(String refactoringId, @Nullable RefactoringEventData beforeData) {
                myStartedId.set(refactoringId);
                myStartedData.set(beforeData);
                myStartedInWriteAction.set(project.getApplication().isWriteAccessAllowed());
            }

            @Override
            public void refactoringDone(String refactoringId, @Nullable RefactoringEventData afterData) {
            }

            @Override
            public void conflictsDetected(String refactoringId, RefactoringEventData conflictsData) {
            }

            @Override
            public void undoRefactoring(String refactoringId) {
            }
        });
    }
}
