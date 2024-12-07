// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.ui;

import consulo.ui.ex.awt.ErrorLabel;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.OpaquePanel;
import consulo.ide.impl.idea.ui.popup.list.IconListPopupRenderer;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

public class PopupListElementRendererWithIcon extends PopupListElementRenderer<Object> implements IconListPopupRenderer {
  protected IconComponent myIconLabel;

  public PopupListElementRendererWithIcon(ListPopupImpl aPopup) {
    super(aPopup);
  }

  @Override
  public boolean isIconAt(@Nonnull Point point) {
    JList list = ((AWTListPopup) myPopup).getList();
    int index = list.locationToIndex(point);
    Rectangle bounds = list.getCellBounds(index, index);
    Component renderer = getListCellRendererComponent(list, list.getSelectedValue(), index, true, true);
    renderer.setBounds(bounds);
    renderer.doLayout();
    point.translate(-bounds.x, -bounds.y);
    return SwingUtilities.getDeepestComponentAt(renderer, point.x, point.y) instanceof IconComponent;
  }

  @Override
  protected void customizeComponent(JList<?> list, Object value, boolean isSelected) {
    super.customizeComponent(list, value, isSelected);
    myTextLabel.setIcon(null);
    myTextLabel.setDisabledIcon(null);
    myIconLabel.setIcon(TargetAWT.to(isSelected ? myDescriptor.getSelectedIconFor(value) : myDescriptor.getIconFor(value)));
  }

  @Override
  protected JComponent createItemComponent() {
    myTextLabel = new ErrorLabel();
    myTextLabel.setOpaque(true);
    myTextLabel.setBorder(JBUI.Borders.empty(1));

    myIconLabel = new IconComponent();

    JPanel panel = new OpaquePanel(new BorderLayout(), JBColor.WHITE);
    panel.add(myIconLabel, BorderLayout.WEST);
    panel.add(myTextLabel, BorderLayout.CENTER);
    return layoutComponent(panel);
  }

  public static class IconComponent extends JLabel {
  }
}
