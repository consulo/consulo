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
import consulo.ui.Tab;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.web.internal.image.WGwtImageUrlCache;
import consulo.ui.shared.Size;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutClientRpc;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutServerRpc;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtTabbedLayoutImpl extends AbstractComponentContainer implements TabbedLayout, VaadinWrapper {
  private Map<WGwtTabImpl, com.vaadin.ui.Component> myTabs = new LinkedHashMap<>();

  private TabbedLayoutServerRpc myRpc = new TabbedLayoutServerRpc() {
    @Override
    public void close(int index) {
      closeTab(index);
    }
  };

  public WGwtTabbedLayoutImpl() {
    registerRpc(myRpc);
  }

  protected void selectTab(int index) {
    getState().mySelected = index;

    getClientRpc().select(index);
  }

  private void closeTab(int index) {
    WGwtTabImpl target = null;
    for (WGwtTabImpl tab : myTabs.keySet()) {
      if (tab.getIndex() == index) {
        target = tab;
        break;
      }
    }

    if (target == null) {
      return;
    }

    boolean last = myTabs.size() == index + 1;

    com.vaadin.ui.Component component = myTabs.remove(target);

    target.getCloseHandler().accept(target, (Component)component);

    int i = 0;
    for (WGwtTabImpl tab : myTabs.keySet()) {
      tab.setIndex(i++);
    }

    if (getState().mySelected == index) {
      if (last) {
        getState().mySelected--;
      }
      else {
        getState().mySelected++;
      }

      if (getState().mySelected < 0) {
        getState().mySelected = 0;
      }
    }

    markAsDirty();
  }

  private TabbedLayoutClientRpc getClientRpc() {
    return getRpcProxy(TabbedLayoutClientRpc.class);
  }

  @Override
  protected TabbedLayoutState getState() {
    return (TabbedLayoutState)super.getState();
  }

  @Override
  public void beforeClientResponse(boolean initial) {
    super.beforeClientResponse(initial);

    List<TabbedLayoutState.TabState> tabStates = getState().myTabStates;
    tabStates.clear();

    for (WGwtTabImpl tab : myTabs.keySet()) {
      TabbedLayoutState.TabState tabState = new TabbedLayoutState.TabState();
      tabState.myImageState = tab.getItem().myImageState;
      tabState.myItemSegments = tab.getItem().myItemSegments;
      BiConsumer<Tab, Component> closeHandler = tab.getCloseHandler();
      if (closeHandler != null) {
        tabState.myCloseButton = WGwtImageUrlCache.map(AllIcons.Actions.CloseNew).getState();
        tabState.myCloseHoverButton = WGwtImageUrlCache.map(AllIcons.Actions.CloseNewHovered).getState();
      }

      tabStates.add(tabState);
    }
  }

  @Nonnull
  @Override
  public Tab createTab() {
    return new WGwtTabImpl(-1, this);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
    addComponent((com.vaadin.ui.Component)component);
    WGwtTabImpl gwtTab = (WGwtTabImpl)tab;
    int index = myTabs.size();
    gwtTab.setIndex(index);

    myTabs.put(gwtTab, (com.vaadin.ui.Component)component);
    markAsDirtyRecursive();

    getState().mySelected = index;
    return tab;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull String tabName, @Nonnull Component component) {
    WGwtTabImpl presentation = new WGwtTabImpl(myTabs.size(), this);
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
  public void setSize(@Nonnull Size size) {
  }
}
