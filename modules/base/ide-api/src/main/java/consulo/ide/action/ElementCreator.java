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

import consulo.application.CommonBundle;
import consulo.ide.IdeBundle;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
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

    public PsiElement[] tryCreate(@Nonnull final String inputString) {
        if (inputString.length() == 0) {
            Messages.showMessageDialog(
                myProject,
                IdeBundle.message("error.name.should.be.specified"),
                CommonBundle.getErrorTitle(),
                Messages.getErrorIcon()
            );
            return PsiElement.EMPTY_ARRAY;
        }

        Ref<List<SmartPsiElementPointer>> createdElements = Ref.create();
        Exception exception = executeCommand(getActionName(inputString), () -> {
            PsiElement[] psiElements = create(inputString);
            SmartPointerManager manager = SmartPointerManager.getInstance(myProject);
            createdElements.set(ContainerUtil.map(psiElements, manager::createSmartPsiElementPointer));
        });
        if (exception != null) {
            handleException(exception);
            return PsiElement.EMPTY_ARRAY;
        }

        return ContainerUtil.mapNotNull(createdElements.get(), SmartPsiElementPointer::getElement).toArray(PsiElement.EMPTY_ARRAY);
    }

    @Nullable
    private Exception executeCommand(String commandName, ThrowableRunnable<Exception> invokeCreate) {
        final Exception[] exception = new Exception[1];
        CommandProcessor.getInstance().executeCommand(
            myProject,
            () -> {
                LocalHistoryAction action = LocalHistory.getInstance().startAction(commandName);
                try {
                    invokeCreate.run();
                }
                catch (Exception ex) {
                    exception[0] = ex;
                }
                finally {
                    action.finish();
                }
            },
            commandName,
            null,
            UndoConfirmationPolicy.REQUEST_CONFIRMATION
        );
        return exception[0];
    }

    private void handleException(Exception t) {
        LOG.info(t);
        String errorMessage = CreateElementActionBase.filterMessage(t.getMessage());
        if (errorMessage == null || errorMessage.length() == 0) {
            errorMessage = t.toString();
        }
        Messages.showMessageDialog(myProject, errorMessage, myErrorTitle, Messages.getErrorIcon());
    }
}
