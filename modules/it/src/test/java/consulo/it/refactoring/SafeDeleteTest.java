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
import consulo.it.HeadlessDialog;
import consulo.it.HeadlessProjectExtension;
import consulo.it.HeadlessAnswers;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteDialogDescriptor;
import consulo.language.editor.refactoring.safeDelete.SafeDeleteHandler;
import consulo.language.editor.refactoring.safeDelete.UnsafeUsagesDialogDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandClass;
import consulo.ui.CheckBox;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;

import static consulo.it.index.ScanningTestSupport.awaitIdle;
import static consulo.it.index.ScanningTestSupport.findClasses;
import static consulo.it.index.ScanningTestSupport.waitFor;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safe Delete end to end: the dialogs are answered through {@link HeadlessAnswers}.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtendWith(HeadlessProjectExtension.class)
public class SafeDeleteTest {
    private static final String SAFE_DELETE = SafeDeleteDialogDescriptor.class.getName();
    private static final String UNSAFE_USAGES = UnsafeUsagesDialogDescriptor.class.getName();

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
    public void unusedFileIsDeleted(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/lonely.sand", "class Lonely {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(SAFE_DELETE, dialog -> {
            dialog.find("searchInComments", CheckBox.class).setValue(false);
            dialog.pressOk();
        });

        safeDelete(fixture.getProject(), file);

        waitFor("the unused file must be deleted", () -> !virtualFile.isValid());
        assertThat(answers.shown()).containsExactly(SAFE_DELETE);
        answers.awaitAllAnswered();
    }

    @Test
    public void cancelKeepsTheFile(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/lonely.sand", "class Lonely {}\n");
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(SAFE_DELETE, HeadlessDialog::pressCancel);

        safeDelete(fixture.getProject(), file);

        answers.awaitAllAnswered();
        awaitIdle(fixture.getProject());
        assertThat(file.getVirtualFile().isValid()).isTrue();
    }

    @Test
    public void usedFileAsksAndCancelKeepsIt(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/some.sand", "class Item {}\n");
        fixture.addFileToProject("src/main.sand", "#include \"some.sand\"\nclass User : Item {}\n");
        HeadlessAnswers answers = HeadlessAnswers.of(myScope)
            .dialog(SAFE_DELETE, HeadlessDialog::pressOk)
            .dialog(UNSAFE_USAGES, HeadlessDialog::pressCancel);

        safeDelete(fixture.getProject(), file);

        answers.awaitAllAnswered();
        awaitIdle(fixture.getProject());
        assertThat(answers.shown()).containsExactly(SAFE_DELETE, UNSAFE_USAGES);
        assertThat(file.getVirtualFile().isValid()).isTrue();
    }

    @Test
    public void usedFileIsDeletedAnyway(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/some.sand", "class Item {}\n");
        fixture.addFileToProject("src/main.sand", "#include \"some.sand\"\nclass User : Item {}\n");
        VirtualFile virtualFile = file.getVirtualFile();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope)
            .dialog(SAFE_DELETE, HeadlessDialog::pressOk)
            .dialog(UNSAFE_USAGES, HeadlessDialog::pressOk);

        safeDelete(fixture.getProject(), file);

        waitFor("the file must be deleted anyway", () -> !virtualFile.isValid());
        assertThat(answers.shown()).containsExactly(SAFE_DELETE, UNSAFE_USAGES);
        answers.awaitAllAnswered();
    }

    @Test
    public void unusedFilesAreDeletedTogether(CodeInsightTestFixture fixture) throws Exception {
        VirtualFile first = fixture.addFileToProject("src/first.sand", "class First {}\n").getVirtualFile();
        VirtualFile second = fixture.addFileToProject("src/second.sand", "class Second {}\n").getVirtualFile();
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(SAFE_DELETE, HeadlessDialog::pressOk);

        safeDelete(project, psiFile(project, first), psiFile(project, second));

        waitFor("both files must be deleted", () -> !first.isValid() && !second.isValid());
        assertThat(answers.shown()).containsExactly(SAFE_DELETE);
        answers.awaitAllAnswered();
    }

    @Test
    public void unusedClassIsDeletedFromItsFile(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/pair.sand", "class Kept {}\nclass Gone {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(SAFE_DELETE, HeadlessDialog::pressOk);

        safeDelete(project, singleClass(project, "Gone"));

        waitFor("the class must be deleted", () -> !ReadAction.compute(file::getText).contains("Gone"));
        assertThat(ReadAction.compute(file::getText)).contains("class Kept");
        assertThat(file.getVirtualFile().isValid()).isTrue();
        answers.awaitAllAnswered();
    }

    @Test
    public void usedClassAsksAndCancelKeepsIt(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope)
            .dialog(SAFE_DELETE, HeadlessDialog::pressOk)
            .dialog(UNSAFE_USAGES, HeadlessDialog::pressCancel);

        safeDelete(project, singleClass(project, "Item"));

        answers.awaitAllAnswered();
        awaitIdle(project);
        assertThat(ReadAction.compute(file::getText)).contains("class Item");
    }

    @Test
    public void usedClassIsDeletedAnyway(CodeInsightTestFixture fixture) throws Exception {
        PsiFile file = fixture.addFileToProject("src/item.sand", "class Item {}\nclass Other {}\n");
        fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope)
            .dialog(SAFE_DELETE, HeadlessDialog::pressOk)
            .dialog(UNSAFE_USAGES, HeadlessDialog::pressOk);

        safeDelete(project, singleClass(project, "Item"));

        waitFor("the used class must be deleted anyway", () -> !ReadAction.compute(file::getText).contains("Item"));
        assertThat(answers.shown()).containsExactly(SAFE_DELETE, UNSAFE_USAGES);
        answers.awaitAllAnswered();
    }

    private static PsiFile psiFile(Project project, VirtualFile file) {
        PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
        assertThat(psiFile).isNotNull();
        return psiFile;
    }

    private static SandClass singleClass(Project project, String name) throws Exception {
        awaitIdle(project);
        Collection<SandClass> classes = findClasses(project, name);
        assertThat(classes).hasSize(1);
        return classes.iterator().next();
    }

    private static void safeDelete(Project project, PsiElement... elements) throws Exception {
        awaitIdle(project);
        project.getUIAccess().giveAndWaitIfNeed(() -> {
            SafeDeleteHandler.invoke(project, elements, true);
            return null;
        });
    }
}
