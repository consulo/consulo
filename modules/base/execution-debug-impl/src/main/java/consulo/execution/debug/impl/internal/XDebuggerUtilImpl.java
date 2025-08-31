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
package consulo.execution.debug.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.WriteAction;
import consulo.application.util.function.Processor;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.*;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XExecutionStack;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XSuspendContext;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.group.XBreakpointFileGroupingRule;
import consulo.execution.debug.impl.internal.evaluate.ValueLookupManagerImpl;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.action.XDebuggerTreeActionBase;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.execution.debug.setting.XDebuggerSettings;
import consulo.execution.debug.ui.DebuggerColors;
import consulo.fileEditor.FileEditorManager;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.*;

/**
 * @author nik
 */
@Singleton
@ServiceImpl
public class XDebuggerUtilImpl extends XDebuggerUtil {
    private XLineBreakpointType<?>[] myLineBreakpointTypes;
    private Map<Class<? extends XBreakpointType>, XBreakpointType<?, ?>> myBreakpointTypeByClass;

    @Override
    public XLineBreakpointType<?>[] getLineBreakpointTypes() {
        if (myLineBreakpointTypes == null) {
            List<XBreakpointType> types = XBreakpointUtil.getBreakpointTypes();
            List<XLineBreakpointType<?>> lineBreakpointTypes = new ArrayList<>();
            for (XBreakpointType type : types) {
                if (type instanceof XLineBreakpointType<?> xLineBreakpointType) {
                    lineBreakpointTypes.add(xLineBreakpointType);
                }
            }
            myLineBreakpointTypes = lineBreakpointTypes.toArray(new XLineBreakpointType<?>[lineBreakpointTypes.size()]);
        }
        return myLineBreakpointTypes;
    }

    @Override
    public void toggleLineBreakpoint(@Nonnull Project project, @Nonnull VirtualFile file, int line, boolean temporary) {
        XLineBreakpointType<?> breakpointType = XLineBreakpointTypeResolver.forFile(project, file, line);
        if (breakpointType != null) {
            toggleLineBreakpoint(project, breakpointType, file, line, temporary);
        }
    }

    @Override
    @RequiredReadAction
    public boolean canPutBreakpointAt(@Nonnull Project project, @Nonnull VirtualFile file, int line) {
        return XLineBreakpointTypeResolver.forFile(project, file, line) != null;
    }

    @Override
    public <P extends XBreakpointProperties> void toggleLineBreakpoint(
        @Nonnull Project project,
        @Nonnull XLineBreakpointType<P> type,
        @Nonnull VirtualFile file,
        int line,
        boolean temporary
    ) {
        XSourcePositionImpl position = XSourcePositionImpl.create(file, line);
        if (position != null) {
            toggleAndReturnLineBreakpoint(project, type, position, temporary, null);
        }
    }

