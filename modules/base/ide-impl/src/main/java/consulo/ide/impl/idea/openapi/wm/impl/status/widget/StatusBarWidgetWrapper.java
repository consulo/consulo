// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.wm.impl.status.widget;

import consulo.ide.impl.idea.ide.HelpTooltipManager;
import consulo.ide.impl.idea.openapi.wm.impl.status.TextPanel;
import consulo.ide.impl.idea.ui.popup.PopupState;
import consulo.ide.impl.project.ui.impl.StatusWidgetBorders;
import consulo.platform.Platform;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public interface StatusBarWidgetWrapper {
  @Nonnull
  static JComponent wrap(@Nonnull StatusBarWidget widget, @Nonnull StatusBarWidget.WidgetPresentation presentation) {
    if (presentation instanceof StatusBarWidget.IconPresentation) {
      return new StatusBarWidgetWrapper.Icon(widget, (StatusBarWidget.IconPresentation)presentation);
    }
    else if (presentation instanceof StatusBarWidget.TextPresentation) {
      return new StatusBarWidgetWrapper.Text(widget, (StatusBarWidget.TextPresentation)presentation);
    }
    else if (presentation instanceof StatusBarWidget.MultipleTextValuesPresentation) {
      return new StatusBarWidgetWrapper.MultipleTextValues(widget, (StatusBarWidget.MultipleTextValuesPresentation)presentation);
    }
    else {
      throw new IllegalArgumentException("Unable to find a wrapper for presentation: " + presentation.getClass().getSimpleName());
    }
  }

  @Nonnull
  StatusBarWidget.WidgetPresentation getPresentation();

  @RequiredUIAccess
  void beforeUpdate();

  default void setWidgetTooltip(JComponent widgetComponent, @Nullable String toolTipText, @Nullable String shortcutText) {
    widgetComponent.setToolTipText(toolTipText);
    widgetComponent.putClientProperty(HelpTooltipManager.SHORTCUT_PROPERTY, shortcutText);
  }

  final class MultipleTextValues extends TextPanel.WithIconAndArrows implements StatusBarWidgetWrapper {
    private final StatusBarWidget myWidget;
    private final StatusBarWidget.MultipleTextValuesPresentation myPresentation;

    public MultipleTextValues(StatusBarWidget widget, @Nonnull final StatusBarWidget.MultipleTextValuesPresentation presentation) {
      myWidget = widget;
      myPresentation = presentation;
      setVisible(StringUtil.isNotEmpty(myPresentation.getSelectedValue()));
      setTextAlignment(Component.CENTER_ALIGNMENT);
      setBorder(StatusWidgetBorders.WIDE);
      new ClickListener() {
        private final PopupState<JBPopup> myPopupState = PopupState.forPopup();

        @Override
        public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
          if (myPopupState.isRecentlyHidden()) return false; // do not show new popup
          final ListPopup popup = myPresentation.getPopupStep();
          if (popup == null) return false;
          final Dimension dimension = popup.getContent().getPreferredSize();
          final Point at = new Point(0, -dimension.height);
          myPopupState.prepareToShow(popup);
          popup.show(new RelativePoint(e.getComponent(), at));
          return true;
        }
      }.installOn(this, true);
    }

    @Override
    public Font getFont() {
      return Platform.current().os().isMac() ? JBUI.Fonts.label(11) : JBFont.label();
    }

    @RequiredUIAccess
    @Override
    public void beforeUpdate() {
      myWidget.beforeUpdate();

      String value = myPresentation.getSelectedValue();
      setText(value);
      setIcon(myPresentation.getIcon());
      setVisible(StringUtil.isNotEmpty(value));
      setWidgetTooltip(this, myPresentation.getTooltipText(), myPresentation.getShortcutText());
    }

    @Nonnull
    @Override
    public StatusBarWidget.WidgetPresentation getPresentation() {
      return myPresentation;
    }
  }

  final class Text extends TextPanel implements StatusBarWidgetWrapper {
    private final StatusBarWidget myWidget;
    private final StatusBarWidget.TextPresentation myPresentation;

    public Text(StatusBarWidget widget, @Nonnull final StatusBarWidget.TextPresentation presentation) {
      myWidget = widget;
      myPresentation = presentation;
      setTextAlignment(presentation.getAlignment());
      setVisible(!myPresentation.getText().isEmpty());
      setBorder(StatusWidgetBorders.INSTANCE);
      Consumer<MouseEvent> clickConsumer = myPresentation.getClickConsumer();
      if (clickConsumer != null) {
        new StatusBarWidgetClickListener(clickConsumer).installOn(this, true);
      }
    }

    @Nonnull
    @Override
    public StatusBarWidget.WidgetPresentation getPresentation() {
      return myPresentation;
    }

    @RequiredUIAccess
    @Override
    public void beforeUpdate() {
      myWidget.beforeUpdate();

      String text = myPresentation.getText();
      setText(text);
      setVisible(!text.isEmpty());
      setWidgetTooltip(this, myPresentation.getTooltipText(), myPresentation.getShortcutText());
    }
  }

  final class Icon extends TextPanel.WithIconAndArrows implements StatusBarWidgetWrapper {
    private final StatusBarWidget myWidget;
    private final StatusBarWidget.IconPresentation myPresentation;

    public Icon(StatusBarWidget widget, @Nonnull final StatusBarWidget.IconPresentation presentation) {
      myWidget = widget;
      myPresentation = presentation;
      setTextAlignment(Component.CENTER_ALIGNMENT);
      setIcon(myPresentation.getIcon());
      setVisible(hasIcon());
      setBorder(StatusWidgetBorders.ICON);
      Consumer<MouseEvent> clickConsumer = myPresentation.getClickConsumer();
      if (clickConsumer != null) {
        new StatusBarWidgetClickListener(clickConsumer).installOn(this, true);
      }
    }

    @Nonnull
    @Override
    public StatusBarWidget.WidgetPresentation getPresentation() {
      return myPresentation;
    }

    @RequiredUIAccess
    @Override
    public void beforeUpdate() {
      myWidget.beforeUpdate();

      setIcon(myPresentation.getIcon());
      setVisible(hasIcon());
      setWidgetTooltip(this, myPresentation.getTooltipText(), myPresentation.getShortcutText());
    }
  }

  class StatusBarWidgetClickListener extends ClickListener {
    private final Consumer<? super MouseEvent> myClickConsumer;

    public StatusBarWidgetClickListener(@Nonnull Consumer<? super MouseEvent> consumer) {
      myClickConsumer = consumer;
    }

    @Override
    public boolean onClick(@Nonnull MouseEvent e, int clickCount) {
      if (!e.isPopupTrigger() && MouseEvent.BUTTON1 == e.getButton()) {
        myClickConsumer.accept(e);
      }
      return true;
    }
  }
}
