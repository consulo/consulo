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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.impl.internal.XDebuggerUtilImpl;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.execution.debug.impl.internal.action.handler.XDebuggerSuspendedActionHandler;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.step.HasSetNextStatement;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.hint.HintManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.ex.action.AnActionEvent;

/**
 * @author VISTALL
 * @since 2026-11-17
 */
@ActionImpl(id = XDebuggerActions.SET_NEXT_STATEMENT)
public class SetNextStatementAction extends XDebuggerActionBase {
    private static final Logger LOG = Logger.getInstance(SetNextStatementAction.class);

    private final DebuggerActionHandler myHandler = new XDebuggerSuspendedActionHandler() {
        @Override
        protected boolean isEnabled(XDebugSession session, DataContext dataContext) {
            return super.isEnabled(session, dataContext)
                && session.getDebugProcess() instanceof HasSetNextStatement
                && XDebuggerUtilImpl.getCaretPosition(session.getProject(), dataContext) != null;
        }

        @Override
        protected void perform(XDebugSession session, DataContext dataContext) {
            XSuspendContext suspendContext = session.getSuspendContext();
            XSourcePosition position = XDebuggerUtilImpl.getCaretPosition(session.getProject(), dataContext);
            if (!(session.getDebugProcess() instanceof HasSetNextStatement hasSetNextStatement)
                || suspendContext == null
                || position == null) {
                return;
            }

            Editor contextEditor = dataContext.getData(Editor.KEY);
            Editor editor = contextEditor != null ? contextEditor : FileEditorManager.getInstance(session.getProject()).getSelectedTextEditor();
            UIAccess uiAccess = UIAccess.current();
            hasSetNextStatement.startSetNextStatement(suspendContext, position).whenComplete((message, error) -> {
                if (error != null) {
                    LOG.error(error);
                }
                else if (message.isNotEmpty() && editor != null) {
                    uiAccess.give(() -> {
                        if (!editor.isDisposed()) {
                            editor.getCaretModel().moveToOffset(position.getOffset());
                            HintManager.getInstance().showErrorHint(editor, message);
                        }
                    });
                }
            });
        }

        @Override
        public boolean isHidden(Project project, AnActionEvent event) {
            return isHiddenWithout(HasSetNextStatement.class, project, event, true);
        }
    };

    public SetNextStatementAction() {
        super(XDebuggerLocalize.actionSetNextStatementText(), XDebuggerLocalize.actionSetNextStatementDescription(), null, true);
    }

    @Override
    protected DebuggerActionHandler getHandler() {
        return myHandler;
    }
}
