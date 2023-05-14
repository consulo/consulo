/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.execution.console.language;

import consulo.application.ApplicationPropertiesComponent;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.impl.CodeEditorSoftWrapModelBase;
import consulo.codeEditor.impl.softwrap.mapping.SoftWrapApplianceManager;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.markup.*;
import consulo.desktop.awt.editor.impl.EditorComponentImpl;
import consulo.document.Document;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentBulkUpdateListener;
import consulo.document.event.DocumentEvent;
import consulo.execution.internal.action.ConsoleExecuteAction;
import consulo.execution.ui.console.language.*;
import consulo.ide.impl.idea.execution.console.ConsoleHistoryController;
import consulo.ide.impl.idea.execution.console.LanguageConsoleImpl;
import consulo.ide.impl.idea.execution.console.UseConsoleInputAction;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
public class DesktopLanguageConsoleBuilder extends LanguageConsoleBuilder {

  private static class MyHelper extends LanguageConsoleImpl.Helper {
    private final BiFunction<VirtualFile, Project, PsiFile> psiFileFactory;

    GutteredLanguageConsole console;

    public MyHelper(@Nonnull Project project, @Nonnull String title, @Nonnull Language language, @Nullable BiFunction<VirtualFile, Project, PsiFile> psiFileFactory) {
      super(project, new LightVirtualFile(title, language, ""));
      this.psiFileFactory = psiFileFactory;
    }

    @Nonnull
    @Override
    public PsiFile getFile() {
      return psiFileFactory == null ? super.getFile() : psiFileFactory.apply(virtualFile, project);
    }

    @Override
    public void setupEditor(@Nonnull EditorEx editor) {
      super.setupEditor(editor);

      console.setupEditor(editor);
    }
  }

  private final static class GutteredLanguageConsole extends LanguageConsoleImpl {
    private final GutterContentProvider gutterContentProvider;

    public GutteredLanguageConsole(@Nonnull MyHelper helper, @Nullable GutterContentProvider gutterContentProvider) {
      super(helper);

      helper.console = this;
      this.gutterContentProvider = gutterContentProvider == null ? new BasicGutterContentProvider() : gutterContentProvider;
    }

    @Override
    public boolean isHistoryViewerForceAdditionalColumnsUsage() {
      return false;
    }

    @Override
    public int getMinHistoryLineCount() {
      return 1;
    }

    void setupEditor(@Nonnull EditorEx editor) {
      if (editor == getConsoleEditor()) {
        return;
      }

      final ConsoleGutterComponent lineStartGutter = new ConsoleGutterComponent(editor, gutterContentProvider, true);
      final ConsoleGutterComponent lineEndGutter = new ConsoleGutterComponent(editor, gutterContentProvider, false);

      editor.getSoftWrapModel().forceAdditionalColumnsUsage();
      ((CodeEditorSoftWrapModelBase)editor.getSoftWrapModel()).getApplianceManager().setWidthProvider(new SoftWrapApplianceManager.VisibleAreaWidthProvider() {
        @Override
        public int getVisibleAreaWidth() {
          int guttersWidth = lineEndGutter.getPreferredWidth() + lineStartGutter.getPreferredWidth();
          EditorEx editor = getHistoryViewer();
          return editor.getScrollingModel().getVisibleArea().width - guttersWidth;
        }
      });
      editor.setHorizontalScrollbarVisible(true);

      JLayeredPane layeredPane = new JLayeredPane() {
        @Override
        public Dimension getPreferredSize() {
          Dimension editorSize = getEditorComponent().getPreferredSize();
          return new Dimension(lineStartGutter.getPreferredSize().width + editorSize.width, editorSize.height);
        }

        @Override
        public Dimension getMinimumSize() {
          Dimension editorSize = getEditorComponent().getMinimumSize();
          return new Dimension(lineStartGutter.getPreferredSize().width + editorSize.width, editorSize.height);
        }

        @Override
        public void doLayout() {
          EditorComponentImpl editor = getEditorComponent();
          int w = getWidth();
          int h = getHeight();
          int lineStartGutterWidth = lineStartGutter.getPreferredSize().width;
          lineStartGutter.setBounds(0, 0, lineStartGutterWidth + gutterContentProvider.getLineStartGutterOverlap(editor.getEditor()), h);

          editor.setBounds(lineStartGutterWidth, 0, w - lineStartGutterWidth, h);

          int lineEndGutterWidth = lineEndGutter.getPreferredSize().width;
          lineEndGutter.setBounds(lineStartGutterWidth + (w - lineEndGutterWidth - editor.getEditor().getScrollPane().getVerticalScrollBar().getWidth()), 0, lineEndGutterWidth, h);
        }

        @Nonnull
        private EditorComponentImpl getEditorComponent() {
          for (int i = getComponentCount() - 1; i >= 0; i--) {
            Component component = getComponent(i);
            if (component instanceof EditorComponentImpl) {
              return (EditorComponentImpl)component;
            }
          }
          throw new IllegalStateException();
        }
      };

      layeredPane.add(lineStartGutter, JLayeredPane.PALETTE_LAYER);

      JScrollPane scrollPane = editor.getScrollPane();
      layeredPane.add(scrollPane.getViewport().getView(), JLayeredPane.DEFAULT_LAYER);

      layeredPane.add(lineEndGutter, JLayeredPane.PALETTE_LAYER);

      scrollPane.setViewportView(layeredPane);

      GutterUpdateScheduler gutterUpdateScheduler = new GutterUpdateScheduler(lineStartGutter, lineEndGutter);
      getProject().getMessageBus().connect(this).subscribe(DocumentBulkUpdateListener.class, gutterUpdateScheduler);
      editor.getDocument().addDocumentListener(gutterUpdateScheduler);
    }

