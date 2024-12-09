/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.action.handler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.document.util.TextRange;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.evaluation.ExpressionInfo;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.impl.internal.evaluate.AbstractValueHint;
import consulo.execution.debug.impl.internal.evaluate.QuickEvaluateHandler;
import consulo.execution.debug.impl.internal.evaluate.ValueHintType;
import consulo.execution.debug.impl.internal.evaluate.XValueHint;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.awt.*;

/**
 * @author nik
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class XQuickEvaluateHandler extends QuickEvaluateHandler {
    private static final Logger LOG = Logger.getInstance(XQuickEvaluateHandler.class);

    public static XQuickEvaluateHandler getInstance() {
        return Application.get().getInstance(XQuickEvaluateHandler.class);
    }

    @Override
    public boolean isEnabled(@Nonnull final Project project) {
        XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
        return session != null && session.getDebugProcess().getEvaluator() != null;
    }

    @Override
    public AbstractValueHint createValueHint(@Nonnull final Project project, @Nonnull final Editor editor, @Nonnull final Point point, final ValueHintType type) {
        final XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
        if (session == null) {
            return null;
        }

        final XDebuggerEvaluator evaluator = session.getDebugProcess().getEvaluator();
        if (evaluator == null) {
            return null;
        }

        return PsiDocumentManager.getInstance(project).commitAndRunReadAction(new Computable<XValueHint>() {
            @Override
            public XValueHint compute() {
                int offset = AbstractValueHint.calculateOffset(editor, point);
                ExpressionInfo expressionInfo = getExpressionInfo(evaluator, project, type, editor, offset);
                if (expressionInfo == null) {
                    return null;
                }

                int textLength = editor.getDocument().getTextLength();
                TextRange range = expressionInfo.getTextRange();
                if (range.getStartOffset() > range.getEndOffset() || range.getStartOffset() < 0 || range.getEndOffset() > textLength) {
                    LOG.error("invalid range: " + range + ", text length = " + textLength + ", evaluator: " + evaluator);
                    return null;
                }

                return new XValueHint(project, editor, point, type, expressionInfo, evaluator, session);
            }
        });
    }

    @Nullable
    private static ExpressionInfo getExpressionInfo(final XDebuggerEvaluator evaluator,
                                                    final Project project,
                                                    final ValueHintType type,
                                                    final Editor editor,
                                                    final int offset) {
        SelectionModel selectionModel = editor.getSelectionModel();
        int selectionStart = selectionModel.getSelectionStart();
        int selectionEnd = selectionModel.getSelectionEnd();
        if ((type == ValueHintType.MOUSE_CLICK_HINT || type == ValueHintType.MOUSE_ALT_OVER_HINT) &&
            selectionModel.hasSelection() &&
            selectionStart <= offset &&
            offset <= selectionEnd) {
            return new ExpressionInfo(new TextRange(selectionStart, selectionEnd));
        }
        return evaluator.getExpressionInfoAtOffset(project, editor.getDocument(), offset,
            type == ValueHintType.MOUSE_CLICK_HINT || type == ValueHintType.MOUSE_ALT_OVER_HINT);
    }

    @Override
    public boolean canShowHint(@Nonnull final Project project) {
        return isEnabled(project);
    }

    @Override
    public int getValueLookupDelay(final Project project) {
        return XDebuggerSettingsManager.getInstance().getDataViewSettings().getValueLookupDelay();
    }
}
