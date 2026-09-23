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
package consulo.execution.impl.internal.console;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.console.ConsoleViewUtil;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.localize.LocalizeValue;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.DockLayout;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * A console for a frontend which draws no awt. What text is taken and how it is coloured is the engine's, and
 * what is left here is an editor to show it in.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
public class UnifiedConsoleViewImpl implements ConsoleViewInternal {
    private final Project myProject;
    private final boolean myViewer;
    private final ConsoleViewEngine myEngine;

    private @Nullable EditorEx myEditor;
    private @Nullable DockLayout myRoot;
    private @Nullable String myHelpId;
    private @Nullable ProcessHandler myProcessHandler;
    private @Nullable BiPredicate<ProcessEvent, Key> myProcessTextFilter;

    public UnifiedConsoleViewImpl(Project project, boolean viewer) {
        myProject = project;
        myViewer = viewer;
        myEngine = new ConsoleViewEngine(() -> myEditor);
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getUIComponent() {
        if (myRoot == null) {
            EditorEx editor = createConsoleEditor();
            myEditor = editor;

            DockLayout root = DockLayout.create();
            root.center(editor.getUIComponent());

            myRoot = root;

            flush();
        }

        return myRoot;
    }

    /**
     * A console is handed around as a swing component by whoever wants to hang a shortcut or a context off it.
     * The bridge answers for that here rather than every caller having to ask which frontend is up.
     */
    @Override
    @RequiredUIAccess
    public javax.swing.JComponent getComponent() {
        return (javax.swing.JComponent) TargetAWT.to(getUIComponent());
    }

    @Override
    @RequiredUIAccess
    public javax.swing.JComponent getPreferredFocusableComponent() {
        return getComponent();
    }

    public Project getProject() {
        return myProject;
    }

    @Override
    public EditorEx createConsoleEditor() {
        return ConsoleViewUtil.setupConsoleEditor(myProject, false, false);
    }

    @Override
    public void releaseConsoleEditor(EditorEx editor) {
        EditorFactory.getInstance().releaseEditor(editor);
    }

    @Override
    public void consoleEditorCreated(EditorEx editor) {
    }

    @Override
    public void scrollToEnd(EditorEx editor) {
        Document document = editor.getDocument();
        editor.getCaretModel().moveToOffset(document.getTextLength());
    }

    @Override
    public boolean isStickToEndCancelled() {
        return false;
    }

    @Override
    public void resetStickToEndCancelled() {
    }

    @Override
    public void heavyFilterStarted(LocalizeValue message) {
    }

    @Override
    public void heavyFilterFinished() {
    }

    @Override
    public void createManualHyperlink(int startOffset, int endOffset, HyperlinkInfo info) {
    }

    @Override
    public @Nullable HyperlinkInfo getHyperlinkInfoByLineAndCol(int line, int column) {
        return null;
    }

    @Override
    public void highlightHyperlinks(Filter filter, int startLine, int endLine) {
    }

    @Override
    public void addHyperlinkHighlighter(int startOffset, int endOffset, consulo.colorScheme.TextAttributes attributes) {
    }

    @Override
    public void clearHyperlinks() {
    }

    @Override
    public void print(String s, ConsoleViewContentType contentType) {
        print(s, contentType, null);
    }

    @Override
    public void printHyperlink(String hyperlinkText, @Nullable HyperlinkInfo info) {
        print(hyperlinkText, ConsoleViewContentType.NORMAL_OUTPUT, info);
    }

    private void print(String text, ConsoleViewContentType contentType, @Nullable HyperlinkInfo info) {
        myEngine.print(text, contentType, info);

        UIAccess uiAccess = myProject.getUIAccess();
        uiAccess.give(this::flush);
    }

    /**
     * Carries whatever is waiting into the document, colouring each token as it lands.
     */
    @RequiredUIAccess
    private void flush() {
        EditorEx editor = myEditor;
        if (editor == null || myProject.isDisposed() || editor.isDisposed()) {
            return;
        }

        List<TokenBuffer.TokenInfo> tokens = myEngine.drain();
        if (tokens.isEmpty()) {
            return;
        }

        boolean stickToEnd = myEngine.isStickingToEnd();

        Document document = editor.getDocument();
        int offset = document.getTextLength();

        document.insertString(offset, TokenBuffer.getRawText(tokens));

        for (TokenBuffer.TokenInfo token : tokens) {
            int end = offset + token.length();
            myEngine.createTokenRangeHighlighter(myProject, token.contentType, offset, end);
            offset = end;
        }

        if (stickToEnd) {
            scrollToEnd(editor);
        }
    }

    @Override
    @RequiredUIAccess
    public void clear() {
        myEngine.clearDeferredOutput();

        EditorEx editor = myEditor;
        if (editor != null && !editor.isDisposed()) {
            Document document = editor.getDocument();
            document.deleteString(0, document.getTextLength());
        }
    }

    @Override
    @RequiredUIAccess
    public void scrollTo(int offset) {
        EditorEx editor = myEditor;
        if (editor != null && !editor.isDisposed()) {
            editor.getCaretModel().moveToOffset(offset);
        }
    }

    @Override
    public void attachToProcess(ProcessHandler processHandler) {
        myProcessHandler = processHandler;

        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                print(event.getText(), ConsoleViewContentType.getConsoleViewType(outputType), null);
            }
        });
    }

    @Override
    public void setOutputPaused(boolean value) {
        myEngine.setOutputPaused(value);
    }

    @Override
    public boolean isOutputPaused() {
        return myEngine.isOutputPaused();
    }

    @Override
    public boolean hasDeferredOutput() {
        return myEngine.hasDeferredOutput();
    }

    @Override
    public void performWhenNoDeferredOutput(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void setHelpId(String helpId) {
        myHelpId = helpId;
    }

    @Override
    public void addMessageFilter(Filter filter) {
    }

    @Override
    public void setProcessTextFilter(@Nullable BiPredicate<ProcessEvent, Key> filter) {
        myProcessTextFilter = filter;
    }

    @Override
    public @Nullable BiPredicate<ProcessEvent, Key> getProcessTextFilter() {
        return myProcessTextFilter;
    }

    @Override
    public int getContentSize() {
        return myEngine.getContentSize();
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public AnAction[] createConsoleActions() {
        return AnAction.EMPTY_ARRAY;
    }

    @Override
    public void allowHeavyFilters() {
    }

    @Override
    public @Nullable Editor getEditor() {
        return myEditor;
    }

    @Override
    public void dispose() {
        EditorEx editor = myEditor;
        myEditor = null;
        myRoot = null;

        myEngine.clearDeferredOutput();

        if (editor != null && !editor.isDisposed()) {
            releaseConsoleEditor(editor);
        }
    }
}
