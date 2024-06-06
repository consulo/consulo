// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package consulo.ide.impl.idea.ui.speedSearch;

import consulo.ui.ex.awt.util.PopupUtil;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awt.SearchTextField;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataProvider;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.ComponentWithEmptyText;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.speedSearch.SpeedSearch;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.util.function.Function;

public class ListWithFilter<T> extends JPanel implements DataProvider {
  private final JList<T> myList;
  private final SearchTextField mySearchField = new SearchTextField(false);
  private final NameFilteringListModel<T> myModel;
  private final JScrollPane myScrollPane;
  private final MySpeedSearch mySpeedSearch;
  private boolean myAutoPackHeight = true;

  @Override
  public Object getData(@Nonnull Key dataId) {
    if (SpeedSearchSupply.SPEED_SEARCH_CURRENT_QUERY == dataId) {
      return mySearchField.getText();
    }
    return null;
  }

  @Nonnull
  public static <T> JComponent wrap(@Nonnull JList<? extends T> list, @Nonnull JScrollPane scrollPane, @Nullable Function<? super T, String> namer) {
    return wrap(list, scrollPane, namer, false);
  }

  @Nonnull
  public static <T> JComponent wrap(@Nonnull JList<? extends T> list, @Nonnull JScrollPane scrollPane, @Nullable Function<? super T, String> namer, boolean highlightAllOccurrences) {
    return new ListWithFilter<>(list, scrollPane, namer, highlightAllOccurrences);
  }

  private ListWithFilter(@Nonnull JList<T> list, @Nonnull JScrollPane scrollPane, @Nullable Function<? super T, String> namer, boolean highlightAllOccurrences) {
    super(new BorderLayout());

    if (list instanceof ComponentWithEmptyText) {
      ((ComponentWithEmptyText)list).getEmptyText().setText(UIBundle.message("message.noMatchesFound"));
    }

    myList = list;
    myScrollPane = scrollPane;

    mySearchField.getTextEditor().setFocusable(false);
    mySearchField.setVisible(false);

    add(mySearchField, BorderLayout.NORTH);
    add(myScrollPane, BorderLayout.CENTER);

    mySpeedSearch = new MySpeedSearch(highlightAllOccurrences);
    mySpeedSearch.setEnabled(namer != null);

    myList.addKeyListener(mySpeedSearch);
    int selectedIndex = myList.getSelectedIndex();
    int modelSize = myList.getModel().getSize();
    myModel = new NameFilteringListModel<>(myList.getModel(), namer, mySpeedSearch::shouldBeShowing, () -> StringUtil.notNullize(mySpeedSearch.getFilter()));
    myList.setModel(myModel);
    if (myModel.getSize() == modelSize) {
      myList.setSelectedIndex(selectedIndex);
    }

    setBackground(list.getBackground());
    //setFocusable(true);
  }

  @Override
  protected void processFocusEvent(FocusEvent e) {
    super.processFocusEvent(e);
    if (e.getID() == FocusEvent.FOCUS_GAINED) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myList, true));
    }
  }

  public boolean resetFilter() {
    boolean hadPattern = mySpeedSearch.isHoldingFilter();
    if (mySearchField.isVisible()) {
      mySpeedSearch.reset();
    }
    return hadPattern;
  }

  public SpeedSearch getSpeedSearch() {
    return mySpeedSearch;
  }

  private class MySpeedSearch extends SpeedSearch {
    boolean searchFieldShown;
    boolean myInUpdate;

    private MySpeedSearch(boolean highlightAllOccurrences) {
      super(highlightAllOccurrences);
      // native mac "clear button" is not captured by SearchTextField.onFieldCleared
      mySearchField.addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(@Nonnull DocumentEvent e) {
          if (myInUpdate) return;
          if (mySearchField.getText().isEmpty()) {
            mySpeedSearch.reset();
          }
        }
      });
      installSupplyTo(myList);
    }

    @Override
    public void update() {
      myInUpdate = true;
      mySearchField.getTextEditor().setBackground(UIUtil.getTextFieldBackground());
      onSpeedSearchPatternChanged();
      mySearchField.setText(getFilter());
      if (isHoldingFilter() && !searchFieldShown) {
        mySearchField.setVisible(true);
        searchFieldShown = true;
      }
      else if (!isHoldingFilter() && searchFieldShown) {
        mySearchField.setVisible(false);
        searchFieldShown = false;
      }

      myInUpdate = false;
      revalidate();
    }

    @Override
    public void noHits() {
      mySearchField.getTextEditor().setBackground(LightColors.RED);
    }

    private void revalidate() {
      JBPopup popup = PopupUtil.getPopupContainerFor(mySearchField);
      if (popup != null) {
        popup.pack(false, myAutoPackHeight);
      }
      ListWithFilter.this.revalidate();
    }
  }

  protected void onSpeedSearchPatternChanged() {
    T prevSelection = myList.getSelectedValue(); // save to restore the selection on filter drop
    myModel.refilter();
    if (myModel.getSize() > 0) {
      int fullMatchIndex = mySpeedSearch.isHoldingFilter() ? myModel.getClosestMatchIndex() : myModel.getElementIndex(prevSelection);
      if (fullMatchIndex != -1) {
        myList.setSelectedIndex(fullMatchIndex);
      }

      if (myModel.getSize() <= myList.getSelectedIndex() || !myModel.contains(myList.getSelectedValue())) {
        myList.setSelectedIndex(0);
      }
    }
    else {
      mySpeedSearch.noHits();
      revalidate();
    }
  }

  @Nonnull
  public JList<T> getList() {
    return myList;
  }

  @Nonnull
  public JScrollPane getScrollPane() {
    return myScrollPane;
  }

  public void setAutoPackHeight(boolean autoPackHeight) {
    myAutoPackHeight = autoPackHeight;
  }

  @Override
  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myList, true));
  }
}
