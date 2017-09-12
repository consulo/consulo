/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import com.intellij.icons.AllIcons;
import com.intellij.util.containers.hash.LinkedHashMap;
import com.vaadin.ui.AbstractComponentContainer;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.Tab;
import consulo.ui.TabbedLayout;
import consulo.ui.internal.image.WGwtImageUrlCache;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtTabbedLayoutImpl extends AbstractComponentContainer implements TabbedLayout {
  private Map<WGwtTabImpl, com.vaadin.ui.Component> myTabs = new LinkedHashMap<>();

  @Override
  protected TabbedLayoutState getState() {
    return (TabbedLayoutState)super.getState();
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    for (WGwtTabImpl tab : myTabs.keySet()) {
      TabbedLayoutState.TabState tabState = new TabbedLayoutState.TabState();
      tabState.myImageState = tab.getItem().myImageState;
      tabState.myItemSegments = tab.getItem().myItemSegments;
      BiConsumer<Tab, Component> closeHandler = tab.getCloseHandler();
      if (closeHandler != null) {
        tabState.myCloseButton = WGwtImageUrlCache.fixSwingImageRef(AllIcons.Actions.CloseNew).getState();
      }

      getState().myTabStates.add(tabState);
    }
  }

  @NotNull
  @Override
  public Tab createTab() {
    return new WGwtTabImpl();
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public Tab addTab(@NotNull Tab tab, @NotNull Component component) {
    addComponent((com.vaadin.ui.Component)component);
    myTabs.put((WGwtTabImpl)tab, (com.vaadin.ui.Component)component);
    markAsDirtyRecursive();
    return tab;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public Tab addTab(@NotNull String tabName, @NotNull Component component) {
    WGwtTabImpl presentation = new WGwtTabImpl();
    presentation.append(tabName);
    return addTab(presentation, component);
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component oldComponent, com.vaadin.ui.Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return myTabs.size();
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    return myTabs.values().iterator();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
  }
}
