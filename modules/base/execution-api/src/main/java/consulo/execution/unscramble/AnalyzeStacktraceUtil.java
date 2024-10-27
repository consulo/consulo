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

package consulo.execution.unscramble;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.execution.ExecutionManager;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.internal.AnalyzeStacktraceService;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.console.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.image.Image;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;

/**
 * @author yole
 */
public class AnalyzeStacktraceUtil {
    private AnalyzeStacktraceUtil() {
    }

    public static void printStacktrace(final ConsoleView consoleView, final String unscrambledTrace) {
        consoleView.clear();
        consoleView.print(unscrambledTrace + "\n", ConsoleViewContentType.ERROR_OUTPUT);
        consoleView.scrollTo(0);
    }

    @Nullable
    public static String getTextInClipboard() {
        return CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
    }

    public interface ConsoleFactory {
        JComponent createConsoleComponent(ConsoleView consoleView, DefaultActionGroup toolbarActions);
    }

    public static void addConsole(Project project, @Nullable ConsoleFactory consoleFactory, final String tabTitle, String text) {
        addConsole(project, consoleFactory, tabTitle, text, null);
    }

    public static RunContentDescriptor addConsole(
        Project project,
        @Nullable ConsoleFactory consoleFactory,
        final String tabTitle,
        String text,
        @Nullable Image icon
    ) {
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.filters(project.getExtensionList(AnalyzeStackTraceFilter.class));
        final ConsoleView consoleView = builder.getConsole();

        final DefaultActionGroup toolbarActions = new DefaultActionGroup();
        JComponent consoleComponent = consoleFactory != null
            ? consoleFactory.createConsoleComponent(consoleView, toolbarActions)
            : new MyConsolePanel(consoleView, toolbarActions);
        final RunContentDescriptor descriptor = new RunContentDescriptor(consoleView, null, consoleComponent, tabTitle, icon) {
            @Override
            public boolean isContentReuseProhibited() {
                return true;
            }
        };

        final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        for (AnAction action : consoleView.createConsoleActions()) {
            toolbarActions.add(action);
        }
        ConsoleViewUtil.enableReplaceActionForConsoleViewEditor(consoleView.getEditor());
        consoleView.getEditor().getSettings().setCaretRowShown(true);
        AnalyzeStacktraceService analyzeStacktraceService = Application.get().getInstance(AnalyzeStacktraceService.class);
        toolbarActions.add(analyzeStacktraceService.createAnnotateStackTraceAction(consoleView));
        ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, descriptor);
        consoleView.allowHeavyFilters();
        if (consoleFactory == null) {
            printStacktrace(consoleView, text);
        }
        return descriptor;
    }

    private static final class MyConsolePanel extends JPanel {
        public MyConsolePanel(ExecutionConsole consoleView, ActionGroup toolbarActions) {
            super(new BorderLayout());
            JPanel toolbarPanel = new JPanel(new BorderLayout());
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false);

            JComponent component = consoleView.getComponent();
            toolbar.setTargetComponent(component);
            toolbarPanel.add(toolbar.getComponent());
            add(toolbarPanel, BorderLayout.WEST);
            add(component, BorderLayout.CENTER);
        }
    }

    public static StacktraceEditorPanel createEditorPanel(Project project, @Nonnull Disposable parentDisposable) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        Editor editor = editorFactory.createEditor(document, project);
        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setLineNumbersShown(false);
        settings.setRightMarginShown(false);

        StacktraceEditorPanel editorPanel = new StacktraceEditorPanel(project, editor);
        editorPanel.setPreferredSize(new Dimension(600, 400));
        Disposer.register(parentDisposable, editorPanel);
        return editorPanel;
    }

    public static final class StacktraceEditorPanel extends JPanel implements DataProvider, Disposable {
        private final Project myProject;
        private final Editor myEditor;

        public StacktraceEditorPanel(Project project, Editor editor) {
            super(new BorderLayout());
            myProject = project;
            myEditor = editor;
            add(myEditor.getComponent());
        }

        @Override
        public Object getData(@Nonnull Key<?> dataId) {
            if (Editor.KEY == dataId) {
                return myEditor;
            }
            return null;
        }

        public Editor getEditor() {
            return myEditor;
        }

        @RequiredUIAccess
        public final void setText(@Nonnull final String text) {
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .groupId(this)
                .inWriteAction()
                .run(() -> {
                    final Document document = myEditor.getDocument();
                    document.replaceString(0, document.getTextLength(), StringUtil.convertLineSeparators(text));
                });
        }

        @RequiredUIAccess
        public void pasteTextFromClipboard() {
            String text = getTextInClipboard();
            if (text != null) {
                setText(text);
            }
        }

        @Override
        public void dispose() {
            EditorFactory.getInstance().releaseEditor(myEditor);
        }

        public String getText() {
            return myEditor.getDocument().getText();
        }

        public JComponent getEditorComponent() {
            return myEditor.getContentComponent();
        }
    }
}
