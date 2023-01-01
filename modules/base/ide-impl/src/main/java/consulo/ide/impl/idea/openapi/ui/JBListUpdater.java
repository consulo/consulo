// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.ui;

import consulo.ide.impl.idea.ui.speedSearch.NameFilteringListModel;
import consulo.ui.ex.popup.ListComponentUpdater;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ui.ex.awt.JBList;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

public class JBListUpdater<T> implements ListComponentUpdater<T> {
  private final JBList myComponent;

  public JBListUpdater(JBList component) {
    myComponent = component;
  }

  @Override
  public void paintBusy(final boolean paintBusy) {
    final Runnable runnable = () -> myComponent.setPaintBusy(paintBusy);
    //ensure start/end order
    SwingUtilities.invokeLater(runnable);
  }

  public JBList getJBList() {
    return myComponent;
  }

  @Override
  public void replaceModel(@Nonnull List<? extends T> data) {
    final Object selectedValue = myComponent.getSelectedValue();
    final int index = myComponent.getSelectedIndex();
    ListModel model = myComponent.getModel();
    if (model instanceof NameFilteringListModel) {
      ((NameFilteringListModel)model).replaceAll(data);
    }
    else if (model instanceof CollectionListModel) {
      ((CollectionListModel)model).replaceAll(data);
    }
    else {
      throw new UnsupportedOperationException("JList model of class " + model.getClass() + " is not supported by JBListUpdater");
    }

    if (index == 0) {
      myComponent.setSelectedIndex(0);
    }
    else {
      myComponent.setSelectedValue(selectedValue, true);
    }
  }
}
