/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.codeEditor.action;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.KeyEvent;
import java.util.List;

public abstract class EditorAction extends AnAction implements DumbAware {
    private EditorActionHandler myHandler;
    private boolean myHandlersLoaded;

    protected EditorAction(EditorActionHandler defaultHandler) {
        myHandler = defaultHandler;
        setEnabledInModalContext(true);
    }

    public EditorActionHandler getHandler() {
        ensureHandlersLoaded();
        return myHandler;
    }

    public final EditorActionHandler setupHandler(@Nonnull EditorActionHandler newHandler) {
        ensureHandlersLoaded();
        EditorActionHandler tmp = myHandler;
        myHandler = newHandler;
        myHandler.setWorksInInjected(isInInjectedContext());
        return tmp;
    }

    private void ensureHandlersLoaded() {
        if (myHandlersLoaded) {
            return;
        }
        myHandlersLoaded = true;
        String id = ActionManager.getInstance().getId(this);
        List<ExtensionEditorActionHandler> extensions = Application.get().getExtensionList(ExtensionEditorActionHandler.class);
        for (int i = extensions.size() - 1; i >= 0; i--) {
            ExtensionEditorActionHandler handler = extensions.get(i);
            if (handler.getActionId().equals(id)) {
                handler.init(myHandler);
                myHandler = (EditorActionHandler)handler;
                myHandler.setWorksInInjected(isInInjectedContext());
            }
        }
    }

    @Override
    public void setInjectedContext(boolean worksInInjected) {
        super.setInjectedContext(worksInInjected);
        // we assume that this method is called in constructor at the point
        // where the chain of handlers is not initialized yet
        // and it's enough to pass the flag to the default handler only
        myHandler.setWorksInInjected(isInInjectedContext());
    }

    @RequiredUIAccess
    @Override
    public final void actionPerformed(@Nonnull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext);
        actionPerformed(editor, dataContext);
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Nullable
    protected Editor getEditor(@Nonnull DataContext dataContext) {
        return dataContext.getData(Editor.KEY);
    }

    @RequiredUIAccess
    public final void actionPerformed(Editor editor, @Nonnull DataContext dataContext) {
        if (editor == null) {
            return;
        }

        EditorActionHandler handler = getHandler();
        Runnable command = () -> handler.execute(editor, null, getProjectAwareDataContext(editor, dataContext));

        if (!handler.executeInCommand(editor, dataContext)) {
            command.run();
            return;
        }

        CommandProcessor.getInstance().newCommand()
            .project(editor.getProject())
            .document(editor.getDocument())
            .name(getTemplatePresentation().getTextValue())
            .groupId(handler.getCommandGroupId(editor))
            .run(command);
    }

    public void update(Editor editor, Presentation presentation, DataContext dataContext) {
        presentation.setEnabled(getHandler().isEnabled(editor, dataContext));
    }

    public void updateForKeyboardAccess(Editor editor, Presentation presentation, DataContext dataContext) {
        update(editor, presentation, dataContext);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        DataContext dataContext = e.getDataContext();
        Editor editor = getEditor(dataContext);
        if (editor == null) {
            presentation.setEnabled(false);
        }
        else if (e.getInputEvent() instanceof KeyEvent) {
            updateForKeyboardAccess(editor, presentation, dataContext);
        }
        else {
            update(editor, presentation, dataContext);
        }
    }

    private static DataContext getProjectAwareDataContext(final Editor editor, @Nonnull final DataContext original) {
        if (original.getData(Project.KEY) == editor.getProject()) {
            return original;
        }

        return new DataContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T getData(@Nonnull Key<T> dataId) {
                if (Project.KEY == dataId) {
                    return (T)editor.getProject();
                }
                return original.getData(dataId);
            }
        };
    }
}
