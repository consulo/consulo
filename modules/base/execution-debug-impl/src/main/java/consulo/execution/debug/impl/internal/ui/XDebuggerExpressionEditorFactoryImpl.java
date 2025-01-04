/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.debug.impl.internal.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.ui.XDebuggerExpressionEditor;
import consulo.execution.debug.ui.XDebuggerExpressionEditorFactory;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-12-09
 */
@ServiceImpl
@Singleton
public class XDebuggerExpressionEditorFactoryImpl implements XDebuggerExpressionEditorFactory {
    private final Project myProject;

    @Inject
    public XDebuggerExpressionEditorFactoryImpl(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public XDebuggerExpressionEditor create(@Nonnull XDebuggerEditorsProvider debuggerEditorsProvider, @Nullable String historyId, @Nullable XSourcePosition sourcePosition, @Nonnull XExpression text, boolean multiline, boolean editorFont, boolean showEditor) {
        return new XDebuggerExpressionEditorImpl(myProject, debuggerEditorsProvider, historyId, sourcePosition, text, multiline, editorFont);
    }
}
