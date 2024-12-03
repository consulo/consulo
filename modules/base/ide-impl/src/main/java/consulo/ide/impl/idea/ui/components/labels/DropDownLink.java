// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.components.labels;

import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.popup.IPopupChooserBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropDownLink<T> extends LinkLabel<Object> {
  private T chosenItem;

  public DropDownLink(@Nonnull T value, @Nonnull Runnable clickAction) {
    super(value.toString(), UIUtil.getTreeExpandedIcon(), (s, d) -> clickAction.run(), null, null);
    chosenItem = value;
    init();
  }

  public DropDownLink(@Nonnull T value, @Nonnull Function<? super DropDownLink, ? extends JBPopup> popupBuilder) {
    super(value.toString(), UIUtil.getTreeExpandedIcon(), null, null, null);
    chosenItem = value;

    setListener((linkLabel, d) -> {
      JBPopup popup = popupBuilder.apply((DropDownLink)linkLabel);
      Point showPoint = new Point(0, getHeight() + JBUIScale.scale(4));
      popup.show(new RelativePoint(this, showPoint));
    }, null);

    init();
  }

  public DropDownLink(@Nonnull T initialItem, @Nonnull List<T> items, @Nullable Consumer<? super T> itemChosenAction, boolean updateLabel) {
    this(initialItem, linkLabel -> {
      IPopupChooserBuilder<T> popupBuilder = JBPopupFactory.getInstance().createPopupChooserBuilder(items).
              setRenderer(new ColoredListCellRenderer() {
                  @Override
                  protected void customizeCellRenderer(@Nonnull JList list, Object value, int index, boolean selected, boolean hasFocus) {
                      append(value == null ? "" : value.toString());
                  }
              }).
              setItemChosenCallback(t -> {
                if (updateLabel) {
                  linkLabel.setText(t.toString());
                }

                if (itemChosenAction != null && !linkLabel.chosenItem.equals(t)) {
                  itemChosenAction.accept(t);
                }
                linkLabel.chosenItem = t;
              });
      return popupBuilder.createPopup();
    });
  }

  private void init() {
    setIconTextGap(JBUIScale.scale(1));
    setHorizontalAlignment(SwingConstants.LEADING);
    setHorizontalTextPosition(SwingConstants.LEADING);
  }

  public T getChosenItem() {
    return chosenItem;
  }
}
