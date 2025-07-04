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

import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author peter
 */
public abstract class ElementCreator {
    private static final Logger LOG = Logger.getInstance(ElementCreator.class);

    private final Project myProject;
    private final String myErrorTitle;

    protected ElementCreator(Project project, String errorTitle) {
        myProject = project;
        myErrorTitle = errorTitle;
    }

    protected abstract PsiElement[] create(String newName) throws Exception;

    protected abstract String getActionName(String newName);

    @RequiredUIAccess
    public PsiElement[] tryCreate(@Nonnull final String inputString) {
        if (inputString.isEmpty()) {
            Messages.showMessageDialog(
                myProject,
                IdeLocalize.errorNameShouldBeSpecified().get(),
                CommonLocalize.titleError().get(),
                UIUtil.getErrorIcon()
            );
            return PsiElement.EMPTY_ARRAY;
        }

        Ref<List<SmartPsiElementPointer>> createdElements = Ref.create();
        Exception exception = executeCommand(
            LocalizeValue.ofNullable(getActionName(inputString)),
            () -> {
                PsiElement[] psiElements = create(inputString);
                SmartPointerManager manager = SmartPointerManager.getInstance(myProject);
                createdElements.set(ContainerUtil.map(psiElements, manager::createSmartPsiElementPointer));
            }
        );
        if (exception != null) {
            handleException(exception);
            return PsiElement.EMPTY_ARRAY;
        }

        return ContainerUtil.mapNotNull(createdElements.get(), SmartPsiElementPointer::getElement).toArray(PsiElement.EMPTY_ARRAY);
    }

    @Nullable
    @RequiredUIAccess
    private Exception executeCommand(@Nonnull LocalizeValue commandName, ThrowableRunnable<Exception> invokeCreate) {
        return CommandProcessor.getInstance().<Exception>newCommand()
            .project(myProject)
            .name(commandName)
            .undoConfirmationPolicy(UndoConfirmationPolicy.REQUEST_CONFIRMATION)
            .compute(() -> {
                LocalHistoryAction action = LocalHistory.getInstance().startAction(commandName);
                try {
                    invokeCreate.run();
                    return null;
                }
                catch (Exception ex) {
                    return ex;
                }
                finally {
                    action.finish();
                }
            });
    }

    @RequiredUIAccess
    private void handleException(Exception t) {
        LOG.info(t);
        String errorMessage = CreateElementActionBase.filterMessage(t.getMessage());
        if (errorMessage == null || errorMessage.length() == 0) {
            errorMessage = t.toString();
        }
        Messages.showMessageDialog(myProject, errorMessage, myErrorTitle, UIUtil.getErrorIcon());
    }
}
