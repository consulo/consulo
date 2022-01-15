// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInsight.hint.HintUtil;
import consulo.disposer.Disposable;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.WindowMoveListener;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.border.BorderStyle;

import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public abstract class BigPopupUI extends BorderLayoutPanel implements Disposable {
  private static final int MINIMAL_SUGGESTIONS_LIST_HEIGHT = 100;

  protected final Project myProject;
  protected TextBoxWithExtensions mySearchField;
  protected JPanel suggestionsPanel;
  protected JBList<Object> myResultsList;
  protected JBPopup myHint;
  protected Runnable searchFinishedHandler = () -> {
  };
  protected final List<ViewTypeListener> myViewTypeListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  protected ViewType myViewType = ViewType.SHORT;
  protected JLabel myHintLabel;

  public BigPopupUI(Project project) {
    myProject = project;
  }

  @Nonnull
  public abstract JBList<Object> createList();

  @Nonnull
  protected abstract ListCellRenderer<Object> createCellRenderer();

  @Nonnull
  protected abstract JPanel createTopLeftPanel();

  @Nonnull
  protected abstract JPanel createSettingsPanel();

  @Nonnull
  protected abstract String getInitialHint();

  protected void installScrollingActions() {
    ScrollingUtil.installActions(myResultsList, (JComponent)TargetAWT.to(getSearchField()));
  }

  //protected static class SearchField extends ExtendableTextField {
  //  public SearchField() {
  //    ExtendableTextField.Extension leftExtension = getLeftExtension();
  //    ExtendableTextField.Extension rightExtension = getRightExtension();
  //    if (leftExtension != null) {
  //      addExtension(leftExtension);
  //    }
  //    if (rightExtension != null) {
  //      addExtension(rightExtension);
  //    }
  //
  //    Insets insets = JBUI.CurrentTheme.BigPopup.searchFieldInsets();
  //    Border empty = JBUI.Borders.empty(insets.top, insets.left, insets.bottom, insets.right);
  //    Border topLine = JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 1, 0, 0, 0);
  //    setBorder(JBUI.Borders.merge(empty, topLine, true));
  //    setBackground(JBUI.CurrentTheme.BigPopup.searchFieldBackground());
  //    setFocusTraversalKeysEnabled(false);
  //
  //    if (Registry.is("new.search.everywhere.use.editor.font")) {
  //      Font editorFont = EditorUtil.getEditorFont();
  //      setFont(editorFont);
  //    }
  //
  //    int fontDelta = Registry.intValue("new.search.everywhere.font.size.delta");
  //    if (fontDelta != 0) {
  //      Font font = getFont();
  //      font = font.deriveFont((float)fontDelta + font.getSize());
  //      setFont(font);
  //    }
  //  }
  //
  //  @Override
  //  public Dimension getPreferredSize() {
  //    Dimension size = super.getPreferredSize();
  //    size.height = Integer.max(JBUIScale.scale(29), size.height);
  //    return size;
  //  }
  //
  //  @Nullable
  //  protected ExtendableTextField.Extension getRightExtension() {
  //    return null;
  //  }
  //
  //  @Nullable
  //  protected ExtendableTextField.Extension getLeftExtension() {
  //    return null;
  //  }
  //}

  @Nonnull
  protected TextBoxWithExtensions createSearchField() {
    return TextBoxWithExtensions.create();
  }

  public void init() {
    withBackground(JBUI.CurrentTheme.BigPopup.headerBackground());

    myResultsList = createList();

    JPanel topLeftPanel = createTopLeftPanel();
    JPanel settingsPanel = createSettingsPanel();
    mySearchField = createSearchField();
    mySearchField.addBorders(BorderStyle.EMPTY, null, 4);
    (TargetAWT.to(mySearchField)).setFocusTraversalKeysEnabled(false);
    suggestionsPanel = createSuggestionsPanel();

    myResultsList.setFocusable(false);
    myResultsList.setCellRenderer(createCellRenderer());

    if (Registry.is("new.search.everywhere.use.editor.font")) {
      Font editorFont = EditorUtil.getEditorFont();
      myResultsList.setFont(editorFont);
    }

    installScrollingActions();

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(topLeftPanel, BorderLayout.WEST);
    top.add(settingsPanel, BorderLayout.EAST);
    top.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 0, 0, 1, 0));

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.setOpaque(false);
    topPanel.add(top, BorderLayout.NORTH);
    topPanel.add(TargetAWT.to(mySearchField), BorderLayout.SOUTH);

    WindowMoveListener moveListener = new WindowMoveListener(this);
    topPanel.addMouseListener(moveListener);
    topPanel.addMouseMotionListener(moveListener);

    addToTop(topPanel);
    addToCenter(suggestionsPanel);

    MnemonicHelper.init(this);
  }

  protected void addListDataListener(@Nonnull AbstractListModel<Object> model) {
    model.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        updateViewType(ViewType.FULL);
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        if (myResultsList.isEmpty() && getSearchPattern().isEmpty()) {
          updateViewType(ViewType.SHORT);
        }
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
        updateViewType(myResultsList.isEmpty() && getSearchPattern().isEmpty() ? ViewType.SHORT : ViewType.FULL);
      }
    });
  }

  @Nonnull
  protected String getSearchPattern() {
    return Optional.ofNullable(mySearchField).map(TextBoxWithExtensions::getValue).orElse("");
  }

  protected void updateViewType(@Nonnull ViewType viewType) {
    if (myViewType != viewType) {
      myViewType = viewType;
      myViewTypeListeners.forEach(listener -> listener.suggestionsShown(viewType));
    }
  }

  private JPanel createSuggestionsPanel() {
    JPanel pnl = new JPanel(new BorderLayout());
    pnl.setOpaque(false);
    pnl.setBorder(JBUI.Borders.customLine(JBUI.CurrentTheme.BigPopup.searchFieldBorderColor(), 1, 0, 0, 0));

    JScrollPane resultsScroll = new JBScrollPane(myResultsList);
    resultsScroll.setBorder(null);
    resultsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    UIUtil.putClientProperty(resultsScroll.getVerticalScrollBar(), JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, true);

    resultsScroll.setPreferredSize(JBUI.size(670, JBUI.CurrentTheme.BigPopup.maxListHeight()));
    pnl.add(resultsScroll, BorderLayout.CENTER);

    myHintLabel = createHint();
    pnl.add(myHintLabel, BorderLayout.SOUTH);

    return pnl;
  }

  @Nonnull
  private JLabel createHint() {
    String hint = getInitialHint();
    JLabel hintLabel = HintUtil.createAdComponent(hint, JBUI.CurrentTheme.BigPopup.advertiserBorder(), SwingConstants.LEFT);
    hintLabel.setForeground(JBUI.CurrentTheme.BigPopup.advertiserForeground());
    hintLabel.setBackground(JBUI.CurrentTheme.BigPopup.advertiserBackground());
    hintLabel.setOpaque(true);
    Dimension size = hintLabel.getPreferredSize();
    size.height = JBUIScale.scale(17);
    hintLabel.setPreferredSize(size);
    return hintLabel;
  }

  @Nonnull
  public TextBoxWithExtensions getSearchField() {
    return mySearchField;
  }

  @Override
  public Dimension getMinimumSize() {
    Dimension size = calcPrefSize(ViewType.SHORT);
    if (getViewType() == ViewType.FULL) {
      size.height += MINIMAL_SUGGESTIONS_LIST_HEIGHT;
    }
    return size;
  }

  @Override
  public Dimension getPreferredSize() {
    return calcPrefSize(myViewType);
  }

  public Dimension getExpandedSize() {
    return calcPrefSize(ViewType.FULL);
  }

  private Dimension calcPrefSize(ViewType viewType) {
    Dimension size = super.getPreferredSize();
    if (viewType == ViewType.SHORT) {
      size.height -= suggestionsPanel.getPreferredSize().height;
    }
    return size;
  }

  public void setSearchFinishedHandler(@Nonnull Runnable searchFinishedHandler) {
    this.searchFinishedHandler = searchFinishedHandler;
  }

  public ViewType getViewType() {
    return myViewType;
  }

  public enum ViewType {
    FULL,
    SHORT
  }

  public interface ViewTypeListener {
    void suggestionsShown(@Nonnull ViewType viewType);
  }

  public void addViewTypeListener(ViewTypeListener listener) {
    myViewTypeListeners.add(listener);
  }

  public void removeViewTypeListener(ViewTypeListener listener) {
    myViewTypeListeners.remove(listener);
  }
}