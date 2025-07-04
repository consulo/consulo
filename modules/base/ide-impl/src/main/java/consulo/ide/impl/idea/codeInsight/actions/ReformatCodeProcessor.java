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

package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.SelectionModel;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.impl.internal.formatting.FormattingProgressTask;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.FutureTask;

public class ReformatCodeProcessor extends AbstractLayoutCodeProcessor {

  public static final String COMMAND_NAME = FormatterUtil.REFORMAT_COMMAND_NAME;

  private static final Logger LOG = Logger.getInstance(ReformatCodeProcessor.class);

  private static final String PROGRESS_TEXT = CodeInsightLocalize.reformatProgressCommonText().get();
  private final Collection<TextRange> myRanges = new ArrayList<>();
  private SelectionModel mySelectionModel;

  public ReformatCodeProcessor(Project project, boolean processChangedTextOnly) {
    super(project, COMMAND_NAME, PROGRESS_TEXT, processChangedTextOnly);
  }

  public ReformatCodeProcessor(@Nonnull PsiFile file, @Nonnull SelectionModel selectionModel) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, false);
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, @Nonnull SelectionModel selectionModel) {
    super(processor, COMMAND_NAME, PROGRESS_TEXT);
    mySelectionModel = selectionModel;
  }

  public ReformatCodeProcessor(AbstractLayoutCodeProcessor processor, boolean processChangedTextOnly) {
    super(processor, COMMAND_NAME, PROGRESS_TEXT);
    setProcessChangedTextOnly(processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, Module module, boolean processChangedTextOnly) {
    super(project, module, COMMAND_NAME, PROGRESS_TEXT, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiDirectory directory, boolean includeSubdirs, boolean processChangedTextOnly) {
    super(project, directory, includeSubdirs, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile file, @Nullable TextRange range, boolean processChangedTextOnly) {
    super(project, file, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
    if (range != null) {
      myRanges.add(range);
    }
  }

  public ReformatCodeProcessor(@Nonnull PsiFile file, boolean processChangedTextOnly) {
    super(file.getProject(), file, PROGRESS_TEXT, COMMAND_NAME, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project, PsiFile[] files, @Nullable Runnable postRunnable, boolean processChangedTextOnly) {
    this(project, files, COMMAND_NAME, postRunnable, processChangedTextOnly);
  }

  public ReformatCodeProcessor(Project project,
                               PsiFile[] files,
                               String commandName,
                               @Nullable Runnable postRunnable,
                               boolean processChangedTextOnly)
  {
    super(project, files, PROGRESS_TEXT, commandName, postRunnable, processChangedTextOnly);
  }

  @Override
  @Nonnull
  protected FutureTask<Boolean> prepareTask(@Nonnull final PsiFile file, final boolean processChangedTextOnly)
          throws IncorrectOperationException
  {
    return new FutureTask<>(() -> {
      FormattingProgressTask.FORMATTING_CANCELLED_FLAG.set(false);
      try {
        Collection<TextRange> ranges = getRangesToFormat(processChangedTextOnly, file);

        CharSequence before = null;
        Document document = PsiDocumentManager.getInstance(myProject).getDocument(file);
        if (getInfoCollector() != null) {
          LOG.assertTrue(document != null);
          before = document.getImmutableCharSequence();
        }

        CaretVisualPositionKeeper caretPositionKeeper = new CaretVisualPositionKeeper(document);

        if (processChangedTextOnly) {
          CodeStyleManager.getInstance(myProject).reformatTextWithContext(file, ranges);
        }
        else {
          CodeStyleManager.getInstance(myProject).reformatText(file, ranges);
        }

        caretPositionKeeper.restoreOriginalLocation();

        if (before != null) {
          prepareUserNotificationMessage(document, before);
        }

        return !FormattingProgressTask.FORMATTING_CANCELLED_FLAG.get();
      }
      catch (FilesTooBigForDiffException e) {
        handleFileTooBigException(LOG, e, file);
        return false;
      }
      catch (IncorrectOperationException e) {
        LOG.error(e);
        return false;
      }
      finally {
        myRanges.clear();
      }
    });
  }

  private void prepareUserNotificationMessage(@Nonnull Document document, @Nonnull CharSequence before) {
    LOG.assertTrue(getInfoCollector() != null);
    int number = FormatChangedTextUtil.getInstance().calculateChangedLinesNumber(document, before);
    if (number > 0) {
      String message = "formatted " + number + " line" + (number > 1 ? "s" : "");
      getInfoCollector().setReformatCodeNotification(message);
    }
  }

  @Nonnull
  @RequiredReadAction
  private Collection<TextRange> getRangesToFormat(boolean processChangedTextOnly, PsiFile file) throws FilesTooBigForDiffException {
    if (mySelectionModel != null) {
      return getSelectedRanges(mySelectionModel);
    }

    if (processChangedTextOnly) {
      return FormatChangedTextUtil.getInstance().getChangedTextRanges(myProject, file);
    }

    return !myRanges.isEmpty() ? myRanges : List.of(file.getTextRange());
  }

  private static class CaretVisualPositionKeeper {
    private final Map<Editor, Integer> myCaretRelativeVerticalPositions = new HashMap<>();

    private CaretVisualPositionKeeper(@Nullable Document document) {
      if (document == null) return;

      Editor[] editors = EditorFactory.getInstance().getEditors(document);
      for (Editor editor : editors) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        Point pos = editor.visualPositionToXY(editor.getCaretModel().getVisualPosition());
        int relativePosition = pos.y - visibleArea.y;
        myCaretRelativeVerticalPositions.put(editor, relativePosition);
      }
    }

    private void restoreOriginalLocation() {
      for (Map.Entry<Editor, Integer> e : myCaretRelativeVerticalPositions.entrySet()) {
        Editor editor = e.getKey();
        int relativePosition = e.getValue();
        Point caretLocation = editor.visualPositionToXY(editor.getCaretModel().getVisualPosition());
        int scrollOffset = caretLocation.y - relativePosition;
        editor.getScrollingModel().disableAnimation();
        editor.getScrollingModel().scrollVertically(scrollOffset);
        editor.getScrollingModel().enableAnimation();
      }
    }
  }
}