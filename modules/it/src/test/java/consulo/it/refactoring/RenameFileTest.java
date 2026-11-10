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
import consulo.it.HeadlessDialog;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.rename.PsiElementRenameHandler;
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.language.editor.ui.EditorBox;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.function.Consumer;

import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rename of a file through its dialog, answered through {@link HeadlessAnswers}.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtendWith(HeadlessProjectExtension.class)
public class RenameFileTest {
    private Disposable myScope;

    @BeforeEach
    public void setUp() {
        myScope = Disposable.newDisposable();
    }

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myScope);
    }

    @Test
    public void renameUpdatesTheIncludes(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(RenameDialog.class, newName("thing.sand"));

        rename(fixture.getProject(), file);

        waitFor("the file must be renamed", () -> "thing.sand".equals(virtualFile.getName()));
        waitFor("the include must follow the rename", () -> ReadAction.compute(user::getText).contains("#include \"thing.sand\""));
        answers.awaitAllAnswered();
    }

    @Test
    public void renameWithoutReferencesKeepsTheIncludes(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(RenameDialog.class, dialog -> {
            dialog.find("searchForReferences", CheckBox.class).setValue(false);
            newName("thing.sand").accept(dialog);
        });

        RefactoringSettings settings = RefactoringSettings.getInstance();
        boolean searchForReferences = settings.RENAME_SEARCH_FOR_REFERENCES_FOR_FILE;
        try {
            rename(fixture.getProject(), file);

            waitFor("the file must be renamed", () -> "thing.sand".equals(virtualFile.getName()));
            awaitIdle(fixture.getProject());
            assertThat(ReadAction.compute(user::getText)).contains("#include \"item.sand\"");
            assertThat(settings.RENAME_SEARCH_FOR_REFERENCES_FOR_FILE).isFalse();
            answers.awaitAllAnswered();
        }
        finally {
            settings.RENAME_SEARCH_FOR_REFERENCES_FOR_FILE = searchForReferences;
        }
    }

    @Test
    public void cancelKeepsTheName(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(RenameDialog.class, dialog -> {
            dialog.find("newName", EditorBox.class).setValue("thing.sand");
            dialog.pressCancel();
        });

        rename(fixture.getProject(), file);

        answers.awaitAllAnswered();
        awaitIdle(fixture.getProject());
        assertThat(file.getVirtualFile().getName()).isEqualTo("item.sand");
    }

    @Test
    public void unchangedNameKeepsOkDisabled(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        boolean[] okEnabled = {true};
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(RenameDialog.class, dialog -> {
            dialog.find("newName", EditorBox.class).setValue("item.sand");
            okEnabled[0] = dialog.getDescriptor().doUpdateOkButtonState();
            dialog.pressCancel();
        });

        rename(fixture.getProject(), file);

        answers.awaitAllAnswered();
        awaitIdle(fixture.getProject());
        assertThat(okEnabled[0]).isFalse();
    }

    private static Consumer<HeadlessDialog> newName(String name) {
        return dialog -> {
            dialog.find("newName", EditorBox.class).setValue(name);
            dialog.pressOk();
        };
    }

    private static void rename(Project project, PsiFile file) throws Exception {
        awaitIdle(project);
        project.getUIAccess().giveAndWaitIfNeed(() -> {
            PsiElementRenameHandler.invoke(file, project, file, null);
            return null;
        });
    }
}
