// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.action.ui;

import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewItemWithTemplatesPopupPanel<T> extends NewItemSimplePopupPanel {

  protected final JList<T> myTemplatesList;

  private final MyListModel myTemplatesListModel;
  private final Box templatesListHolder;

  private final Collection<TemplatesListVisibilityListener> myVisibilityListeners = new ArrayList<>();

  public NewItemWithTemplatesPopupPanel(List<T> templatesList, ListCellRenderer<T> renderer) {
    setBackground(JBCurrentTheme.NewClassDialog.panelBackground());

    myTemplatesListModel = new MyListModel(templatesList);
    myTemplatesList = createTemplatesList(myTemplatesListModel, renderer);

    ScrollingUtil.installMoveUpAction(myTemplatesList, (JComponent)TargetAWT.to(myTextField));
    ScrollingUtil.installMoveDownAction(myTemplatesList, (JComponent)TargetAWT.to(myTextField));

    JBScrollPane scrollPane = new JBScrollPane(myTemplatesList);
    scrollPane.setBorder(JBUI.Borders.empty());
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    templatesListHolder = new Box(BoxLayout.Y_AXIS);
    Border border = JBUI.Borders
            .merge(JBUI.Borders.emptyTop(JBCurrentTheme.NewClassDialog.fieldsSeparatorWidth()), JBUI.Borders.customLine(JBCurrentTheme.NewClassDialog.bordersColor(), 1, 0, 0, 0), true);

    templatesListHolder.setBorder(border);
    templatesListHolder.add(scrollPane);

    add(templatesListHolder, BorderLayout.CENTER);
  }

  public void addTemplatesVisibilityListener(TemplatesListVisibilityListener listener) {
    myVisibilityListeners.add(listener);
  }

  public void removeTemplatesVisibilityListener(TemplatesListVisibilityListener listener) {
    myVisibilityListeners.remove(listener);
  }

  protected void setTemplatesListVisible(boolean visible) {
    if (templatesListHolder.isVisible() != visible) {
      templatesListHolder.setVisible(visible);
      myVisibilityListeners.forEach(l -> l.visibilityChanged(visible));
    }
  }

  protected void updateTemplatesList(List<T> templatesList) {
    myTemplatesListModel.update(templatesList);
  }

  @Nonnull
  private JBList<T> createTemplatesList(@Nonnull ListModel<T> model, ListCellRenderer<T> renderer) {
    JBList<T> list = new JBList<>(model);
    MouseAdapter mouseListener = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (myApplyAction != null && e.getClickCount() > 1) myApplyAction.accept(e);
      }
    };

    list.addMouseListener(mouseListener);
    list.setCellRenderer(renderer);
    list.setFocusable(false);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    Border border = JBUI.Borders.merge(JBUI.Borders.empty(0), JBUI.Borders.customLine(JBCurrentTheme.NewClassDialog.bordersColor(), 1, 0, 0, 0), true);
    list.setBorder(border);
    return list;
  }

  protected class MyListModel extends AbstractListModel<T> {

    private final List<T> myItems = new ArrayList<>();

    private MyListModel(List<T> items) {
      myItems.addAll(items);
    }

    public void update(List<T> newItems) {
      if (!myItems.isEmpty()) {
        int end = myItems.size() - 1;
        myItems.clear();
        fireIntervalRemoved(this, 0, end);
      }
      if (!newItems.isEmpty()) {
        myItems.addAll(newItems);
        fireIntervalAdded(this, 0, myItems.size() - 1);
      }
    }

    @Override
    public int getSize() {
      return myItems.size();
    }

    @Override
    public T getElementAt(int index) {
      return myItems.get(index);
    }
  }
}
