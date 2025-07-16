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
package consulo.execution.debug.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.LogicalPosition;
import consulo.execution.debug.evaluation.ValueLookupManager;
import consulo.execution.debug.impl.internal.action.handler.DebuggerActionHandler;
import consulo.execution.debug.impl.internal.action.handler.XQuickEvaluateHandler;
import consulo.execution.debug.impl.internal.evaluate.QuickEvaluateHandler;
import consulo.execution.debug.impl.internal.evaluate.ValueHintType;
import consulo.execution.debug.impl.internal.evaluate.ValueLookupManagerImpl;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
@ActionImpl(id = "QuickEvaluateExpression")
public class QuickEvaluateAction extends XDebuggerActionBase {
    public QuickEvaluateAction() {
        super(ActionLocalize.actionQuickevaluateexpressionText(), ActionLocalize.actionQuickevaluateexpressionDescription(), null, true);
    }

    @Override
    @Nonnull
    protected DebuggerActionHandler getHandler() {
        return new QuickEvaluateHandlerWrapper(XQuickEvaluateHandler.getInstance());
    }

    private static class QuickEvaluateHandlerWrapper extends DebuggerActionHandler {
        private final QuickEvaluateHandler myHandler;

        public QuickEvaluateHandlerWrapper(final QuickEvaluateHandler handler) {
            myHandler = handler;
        }

        @Override
        public void perform(@Nonnull Project project, AnActionEvent event) {
            Editor editor = event.getRequiredData(Editor.KEY);
            LogicalPosition logicalPosition = editor.getCaretModel().getLogicalPosition();
            ((ValueLookupManagerImpl) ValueLookupManager.getInstance(project))
                .showHint(myHandler, editor, editor.logicalPositionToXY(logicalPosition), ValueHintType.MOUSE_CLICK_HINT);
        }

        @Override
        public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
            return myHandler.isEnabled(project) && event.hasData(Editor.KEY) && !event.hasData(EditorGutter.KEY);
        }
    }
}
