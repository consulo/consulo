// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.impl.livePreview;

import consulo.ide.impl.idea.find.*;
import consulo.ide.impl.idea.find.impl.FindResultImpl;
import consulo.application.ApplicationManager;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindResult;
import consulo.language.util.ReadonlyStatusHandlerUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.codeEditor.SelectionModel;
import consulo.document.event.BulkAwareDocumentListener;
import consulo.document.event.DocumentListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.ui.ex.awt.util.Alarm;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;

public class LivePreviewController implements LivePreview.Delegate, FindUtil.ReplaceDelegate {
  public static final int USER_ACTIVITY_TRIGGERING_DELAY = 30;
  public static final int MATCHES_LIMIT = 10000;
  protected EditorSearchSession myComponent;

  private int myUserActivityDelay = USER_ACTIVITY_TRIGGERING_DELAY;

  private final Alarm myLivePreviewAlarm;
  protected SearchResults mySearchResults;
  private LivePreview myLivePreview;
  private static final boolean myReplaceDenied = false;
  private boolean mySuppressUpdate;

  private boolean myTrackingDocument;
  private boolean myChanged;

  private boolean myListeningSelection;

  private final SelectionListener mySelectionListener = new SelectionListener() {
    @Override
    public void selectionChanged(@Nonnull SelectionEvent e) {
      smartUpdate();
    }
  };
  private boolean myDisposed;

  public void setTrackingSelection(boolean b) {
    if (b) {
      if (!myListeningSelection) {
        getEditor().getSelectionModel().addSelectionListener(mySelectionListener);
      }
    }
    else {
      if (myListeningSelection) {
        getEditor().getSelectionModel().removeSelectionListener(mySelectionListener);
      }
    }
    myListeningSelection = b;
  }


  private final DocumentListener myDocumentListener = new BulkAwareDocumentListener.Simple() {
    @Override
    public void afterDocumentChange(@Nonnull final Document document) {
      if (!myTrackingDocument) {
        myChanged = true;
        return;
      }
      if (!mySuppressUpdate) {
        smartUpdate();
      }
      else {
        mySuppressUpdate = false;
      }
    }
  };

  private void smartUpdate() {
    if (myLivePreview == null) return;
    FindModel findModel = mySearchResults.getFindModel();
    if (findModel != null) {
      updateInBackground(findModel, false);
    }
  }

  public void moveCursor(SearchResults.Direction direction) {
    if (direction == SearchResults.Direction.UP) {
      mySearchResults.prevOccurrence(false);
    }
    else {
      mySearchResults.nextOccurrence(false);
    }
  }

  public boolean isReplaceDenied() {
    return myReplaceDenied;
  }

  public LivePreviewController(SearchResults searchResults, @Nullable EditorSearchSession component, @Nonnull Disposable parentDisposable) {
    mySearchResults = searchResults;
    myComponent = component;
    getEditor().getDocument().addDocumentListener(myDocumentListener);
    myLivePreviewAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
  }

  public void setUserActivityDelay(int userActivityDelay) {
    myUserActivityDelay = userActivityDelay;
  }

