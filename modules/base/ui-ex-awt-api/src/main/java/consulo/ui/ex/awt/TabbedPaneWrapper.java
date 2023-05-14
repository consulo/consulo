/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ui.ex.PrevNextActionsDescriptor;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.internal.TabFactoryBuilder;
import consulo.ui.ex.awt.internal.TabbedPaneHolder;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseListener;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class TabbedPaneWrapper {
  public static final PrevNextActionsDescriptor DEFAULT_PREV_NEXT_SHORTCUTS = new PrevNextActionsDescriptor(IdeActions.ACTION_NEXT_TAB, IdeActions.ACTION_PREVIOUS_TAB);

  protected TabbedPane myTabbedPane;
  protected JComponent myTabbedPaneHolder;

  private TabFactoryBuilder.TabFactory myFactory;

  protected TabbedPaneWrapper(boolean construct) {
    if (construct) {
      init(SwingConstants.TOP, DEFAULT_PREV_NEXT_SHORTCUTS, TabFactoryBuilder.getInstance().createJTabbedPanel(this));
    }
  }

  public TabbedPaneWrapper(@Nonnull Disposable parentDisposable) {
    this(SwingConstants.TOP, DEFAULT_PREV_NEXT_SHORTCUTS, parentDisposable);
  }

  /**
   * Creates tabbed pane wrapper with specified tab placement
   *
   * @param tabPlacement tab placement. It one of the <code>SwingConstants.TOP</code>,
   *                     <code>SwingConstants.LEFT</code>, <code>SwingConstants.BOTTOM</code> or
   *                     <code>SwingConstants.RIGHT</code>.
   */
  public TabbedPaneWrapper(int tabPlacement, PrevNextActionsDescriptor installKeyboardNavigation, @Nonnull Disposable parentDisposable) {
    final TabFactoryBuilder.TabFactory factory;
    if (SwingConstants.BOTTOM == tabPlacement || SwingConstants.TOP == tabPlacement) {
      factory = TabFactoryBuilder.getInstance().createEditorTabPanel(this, null, parentDisposable);
    }
    else {
      factory = TabFactoryBuilder.getInstance().createJTabbedPanel(this);
    }

    init(tabPlacement, installKeyboardNavigation, factory);
  }

  public void init(int tabPlacement, PrevNextActionsDescriptor installKeyboardNavigation, TabFactoryBuilder.TabFactory tabbedPaneFactory) {
    myFactory = tabbedPaneFactory;

    myTabbedPane = createTabbedPane(tabPlacement);
    myTabbedPane.putClientProperty(TabbedPaneWrapper.class, myTabbedPane);
    myTabbedPane.setKeyboardNavigation(installKeyboardNavigation);

    myTabbedPaneHolder = createTabbedPaneHolder();
    myTabbedPaneHolder.add(myTabbedPane.getComponent(), BorderLayout.CENTER);
    myTabbedPaneHolder.setFocusCycleRoot(true);
    myTabbedPaneHolder.setFocusTraversalPolicy(new _MyFocusTraversalPolicy());

    assertIsDispatchThread();
  }

  public boolean isDisposed() {
    return myTabbedPane != null && myTabbedPane.isDisposed();
  }

  private void assertIsDispatchThread() {
    final Application application = ApplicationManager.getApplication();
    if (application != null) {
      application.assertIsDispatchThread();
    }
  }

  public final void addChangeListener(final ChangeListener listener) {
    assertIsDispatchThread();
    myTabbedPane.addChangeListener(listener);
  }

  public final void removeChangeListener(final ChangeListener listener) {
    assertIsDispatchThread();
    myTabbedPane.removeChangeListener(listener);
  }

  protected TabbedPaneHolder createTabbedPaneHolder() {
    return myFactory.createTabbedPaneHolder();
  }

  public final JComponent getComponent() {
    assertIsDispatchThread();
    return myTabbedPaneHolder;
  }

  public final synchronized void addTab(final String title, final consulo.ui.image.Image icon, final JComponent component, final String tip) {
    insertTab(title, icon, component, tip, myTabbedPane.getTabCount());
  }

  public final synchronized void addTab(final String title, final JComponent component) {
    insertTab(title, null, component, null, myTabbedPane.getTabCount());
  }

  public synchronized void insertTab(final String title, final consulo.ui.image.Image icon, final JComponent component, final String tip, final int index) {
    myTabbedPane.insertTab(title, icon, createTabWrapper(component), tip, index);
  }

  protected TabWrapper createTabWrapper(JComponent component) {
    return myFactory.createTabWrapper(component);
  }

  protected TabbedPane createTabbedPane(final int tabPlacement) {
    return myFactory.createTabbedPane(tabPlacement);
  }

  /**
   * @see javax.swing.JTabbedPane#setTabPlacement
   */
  public final void setTabPlacement(final int tabPlacement) {
    assertIsDispatchThread();
    myTabbedPane.setTabPlacement(tabPlacement);
  }

  public final void addMouseListener(final MouseListener listener) {
    assertIsDispatchThread();
    myTabbedPane.addMouseListener(listener);
  }

  public final synchronized int getSelectedIndex() {
    return myTabbedPane.getSelectedIndex();
  }

  /**
   * @see javax.swing.JTabbedPane#getSelectedComponent()
   */
  public final synchronized JComponent getSelectedComponent() {
    // Workaround for JDK 6 bug
    final TabWrapper tabWrapper = myTabbedPane.getTabCount() > 0 ? (TabWrapper)myTabbedPane.getSelectedComponent() : null;
    return tabWrapper != null ? tabWrapper.getComponent() : null;
  }

  public final void setSelectedIndex(final int index) {
    setSelectedIndex(index, true);
  }

  public final void setSelectedIndex(final int index, boolean requestFocus) {
    assertIsDispatchThread();

    final boolean hadFocus = IJSwingUtilities.hasFocus2(myTabbedPaneHolder);
    myTabbedPane.setSelectedIndex(index);
    if (hadFocus && requestFocus) {
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTabbedPaneHolder);
    }
  }

  public final void setSelectedComponent(final JComponent component) {
    assertIsDispatchThread();

    final int index = indexOfComponent(component);
    if (index == -1) {
      throw new IllegalArgumentException("component not found in tabbed pane wrapper");
    }
    setSelectedIndex(index);
  }

  public final synchronized void removeTabAt(final int index) {
    assertIsDispatchThread();

    final boolean hadFocus = IJSwingUtilities.hasFocus2(myTabbedPaneHolder);
    final TabWrapper wrapper = getWrapperAt(index);
    try {
      myTabbedPane.removeTabAt(index);
      if (myTabbedPane.getTabCount() == 0) {
        // to clear BasicTabbedPaneUI.visibleComponent field
        myTabbedPane.revalidate();
      }
      if (hadFocus) {
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTabbedPaneHolder);
      }
    }
    finally {
      wrapper.dispose();
    }
  }

  public final synchronized int getTabCount() {
    return myTabbedPane.getTabCount();
  }

  public final Color getForegroundAt(final int index) {
    assertIsDispatchThread();
    return myTabbedPane.getForegroundAt(index);
  }

  /**
   * @see javax.swing.JTabbedPane#setForegroundAt(int, java.awt.Color)
   */
  public final void setForegroundAt(final int index, final Color color) {
    assertIsDispatchThread();
    myTabbedPane.setForegroundAt(index, color);
  }

  /**
   * @see javax.swing.JTabbedPane#setComponentAt(int, java.awt.Component)
   */
  public final synchronized JComponent getComponentAt(final int i) {
    return getWrapperAt(i).getComponent();
  }

  private TabWrapper getWrapperAt(final int i) {
    return (TabWrapper)myTabbedPane.getComponentAt(i);
  }

  public final void setTitleAt(final int index, final String title) {
    assertIsDispatchThread();
    myTabbedPane.setTitleAt(index, title);
  }

  public final void setToolTipTextAt(final int index, final String toolTipText) {
    assertIsDispatchThread();
    myTabbedPane.setToolTipTextAt(index, toolTipText);
  }

  /**
   * @see javax.swing.JTabbedPane#setComponentAt(int, java.awt.Component)
   */
  public final synchronized void setComponentAt(final int index, final JComponent component) {
    assertIsDispatchThread();
    myTabbedPane.setComponentAt(index, createTabWrapper(component));
  }

  /**
   * @see javax.swing.JTabbedPane#setIconAt(int, javax.swing.Icon)
   */
  public final void setIconAt(final int index, final consulo.ui.image.Image icon) {
    assertIsDispatchThread();
    myTabbedPane.setIconAt(index, icon);
  }

  public final void setEnabledAt(final int index, final boolean enabled) {
    assertIsDispatchThread();
    myTabbedPane.setEnabledAt(index, enabled);
  }

  /**
   * @see javax.swing.JTabbedPane#indexOfComponent(java.awt.Component)
   */
  public final synchronized int indexOfComponent(final JComponent component) {
    for (int i = 0; i < myTabbedPane.getTabCount(); i++) {
      final JComponent c = getWrapperAt(i).getComponent();
      if (c == component) {
        return i;
      }
    }
    return -1;
  }

  /**
   * @see javax.swing.JTabbedPane#getTabLayoutPolicy
   */
  public final synchronized int getTabLayoutPolicy() {
    return myTabbedPane.getTabLayoutPolicy();
  }

  /**
   * @see javax.swing.JTabbedPane#setTabLayoutPolicy
   */
  public final synchronized void setTabLayoutPolicy(final int policy) {
    myTabbedPane.setTabLayoutPolicy(policy);
    final int index = myTabbedPane.getSelectedIndex();
    if (index != -1) {
      myTabbedPane.scrollTabToVisible(index);
    }
  }

  /**
   * @deprecated Keyboard navigation is installed/deinstalled automatically. This method does nothing now.
   */
  public final void installKeyboardNavigation() {
  }

  /**
   * @deprecated Keyboard navigation is installed/deinstalled automatically. This method does nothing now.
   */
  public final void uninstallKeyboardNavigation() {
  }

  public final String getTitleAt(final int i) {
    return myTabbedPane.getTitleAt(i);
  }

  public void setSelectedTitle(@Nullable final String title) {
    if (title == null) return;

    for (int i = 0; i < myTabbedPane.getTabCount(); i++) {
      final String each = myTabbedPane.getTitleAt(i);
      if (title.equals(each)) {
        myTabbedPane.setSelectedIndex(i);
        break;
      }
    }
  }

  @Nullable
  public String getSelectedTitle() {
    return getSelectedIndex() < 0 ? null : getTitleAt(getSelectedIndex());
  }

  public void removeAll() {
    myTabbedPane.removeAll();
  }

  public static final class TabWrapper extends JPanel implements DataProvider {
    private JComponent myComponent;

    private boolean myCustomFocus = true;

    public TabWrapper(@Nonnull final JComponent component) {
      super(new BorderLayout());
      myComponent = component;
      add(component, BorderLayout.CENTER);
    }

    /*
     * Make possible to search down for DataProviders
     */
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (myComponent instanceof DataProvider) {
        return ((DataProvider)myComponent).getData(dataId);
      }
      else {
        return null;
      }
    }

    public JComponent getComponent() {
      return myComponent;
    }

    /**
     * TabWrappers are never reused so we can fix the leak in some LAF's TabbedPane UI by cleanuping ourselves.
     */
    public void dispose() {
      if (myComponent != null) {
        remove(myComponent);
        myComponent = null;
      }
    }

    public void setCustomFocus(boolean customFocus) {
      myCustomFocus = customFocus;
    }

    @Override
    public boolean requestDefaultFocus() {
      if (!myCustomFocus) return super.requestDefaultFocus();
      if (myComponent == null) return false; // Just in case someone requests the focus when we're already removed from the Swing tree.
      final JComponent preferredFocusedComponent = IdeFocusTraversalPolicy.getPreferredFocusedComponent(myComponent);
      if (preferredFocusedComponent != null) {
        if (!preferredFocusedComponent.requestFocusInWindow()) {
          IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(preferredFocusedComponent);
        }
        return true;
      }
      else {
        return myComponent.requestDefaultFocus();
      }
    }

    @Override
    public void requestFocus() {
      if (!myCustomFocus) {
        super.requestFocus();
      }
      else {
        requestDefaultFocus();
      }
    }

    @Override
    public boolean requestFocusInWindow() {
      if (!myCustomFocus) return super.requestFocusInWindow();
      return requestDefaultFocus();
    }
  }

  private final class _MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
    @Override
    public final Component getDefaultComponent(final Container focusCycleRoot) {
      final JComponent component = getSelectedComponent();
      if (component != null) {
        return IdeFocusTraversalPolicy.getPreferredFocusedComponent(component, this);
      }
      else {
        return null;
      }
    }
  }

  public static TabbedPaneWrapper get(JTabbedPane tabs) {
    return (TabbedPaneWrapper)tabs.getClientProperty(TabbedPaneWrapper.class);
  }

  public TabbedPane getTabbedPane() {
    return myTabbedPane;
  }
}
