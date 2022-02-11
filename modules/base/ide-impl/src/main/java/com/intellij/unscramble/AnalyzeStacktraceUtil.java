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

package com.intellij.unscramble;

import consulo.execution.ExecutionManager;
import consulo.execution.executor.Executor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ConsoleViewUtil;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.execution.ui.ExecutionConsole;
import consulo.execution.ui.RunContentDescriptor;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.*;
import consulo.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import consulo.document.Document;
import consulo.editor.Editor;
import consulo.editor.EditorFactory;
import consulo.editor.EditorSettings;
import consulo.component.extension.ExtensionPointName;
import com.intellij.openapi.ide.CopyPasteManager;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;

/**
 * @author yole
 */
public class AnalyzeStacktraceUtil {
  public static final ExtensionPointName<Filter> EP_NAME = ExtensionPointName.create("consulo.analyzeStacktraceFilter");

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

  public static RunContentDescriptor addConsole(Project project, @Nullable ConsoleFactory consoleFactory, final String tabTitle, String text, @Nullable consulo.ui.image.Image icon) {
    final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
    builder.filters(EP_NAME.getExtensions(project));
    final ConsoleView consoleView = builder.getConsole();

    final DefaultActionGroup toolbarActions = new DefaultActionGroup();
    JComponent consoleComponent = consoleFactory != null ? consoleFactory.createConsoleComponent(consoleView, toolbarActions) : new MyConsolePanel(consoleView, toolbarActions);
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
    final ConsoleViewImpl console = (ConsoleViewImpl)consoleView;
    ConsoleViewUtil.enableReplaceActionForConsoleViewEditor(console.getEditor());
    console.getEditor().getSettings().setCaretRowShown(true);
    toolbarActions.add(new AnnotateStackTraceAction(console.getEditor(), console.getHyperlinks()));
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
      toolbarPanel.add(ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false).getComponent());
      add(toolbarPanel, BorderLayout.WEST);
      add(consoleView.getComponent(), BorderLayout.CENTER);
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
      if (CommonDataKeys.EDITOR == dataId) {
        return myEditor;
      }
      return null;
    }

    public Editor getEditor() {
      return myEditor;
    }

    public final void setText(@Nonnull final String text) {
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              final Document document = myEditor.getDocument();
              document.replaceString(0, document.getTextLength(), StringUtil.convertLineSeparators(text));
            }
          });
        }
      };
      CommandProcessor.getInstance().executeCommand(myProject, runnable, "", this);
    }

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