    @Nonnull
    public static <P extends XBreakpointProperties> AsyncResult<XLineBreakpoint> toggleAndReturnLineBreakpoint(
        @Nonnull final Project project,
        @Nonnull final XLineBreakpointType<P> type,
        @Nonnull XSourcePosition position,
        final boolean temporary,
        @Nullable final Editor editor
    ) {
        return WriteAction.compute(() -> {
            final VirtualFile file = position.getFile();
            final int line = position.getLine();
            final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
            XLineBreakpoint<P> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
            if (breakpoint != null) {
                breakpointManager.removeBreakpoint(breakpoint);
            }
            else {
                List<? extends XLineBreakpointType<P>.XLineBreakpointVariant> variants = type.computeVariants(project, position);
                if (!variants.isEmpty() && editor != null) {
                    RelativePoint relativePoint = DebuggerUIImplUtil.getPositionForPopup(editor, line);
                    if (variants.size() > 1 && relativePoint != null) {
                        final AsyncResult<XLineBreakpoint> res = new AsyncResult<>();
                        class MySelectionListener implements ListSelectionListener {
                            RangeHighlighter myHighlighter = null;

                            @Override
                            public void valueChanged(ListSelectionEvent e) {
                                if (!e.getValueIsAdjusting()) {
                                    clearHighlighter();
                                    Object value = ((JList) e.getSource()).getSelectedValue();
                                    if (value instanceof XLineBreakpointType.XLineBreakpointVariant) {
                                        TextRange range = ((XLineBreakpointType.XLineBreakpointVariant) value).getHighlightRange();
                                        TextRange lineRange = DocumentUtil.getLineTextRange(editor.getDocument(), line);
                                        if (range != null) {
                                            range = range.intersection(lineRange);
                                        }
                                        else {
                                            range = lineRange;
                                        }
                                        if (range != null && !range.isEmpty()) {
                                            myHighlighter = editor.getMarkupModel().addRangeHighlighter(
                                                DebuggerColors.BREAKPOINT_ATTRIBUTES,
                                                range.getStartOffset(),
                                                range.getEndOffset(),
                                                DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER,
                                                HighlighterTargetArea.EXACT_RANGE
                                            );
                                        }
                                    }
                                }
                            }

                            private void clearHighlighter() {
                                if (myHighlighter != null) {
                                    myHighlighter.dispose();
                                }
                            }
                        }

                        // calculate default item
                        int caretOffset = editor.getCaretModel().getOffset();
                        XLineBreakpointType<P>.XLineBreakpointVariant defaultVariant = null;
                        for (XLineBreakpointType<P>.XLineBreakpointVariant variant : variants) {
                            TextRange range = variant.getHighlightRange();
                            if (range != null && range.contains(caretOffset)) {
                                //noinspection ConstantConditions
                                if (defaultVariant == null || defaultVariant.getHighlightRange().getLength() > range.getLength()) {
                                    defaultVariant = variant;
                                }
                            }
                        }
                        final int defaultIndex = defaultVariant != null ? variants.indexOf(defaultVariant) : 0;

                        final MySelectionListener selectionListener = new MySelectionListener();
                        BaseListPopupStep<XLineBreakpointType.XLineBreakpointVariant> step = new BaseListPopupStep<XLineBreakpointType.XLineBreakpointVariant>(
                            "Create breakpoint for",
                            variants
                        ) {
                            @Nonnull
                            @Override
                            public String getTextFor(XLineBreakpointType.XLineBreakpointVariant value) {
                                return value.getText();
                            }

                            @Override
                            public Image getIconFor(XLineBreakpointType.XLineBreakpointVariant value) {
                                return value.getIcon();
                            }

                            @Override
                            public void canceled() {
                                selectionListener.clearHighlighter();
                            }

                            @Override
                            @RequiredUIAccess
                            public PopupStep onChosen(XLineBreakpointType.XLineBreakpointVariant selectedValue, boolean finalChoice) {
                                selectionListener.clearHighlighter();
                                project.getApplication().runWriteAction(() -> {
                                    P properties = (P) selectedValue.createProperties();
                                    res.setDone(breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
                                });
                                return FINAL_CHOICE;
                            }

                            @Override
                            public int getDefaultOptionIndex() {
                                return defaultIndex;
                            }
                        };

                        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
                        DebuggerUIImplUtil.registerExtraHandleShortcuts(popup, IdeActions.ACTION_TOGGLE_LINE_BREAKPOINT);
                        popup.setAdText(DebuggerUIImplUtil.getSelectionShortcutsAdText(IdeActions.ACTION_TOGGLE_LINE_BREAKPOINT));
                        popup.addListSelectionListener(selectionListener);
                        popup.show(relativePoint);
                        return res;
                    }
                    else {
                        P properties = variants.get(0).createProperties();
                        return AsyncResult.done((XLineBreakpoint) breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
                    }
                }
                P properties = type.createBreakpointProperties(file, line);
                return AsyncResult.done((XLineBreakpoint) breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary));
            }
            return AsyncResult.<XLineBreakpoint>rejected();
        });
    }

    public static <P extends XBreakpointProperties> XLineBreakpoint toggleAndReturnLineBreakpoint(
        @Nonnull Project project,
        @Nonnull XLineBreakpointType<P> type,
        @Nonnull VirtualFile file,
        int line,
        boolean temporary
    ) {
        return WriteAction.compute(() -> {
            XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
            XLineBreakpoint<P> breakpoint = breakpointManager.findBreakpointAtLine(type, file, line);
            if (breakpoint != null) {
                breakpointManager.removeBreakpoint(breakpoint);
                return null;
            }
            else {
                P properties = type.createBreakpointProperties(file, line);
                return breakpointManager.addLineBreakpoint(type, file.getUrl(), line, properties, temporary);
            }
        });
    }

    @Override
    public void removeBreakpoint(Project project, XBreakpoint<?> breakpoint) {
        WriteAction.run(() -> XDebuggerManager.getInstance(project).getBreakpointManager().removeBreakpoint(breakpoint));
    }

    @Override
    public <B extends XBreakpoint<?>> XBreakpointType<B, ?> findBreakpointType(@Nonnull Class<? extends XBreakpointType<B, ?>> typeClass) {
        if (myBreakpointTypeByClass == null) {
            myBreakpointTypeByClass = new HashMap<>();
            for (XBreakpointType<?, ?> breakpointType : XBreakpointUtil.getBreakpointTypes()) {
                myBreakpointTypeByClass.put(breakpointType.getClass(), breakpointType);
            }
        }
        XBreakpointType<?, ?> type = myBreakpointTypeByClass.get(typeClass);
        //noinspection unchecked
        return (XBreakpointType<B, ?>) type;
    }

    @Override
    public <T extends XDebuggerSettings<?>> T getDebuggerSettings(Class<T> aClass) {
        return XDebuggerSettingManagerImpl.getInstanceImpl().getSettings(aClass);
    }

    @Override
    public XValueContainer getValueContainer(DataContext dataContext) {
        return XDebuggerTreeActionBase.getSelectedValue(dataContext);
    }

    @Override
    public <B extends XLineBreakpoint<?>> XBreakpointGroupingRule<B, ?> getGroupingByFileRule() {
        return new XBreakpointFileGroupingRule<>();
    }

    @Nullable
    public static XSourcePosition getCaretPosition(@Nonnull Project project, DataContext context) {
        Editor editor = getEditor(project, context);
        if (editor == null) {
            return null;
        }

        Document document = editor.getDocument();
        int line = editor.getCaretModel().getLogicalPosition().line;
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        return XSourcePositionImpl.create(file, line);
    }

    @Nonnull
    public static Collection<XSourcePosition> getAllCaretsPositions(@Nonnull Project project, DataContext context) {
        Editor editor = getEditor(project, context);
        if (editor == null) {
            return Collections.emptyList();
        }

        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        Collection<XSourcePosition> res = new ArrayList<>();
        List<Caret> carets = editor.getCaretModel().getAllCarets();
        for (Caret caret : carets) {
            int line = caret.getLogicalPosition().line;
            XSourcePositionImpl position = XSourcePositionImpl.create(file, line);
            if (position != null) {
                res.add(position);
            }
        }
        return res;
    }

    @Nullable
    private static Editor getEditor(@Nonnull Project project, DataContext context) {
        Editor editor = context.getData(Editor.KEY);
        if (editor == null) {
            return FileEditorManager.getInstance(project).getSelectedTextEditor();
        }
        return editor;
    }

    @Override
    public <B extends XBreakpoint<?>> Comparator<B> getDefaultBreakpointComparator(XBreakpointType<B, ?> type) {
        return (o1, o2) -> type.getDisplayText(o1).compareTo(type.getDisplayText(o2));
    }

    @Override
    public <P extends XBreakpointProperties> Comparator<XLineBreakpoint<P>> getDefaultLineBreakpointComparator() {
        return (o1, o2) -> {
            int fileCompare = o1.getFileUrl().compareTo(o2.getFileUrl());
            if (fileCompare != 0) {
                return fileCompare;
            }
            return o1.getLine() - o2.getLine();
        };
    }

    @Nullable
    public static XDebuggerEvaluator getEvaluator(XSuspendContext suspendContext) {
        XExecutionStack executionStack = suspendContext.getActiveExecutionStack();
        if (executionStack != null) {
            XStackFrame stackFrame = executionStack.getTopFrame();
            if (stackFrame != null) {
                return stackFrame.getEvaluator();
            }
        }
        return null;
    }

    @RequiredReadAction
    @Override
    public void iterateLine(@Nonnull Project project, @Nonnull Document document, int line, @Nonnull Processor<PsiElement> processor) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (file == null) {
            return;
        }

        int lineStart;
        int lineEnd;
        try {
            lineStart = document.getLineStartOffset(line);
            lineEnd = document.getLineEndOffset(line);
        }
        catch (IndexOutOfBoundsException ignored) {
            return;
        }

        PsiElement element;
        int offset = lineStart;
        while (offset < lineEnd) {
            element = file.findElementAt(offset);
            if (element != null) {
                if (!processor.process(element)) {
                    return;
                }
                else {
                    offset = element.getTextRange().getEndOffset();
                }
            }
            else {
                offset++;
            }
        }
    }

