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
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.CodeInsightTestFixture;
import consulo.it.HeadlessAnswers;
import consulo.it.HeadlessDialog;
import consulo.it.HeadlessProjectExtension;
import consulo.language.editor.refactoring.action.RefactoringActionHandlerFactory;
import consulo.language.editor.refactoring.ui.ConflictsDialogDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandClass;
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
 * Inline of a sand class into its subclasses; the conflicts dialog is answered through {@link HeadlessAnswers}.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtendWith(HeadlessProjectExtension.class)
public class InlineClassTest {
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
    public void subclassesExtendTheInlinedParent(CodeInsightTestFixture fixture) throws Exception {
        PsiFile item = fixture.addFileToProject("src/item.sand", "class Base {}\nclass Item : Base {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope);

        inline(project, singleClass(project, "Item"));

        waitFor("the class must be inlined", () -> !text(item).contains("Item"));
        assertThat(text(user)).contains("class User : Base {}");
        assertThat(text(item)).contains("class Base {}");
        awaitIdle(project);
        assertThat(answers.shown()).isEmpty();
        answers.awaitAllAnswered();
    }

    @Test
    public void inliningARootClassDropsTheExtends(CodeInsightTestFixture fixture) throws Exception {
        PsiFile item = fixture.addFileToProject("src/item.sand", "class Item {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "#include \"item.sand\"\nclass User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope);

        inline(project, singleClass(project, "Item"));

        waitFor("the class must be inlined", () -> !text(item).contains("Item"));
        assertThat(text(user)).contains("class User {}");
        answers.awaitAllAnswered();
    }

    @Test
    public void conflictCancelKeepsEverything(CodeInsightTestFixture fixture) throws Exception {
        PsiFile item = fixture.addFileToProject("src/item.sand", "class Item : User {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "class User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(ConflictsDialogDescriptor.class, HeadlessDialog::pressCancel);

        inline(project, singleClass(project, "Item"));

        answers.awaitAllAnswered();
        awaitIdle(project);
        assertThat(text(item)).isEqualTo("class Item : User {}\n");
        assertThat(text(user)).isEqualTo("class User : Item {}\n");
        assertThat(answers.shown()).containsExactly(ConflictsDialogDescriptor.class.getName());
    }

    @Test
    public void conflictContinueInlines(CodeInsightTestFixture fixture) throws Exception {
        PsiFile item = fixture.addFileToProject("src/item.sand", "class Item : User {}\n");
        PsiFile user = fixture.addFileToProject("src/user.sand", "class User : Item {}\n");
        Project project = fixture.getProject();
        HeadlessAnswers answers = HeadlessAnswers.of(myScope).dialog(ConflictsDialogDescriptor.class, HeadlessDialog::pressOk);

        inline(project, singleClass(project, "Item"));

        waitFor("the class must be inlined", () -> !text(item).contains("Item"));
        assertThat(text(user)).contains("class User : User {}");
        answers.awaitAllAnswered();
    }

    private static String text(PsiFile file) {
        return ReadAction.compute(file::getText);
    }

    private static SandClass singleClass(Project project, String name) throws Exception {
        awaitIdle(project);
        Collection<SandClass> classes = findClasses(project, name);
        assertThat(classes).hasSize(1);
        return classes.iterator().next();
    }

    private static void inline(Project project, PsiElement element) throws Exception {
        awaitIdle(project);
        project.getUIAccess().giveAndWaitIfNeed(() -> {
            RefactoringActionHandlerFactory.getInstance().createInlineHandler().invoke(project, new PsiElement[]{element}, DataContext.EMPTY_CONTEXT);
            return null;
        });
    }
}
