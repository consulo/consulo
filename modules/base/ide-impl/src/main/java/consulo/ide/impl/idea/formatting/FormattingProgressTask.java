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
package consulo.ide.impl.idea.formatting;

import consulo.application.progress.ProgressIndicator;
import consulo.document.Document;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.util.SequentialModalProgressTask;
import consulo.ide.impl.idea.util.SequentialTask;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.codeStyle.internal.LeafBlockWrapper;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.ProjectUndoManager;
import consulo.undoRedo.UndoManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

  private final ConcurrentMap<EventType, Collection<Runnable>> myCallbacks = ContainerUtil.newConcurrentMap();

  private final WeakReference<VirtualFile> myFile;
  private final WeakReference<Document>    myDocument;
  private final int                        myFileTextLength;

  @Nonnull
  private FormattingStateId myLastState                       = FormattingStateId.WRAPPING_BLOCKS;
  private long              myDocumentModificationStampBefore = -1;

  private int myBlocksToModifyNumber;
  private int myModifiedBlocksNumber;

  public FormattingProgressTask(@Nullable Project project, @Nonnull PsiFile file, @Nonnull Document document) {
    super(project, getTitle(file));
    myFile = new WeakReference<VirtualFile>(file.getVirtualFile());
    myDocument = new WeakReference<Document>(document);
    myFileTextLength = file.getTextLength();
    addCallback(EventType.CANCEL, new MyCancelCallback());
  }

  @Nonnull
  private static String getTitle(@Nonnull PsiFile file) {
    VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null) {
      return CodeInsightBundle.message("reformat.progress.common.text");
    }
    else {
      return CodeInsightBundle.message("reformat.progress.file.with.known.name.text", virtualFile.getName());
    }
  }

  @Override
  protected void prepare(@Nonnull final SequentialTask task) {
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
  public boolean addCallback(@Nonnull EventType eventType, @Nonnull Runnable callback) {
    return getCallbacks(eventType).add(callback);
  }

  @RequiredUIAccess
  @Override
  public void onSuccess() {
    super.onSuccess();
    for (Runnable callback : getCallbacks(EventType.SUCCESS)) {
      callback.run();
    }
  }

  @RequiredUIAccess
  @Override
  public void onCancel() {
    super.onCancel();
    for (Runnable callback : getCallbacks(EventType.CANCEL)) {
      callback.run();
    }
  }

  private Collection<Runnable> getCallbacks(@Nonnull EventType eventType) {
    Collection<Runnable> result = myCallbacks.get(eventType);
    if (result == null) {
      Collection<Runnable> candidate = myCallbacks.putIfAbsent(eventType, result = ContainerUtil.newConcurrentSet());
      if (candidate != null) {
        result = candidate;
      }
    }
    return result;
  }

  @Override
  public void afterWrappingBlock(@Nonnull LeafBlockWrapper wrapped) {
    update(FormattingStateId.WRAPPING_BLOCKS, MAX_PROGRESS_VALUE * wrapped.getEndOffset() / myFileTextLength);
  }

  @Override
  public void afterProcessingBlock(@Nonnull LeafBlockWrapper block) {
    update(FormattingStateId.PROCESSING_BLOCKS, MAX_PROGRESS_VALUE * block.getEndOffset() / myFileTextLength);
  }

  @Override
  public void beforeApplyingFormatChanges(@Nonnull Collection<LeafBlockWrapper> modifiedBlocks) {
    myBlocksToModifyNumber = modifiedBlocks.size();
    updateTextIfNecessary(FormattingStateId.APPLYING_CHANGES);
    setCancelText(IdeLocalize.actionStop());
  }

  @Override
  public void afterApplyingChange(@Nonnull LeafBlockWrapper block) {
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
  private void update(@Nonnull FormattingStateId state, double completionRate) {
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

  private void updateTextIfNecessary(@Nonnull FormattingStateId currentState) {
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
      FileEditor editor = FileEditorManager.getInstance((Project)myProject).getSelectedEditor(file);
      if (editor == null) {
        return;
      }

      UndoManager manager = ProjectUndoManager.getInstance((Project)myProject);
      while (manager.isUndoAvailable(editor) && document.getModificationStamp() != myDocumentModificationStampBefore) {
        manager.undo(editor);
      }
    }
  }
}
