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
package com.intellij.util.ui;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.UndoConfirmationPolicy;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;

import java.util.ArrayList;
import java.util.Collection;

class Line {
  private final String myValue;
  private final TextAttributes myTextAttributes;

  public Line(String value, TextAttributes textAttributes) {
    myValue = value.replaceAll("\r", "") + "\n";
    myTextAttributes = textAttributes;
  }

  public String getValue() {
    return myValue;
  }

  public TextAttributes getAttributes() {
    return myTextAttributes;
  }
}

public class EditorAdapter {
  private static final Logger LOG = Logger.getInstance(EditorAdapter.class);

  private final Editor myEditor;

  private final Alarm myFlushAlarm = new Alarm();
  private final Collection<Line> myLines = new ArrayList<Line>();
  private final Project myProject;
  private final boolean myScrollToTheEndOnAppend;

  private final Runnable myFlushDeferredRunnable = new Runnable() {
    public void run() {
      flushStoredLines();
    }
  };

  private synchronized void flushStoredLines() {
    Collection<Line> lines;
    synchronized (myLines) {
      lines = new ArrayList<Line>(myLines);
      myLines.clear();
    }
    ApplicationManager.getApplication().runWriteAction(writingCommand(lines));
  }


  public EditorAdapter(Editor editor, Project project, boolean scrollToTheEndOnAppend) {
    myEditor = editor;
    myProject = project;
    myScrollToTheEndOnAppend = scrollToTheEndOnAppend;
    LOG.assertTrue(myEditor.isViewer());
  }

  public Editor getEditor() {
    return myEditor;
  }

  public void appendString(String string, TextAttributes attrs) {
    synchronized (myLines) {
      myLines.add(new Line(string, attrs));
    }

    if (myFlushAlarm.getActiveRequestCount() == 0) {
      myFlushAlarm.addRequest(myFlushDeferredRunnable, 200, IdeaModalityState.NON_MODAL);
    }
  }

  private Runnable writingCommand(final Collection<Line> lines) {
    final Runnable command = new Runnable() {
      public void run() {

        Document document = myEditor.getDocument();

        StringBuilder buffer = new StringBuilder();
        for (Line line : lines) {
          buffer.append(line.getValue());
        }
        int endBefore = document.getTextLength();
        int endBeforeLine = endBefore;
        document.insertString(endBefore, buffer.toString());
        for (Line line : lines) {
          myEditor.getMarkupModel()
              .addRangeHighlighter(endBeforeLine, Math.min(document.getTextLength(), endBeforeLine + line.getValue().length()), HighlighterLayer.ADDITIONAL_SYNTAX,
                                   line.getAttributes(), HighlighterTargetArea.EXACT_RANGE);
          endBeforeLine += line.getValue().length();
          if (endBeforeLine > document.getTextLength()) break;
        }
        shiftCursorToTheEndOfDocument();
      }
    };
    return new Runnable() {
      public void run() {
        CommandProcessor.getInstance().executeCommand(myProject, command, "", null, UndoConfirmationPolicy.DEFAULT, myEditor.getDocument());
      }
    };
  }

  private void shiftCursorToTheEndOfDocument() {
    if (myScrollToTheEndOnAppend) {
      myEditor.getCaretModel().moveToOffset(myEditor.getDocument().getTextLength());
      myEditor.getSelectionModel().removeSelection();
      myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    }
  }
}
