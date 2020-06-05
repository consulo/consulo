/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CloseAction;
import com.intellij.ide.actions.ShowContentAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.actionSystem.impl.MenuItemPresentationFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.ui.content.*;
import com.intellij.ui.content.tabs.PinToolwindowTabAction;
import com.intellij.ui.content.tabs.TabbedContentAction;
import com.intellij.util.Alarm;
import com.intellij.util.ContentUtilEx;
import consulo.disposer.Disposer;
import consulo.ui.layout.DockLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.wm.impl.layout.UnifiedComboContentLayout;
import consulo.wm.impl.layout.UnifiedContentLayout;
import consulo.wm.impl.layout.UnifiedTabContentLayout;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class UnifiedToolWindowContentUI implements ToolWindowContentUI, PropertyChangeListener, DataProvider {
  public static final String POPUP_PLACE = ToolWindowContentUI.POPUP_PLACE;
  // when client property is put in toolwindow component, hides toolwindow label
  public static final String HIDE_ID_LABEL = ToolWindowContentUI.HIDE_ID_LABEL;

  ContentManager myManager;

  private final DockLayout myContent = DockLayout.create();
  private ToolWindowBase myWindow;

  TabbedContentAction.CloseAllAction myCloseAllAction;
  TabbedContentAction.MyNextTabAction myNextTabAction;
  TabbedContentAction.MyPreviousTabAction myPreviousTabAction;

  ShowContentAction myShowContent;

  private UnifiedContentLayout myTabsLayout = new UnifiedTabContentLayout(this);
  private UnifiedContentLayout myComboLayout = new UnifiedComboContentLayout(this);

  private ToolWindowContentUiType myType = ToolWindowContentUiType.TABBED;
  private boolean myShouldNotShowPopup;

  public UnifiedToolWindowContentUI(ToolWindowBase window) {
    myWindow = window;
    // myContent.setOpaque(false);
    // myContent.setFocusable(false);
    //setOpaque(false);

   // myShowContent = new ShowContentAction(myWindow, myContent);

    //setBorder(JBUI.Borders.empty(0, 0, 0, 2));
  }

  public void setType(@Nonnull ToolWindowContentUiType type) {
    if (myType != type) {

      if (myType != null) {
        getCurrentLayout().reset();
      }

      myType = type;

      getCurrentLayout().init();
      rebuild();
    }
  }

  private UnifiedContentLayout getCurrentLayout() {
    assert myManager != null;
    return myType == ToolWindowContentUiType.TABBED ? myTabsLayout : myComboLayout;
  }

  @Override
  public JComponent getComponent() {
    throw new AbstractMethodError();
  }

  @Nonnull
  @Override
  public consulo.ui.Component getUIComponent() {
    return myContent;
  }

  @Override
  public void setManager(@Nonnull final ContentManager manager) {
    if (myManager != null) {
      getCurrentLayout().reset();
    }

    myManager = manager;

    getCurrentLayout().init();

    myManager.addContentManagerListener(new ContentManagerListener() {
      @Override
      public void contentAdded(final ContentManagerEvent event) {
        getCurrentLayout().contentAdded(event);
        event.getContent().addPropertyChangeListener(UnifiedToolWindowContentUI.this);
        rebuild();
      }

      @Override
      public void contentRemoved(final ContentManagerEvent event) {
        event.getContent().removePropertyChangeListener(UnifiedToolWindowContentUI.this);
        getCurrentLayout().contentRemoved(event);
        ensureSelectedContentVisible();
        rebuild();
      }

      @Override
      public void contentRemoveQuery(final ContentManagerEvent event) {
      }

      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        ensureSelectedContentVisible();

        update();

        //myContent.revalidate();
        //myContent.repaint();
      }
    });

    //initMouseListeners(this, this);

    rebuild();

    //myCloseAllAction = new TabbedContentAction.CloseAllAction(myManager);
    //myNextTabAction = new TabbedContentAction.MyNextTabAction(myManager);
   // myPreviousTabAction = new TabbedContentAction.MyPreviousTabAction(myManager);
  }

  @RequiredUIAccess
  private void ensureSelectedContentVisible() {
    final Content selected = myManager.getSelectedContent();
    if (selected == null) {
      myContent.removeAll();
      return;
    }

    /*if (myContent.getComponentCount() == 1) {
      final Component visible = myContent.getComponent(0);
      if (visible == selected.getComponent()) return;
    } */

    myContent.removeAll();
    myContent.center(selected.getUIComponent());
  }


  private void rebuild() {
    getCurrentLayout().rebuild();
    getCurrentLayout().update();


    if (myManager.getContentCount() == 0 && myWindow.isToHideOnEmptyContent()) {
      myWindow.hide(null);
    }
  }


  /*@Override
  public void doLayout() {
    getCurrentLayout().layout();
  }


  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    getCurrentLayout().paintComponent(g);
  }

  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    getCurrentLayout().paintChildren(g);
  }

  @Override
  public Dimension getMinimumSize() {
    Insets insets = getInsets();
    return new Dimension(insets.left + insets.right + getCurrentLayout().getMinimumWidth(), super.getMinimumSize().height);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    size.height = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      final Component each = getComponent(i);
      size.height = Math.max(each.getPreferredSize().height, size.height);
    }
    return size;
  }  */

  @Override
  public void propertyChange(final PropertyChangeEvent evt) {
    update();
  }

  private void update() {
    getCurrentLayout().update();
  }

  @Override
  public boolean isSingleSelection() {
    return true;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return false;
  }

  @Override
  public boolean canBeEmptySelection() {
    return false;
  }

  @Override
  public void beforeDispose() {
  }

  @Override
  public boolean canChangeSelectionTo(@Nonnull Content content, boolean implicit) {
    return true;
  }

  @Nonnull
  @Override
  public String getCloseActionName() {
    return getCurrentLayout().getCloseActionName();
  }

  @Nonnull
  @Override
  public String getCloseAllButThisActionName() {
    return getCurrentLayout().getCloseAllButThisActionName();
  }

  @Nonnull
  @Override
  public String getPreviousContentActionName() {
    return getCurrentLayout().getPreviousContentActionName();
  }

  @Nonnull
  @Override
  public String getNextContentActionName() {
    return getCurrentLayout().getNextContentActionName();
  }


  private void initActionGroup(DefaultActionGroup group, final Content content) {
    if (content == null) {
      return;
    }
    group.addSeparator();
    group.add(new TabbedContentAction.CloseAction(content));
    group.add(myCloseAllAction);
    group.add(new TabbedContentAction.CloseAllButThisAction(content));
    group.addSeparator();
    if (content.isPinnable()) {
      group.add(PinToolwindowTabAction.getPinAction());
      group.addSeparator();
    }

    group.add(myNextTabAction);
    group.add(myPreviousTabAction);
    group.add(myShowContent);

    if (content instanceof TabbedContent && ((TabbedContent)content).getTabs().size() > 1) {
      group.addAction(createSplitTabsAction((TabbedContent)content));
    }

    if (Boolean.TRUE == content.getUserData(Content.TABBED_CONTENT_KEY)) {
      final String groupName = content.getUserData(Content.TAB_GROUP_NAME_KEY);
      if (groupName != null) {
        group.addAction(createMergeTabsAction(myManager, groupName));
      }
    }

    group.addSeparator();
  }

  public void showContextMenu(Component comp, int x, int y, ActionGroup toolWindowGroup, @Nullable Content selectedContent) {
    if (selectedContent == null && toolWindowGroup == null) {
      return;
    }
    DefaultActionGroup group = new DefaultActionGroup();
    if (selectedContent != null) {
      initActionGroup(group, selectedContent);
    }

    if (toolWindowGroup != null) {
      group.addAll(toolWindowGroup);
    }

    final ActionPopupMenu popupMenu = ((ActionManagerImpl)ActionManager.getInstance()).createActionPopupMenu(POPUP_PLACE, group, new MenuItemPresentationFactory(true));
    popupMenu.getComponent().show(comp, x, y);
  }

  private static AnAction createSplitTabsAction(final TabbedContent content) {
    return new DumbAwareAction("Split '" + content.getTitlePrefix() + "' group") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        content.split();
      }
    };
  }

  private static AnAction createMergeTabsAction(final ContentManager manager, final String tabPrefix) {
    return new DumbAwareAction("Merge tabs to '" + tabPrefix + "' group") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        final Content selectedContent = manager.getSelectedContent();
        final List<Pair<String, JComponent>> tabs = new ArrayList<Pair<String, JComponent>>();
        int selectedTab = -1;
        for (Content content : manager.getContents()) {
          if (tabPrefix.equals(content.getUserData(Content.TAB_GROUP_NAME_KEY))) {
            final String label = content.getTabName().substring(tabPrefix.length() + 2);
            final JComponent component = content.getComponent();
            if (content == selectedContent) {
              selectedTab = tabs.size();
            }
            tabs.add(Pair.create(label, component));
            manager.removeContent(content, false);
            content.setComponent(null);
            Disposer.dispose(content);
          }
        }
        PropertiesComponent.getInstance().unsetValue(TabbedContent.SPLIT_PROPERTY_PREFIX + tabPrefix);
        for (int i = 0; i < tabs.size(); i++) {
          final Pair<String, JComponent> tab = tabs.get(i);
          ContentUtilEx.addTabbedContent(manager, tab.second, tabPrefix, tab.first, i == selectedTab);
        }
      }
    };
  }

  private void hideWindow(final MouseEvent e) {
    if (e.isControlDown()) {
      myWindow.fireHiddenSide();
    }
    else {
      myWindow.fireHidden();
    }
  }

  @Override
  @Nullable
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (PlatformDataKeys.TOOL_WINDOW == dataId) return myWindow;

    if (CloseAction.CloseTarget.KEY == dataId) {
      return computeCloseTarget();
    }

    return null;
  }


  private CloseAction.CloseTarget computeCloseTarget() {
    if (myManager.canCloseContents()) {
      Content selected = myManager.getSelectedContent();
      if (selected != null && selected.isCloseable()) {
        return new CloseContentTarget(selected);
      }
    }

    return new HideToolwindowTarget();
  }

  private class HideToolwindowTarget implements CloseAction.CloseTarget {
    @Override
    public void close() {
      myWindow.fireHidden();
    }
  }

  private class CloseContentTarget implements CloseAction.CloseTarget {

    private Content myContent;

    private CloseContentTarget(Content content) {
      myContent = content;
    }

    @Override
    public void close() {
      myManager.removeContent(myContent, true, true, true);
    }
  }

  @Override
  public void dispose() {

  }

  boolean isCurrent(UnifiedContentLayout layout) {
    return getCurrentLayout() == layout;
  }

  public void toggleContentPopup() {
    if (myShouldNotShowPopup) {
      myShouldNotShowPopup = false;
      return;
    }
    final Ref<AnAction> selected = Ref.create();
    final Ref<AnAction> selectedTab = Ref.create();
    final Content[] contents = myManager.getContents();
    final Content selectedContent = myManager.getSelectedContent();
    final AnAction[] actions = new AnAction[contents.length];
    for (int i = 0; i < actions.length; i++) {
      final Content content = contents[i];
      if (content instanceof TabbedContent) {
        final TabbedContent tabbedContent = (TabbedContent)content;

        final List<Pair<String, JComponent>> tabs = ((TabbedContent)content).getTabs();
        final AnAction[] tabActions = new AnAction[tabs.size()];
        for (int j = 0; j < tabActions.length; j++) {
          final int index = j;
          tabActions[j] = new DumbAwareAction(tabs.get(index).first) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
              myManager.setSelectedContent(tabbedContent);
              tabbedContent.selectContent(index);
            }
          };
        }
        final DefaultActionGroup group = new DefaultActionGroup(tabActions);
        group.getTemplatePresentation().setText(((TabbedContent)content).getTitlePrefix());
        group.setPopup(true);
        actions[i] = group;
        if (content == selectedContent) {
          selected.set(group);
          final int selectedIndex = ContentUtilEx.getSelectedTab(tabbedContent);
          if (selectedIndex != -1) {
            selectedTab.set(tabActions[selectedIndex]);
          }
        }
      }
      else {
        actions[i] = new DumbAwareAction(content.getTabName()) {
          @RequiredUIAccess
          @Override
          public void actionPerformed(@Nonnull AnActionEvent e) {
            myManager.setSelectedContent(content, true, true);
          }
        };
        if (content == selectedContent) {
          selected.set(actions[i]);
        }
      }
    }

    final ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(null, new DefaultActionGroup(actions), DataManager.getInstance().getDataContext(myManager.getComponent()), false, true, true, null, -1, new Condition<AnAction>() {
              @Override
              public boolean value(AnAction action) {
                return action == selected.get() || action == selectedTab.get();
              }
            });

    getCurrentLayout().showContentPopup(popup);

    if (selectedContent instanceof TabbedContent) {
      new Alarm(Alarm.ThreadToUse.SWING_THREAD, popup).addRequest(new Runnable() {
        @Override
        public void run() {
          popup.handleSelect(true);
        }
      }, 30);
    }
  }
}
