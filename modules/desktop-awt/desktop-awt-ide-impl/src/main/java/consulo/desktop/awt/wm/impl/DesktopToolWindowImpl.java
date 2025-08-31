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
package consulo.desktop.awt.wm.impl;

import consulo.component.util.BusyObject;
import consulo.desktop.awt.wm.impl.content.DesktopToolWindowContentUi;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.util.concurrent.AsyncResult;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopToolWindowImpl extends ToolWindowBase {
  private DesktopToolWindowContentUi myContentUI;
  private JComponent myComponent;

  private final BusyObject.Impl myShowing = new BusyObject.Impl() {
    @Override
    public boolean isReady() {
      return myComponent != null && myComponent.isShowing();
    }
  };

  protected DesktopToolWindowImpl(DesktopToolWindowManagerImpl toolWindowManager, String id, LocalizeValue displayName, boolean canCloseContent, @Nullable JComponent component, boolean avaliable) {
    super(toolWindowManager, id, displayName, canCloseContent, component, avaliable);
  }

  @RequiredUIAccess
  @Override
  protected void init(boolean canCloseContent, @Nullable Object component) {
    ContentFactory contentFactory = ContentFactory.getInstance();
    myContentUI = new DesktopToolWindowContentUi(this);
    ContentManager contentManager = myContentManager = contentFactory.createContentManager(myContentUI, canCloseContent, myToolWindowManager.getProject());

    if (component != null) {
      Content content = contentFactory.createContent((JComponent)component, "", false);
      contentManager.addContent(content);
      contentManager.setSelectedContent(content, false);
    }

    myComponent = contentManager.getComponent();

    DesktopInternalDecorator.installFocusTraversalPolicy(myComponent, new LayoutFocusTraversalPolicy());

    UiNotifyConnector notifyConnector = new UiNotifyConnector(myComponent, new Activatable() {
      @Override
      public void showNotify() {
        myShowing.onReady();
      }
    });
    Disposer.register(contentManager, notifyConnector);
  }

  public DesktopToolWindowContentUi getContentUI() {
    return myContentUI;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> getReady(@Nonnull Object requestor) {
    AsyncResult<Void> result = AsyncResult.undefined();
    myShowing.getReady(this).doWhenDone(() -> {
      ProjectIdeFocusManager.getInstance(myToolWindowManager.getProject()).doWhenFocusSettlesDown(() -> {
        if (myContentManager.isDisposed()) return;
        myContentManager.getReady(requestor).notify(result);
      });
    });
    return result;
  }

  @Override
  public void setTabDoubleClickActions(@Nonnull AnAction... actions) {
    myContentUI.setTabDoubleClickActions(actions);
  }

  // to avoid ensureContentInitialized call - myContentManager can report canCloseContents without full initialization
  public boolean canCloseContents() {
    return myContentManager.canCloseContents();
  }

  /**
   * @return <code>true</code> if the component passed into constructor is not instance of
   * <code>ContentManager</code> class. Otherwise it delegates the functionality to the
   * passed content manager.
   */
  @Override
  public boolean isAvailable() {
    return myAvailable && myComponent != null;
  }

  @Nullable
  @Override
  public final JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void stretchWidth(int value) {
    ((DesktopToolWindowManagerImpl)myToolWindowManager).stretchWidth(this, value);
  }

  @Override
  public void stretchHeight(int value) {
    ((DesktopToolWindowManagerImpl)myToolWindowManager).stretchHeight(this, value);
  }

  @Override
  public void showContentPopup(InputEvent inputEvent) {
    // called only when tool window is already opened, so, content should be already created
    DesktopToolWindowContentUi.toggleContentPopup(myContentUI, myContentManager);
  }
}
