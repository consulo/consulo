// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.uiOld.components.fields;

import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.application.AllIcons;
import consulo.ui.ex.awt.UIUtil;
import consulo.desktop.awt.uiOld.Expandable;
import consulo.desktop.awt.uiOld.components.fields.ExtendableTextComponent.Extension;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.createTooltipText;
import static java.awt.event.InputEvent.CTRL_MASK;
import static java.beans.EventHandler.create;
import static java.util.Collections.singletonList;
import static javax.swing.KeyStroke.getKeyStroke;

/**
 * @author Sergey Malenkov
 */
public abstract class ExpandableSupport<Source extends JComponent> implements Expandable {
  private final Source source;
  private final Function<? super String, String> onShow;
  private final Function<? super String, String> onHide;
  private JBPopup popup;
  private String title;
  private String comment;

  public ExpandableSupport(@Nonnull Source source, Function<? super String, String> onShow, Function<? super String, String> onHide) {
    this.source = source;
    this.onShow = onShow != null ? onShow : Function.identity();
    this.onHide = onHide != null ? onHide : Function.identity();
    source.putClientProperty(Expandable.class, this);
    source.addAncestorListener(create(AncestorListener.class, this, "collapse"));
    source.addComponentListener(create(ComponentListener.class, this, "collapse"));
  }

  /**
   * @param source the source expandable component covered by the popup
   * @param onShow a string converter from the source to the popup content
   * @return a specific content to create the popup
   */
  @Nonnull
  protected abstract Content prepare(@Nonnull Source source, @Nonnull Function<? super String, String> onShow);

  protected interface Content {
    /**
     * @return a component to show on the popup
     */
    @Nonnull
    JComponent getContentComponent();

    /**
     * @return a component to focus on after showing the popup
     */
    JComponent getFocusableComponent();

    /**
     * This method is called after closing the popup.
     *
     * @param onHide a string converter from the popup content to the source
     */
    void cancel(@Nonnull Function<? super String, String> onHide);
  }

  /**
   * @return a text from the popup's header or {@code null} if header is hidden.
   */
  public final String getTitle() {
    return title;
  }

  /**
   * @param title a text for the popup's header or {@code null} if header is not needed
   */
  public final void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return a text from the popup's footer or {@code null} if footer is hidden.
   */
  public final String getComment() {
    return comment;
  }

  /**
   * @param comment a text for the popup's footer or {@code null} if footer is not needed
   */
  public final void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public final boolean isExpanded() {
    return popup != null;
  }

  @Override
  public final void collapse() {
    if (popup != null) popup.cancel();
  }

  @Override
  public final void expand() {
    if (popup != null || !source.isEnabled()) return;

    Content content = prepare(source, onShow);
    JComponent component = content.getContentComponent();
    Dimension size = component.getPreferredSize();
    if (size.width - 50 < source.getWidth()) size.width = source.getWidth();
    if (size.height < 2 * source.getHeight()) size.height = 2 * source.getHeight();

    Point location = new Point(0, 0);
    SwingUtilities.convertPointToScreen(location, source);
    Rectangle screen = ScreenUtil.getScreenRectangle(source);
    int bottom = screen.y - location.y + screen.height;
    if (bottom < size.height) {
      int top = location.y - screen.y + source.getHeight();
      if (top < bottom) {
        size.height = bottom;
      }
      else {
        if (size.height > top) size.height = top;
        location.y -= size.height - source.getHeight();
      }
    }
    component.setPreferredSize(size);

    popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, content.getFocusableComponent()).setMayBeParent(true) // this creates a popup as a dialog with alwaysOnTop=false
            .setFocusable(true).setRequestFocus(true).setTitle(title).setAdText(comment).setLocateByContent(true).setCancelOnWindowDeactivation(false)
            .setKeyboardActions(singletonList(Pair.create(event -> {
              collapse();
              Window window = UIUtil.getWindow(source);
              if (window != null) {
                window.dispatchEvent(new KeyEvent(source, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), CTRL_MASK, KeyEvent.VK_ENTER, '\r'));
              }
            }, getKeyStroke(KeyEvent.VK_ENTER, CTRL_MASK)))).setCancelCallback(() -> {
              try {
                content.cancel(onHide);
                popup = null;
                return true;
              }
              catch (Exception ignore) {
                return false;
              }
            }).createPopup();
    popup.show(new RelativePoint(location));
  }

  @Nonnull
  public Extension createCollapseExtension() {
    return Extension.create(AllIcons.General.CollapseComponent, AllIcons.General.CollapseComponentHover, createTooltipText("Collapse", "CollapseExpandableComponent"), this::collapse);
  }

  @Nonnull
  public Extension createExpandExtension() {
    return Extension.create(AllIcons.General.ExpandComponent, AllIcons.General.ExpandComponentHover, createTooltipText("Expand", "ExpandExpandableComponent"), this::expand);
  }

  @Nonnull
  public static JLabel createLabel(@Nonnull Extension extension) {
    return new JLabel(TargetAWT.to(extension.getIcon(false))) {{
      setToolTipText(extension.getTooltip());
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent event) {
          setIcon(TargetAWT.to(extension.getIcon(true)));
        }

        @Override
        public void mouseExited(MouseEvent event) {
          setIcon(TargetAWT.to(extension.getIcon(false)));
        }

        @Override
        public void mousePressed(MouseEvent event) {
          Consumer<AWTEvent> action = extension.getActionOnClick();
          if (action != null) action.accept(event);
        }
      });
    }};
  }
}
