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

package consulo.execution.debug;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.setting.XDebuggerSettings;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class XDebuggerUtil {
    public static XDebuggerUtil getInstance() {
        return Application.get().getInstance(XDebuggerUtil.class);
    }

    public abstract XLineBreakpointType<?>[] getLineBreakpointTypes();

    public void toggleLineBreakpoint(@Nonnull Project project, @Nonnull VirtualFile file, int line) {
        toggleLineBreakpoint(project, file, line, false);
    }

    public abstract void toggleLineBreakpoint(@Nonnull Project project, @Nonnull VirtualFile file, int line, boolean temporary);

    public abstract boolean canPutBreakpointAt(@Nonnull Project project, @Nonnull VirtualFile file, int line);

    public <P extends XBreakpointProperties> void toggleLineBreakpoint(@Nonnull Project project, @Nonnull XLineBreakpointType<P> type, @Nonnull VirtualFile file, int line) {
        toggleLineBreakpoint(project, type, file, line, false);
    }

    public abstract <P extends XBreakpointProperties> void toggleLineBreakpoint(@Nonnull Project project, @Nonnull XLineBreakpointType<P> type, @Nonnull VirtualFile file, int line, boolean temporary);

    public abstract void removeBreakpoint(Project project, XBreakpoint<?> breakpoint);

    public abstract <B extends XBreakpoint<?>> XBreakpointType<B, ?> findBreakpointType(@Nonnull Class<? extends XBreakpointType<B, ?>> typeClass);

    /**
     * Create {@link XSourcePosition} instance by line number
     *
     * @param file file
     * @param line 0-based line number
     * @return source position
     */
    @Nullable
    @Deprecated
    public XSourcePosition createPosition(@Nullable VirtualFile file, int line) {
        XSourcePositionFactory factory = Application.get().getInstance(XSourcePositionFactory.class);
        return factory.createPosition(file, line);
    }

    /**
     * Create {@link XSourcePosition} instance by line and column number
     *
     * @param file   file
     * @param line   0-based line number
     * @param column 0-based column number
     * @return source position
     */
    @Nullable
    @Deprecated
    public XSourcePosition createPosition(@Nullable VirtualFile file, int line, int column) {
        XSourcePositionFactory factory = Application.get().getInstance(XSourcePositionFactory.class);
        return factory.createPosition(file, line, column);
    }

    /**
     * Create {@link XSourcePosition} instance by line number
     *
     * @param file   file
     * @param offset offset from the beginning of file
     * @return source position
     */
    @Nullable
    @Deprecated
    public XSourcePosition createPositionByOffset(@Nullable VirtualFile file, int offset) {
        XSourcePositionFactory factory = Application.get().getInstance(XSourcePositionFactory.class);
        return factory.createPositionByOffset(file, offset);
    }

    @Nullable
    @Deprecated
    public XSourcePosition createPositionByElement(@Nullable PsiElement element) {
        XSourcePositionFactory factory = Application.get().getInstance(XSourcePositionFactory.class);
        return factory.createPositionByElement(element);
    }

    public abstract <B extends XLineBreakpoint<?>> XBreakpointGroupingRule<B, ?> getGroupingByFileRule();

    public abstract <B extends XLineBreakpoint<?>> List<XBreakpointGroupingRule<B, ?>> getGroupingByFileRuleAsList();

    public abstract <B extends XBreakpoint<?>> Comparator<B> getDefaultBreakpointComparator(XBreakpointType<B, ?> type);

    public abstract <P extends XBreakpointProperties> Comparator<XLineBreakpoint<P>> getDefaultLineBreakpointComparator();

    public abstract <T extends XDebuggerSettings<?>> T getDebuggerSettings(Class<T> aClass);

    @Nullable
    public abstract XValueContainer getValueContainer(DataContext dataContext);

    /**
     * Process all {@link PsiElement}s on the specified line
     *
     * @param project   project
     * @param document  document
     * @param line      0-based line number
     * @param processor processor
     */
    @RequiredReadAction
    public abstract void iterateLine(@Nonnull Project project, @Nonnull Document document, int line, @Nonnull Processor<PsiElement> processor);

    /**
     * Disable value lookup in specified editor
     */
    public abstract void disableValueLookup(@Nonnull Editor editor);

    @Nullable
    @RequiredReadAction
    public abstract PsiElement findContextElement(@Nonnull VirtualFile virtualFile, int offset, @Nonnull Project projectl);

    @Nullable
    @RequiredReadAction
    @Deprecated
    @DeprecationInfo("'checkXml' always ignored")
    public PsiElement findContextElement(@Nonnull VirtualFile virtualFile, int offset, @Nonnull Project project, boolean checkXml) {
        return findContextElement(virtualFile, offset, project);
    }

    @Nonnull
    public abstract XExpression createExpression(@Nonnull String text, Language language, String custom, EvaluationMode mode);

    public boolean isEmptyExpression(@Nullable XExpression expression) {
        return expression == null || StringUtil.isEmptyOrSpaces(expression.getExpression());
    }

    public void rebuildAllSessionsViews(@Nullable Project project) {
        if (project == null) {
            return;
        }
        for (XDebugSession session : XDebuggerManager.getInstance(project).getDebugSessions()) {
            if (session.isSuspended()) {
                session.rebuildViews();
            }
        }
    }
}
