/*
 * Copyright 2013-2017 consulo.io
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
package consulo.wm.impl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.content.*;
import com.intellij.util.EventDispatcher;
import com.intellij.util.SmartList;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.wm.ContentEx;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 15-Oct-17
 * <p>
 * Extracted base part of IDEA com.intellij.ui.content.impl.ContentManagerImpl
 */
public abstract class ContentManagerBase implements ContentManager, PropertyChangeListener, Disposable.Parent {
  private static final Logger LOG = Logger.getInstance(ContentManagerBase.class);

  protected ContentUI myUI;
  protected final List<Content> myContents = new ArrayList<>();
  private final EventDispatcher<ContentManagerListener> myDispatcher = EventDispatcher.create(ContentManagerListener.class);
  private final List<Content> mySelection = new ArrayList<>();
  private final boolean myCanCloseContents;

  private final Set<Content> myContentWithChangedComponent = new HashSet<>();

  private boolean myDisposed;
  protected final Project myProject;

  protected final List<DataProvider> myDataProviders = new SmartList<>();
  private List<Content> mySelectionHistory = new ArrayList<>();

  /**
   * WARNING: as this class adds listener to the ProjectManager which is removed on projectClosed event, all instances of this class
   * must be created on already OPENED projects, otherwise there will be memory leak!
   */
  public ContentManagerBase(@Nonnull ContentUI contentUI, boolean canCloseContents, @Nonnull Project project) {
    myProject = project;
    myCanCloseContents = canCloseContents;
    myUI = contentUI;
    myUI.setManager(this);

    Disposer.register(project, this);
    Disposer.register(this, contentUI);
  }