    @Override
    public <B extends XLineBreakpoint<?>> List<XBreakpointGroupingRule<B, ?>> getGroupingByFileRuleAsList() {
        return Collections.<XBreakpointGroupingRule<B, ?>>singletonList(this.<B>getGroupingByFileRule());
    }

    @RequiredReadAction
    @Override
    @Nullable
    public PsiElement findContextElement(@Nonnull VirtualFile virtualFile, int offset, @Nonnull Project project) {
        if (!virtualFile.isValid()) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        PsiFile file = document == null ? null : PsiManager.getInstance(project).findFile(virtualFile);
        if (file == null) {
            return null;
        }

        if (offset < 0) {
            offset = 0;
        }
        if (offset > document.getTextLength()) {
            offset = document.getTextLength();
        }
        int startOffset = offset;

        int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
        PsiElement result = null;
        do {
            PsiElement element = file.findElementAt(offset);
            if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
                result = element;
                break;
            }

            offset = element.getTextRange().getEndOffset() + 1;
        }
        while (offset < lineEndOffset);

        if (result == null) {
            result = file.findElementAt(startOffset);
        }

        return result;
    }

    @Override
    public void disableValueLookup(@Nonnull Editor editor) {
        ValueLookupManagerImpl.DISABLE_VALUE_LOOKUP.set(editor, Boolean.TRUE);
    }

    @Nullable
    public static Editor createEditor(@Nonnull OpenFileDescriptor descriptor) {
        return descriptor.canNavigate()
            ? FileEditorManager.getInstance((Project) descriptor.getProject()).openTextEditor(descriptor, false) : null;
    }

    @Nonnull
    @Override
    public XExpression createExpression(@Nonnull String text, Language language, String custom, EvaluationMode mode) {
        return new XExpressionImpl(text, language, custom, mode);
    }

    public static Image getVerifiedIcon(@Nonnull XBreakpoint breakpoint) {
        return breakpoint.getSuspendPolicy() == SuspendPolicy.NONE
            ? ExecutionDebugIconGroup.breakpointBreakpointunsuspendentvalid()
            : ExecutionDebugIconGroup.breakpointBreakpointvalid();
    }
}