    @Override
    public void doAddPromptToHistory() {
      gutterContentProvider.beforeEvaluate(getHistoryViewer());
    }

    @Override
    public void dispose() {
      final PsiFile file = getFile();
      DaemonCodeAnalyzer.getInstance(file.getProject()).setHighlightingEnabled(file, true);

      super.dispose();
    }

    private final class GutterUpdateScheduler extends DocumentAdapter implements DocumentBulkUpdateListener {
      private final ConsoleGutterComponent lineStartGutter;
      private final ConsoleGutterComponent lineEndGutter;

      private Task gutterSizeUpdater;
      private RangeHighlighter lineSeparatorPainter;

      private final CustomHighlighterRenderer renderer = new CustomHighlighterRenderer() {
        @Override
        public void paint(@Nonnull Editor editor, @Nonnull RangeHighlighter highlighter, @Nonnull Graphics g) {
          Rectangle clip = g.getClipBounds();
          int lineHeight = editor.getLineHeight();
          int startLine = clip.y / lineHeight;
          int endLine = Math.min(((clip.y + clip.height) / lineHeight) + 1, ((RealEditor)editor).getVisibleLineCount());
          if (startLine >= endLine) {
            return;
          }

          // workaround - editor ask us to paint line 4-6, but we should draw line for line 3 (startLine - 1) also, otherwise it will be not rendered
          int actualStartLine = startLine == 0 ? 0 : startLine - 1;
          int y = (actualStartLine + 1) * lineHeight;
          g.setColor(TargetAWT.to(editor.getColorsScheme().getColor(EditorColors.INDENT_GUIDE_COLOR)));
          for (int visualLine = actualStartLine; visualLine < endLine; visualLine++) {
            if (gutterContentProvider.isShowSeparatorLine(editor.visualToLogicalPosition(new VisualPosition(visualLine, 0)).line, editor)) {
              g.drawLine(clip.x, y, clip.x + clip.width, y);
            }
            y += lineHeight;
          }
        }
      };

      public GutterUpdateScheduler(@Nonnull ConsoleGutterComponent lineStartGutter, @Nonnull ConsoleGutterComponent lineEndGutter) {
        this.lineStartGutter = lineStartGutter;
        this.lineEndGutter = lineEndGutter;

        // console view can invoke markupModel.removeAllHighlighters(), so, we must be aware of it
        getHistoryViewer().getMarkupModel().addMarkupModelListener(GutteredLanguageConsole.this, new MarkupModelListener() {
          @Override
          public void beforeRemoved(@Nonnull RangeHighlighter highlighter) {
            if (lineSeparatorPainter == highlighter) {
              lineSeparatorPainter = null;
            }
          }
        });
      }