  @Override
  public boolean canCloseContents() {
    return myCanCloseContents;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> getReady(@Nonnull Object requestor) {
    Content selected = getSelectedContent();
    if (selected == null) return AsyncResult.done(null);
    BusyObject busyObject = selected.getBusyObject();
    return busyObject != null ? busyObject.getReady(requestor) : AsyncResult.done(null);
  }

  @Override
  public void addContent(@Nonnull Content content, final int order) {
    doAddContent(content, order);
  }

  @Override
  public void addContent(@Nonnull Content content) {
    doAddContent(content, -1);
  }

  @Override
  public void addContent(@Nonnull final Content content, final Object constraints) {
    doAddContent(content, -1);
  }

  @RequiredUIAccess
  private void doAddContent(@Nonnull final Content content, final int index) {
    UIAccess.assertIsUIThread();
    if (myContents.contains(content)) {
      myContents.remove(content);
      myContents.add(index == -1 ? myContents.size() : index, content);
      return;
    }

    ((ContentEx)content).setManager(this);
    final int insertIndex = index == -1 ? myContents.size() : index;
    myContents.add(insertIndex, content);
    content.addPropertyChangeListener(this);
    fireContentAdded(content, insertIndex);
    if (myUI.isToSelectAddedContent() || mySelection.isEmpty() && !myUI.canBeEmptySelection()) {
      if (myUI.isSingleSelection()) {
        setSelectedContent(content);
      }
      else {
        addSelectedContent(content);
      }
    }

    Disposer.register(this, content);
  }

  @Override
  public boolean removeContent(@Nonnull Content content, final boolean dispose) {
    return removeContent(content, true, dispose).isDone();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> removeContent(@Nonnull Content content, boolean dispose, final boolean trackFocus, final boolean forcedFocus) {
    final AsyncResult<Void> result = new AsyncResult<Void>();
    removeContent(content, true, dispose).doWhenDone(() -> {
      if (trackFocus) {
        Content current = getSelectedContent();
        if (current != null) {
          setSelectedContent(current, true, true, !forcedFocus);
        }
        else {
          result.setDone();
        }
      }
      else {
        result.setDone();
      }
    });

    return result;
  }

  @Nonnull
  private ActionCallback removeContent(@Nonnull Content content, boolean trackSelection, boolean dispose) {
    UIAccess.assertIsUIThread();
    int indexToBeRemoved = getIndexOfContent(content);
    if (indexToBeRemoved == -1) return ActionCallback.REJECTED;

    try {
      Content selection = mySelection.isEmpty() ? null : mySelection.get(mySelection.size() - 1);
      int selectedIndex = selection != null ? myContents.indexOf(selection) : -1;

      if (!fireContentRemoveQuery(content, indexToBeRemoved, ContentManagerEvent.ContentOperation.undefined)) {
        return ActionCallback.REJECTED;
      }
      if (!content.isValid()) {
        return ActionCallback.REJECTED;
      }

      boolean wasSelected = isSelected(content);
      if (wasSelected) {
        removeFromSelection(content);
      }

      int indexToSelect = -1;
      if (wasSelected) {
        int i = indexToBeRemoved - 1;
        if (i >= 0) {
          indexToSelect = i;
        }
        else if (getContentCount() > 1) {
          indexToSelect = 0;
        }
      }
      else if (selectedIndex > indexToBeRemoved) {
        indexToSelect = selectedIndex - 1;
      }

      mySelectionHistory.remove(content);
      myContents.remove(content);
      content.removePropertyChangeListener(this);

      fireContentRemoved(content, indexToBeRemoved);
      ((ContentEx)content).setManager(null);


      if (dispose) {
        Disposer.dispose(content);
      }

      int newSize = myContents.size();
      if (newSize > 0 && trackSelection) {
        ActionCallback result = new ActionCallback();
        if (indexToSelect > -1) {
          final Content toSelect = mySelectionHistory.size() > 0 ? mySelectionHistory.get(0) : myContents.get(indexToSelect);
          if (!isSelected(toSelect)) {
            if (myUI.isSingleSelection()) {
              setSelectedContentCB(toSelect).notify(result);
            }
            else {
              addSelectedContent(toSelect);
              result.setDone();
            }
          }
        }
        return result;
      }
      else {
        mySelection.clear();
        return ActionCallback.DONE;
      }
    }
    finally {
      if (ApplicationManager.getApplication().isDispatchThread()) {
        if (!myDisposed) {
          updateUI();
        }
      }
    }
  }

  protected void updateUI() {
  }

  @Override
  public void removeAllContents(final boolean dispose) {
    Content[] contents = getContents();
    for (Content content : contents) {
      removeContent(content, dispose);
    }
  }

  @Override
  public int getContentCount() {
    return myContents.size();
  }

  @Override
  @Nonnull
  public Content[] getContents() {
    return myContents.toArray(new Content[myContents.size()]);
  }

  //TODO[anton,vova] is this method needed?
  @Override
  public Content findContent(String displayName) {
    for (Content content : myContents) {
      if (content.getDisplayName().equals(displayName)) {
        return content;
      }
    }
    return null;
  }

  @Override
  public Content getContent(int index) {
    return index >= 0 && index < myContents.size() ? myContents.get(index) : null;
  }

  @Override
  public Content getContent(@Nonnull consulo.ui.Component component) {
    Content[] contents = getContents();
    for (Content content : contents) {
      if (Comparing.equal(component, content.getUIComponent())) {
        return content;
      }
    }
    return null;
  }


  @Override
  public int getIndexOfContent(Content content) {
    return myContents.indexOf(content);
  }

  @Nonnull
  @Override
  public String getCloseActionName() {
    return myUI.getCloseActionName();
  }

  @Nonnull
  @Override
  public String getCloseAllButThisActionName() {
    return myUI.getCloseAllButThisActionName();
  }

  @Nonnull
  @Override
  public String getPreviousContentActionName() {
    return myUI.getPreviousContentActionName();
  }

  @Nonnull
  @Override
  public String getNextContentActionName() {
    return myUI.getNextContentActionName();
  }

  @Override
  public List<AnAction> getAdditionalPopupActions(@Nonnull final Content content) {
    return null;
  }

  @Override
  public boolean canCloseAllContents() {
    if (!canCloseContents()) {
      return false;
    }
    for (Content content : myContents) {
      if (content.isCloseable()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void addSelectedContent(@Nonnull final Content content) {
    if (!checkSelectionChangeShouldBeProcessed(content, false)) return;

    if (getIndexOfContent(content) == -1) {
      throw new IllegalArgumentException("content not found: " + content);
    }
    if (!isSelected(content)) {
      mySelection.add(content);
      fireSelectionChanged(content, ContentManagerEvent.ContentOperation.add);
    }
  }

  private boolean checkSelectionChangeShouldBeProcessed(Content content, boolean implicit) {
    if (!myUI.canChangeSelectionTo(content, implicit)) {
      return false;
    }

    final boolean result = !isSelected(content) || myContentWithChangedComponent.contains(content);
    myContentWithChangedComponent.remove(content);

    return result;
  }

  @Override
  public void removeFromSelection(@Nonnull Content content) {
    if (!isSelected(content)) return;
    mySelection.remove(content);
    fireSelectionChanged(content, ContentManagerEvent.ContentOperation.remove);
  }

  @Override
  public boolean isSelected(@Nonnull Content content) {
    return mySelection.contains(content);
  }

  @Override
  @Nonnull
  public Content[] getSelectedContents() {
    return mySelection.toArray(new Content[mySelection.size()]);
  }

  @Override
  @Nullable
  public Content getSelectedContent() {
    return mySelection.isEmpty() ? null : mySelection.get(0);
  }

  @Override
  public void setSelectedContent(@Nonnull Content content, boolean requestFocus) {
    setSelectedContentCB(content, requestFocus);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> setSelectedContentCB(@Nonnull final Content content, final boolean requestFocus) {
    return setSelectedContentCB(content, requestFocus, true);
  }

  @Override
  public void setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus) {
    setSelectedContentCB(content, requestFocus, forcedFocus);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> setSelectedContentCB(@Nonnull final Content content, final boolean requestFocus, final boolean forcedFocus) {
    return setSelectedContent(content, requestFocus, forcedFocus, false);
  }

  @Nonnull
  @Override
  public AsyncResult<Void> setSelectedContent(@Nonnull final Content content, final boolean requestFocus, final boolean forcedFocus, boolean implicit) {
    mySelectionHistory.remove(content);
    mySelectionHistory.add(0, content);
    if (isSelected(content) && requestFocus) {
      return requestFocus(content, forcedFocus);
    }

    if (!checkSelectionChangeShouldBeProcessed(content, implicit)) {
      return AsyncResult.rejected();
    }
    if (!myContents.contains(content)) {
      throw new IllegalArgumentException("Cannot find content:" + content.getDisplayName());
    }

    final boolean focused = isSelectionHoldsFocus();

    final Content[] old = getSelectedContents();

    final ActiveRunnable selection = new ActiveRunnable() {
      @Nonnull
      @Override
      public AsyncResult<Void> run() {
        if (myDisposed || getIndexOfContent(content) == -1) return AsyncResult.rejected();

        for (Content each : old) {
          removeFromSelection(each);
        }

        addSelectedContent(content);

        if (requestFocus) {
          return requestFocus(content, forcedFocus);
        }
        return AsyncResult.done(null);
      }
    };

    final AsyncResult<Void> result = new AsyncResult<Void>();
    boolean enabledFocus = getFocusManager().isFocusTransferEnabled();
    if (focused || requestFocus) {
      if (enabledFocus) {
        return requestFocusForComponent().doWhenProcessed(() -> selection.run().notify(result));
      }
      return selection.run().notify(result);
    }
    else {
      return selection.run().notify(result);
    }
  }

  @Nonnull
  protected abstract AsyncResult<Void> requestFocusForComponent();

  protected abstract boolean isSelectionHoldsFocus();

  @Nonnull
  @Override
  public AsyncResult<Void> setSelectedContentCB(@Nonnull Content content) {
    return setSelectedContentCB(content, false);
  }

  @Override
  public void setSelectedContent(@Nonnull final Content content) {
    setSelectedContentCB(content);
  }

  @Override
  public AsyncResult<Void> selectPreviousContent() {
    int contentCount = getContentCount();
    LOG.assertTrue(contentCount > 1);
    Content selectedContent = getSelectedContent();
    int index = getIndexOfContent(selectedContent);
    index = (index - 1 + contentCount) % contentCount;
    final Content content = getContent(index);
    if (content == null) {
      return null;
    }
    return setSelectedContentCB(content, true);
  }

  @Override
  public AsyncResult<Void> selectNextContent() {
    int contentCount = getContentCount();
    LOG.assertTrue(contentCount > 1);
    Content selectedContent = getSelectedContent();
    int index = getIndexOfContent(selectedContent);
    index = (index + 1) % contentCount;
    final Content content = getContent(index);
    if (content == null) {
      return null;
    }
    return setSelectedContentCB(content, true);
  }

  @Override
  public void addContentManagerListener(@Nonnull ContentManagerListener l) {
    myDispatcher.getListeners().add(0, l);
  }

  @Override
  public void removeContentManagerListener(@Nonnull ContentManagerListener l) {
    myDispatcher.removeListener(l);
  }

  private void fireContentAdded(@Nonnull Content content, int newIndex) {
    ContentManagerEvent e = new ContentManagerEvent(this, content, newIndex, ContentManagerEvent.ContentOperation.add);
    myDispatcher.getMulticaster().contentAdded(e);
  }

  private void fireContentRemoved(@Nonnull Content content, int oldIndex) {
    ContentManagerEvent e = new ContentManagerEvent(this, content, oldIndex, ContentManagerEvent.ContentOperation.remove);
    myDispatcher.getMulticaster().contentRemoved(e);
  }

  private void fireSelectionChanged(@Nonnull Content content, ContentManagerEvent.ContentOperation operation) {
    ContentManagerEvent e = new ContentManagerEvent(this, content, getIndexOfContent(content), operation);
    myDispatcher.getMulticaster().selectionChanged(e);
  }

  private boolean fireContentRemoveQuery(@Nonnull Content content, int oldIndex, ContentManagerEvent.ContentOperation operation) {
    ContentManagerEvent event = new ContentManagerEvent(this, content, oldIndex, operation);
    for (ContentManagerListener listener : myDispatcher.getListeners()) {
      listener.contentRemoveQuery(event);
      if (event.isConsumed()) {
        return false;
      }
    }
    return true;
  }

  protected IdeFocusManager getFocusManager() {
    return IdeFocusManager.getInstance(myProject);
  }

  @Override
  public void addDataProvider(@Nonnull final DataProvider provider) {
    myDataProviders.add(provider);
  }

  @Override
  public void propertyChange(@Nonnull PropertyChangeEvent event) {
    if (Content.PROP_COMPONENT.equals(event.getPropertyName())) {
      myContentWithChangedComponent.add((Content)event.getSource());
    }
  }

  @Override
  public void beforeTreeDispose() {
    myUI.beforeDispose();
  }

  @Override
  public void dispose() {
    myDisposed = true;

    myContents.clear();
    mySelection.clear();
    myContentWithChangedComponent.clear();
    myUI = null;
    myDispatcher.getListeners().clear();
    myDataProviders.clear();
  }

  @Override
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public boolean isSingleSelection() {
    return myUI.isSingleSelection();
  }
}
