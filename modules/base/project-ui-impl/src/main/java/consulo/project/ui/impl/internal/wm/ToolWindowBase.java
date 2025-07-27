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
package consulo.project.ui.impl.internal.wm;

import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.Label;
import consulo.ui.ModalityState;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.UiActivity;
import consulo.ui.ex.UiActivityMonitor;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.internal.ToolWindowEx;
import consulo.ui.ex.toolWindow.*;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.lazy.LazyValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;

/**
 * @author VISTALL
 * @since 14-Oct-17
 * <p>
 * Extracted part independent part from IDEA ToolWindowImpl (named DesktopToolWindowImpl)
 */
public abstract class ToolWindowBase extends UserDataHolderBase implements ToolWindowEx {
  private static final Logger LOG = Logger.getInstance(ToolWindowBase.class);

  protected final ToolWindowManagerBase myToolWindowManager;
  protected ContentManager myContentManager;

  private final PropertyChangeSupport myChangeSupport;
  private final String myId;
  private LocalizeValue myDisplayName;
  protected boolean myAvailable;

  private ToolWindowInternalDecorator myDecorator;

  private boolean myHideOnEmptyContent = false;
  private boolean myPlaceholderMode;
  private ToolWindowFactory myContentFactory;

  private Image myIcon;

  private boolean myUseLastFocused = true;

    private final LazyValue<Content> myNoneConteLazyValue =
        LazyValue.notNull(() -> ContentFactory.getInstance().createUIContent(Label.create(), "", false));

  @RequiredUIAccess
  protected ToolWindowBase(ToolWindowManagerBase toolWindowManager,
                           String id,
                           LocalizeValue displayName,
                           boolean canCloseContent,
                           @Nullable Object component,
                           boolean available) {
    myToolWindowManager = toolWindowManager;
    myChangeSupport = new PropertyChangeSupport(this);
    myId = id;
    myDisplayName = displayName;
    myAvailable = available;

    init(canCloseContent, component);
  }

