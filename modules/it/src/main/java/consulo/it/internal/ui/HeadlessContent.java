/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.ui;

import consulo.component.util.BusyObject;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.content.AlertIcon;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Headless {@link Content}: a plain value holder without any presentation logic, produced by
 * {@link consulo.it.internal.HeadlessContentFactory}.
 *
 * @author VISTALL
 */
public class HeadlessContent extends UserDataHolderBase implements Content {
    private @Nullable Component myUIComponent;
    private String myDisplayName;
    private String myTabName;
    private String myToolwindowTitle;
    private String myDescription;
    private boolean myPinned;
    private boolean myPinnable;
    private boolean myCloseable;
    private long myExecutionId;

    private @Nullable ContentManager myManager;
    private @Nullable Disposable myDisposer;
    private @Nullable Image myIcon;
    private @Nullable Image myPopupIcon;
    private @Nullable BusyObject myBusyObject;
    private @Nullable AlertIcon myAlertIcon;
    private @Nullable String mySeparator;

    public HeadlessContent(@Nullable Component component, String displayName, boolean isPinnable) {
        myUIComponent = component;
        myDisplayName = displayName;
        myTabName = displayName;
        myToolwindowTitle = displayName;
        myDescription = displayName;
        myPinnable = isPinnable;
        myCloseable = true;
    }

    public void setManager(@Nullable ContentManager manager) {
        myManager = manager;
    }

    @Override
    public void setUIComponent(@Nullable Component component) {
        myUIComponent = component;
    }

    @Override
    public @Nullable Component getUIComponent() {
        return myUIComponent;
    }

    @Override
    public @Nullable Component getUIPreferredFocusableComponent() {
        return myUIComponent;
    }

    @Override
    public void setUIPreferredFocusableComponent(Component component) {
    }

    @Override
    public void setIcon(Image icon) {
        myIcon = icon;
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public void setDisplayName(String displayName) {
        myDisplayName = displayName;
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
        return myTabName;
    }

    @Override
    public void setToolwindowTitle(String toolwindowTitle) {
        myToolwindowTitle = toolwindowTitle;
    }

    @Override
    public String getToolwindowTitle() {
        return myToolwindowTitle;
    }

    @Override
    public @Nullable Disposable getDisposer() {
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
        myDescription = description;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public @Nullable ContentManager getManager() {
        return myManager;
    }

    @Override
    public boolean isSelected() {
        return myManager != null && myManager.isSelected(this);
    }

    @Override
    public void release() {
        if (myDisposer != null) {
            myDisposer.disposeWithTree();
            myDisposer = null;
        }
    }

    @Override
    public boolean isValid() {
        return myManager != null;
    }

    @Override
    public boolean isPinned() {
        return myPinned;
    }

    @Override
    public void setPinned(boolean pinned) {
        myPinned = pinned;
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
        myCloseable = closeable;
    }

    @Override
    public void setActions(ActionGroup actions, String place, @Nullable JComponent contextComponent) {
    }

    @Override
    public void setSearchComponent(@Nullable JComponent comp) {
    }

    @Override
    public @Nullable ActionGroup getActions() {
        return null;
    }

    @Override
    public @Nullable JComponent getSearchComponent() {
        return null;
    }

    @Override
    public @Nullable String getPlace() {
        return null;
    }

    @Override
    public @Nullable JComponent getActionsContextComponent() {
        return null;
    }

    @Override
    public void setAlertIcon(@Nullable AlertIcon icon) {
        myAlertIcon = icon;
    }

    @Override
    public @Nullable AlertIcon getAlertIcon() {
        return myAlertIcon;
    }

    @Override
    public void fireAlert() {
    }

    @Override
    public @Nullable BusyObject getBusyObject() {
        return myBusyObject;
    }

    @Override
    public void setBusyObject(BusyObject object) {
        myBusyObject = object;
    }

    @Override
    public @Nullable String getSeparator() {
        return mySeparator;
    }

    @Override
    public void setSeparator(String separator) {
        mySeparator = separator;
    }

    @Override
    public void setPopupIcon(@Nullable Image icon) {
        myPopupIcon = icon;
    }

    @Override
    public @Nullable Image getPopupIcon() {
        return myPopupIcon;
    }

    @Override
    public void setExecutionId(long executionId) {
        myExecutionId = executionId;
    }

    @Override
    public long getExecutionId() {
        return myExecutionId;
    }

    @Override
    public void dispose() {
        release();
        myManager = null;
        myUIComponent = null;
    }
}
