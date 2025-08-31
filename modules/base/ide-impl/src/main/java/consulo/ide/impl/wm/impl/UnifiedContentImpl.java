/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.wm.impl;

import consulo.component.util.BusyObject;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.AlertIcon;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.util.function.Supplier;

public class UnifiedContentImpl extends UserDataHolderBase implements ContentEx {
  private String myDisplayName;
  private String myDescription;
  private Component myComponent;

  private Image myIcon;

  private final PropertyChangeSupport myChangeSupport = new PropertyChangeSupport(this);
  private ContentManager myManager = null;
  private boolean myIsPinned = false;
  private boolean myPinnable = true;
  private Disposable myDisposer = null;
  private String myTabName;
  private String myToolwindowTitle;
  private boolean myCloseable = true;
  private ActionGroup myActions;
  private String myPlace;

  private AlertIcon myAlertIcon;

  private JComponent myActionsContextComponent;
  private JComponent mySearchComponent;

  private Supplier<Component> myFocusRequest;
  private BusyObject myBusyObject;
  private String mySeparator;
  private Image myPopupIcon;
  private long myExecutionId;

  public UnifiedContentImpl(Component component, String displayName, boolean isPinnable) {
    myComponent = component;
    myDisplayName = displayName;
    myPinnable = isPinnable;
  }

  @Nullable
  @Override
  public Component getUIComponent() {
    return myComponent;
  }

  @Override
  public JComponent getComponent() {
    return (JComponent)TargetAWT.to(getUIComponent());
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return (JComponent)TargetAWT.to(getUIPreferredFocusableComponent());
  }

  @Override
  public void setUIComponent(Component component) {
    Component oldComponent = myComponent;
    myComponent = component;
    myChangeSupport.firePropertyChange(PROP_COMPONENT, oldComponent, myComponent);
  }

  @Override
  public Component getUIPreferredFocusableComponent() {
    return myFocusRequest == null ? myComponent : myFocusRequest.get();
  }

  @Override
  public void setUIPreferredFocusableComponent(Component c) {
    setUIPreferredFocusedComponent(() -> c);
  }

  @Override
  public void setUIPreferredFocusedComponent(Supplier<Component> computable) {
    myFocusRequest = computable;
  }

  @Override
  public void setIcon(Image icon) {
    Image oldValue = getIcon();
    myIcon = icon;
    myChangeSupport.firePropertyChange(PROP_ICON, oldValue, getIcon());
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }

  @Override
  public void setDisplayName(String displayName) {
    String oldValue = myDisplayName;
    myDisplayName = displayName;
    myChangeSupport.firePropertyChange(PROP_DISPLAY_NAME, oldValue, myDisplayName);
  }

  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  @Override
  public void setTabName(String tabName) {
    myTabName = tabName;
  }

  @Override
  public String getTabName() {
    if (myTabName != null) return myTabName;
    return myDisplayName;
  }

  @Override
  public void setToolwindowTitle(String toolwindowTitle) {
    myToolwindowTitle = toolwindowTitle;
  }

  @Override
  public String getToolwindowTitle() {
    if (myToolwindowTitle != null) return myToolwindowTitle;
    return myDisplayName;
  }

  @Override
  public Disposable getDisposer() {
    return myDisposer;
  }

  @Override
  public void setDisposer(Disposable disposer) {
    myDisposer = disposer;
  }

  @Override
  public String getDescription() {
    return myDescription;
  }

  @Override
  public void setDescription(String description) {
    String oldValue = myDescription;
    myDescription = description;
    myChangeSupport.firePropertyChange(PROP_DESCRIPTION, oldValue, myDescription);
  }

  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    myChangeSupport.addPropertyChangeListener(l);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    myChangeSupport.removePropertyChangeListener(l);
  }

  @Override
  public void setManager(ContentManager manager) {
    myManager = manager;
  }

  @Override
  public ContentManager getManager() {
    return myManager;
  }

  @Override
  public boolean isSelected() {
    return myManager != null && myManager.isSelected(this);
  }

  @Override
  public final void release() {
    Disposer.dispose(this);
  }

  @Override
  public boolean isValid() {
    return myManager != null;
  }

  @Override
  public boolean isPinned() {
    return myIsPinned;
  }

  @Override
  public void setPinned(boolean pinned) {
    if (isPinnable() && myIsPinned != pinned) {
      boolean wasPinned = isPinned();
      myIsPinned = pinned;
      myChangeSupport.firePropertyChange(PROP_PINNED, wasPinned, pinned);
    }
  }

  @Override
  public boolean isPinnable() {
    return myPinnable;
  }

  @Override
  public void setPinnable(boolean pinnable) {
    myPinnable = pinnable;
  }

  @Override
  public boolean isCloseable() {
    return myCloseable;
  }

  @Override
  public void setCloseable(boolean closeable) {
    if (closeable == myCloseable) return;

    boolean old = myCloseable;
    myCloseable = closeable;
    myChangeSupport.firePropertyChange(IS_CLOSABLE, old, closeable);
  }

  @Override
  public void setActions(ActionGroup actions, String place, @Nullable JComponent contextComponent) {
    ActionGroup oldActions = myActions;
    myActions = actions;
    myPlace = place;
    myActionsContextComponent = contextComponent;
    myChangeSupport.firePropertyChange(PROP_ACTIONS, oldActions, myActions);
  }

  @Override
  public JComponent getActionsContextComponent() {
    return myActionsContextComponent;
  }

  @Override
  public ActionGroup getActions() {
    return myActions;
  }

  @Override
  public String getPlace() {
    return myPlace;
  }

  @NonNls
  public String toString() {
    StringBuilder sb = new StringBuilder("Content name=").append(myDisplayName);
    if (myIsPinned) sb.append(", pinned");
    if (myExecutionId != 0) sb.append(", executionId=").append(myExecutionId);
    return sb.toString();
  }

  @Override
  public void dispose() {
    myComponent = null;
    myFocusRequest = null;
    myManager = null;

    clearUserData();
    if (myDisposer != null) {
      Disposer.dispose(myDisposer);
      myDisposer = null;
    }
  }

  @Override
  @Nullable
  public AlertIcon getAlertIcon() {
    return myAlertIcon;
  }

  @Override
  public void setAlertIcon(@Nullable AlertIcon icon) {
    myAlertIcon = icon;
  }

  @Override
  public void fireAlert() {
    myChangeSupport.firePropertyChange(PROP_ALERT, null, true);
  }

  @Override
  public void setBusyObject(BusyObject object) {
    myBusyObject = object;
  }

  @Override
  public String getSeparator() {
    return mySeparator;
  }

  @Override
  public void setSeparator(String separator) {
    mySeparator = separator;
  }

  @Override
  public void setPopupIcon(Image icon) {
    myPopupIcon = icon;
  }

  @Override
  public Image getPopupIcon() {
    return myPopupIcon != null ? myPopupIcon : getIcon();
  }

  @Override
  public BusyObject getBusyObject() {
    return myBusyObject;
  }

  @Override
  public void setSearchComponent(@Nullable JComponent comp) {
    mySearchComponent = comp;
  }

  @Override
  public JComponent getSearchComponent() {
    return mySearchComponent;
  }

  @Override
  public void setExecutionId(long executionId) {
    myExecutionId = executionId;
  }

  @Override
  public long getExecutionId() {
    return myExecutionId;
  }
}