  public void updateInBackground(@Nonnull FindModel findModel, final boolean allowedToChangedEditorSelection) {
    final int stamp = mySearchResults.getStamp();
    myLivePreviewAlarm.cancelAllRequests();
    final FindModel copy = new FindModel();
    copy.copyFrom(findModel);
    mySearchResults.setUpdating(true);
    if (myComponent != null) {
      myComponent.getComponent().updateActions();
    }
    Runnable request = () -> {
      mySearchResults.updateThreadSafe(copy, allowedToChangedEditorSelection, null, stamp).doWhenRejected(() -> updateInBackground(findModel, allowedToChangedEditorSelection));
    };
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      request.run();
    }
    else {
      myLivePreviewAlarm.addRequest(request, myUserActivityDelay);
    }
  }

  @Override
  public String getStringToReplace(@Nonnull Editor editor, @Nullable FindResult findResult) throws FindManager.MalformedReplacementStringException {
    if (findResult == null) {
      return null;
    }
    String foundString = editor.getDocument().getText(findResult);
    CharSequence documentText = editor.getDocument().getImmutableCharSequence();
    FindModel currentModel = mySearchResults.getFindModel();

    if (currentModel != null && currentModel.isReplaceState()) {
      FindManager findManager = FindManager.getInstance(mySearchResults.getProject());
      return findManager.getStringToReplace(foundString, currentModel, findResult.getStartOffset(), documentText);
    }
    return null;
  }

  @Nullable
  public TextRange performReplace(final FindResult occurrence, final String replacement, final Editor editor) {
    Project project = mySearchResults.getProject();
    if (myReplaceDenied || !ReadonlyStatusHandlerUtil.ensureDocumentWritable(project, editor.getDocument())) return null;
    FindModel findModel = mySearchResults.getFindModel();
    CommandProcessor.getInstance().runUndoTransparentAction(() -> getEditor().getCaretModel().moveToOffset(occurrence.getEndOffset()));
    TextRange result = FindUtil.doReplace(project, editor.getDocument(), findModel, new FindResultImpl(occurrence.getStartOffset(), occurrence.getEndOffset()), replacement, true, new ArrayList<>());
    mySearchResults.updateThreadSafe(findModel, true, result, mySearchResults.getStamp());
    return result;
  }

  private void performReplaceAll(Editor e) {
    Project project = mySearchResults.getProject();
    if (!ReadonlyStatusHandlerUtil.ensureDocumentWritable(project, e.getDocument())) {
      return;
    }
    if (mySearchResults.getFindModel() != null) {
      final FindModel copy = new FindModel();
      copy.copyFrom(mySearchResults.getFindModel());

      final SelectionModel selectionModel = mySearchResults.getEditor().getSelectionModel();

      final int offset;
      if (!selectionModel.hasSelection() || copy.isGlobal()) {
        copy.setGlobal(true);
        offset = 0;
      }
      else {
        offset = selectionModel.getBlockSelectionStarts()[0];
      }
      FindUtil.replace(project, e, offset, copy, this);
    }
  }

  @Override
  public boolean shouldReplace(TextRange range, String replace) {
    for (RangeMarker r : mySearchResults.getExcluded()) {
      if (TextRange.areSegmentsEqual(r, range)) {
        return false;
      }
    }
    return true;
  }

  public boolean canReplace() {
    if (mySearchResults != null && mySearchResults.getCursor() != null && !isReplaceDenied()) {

      final String replacement;
      try {
        replacement = getStringToReplace(getEditor(), mySearchResults.getCursor());
      }
      catch (FindManager.MalformedReplacementStringException e) {
        return false;
      }
      return replacement != null;
    }
    return false;
  }

  private Editor getEditor() {
    return mySearchResults.getEditor();
  }

  public void performReplace() throws FindManager.MalformedReplacementStringException {
    mySuppressUpdate = true;
    String replacement = getStringToReplace(getEditor(), mySearchResults.getCursor());
    if (replacement == null) {
      return;
    }
    final TextRange textRange = performReplace(mySearchResults.getCursor(), replacement, getEditor());
    if (textRange == null) {
      mySuppressUpdate = false;
    }
    if (myComponent != null) {
      myComponent.addTextToRecent(myComponent.getComponent().getReplaceTextComponent());
      myComponent.clearUndoInTextFields();
    }
  }

  public void exclude() {
    mySearchResults.exclude(mySearchResults.getCursor());
  }

  public void performReplaceAll() {
    performReplaceAll(getEditor());
  }

  public void setTrackingDocument(boolean trackingDocument) {
    myTrackingDocument = trackingDocument;
  }

  public void setLivePreview(LivePreview livePreview) {
    if (myLivePreview != null) {
      myLivePreview.dispose();
      myLivePreview.setDelegate(null);
    }
    myLivePreview = livePreview;
    if (myLivePreview != null) {
      myLivePreview.setDelegate(this);
    }
  }

  public void dispose() {
    if (myDisposed) return;

    off();

    mySearchResults.dispose();
    getEditor().getDocument().removeDocumentListener(myDocumentListener);
    myDisposed = true;
  }

  public void on() {
    if (myDisposed) return;

    mySearchResults.setMatchesLimit(MATCHES_LIMIT);
    setTrackingDocument(true);

    if (myChanged) {
      mySearchResults.clear();
      myChanged = false;
    }

    setLivePreview(new LivePreview(mySearchResults));
  }

  public void off() {
    if (myDisposed) return;

    setTrackingDocument(false);
    setLivePreview(null);
    setTrackingSelection(false);
  }
}