      private void addLineSeparatorPainterIfNeed() {
        if (lineSeparatorPainter != null) {
          return;
        }

        RangeHighlighter highlighter =
                getHistoryViewer().getMarkupModel().addRangeHighlighter(0, getDocument().getTextLength(), HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.EXACT_RANGE);
        highlighter.setGreedyToRight(true);
        highlighter.setCustomRenderer(renderer);
        lineSeparatorPainter = highlighter;
      }

      private Document getDocument() {
        return getHistoryViewer().getDocument();
      }

      @Override
      public void documentChanged(DocumentEvent event) {
        Document document = getDocument();
        if (document.isInBulkUpdate()) {
          return;
        }

        if (document.getTextLength() > 0) {
          addLineSeparatorPainterIfNeed();
          int startDocLine = document.getLineNumber(event.getOffset());
          int endDocLine = document.getLineNumber(event.getOffset() + event.getNewLength());
          if (event.getOldLength() > event.getNewLength() || startDocLine != endDocLine || StringUtil.indexOf(event.getOldFragment(), '\n') != -1) {
            updateGutterSize(startDocLine, endDocLine);
          }
        }
        else if (event.getOldLength() > 0) {
          documentCleared();
        }
      }

      private void documentCleared() {
        gutterSizeUpdater = null;
        lineEndGutter.documentCleared();
        gutterContentProvider.documentCleared(getHistoryViewer());
      }

      @Override
      public void updateStarted(@Nonnull Document document) {
      }

      @Override
      public void updateFinished(@Nonnull Document document) {
        if (getDocument().getTextLength() == 0) {
          documentCleared();
        }
        else {
          addLineSeparatorPainterIfNeed();
          updateGutterSize(0, Integer.MAX_VALUE);
        }
      }

      private void updateGutterSize(int start, int end) {
        if (gutterSizeUpdater != null) {
          gutterSizeUpdater.start = Math.min(start, gutterSizeUpdater.start);
          gutterSizeUpdater.end = Math.max(end, gutterSizeUpdater.end);
          return;
        }

        gutterSizeUpdater = new Task(start, end);
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(gutterSizeUpdater);
      }

      private final class Task implements Runnable {
        private int start;
        private int end;

        public Task(int start, int end) {
          this.start = start;
          this.end = end;
        }

        @Override
        public void run() {
          if (!getHistoryViewer().isDisposed()) {
            lineStartGutter.updateSize(start, end);
            lineEndGutter.updateSize(start, end);
          }
          gutterSizeUpdater = null;
        }
      }
    }
  }

  @Nonnull
  public LanguageConsoleView build(@Nonnull Project project, @Nonnull Language language) {
    final MyHelper helper = new MyHelper(project, language.getDisplayName() + " Console", language, psiFileFactory);
    GutteredLanguageConsole consoleView = new GutteredLanguageConsole(helper, gutterContentProvider);
    if (oneLineInput) {
      consoleView.getConsoleEditor().setOneLineMode(true);
    }
    if (executeActionHandler != null) {
      assert historyType != null;
      doInitAction(consoleView, executeActionHandler, historyType);
    }

    if (processInputStateKey != null) {
      assert executeActionHandler != null;
      if (ApplicationPropertiesComponent.getInstance().getBoolean(processInputStateKey)) {
        executeActionHandler.setUseProcessStdIn(true);

        DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(consoleView.getProject());
        daemonCodeAnalyzer.setHighlightingEnabled(consoleView.getFile(), false);
      }
      consoleView.addCustomConsoleAction(new UseConsoleInputAction(processInputStateKey));
    }
    return consoleView;
  }

  private void doInitAction(@Nonnull LanguageConsoleView console, @Nonnull BaseConsoleExecuteActionHandler executeActionHandler, @Nonnull String historyType) {
    ConsoleExecuteAction action = new ConsoleExecuteAction(console, executeActionHandler, executionEnabled);
    action.registerCustomShortcutSet(action.getShortcutSet(), console.getConsoleEditor().getComponent());
    new ConsoleHistoryController(new MyConsoleRootType(historyType), null, console).install();
  }
}
