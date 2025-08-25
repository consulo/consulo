/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.action;

import consulo.ide.IdeView;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * The base class for actions which create new file elements.
 */
public abstract class CreateElementActionBase extends CreateInDirectoryActionBase {
    protected CreateElementActionBase() {
    }

    protected CreateElementActionBase(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected CreateElementActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected CreateElementActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected CreateElementActionBase(@Nullable String text, @Nullable String description, @Nullable Image icon) {
        super(text, description, icon);
    }

    protected abstract void invokeDialog(
        @Nonnull Project project,
        PsiDirectory directory,
        @Nonnull Consumer<PsiElement[]> elementsConsumer
    );

    /**
     * @return created elements. Never null.
     */
    @Nonnull
    @RequiredUIAccess
    protected abstract PsiElement[] create(String newName, PsiDirectory directory) throws Exception;

    protected abstract LocalizeValue getErrorTitle();

    protected abstract LocalizeValue getCommandName();

    protected abstract LocalizeValue getActionName(PsiDirectory directory, String newName);

    @Override
    @RequiredUIAccess
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        IdeView view = e.getData(IdeView.KEY);
        if (view == null) {
            return;
        }

        Project project = e.getRequiredData(Project.KEY);

        PsiDirectory dir = view.getOrChooseDirectory();
        if (dir == null) {
            return;
        }

        invokeDialog(project, dir, elements -> {
            for (PsiElement createdElement : elements) {
                view.selectElement(createdElement);
            }
        });
    }

    public static String filterMessage(String message) {
        if (message == null) {
            return null;
        }
        String ioExceptionPrefix = "java.io.IOException:";
        message = StringUtil.trimStart(message, ioExceptionPrefix);
        return message;
    }

    protected class MyInputValidator extends ElementCreator implements InputValidator {
        private final PsiDirectory myDirectory;
        private PsiElement[] myCreatedElements = PsiElement.EMPTY_ARRAY;

        public MyInputValidator(Project project, PsiDirectory directory) {
            super(project, getErrorTitle());
            myDirectory = directory;
        }

        public PsiDirectory getDirectory() {
            return myDirectory;
        }

        @RequiredUIAccess
        @Override
        public boolean checkInput(String inputString) {
            return true;
        }

        @Override
        @RequiredUIAccess
        public PsiElement[] create(String newName) throws Exception {
            return CreateElementActionBase.this.create(newName, myDirectory);
        }

        @Override
        public LocalizeValue getActionName(String newName) {
            return CreateElementActionBase.this.getActionName(myDirectory, newName);
        }

        @RequiredUIAccess
        @Override
        public boolean canClose(String inputString) {
            myCreatedElements = tryCreate(inputString);
            return myCreatedElements.length > 0;
        }

        public final PsiElement[] getCreatedElements() {
            return myCreatedElements;
        }
    }
}
