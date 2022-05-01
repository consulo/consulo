// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.popup;

import consulo.language.editor.PlatformDataKeys;
import consulo.ide.impl.idea.openapi.ui.JBListUpdater;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.ex.awt.JBList;
import consulo.ide.impl.idea.ui.speedSearch.ListWithFilter;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataProvider;
import consulo.ui.ex.popup.ListComponentUpdater;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author yole
 */
public class PopupListAdapter<T> implements PopupChooserBuilder.PopupComponentAdapter<T> {
  private final JList myList;
  private final PopupChooserBuilder myBuilder;
  private ListWithFilter myListWithFilter;

  public PopupListAdapter(PopupChooserBuilder builder, JList list) {
    myBuilder = builder;
    myList = list;
  }

  @Override
  public JComponent getComponent() {
    return myList;
  }

  @Override
  public void setRenderer(ListCellRenderer renderer) {
    myList.setCellRenderer(renderer);
  }

  @Override
  public void setItemChosenCallback(Consumer<? super T> callback) {
    myBuilder.setItemChoosenCallback(() -> {
      Object selectedValue = myList.getSelectedValue();
      if (selectedValue != null) {
        callback.accept((T)selectedValue);
      }
    });
  }

  @Override
  public void setItemsChosenCallback(Consumer<? super Set<T>> callback) {
    myBuilder.setItemChoosenCallback(() -> {
      List<T> list = myList.getSelectedValuesList();
      callback.accept(list != null ? new HashSet<>(list) : Collections.<T>emptySet());
    });
  }

  @Override
  public JScrollPane createScrollPane() {
    return myListWithFilter.getScrollPane();
  }

  @Override
  public boolean hasOwnScrollPane() {
    return true;
  }

  @Nullable
  @Override
  public Predicate<KeyEvent> getKeyEventHandler() {
    return InputEvent::isConsumed;
  }

  @Override
  public JComponent buildFinalComponent() {
    myListWithFilter = (ListWithFilter)ListWithFilter.wrap(myList, new MyListWrapper(myList), myBuilder.getItemsNamer());
    myListWithFilter.setAutoPackHeight(myBuilder.isAutoPackHeightOnFiltering());
    return myListWithFilter;
  }

  @Override
  public void addMouseListener(MouseListener listener) {
    myList.addMouseListener(listener);
  }

  @Override
  public void autoSelect() {
    JList list = myList;
    if (list.getSelectedIndex() == -1) {
      list.setSelectedIndex(0);
    }
  }

  @Override
  public ListComponentUpdater<T> getBackgroundUpdater() {
    return new JBListUpdater<>((JBList)(myList));
  }

  @Override
  public void setSelectedValue(T preselection, boolean shouldScroll) {
    myList.setSelectedValue(preselection, shouldScroll);
  }

  @Override
  public void setItemSelectedCallback(Consumer<? super T> c) {
    myList.addListSelectionListener(e -> {
      Object selectedValue = myList.getSelectedValue();
      c.accept((T)selectedValue);
    });
  }

  @Override
  public void setSelectionMode(int selection) {
    myList.setSelectionMode(selection);
  }

  @Override
  public boolean checkResetFilter() {
    return myListWithFilter.resetFilter();
  }

  private class MyListWrapper extends JBScrollPane implements DataProvider {
    private final JList myList;

    private MyListWrapper(final JList list) {
      super(-1);
      list.setVisibleRowCount(myBuilder.getVisibleRowCount());
      setViewportView(list);


      if (myBuilder.isAutoselectOnMouseMove()) {
        ListUtil.installAutoSelectOnMouseMove(list);
      }

      ScrollingUtil.installActions(list);

      setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
      myList = list;
    }

    @Override
    @Nullable
    public Object getData(@Nonnull @NonNls Key dataId) {
      if (PlatformDataKeys.SELECTED_ITEM == dataId) {
        return myList.getSelectedValue();
      }
      if (PlatformDataKeys.SELECTED_ITEMS == dataId) {
        return myList.getSelectedValues();
      }
      return null;
    }

    @Override
    public void setBorder(Border border) {
      if (myList != null) {
        myList.setBorder(border);
      }
    }

    @Override
    public void requestFocus() {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myList, true));
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
      myList.addMouseListener(l);
    }
  }
}