  @RequiredUIAccess
  protected abstract void init(boolean canCloseContent, @Nullable Object component);

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return myDisplayName;
  }

  @Override
  public void setDisplayName(@Nonnull LocalizeValue displayName) {
    myDisplayName = displayName;
  }

  @Override
  public final void addPropertyChangeListener(PropertyChangeListener l) {
    myChangeSupport.addPropertyChangeListener(l);
  }

  @Override
  public final void removePropertyChangeListener(PropertyChangeListener l) {
    myChangeSupport.removePropertyChangeListener(l);
  }

  @RequiredUIAccess
  @Override
  public final void activate(Runnable runnable) {
    activate(runnable, true);
  }

  @RequiredUIAccess
  @Override
  public void activate(@Nullable Runnable runnable, boolean autoFocusContents) {
    activate(runnable, autoFocusContents, true);
  }

  @RequiredUIAccess
  @Override
  public void activate(@Nullable Runnable runnable, boolean autoFocusContents, boolean forced) {
    UIAccess.assertIsUIThread();

    UiActivity activity = new UiActivity.Focus("toolWindow:" + myId);
    UiActivityMonitor.getInstance().addActivity(myToolWindowManager.getProject(), activity, ModalityState.nonModal());

    myToolWindowManager.activateToolWindow(myId, forced, autoFocusContents);

    myToolWindowManager.invokeLater(() -> {
      if (runnable != null) {
        runnable.run();
      }
      UiActivityMonitor.getInstance().removeActivity(myToolWindowManager.getProject(), activity);
    });
  }

  @RequiredUIAccess
  @Override
  public final boolean isActive() {
    UIAccess.assertIsUIThread();
    if (myToolWindowManager.isEditorComponentActive()) return false;
    return myToolWindowManager.isToolWindowActive(myId) || myDecorator != null && myDecorator.isFocused();
  }

  @RequiredUIAccess
  @Override
  public final void show(Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.showToolWindow(myId);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public final void hide(@Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.hideToolWindow(myId, false);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public final boolean isVisible() {
    return myToolWindowManager.isToolWindowRegistered(myId) && myToolWindowManager.isToolWindowVisible(myId);
  }

  @Override
  public final ToolWindowAnchor getAnchor() {
    return myToolWindowManager.getToolWindowAnchor(myId);
  }

  @RequiredUIAccess
  @Override
  public final void setAnchor(@Nonnull ToolWindowAnchor anchor, @Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowAnchor(myId, anchor);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public boolean isSplitMode() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.isSplitMode(myId);
  }

  @RequiredUIAccess
  @Override
  public void setContentUiType(@Nonnull ToolWindowContentUiType type, @Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setContentUiType(myId, type);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public void setDefaultContentUiType(@Nonnull ToolWindowContentUiType type) {
    myToolWindowManager.setDefaultContentUiType(this, type);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ToolWindowContentUiType getContentUiType() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.getContentUiType(myId);
  }

  @RequiredUIAccess
  @Override
  public void setSplitMode(boolean isSideTool, @Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setSideTool(myId, isSideTool);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public final void setAutoHide(boolean state) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowAutoHide(myId, state);
  }

  @RequiredUIAccess
  @Override
  public final boolean isAutoHide() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.isToolWindowAutoHide(myId);
  }

  @Nonnull
  @Override
  public final ToolWindowType getType() {
    return myToolWindowManager.getToolWindowType(myId);
  }

  @RequiredUIAccess
  @Override
  public final void setType(@Nonnull ToolWindowType type, @Nullable Runnable runnable) {
    UIAccess.assertIsUIThread();
    myToolWindowManager.setToolWindowType(myId, type);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @RequiredUIAccess
  @Override
  public final ToolWindowType getInternalType() {
    UIAccess.assertIsUIThread();
    return myToolWindowManager.getToolWindowInternalType(myId);
  }

  @Override
  public ToolWindowInternalDecorator getDecorator() {
    return myDecorator;
  }

  @Override
  public void setAdditionalGearActions(ActionGroup additionalGearActions) {
    getDecorator().setAdditionalGearActions(additionalGearActions);
  }

  @Override
  public void setTitleActions(@Nonnull AnAction... actions) {
    getDecorator().setTitleActions(actions);
  }

  @Override
  public void setTabActions(@Nonnull AnAction... actions) {
    getDecorator().setTabActions(actions);
  }

  @RequiredUIAccess
  @Override
  public final void setAvailable(boolean available, Runnable runnable) {
    UIAccess.assertIsUIThread();
    Boolean oldAvailable = myAvailable ? Boolean.TRUE : Boolean.FALSE;
    myAvailable = available;
    myChangeSupport.firePropertyChange(PROP_AVAILABLE, oldAvailable, myAvailable ? Boolean.TRUE : Boolean.FALSE);
    if (runnable != null) {
      myToolWindowManager.invokeLater(runnable);
    }
  }

  @Override
  public void installWatcher(@Nonnull ContentManager contentManager) {
    ContentManagerWatcher.watchContentManager(this, contentManager);
  }

  /**
   * @return <code>true</code> if the component passed into constructor is not instance of
   * <code>ContentManager</code> class. Otherwise it delegates the functionality to the
   * passed content manager.
   */
  @Override
  public boolean isAvailable() {
    return myAvailable;
  }

  @Nonnull
  @Override
  public ContentManager getContentManager() {
    ensureContentInitialized();
    return myContentManager;
  }

  @Nullable
  @Override
  public ContentManager getContentManagerIfCreated() {
    return myContentManager;
  }

  @Override
  @Nonnull
  public final String getId() {
    return myId;
  }

  @RequiredUIAccess
  @Override
  public final String getTitle() {
    UIAccess.assertIsUIThread();
    return getSelectedContent().getDisplayName();
  }

  @RequiredUIAccess
  @Override
  public final void setTitle(String title) {
    UIAccess.assertIsUIThread();
    String oldTitle = getTitle();
    getSelectedContent().setDisplayName(title);
    myChangeSupport.firePropertyChange(PROP_TITLE, oldTitle, title);
  }

  private Content getSelectedContent() {
    Content selected = getContentManager().getSelectedContent();
    return selected != null ? selected : myNoneConteLazyValue.get();
  }

  public void setDecorator(ToolWindowInternalDecorator decorator) {
    myDecorator = decorator;
  }

  public void fireActivated() {
    if (myDecorator != null) {
      myDecorator.fireActivated();
    }
  }

  public void fireHidden() {
    if (myDecorator != null) {
      myDecorator.fireHidden();
    }
  }

  public void fireHiddenSide() {
    if (myDecorator != null) {
      myDecorator.fireHiddenSide();
    }
  }

  public ToolWindowManagerBase getToolWindowManager() {
    return myToolWindowManager;
  }

  @Nullable
  public ActionGroup getPopupGroup() {
    return myDecorator != null ? myDecorator.createPopupGroup() : null;
  }

  @Override
  public void setDefaultState(@Nullable ToolWindowAnchor anchor, @Nullable ToolWindowType type, @Nullable Rectangle2D floatingBounds) {
    myToolWindowManager.setDefaultState(this, anchor, type, floatingBounds);
  }

  @Override
  public void setToHideOnEmptyContent(boolean hideOnEmpty) {
    myHideOnEmptyContent = hideOnEmpty;
  }

  @Override
  public boolean isToHideOnEmptyContent() {
    return myHideOnEmptyContent;
  }

  @Override
  public void setShowStripeButton(boolean show) {
    myToolWindowManager.setShowStripeButton(myId, show);
  }

  @Override
  public boolean isShowStripeButton() {
    return myToolWindowManager.isShowStripeButton(myId);
  }

  @Override
  public boolean isDisposed() {
    return myContentManager.isDisposed();
  }

  public boolean isPlaceholderMode() {
    return myPlaceholderMode;
  }

  public void setPlaceholderMode(boolean placeholderMode) {
    myPlaceholderMode = placeholderMode;
  }

  public void setContentFactory(ToolWindowFactory contentFactory) {
    myContentFactory = contentFactory;
  }

  public void ensureContentInitialized() {
    if (myContentFactory != null) {
      ToolWindowFactory contentFactory = myContentFactory;
      // clear it first to avoid SOE
      myContentFactory = null;

      if (!myToolWindowManager.isUnified()) {
        myContentManager.removeAllContents(false);
        contentFactory.createToolWindowContent(myToolWindowManager.getProject(), this);
      }
      else {
        if (contentFactory.isUnified()) {
          myContentManager.removeAllContents(false);
          contentFactory.createToolWindowContent(myToolWindowManager.getProject(), this);
        }
        else {
          myContentFactory = null;
          // nothing
          // FIXME just leave initialize label
        }
      }
    }
  }

  @Override
  public void setUseLastFocusedOnActivation(boolean focus) {
    myUseLastFocused = focus;
  }

  @Override
  public boolean isUseLastFocusedOnActivation() {
    return myUseLastFocused;
  }

  @RequiredUIAccess
  @Override
  public void setIcon(@Nullable Image icon) {
    UIAccess.assertIsUIThread();
    Object oldIcon = myIcon;
    myIcon = icon;
    myChangeSupport.firePropertyChange(PROP_ICON, oldIcon, icon);
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public Image getIcon() {
    UIAccess.assertIsUIThread();
    return myIcon;
  }
}
