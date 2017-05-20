/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.formatting;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.SequentialModalProgressTask;
import com.intellij.util.SequentialTask;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.containers.ConcurrentHashSet;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

/**
 * Formatting progressable task.
 *
 * @author Denis Zhdanov
 * @since 2/10/11 3:00 PM
 */
public class FormattingProgressTask extends SequentialModalProgressTask implements FormattingProgressCallback {

  /**
   * Holds flag that indicates whether formatting was cancelled by end-user or not.
   */
  public static final ThreadLocal<Boolean> FORMATTING_CANCELLED_FLAG = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  /**
   * Holds max allowed progress bar value (defined at ProgressWindow.MyDialog.initDialog()).
   */
  private static final double MAX_PROGRESS_VALUE = 1;
  private static final double TOTAL_WEIGHT;

  static {
    double weight = 0;
    for (FormattingStateId state : FormattingStateId.values()) {
      weight += state.getProgressWeight();
    }
    TOTAL_WEIGHT = weight;
  }

  private final ConcurrentMap<EventType, Collection<Runnable>> myCallbacks = new ConcurrentHashMap<EventType, Collection<Runnable>>();

  private final WeakReference<VirtualFile> myFile;
  private final WeakReference<Document>    myDocument;
  private final int                        myFileTextLength;

  @NotNull
  private FormattingStateId myLastState                       = FormattingStateId.WRAPPING_BLOCKS;
  private long              myDocumentModificationStampBefore = -1;

  private int myBlocksToModifyNumber;
  private int myModifiedBlocksNumber;

  public FormattingProgressTask(@Nullable Project project, @NotNull PsiFile file, @NotNull Document document) {
    super(project, getTitle(file));
    myFile = new WeakReference<VirtualFile>(file.getVirtualFile());
    myDocument = new WeakReference<Document>(document);
    myFileTextLength = file.getTextLength();
    addCallback(EventType.CANCEL, new MyCancelCallback());
  }

  @NotNull
  private static String getTitle(@NotNull PsiFile file) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      return CodeInsightBundle.message("reformat.progress.common.text");
    }
    else {
      return CodeInsightBundle.message("reformat.progress.file.with.known.name.text", virtualFile.getName());
    }
  }

  @Override
  protected void prepare(@NotNull final SequentialTask task) {
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        Document document = myDocument.get();
        if (document != null) {
          myDocumentModificationStampBefore = document.getModificationStamp();
        }
        task.prepare();
      }
    });
  }

  @Override
  public boolean addCallback(@NotNull EventType eventType, @NotNull Runnable callback) {
    return getCallbacks(eventType).add(callback);
  }

  @RequiredDispatchThread
  @Override
  public void onSuccess() {
    super.onSuccess();
    for (Runnable callback : getCallbacks(EventType.SUCCESS)) {
      callback.run();
    }
  }

  @RequiredDispatchThread
  @Override
  public void onCancel() {
    super.onCancel();
    for (Runnable callback : getCallbacks(EventType.CANCEL)) {
      callback.run();
    }
  }

  private Collection<Runnable> getCallbacks(@NotNull EventType eventType) {
    Collection<Runnable> result = myCallbacks.get(eventType);
    if (result == null) {
      Collection<Runnable> candidate = myCallbacks.putIfAbsent(eventType, result = new ConcurrentHashSet<Runnable>());
      if (candidate != null) {
        result = candidate;
      }
    }
    return result;
  }

  @Override
  public void afterWrappingBlock(@NotNull LeafBlockWrapper wrapped) {
    update(FormattingStateId.WRAPPING_BLOCKS, MAX_PROGRESS_VALUE * wrapped.getEndOffset() / myFileTextLength);
  }

  @Override
  public void afterProcessingBlock(@NotNull LeafBlockWrapper block) {
    update(FormattingStateId.PROCESSING_BLOCKS, MAX_PROGRESS_VALUE * block.getEndOffset() / myFileTextLength);
  }

  @Override
  public void beforeApplyingFormatChanges(@NotNull Collection<LeafBlockWrapper> modifiedBlocks) {
    myBlocksToModifyNumber = modifiedBlocks.size();
    updateTextIfNecessary(FormattingStateId.APPLYING_CHANGES);
    setCancelText(IdeBundle.message("action.stop"));
  }

  @Override
  public void afterApplyingChange(@NotNull LeafBlockWrapper block) {
    if (myModifiedBlocksNumber++ >= myBlocksToModifyNumber) {
      return;
    }

    update(FormattingStateId.APPLYING_CHANGES, MAX_PROGRESS_VALUE * myModifiedBlocksNumber / myBlocksToModifyNumber);
  }

  /**
   * Updates current progress state if necessary.
   *
   * @param state          current state
   * @param completionRate completion rate of the given state. Is assumed to belong to <code>[0; 1]</code> interval
   */
  private void update(@NotNull FormattingStateId state, double completionRate) {
    ProgressIndicator indicator = getIndicator();
    if (indicator == null) {
      return;
    }

    updateTextIfNecessary(state);

    myLastState = state;
    double newFraction = 0;
    for (FormattingStateId prevState : state.getPreviousStates()) {
      newFraction += MAX_PROGRESS_VALUE * prevState.getProgressWeight() / TOTAL_WEIGHT;
    }
    newFraction += completionRate * state.getProgressWeight() / TOTAL_WEIGHT;

    // We don't bother about imprecise floating point arithmetic here because that is enough for progress representation.
    double currentFraction = indicator.getFraction();
    if (newFraction - currentFraction < MAX_PROGRESS_VALUE / 100) {
      return;
    }

    indicator.setFraction(newFraction);
  }

  private void updateTextIfNecessary(@NotNull FormattingStateId currentState) {
    ProgressIndicator indicator = getIndicator();
    if (myLastState != currentState && indicator != null) {
      indicator.setText(currentState.getDescription());
    }
  }

  private class MyCancelCallback implements Runnable {
    @Override
    public void run() {
      FORMATTING_CANCELLED_FLAG.set(true);
      VirtualFile file = myFile.get();
      Document document = myDocument.get();
      if (file == null || document == null || myDocumentModificationStampBefore < 0) {
        return;
      }
      FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor(file);
      if (editor == null) {
        return;
      }

      UndoManager manager = UndoManager.getInstance(myProject);
      while (manager.isUndoAvailable(editor) && document.getModificationStamp() != myDocumentModificationStampBefore) {
        manager.undo(editor);
      }
    }
  }
}
