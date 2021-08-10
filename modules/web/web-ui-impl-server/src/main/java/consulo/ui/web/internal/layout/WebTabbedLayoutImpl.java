/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.web.internal.layout;

import com.intellij.icons.AllIcons;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Tab;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.ui.web.servlet.WebImageMapper;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutClientRpc;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutServerRpc;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebTabbedLayoutImpl extends VaadinComponentDelegate<WebTabbedLayoutImpl.Vaadin> implements TabbedLayout {
  public static class Vaadin extends VaadinComponentContainer {
    private Map<WebTabImpl, com.vaadin.ui.Component> myTabs = new LinkedHashMap<>();

    protected void selectTab(int index) {
      getState().mySelected = index;

      getClientRpc().select(index);
    }

    private void closeTab(int index) {
      WebTabImpl target = null;
      for (WebTabImpl tab : myTabs.keySet()) {
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

      target.getCloseHandler().accept(target, TargetVaddin.from(component));

      int i = 0;
      for (WebTabImpl tab : myTabs.keySet()) {
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

    public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
      com.vaadin.ui.Component vaadinComponent = TargetVaddin.to(component);
      addComponent(vaadinComponent);
      WebTabImpl gwtTab = (WebTabImpl)tab;
      int index = myTabs.size();
      gwtTab.setIndex(index);

      myTabs.put(gwtTab, vaadinComponent);
      markAsDirtyRecursive();

      getState().mySelected = index;
      return tab;
    }

    public Vaadin() {
      registerRpc(new TabbedLayoutServerRpc() {
        @Override
        public void close(int index) {
          closeTab(index);
        }
      });
    }

    @Override
    public TabbedLayoutState getState() {
      return (TabbedLayoutState)super.getState();
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      List<TabbedLayoutState.TabState> tabStates = getState().myTabStates;
      tabStates.clear();

      for (WebTabImpl tab : myTabs.keySet()) {
        TabbedLayoutState.TabState tabState = new TabbedLayoutState.TabState();
        tabState.myImageState = tab.getItem().myImageState;
        tabState.myItemSegments = tab.getItem().myItemSegments;
        BiConsumer<Tab, Component> closeHandler = tab.getCloseHandler();
        if (closeHandler != null) {
          tabState.myCloseButton = WebImageMapper.map(AllIcons.Actions.Close).getState();
          tabState.myCloseHoverButton = WebImageMapper.map(AllIcons.Actions.CloseHovered).getState();
        }

        tabStates.add(tabState);
      }
    }

    @Override
    public int getComponentCount() {
      return myTabs.size();
    }

    @Override
    public Iterator<com.vaadin.ui.Component> iterator() {
      return myTabs.values().iterator();
    }
  }

  void selectTab(int index) {
    getVaadinComponent().selectTab(index);
  }

  void markAsDirty() {
    getVaadinComponent().markAsDirty();
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Nonnull
  @Override
  public Tab createTab() {
    return new WebTabImpl(-1, this);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull Tab tab, @Nonnull Component component) {
    getVaadinComponent().addTab(tab, component);
    return tab;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Tab addTab(@Nonnull String tabName, @Nonnull Component component) {
    WebTabImpl presentation = new WebTabImpl(getVaadinComponent().myTabs.size(), this);
    presentation.append(tabName);
    return addTab(presentation, component);
  }

  @Override
  public void removeTab(@Nonnull Tab tab) {
    // todo
  }
}
