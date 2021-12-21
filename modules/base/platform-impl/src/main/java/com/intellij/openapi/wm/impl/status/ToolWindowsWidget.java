// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.util.Alarm;
import com.intellij.util.ui.BaseButtonBehavior;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TimedDeadzone;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.desktop.wm.impl.DesktopIdeFrameUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class ToolWindowsWidget implements CustomStatusBarWidget, StatusBarWidget, Disposable, UISettingsListener {
  private final Alarm myAlarm;
  private StatusBar myStatusBar;
  private JBPopup popup;
  private boolean wasExited = false;

  private Label myLabel;

  public ToolWindowsWidget(@Nonnull Disposable parent) {
    myLabel = Label.create();

    // FIXME [VISTALL] desktop hack
    if(Application.get().isSwingApplication()) {
      JComponent awtLabel = (JComponent)TargetAWT.to(myLabel);
      
      new BaseButtonBehavior(awtLabel, TimedDeadzone.NULL) {
        @Override
        protected void execute(MouseEvent e) {
          performAction();
        }
      }.setActionTrigger(MouseEvent.MOUSE_PRESSED);

      IdeEventQueue.getInstance().addDispatcher(e -> {
        if (e instanceof MouseEvent) {
          MouseEvent mouseEvent = (MouseEvent)e;
          if (mouseEvent.getComponent() == null || !SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), SwingUtilities.getWindowAncestor(awtLabel))) {
            return false;
          }

          if (e.getID() == MouseEvent.MOUSE_MOVED && awtLabel.isShowing()) {
            Point p = mouseEvent.getLocationOnScreen();
            Point screen = awtLabel.getLocationOnScreen();
            if (new Rectangle(screen.x - 4, screen.y - 2, awtLabel.getWidth() + 4, awtLabel.getHeight() + 4).contains(p)) {
              mouseEntered();
              wasExited = false;
            }
            else {
              if (!wasExited) {
                wasExited = mouseExited(p);
              }
            }
          }
          else if (e.getID() == MouseEvent.MOUSE_EXITED) {
            //mouse exits WND
            mouseExited(mouseEvent.getLocationOnScreen());
          }
        }
        return false;
      }, parent);
    }

    Disposer.register(this, FocusManager.get().addListener(this::updateIcon));

    Application.get().getMessageBus().connect(this).subscribe(UISettingsListener.TOPIC, this);
    
    myAlarm = new Alarm(parent);
  }

  public boolean mouseExited(Point currentLocationOnScreen) {
    myAlarm.cancelAllRequests();
    if (popup != null && popup.isVisible()) {
      final Point screen = popup.getLocationOnScreen();
      final Rectangle popupScreenRect = new Rectangle(screen.x, screen.y, popup.getSize().width, popup.getSize().height);
      if (!popupScreenRect.contains(currentLocationOnScreen)) {
        myAlarm.cancelAllRequests();
        myAlarm.addRequest(() -> {
          if (popup != null && popup.isVisible()) {
            popup.cancel();
          }
        }, 300);
        return true;
      }
    }
    return false;
  }

  public void mouseEntered() {
    final boolean active = ApplicationManager.getApplication().isActive();
    if (!active) {
      return;
    }
    if (myAlarm.getActiveRequestCount() == 0) {
      myAlarm.addRequest(() -> {
        final IdeFrameEx frame = DesktopIdeFrameUtil.findIdeFrameExFromParent(TargetAWT.to(myLabel));
        if (frame == null) return;

        List<ToolWindow> toolWindows = new ArrayList<>();
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(frame.getProject());
        for (String id : toolWindowManager.getToolWindowIds()) {
          final ToolWindow tw = toolWindowManager.getToolWindow(id);
          if (tw.isAvailable() && tw.isShowStripeButton()) {
            toolWindows.add(tw);
          }
        }
        Collections.sort(toolWindows, (o1, o2) -> StringUtil.naturalCompare(o1.getDisplayName().getValue(), o2.getDisplayName().getValue()));

        final JBList<ToolWindow> list = new JBList<>(toolWindows);
        list.setCellRenderer(new ColoredListCellRenderer<ToolWindow>() {
          @Override
          @RequiredUIAccess
          protected void customizeCellRenderer(@Nonnull JList<? extends ToolWindow> list, ToolWindow value, int index, boolean selected, boolean hasFocus) {
            append(value.getDisplayName().getValue());
            String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(value.getId());
            KeyboardShortcut shortcut = ActionManager.getInstance().getKeyboardShortcut(activateActionId);
            if (shortcut != null) {
              append(" " + KeymapUtil.getShortcutText(shortcut), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
            setIcon(value.getIcon());
            setBorder(JBUI.Borders.empty(2, 10));
          }
        });

        final Dimension size = list.getPreferredSize();
        final JComponent c = (JComponent)TargetAWT.to(myLabel);
        final Insets padding = UIUtil.getListViewportPadding();
        final RelativePoint point = new RelativePoint(c, new Point(-4, -padding.top - padding.bottom - 4 - size.height + (SystemInfo.isMac ? 2 : 0)));

        if (popup != null && popup.isVisible()) {
          return;
        }

        list.setSelectedIndex(list.getItemsCount() - 1);
        PopupChooserBuilder<ToolWindow> builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
        popup = builder.setAutoselectOnMouseMove(true).setRequestFocus(false).setItemChosenCallback((selectedValue) -> {
          if (popup != null) popup.closeOk(null);
          selectedValue.activate(null, true, true);
        }).createPopup();

        list.setVisibleRowCount(30); // override default of 15 set when createPopup() is called

        popup.show(point);
      }, 300);
    }
  }

  @Override
  public void uiSettingsChanged(UISettings uiSettings) {
    updateIcon();
  }

  private void performAction() {
    if (isActive()) {
      UISettings.getInstance().setHideToolStripes(!UISettings.getInstance().getHideToolStripes());
      UISettings.getInstance().fireUISettingsChanged();
    }
  }

  @RequiredUIAccess
  private void updateIcon() {
    myLabel.setToolTipText(null);
    if (isActive()) {
      boolean changes = false;

      if (!myLabel.isVisible()) {
        myLabel.setVisible(true);
        changes = true;
      }

      Image icon = UISettings.getInstance().getHideToolStripes() ? AllIcons.General.TbShown : AllIcons.General.TbHidden;
      if (icon != myLabel.getImage()) {
        myLabel.setImage(icon);
        changes = true;
      }

      //Set<Integer> vks = ToolWindowManagerImpl.getActivateToolWindowVKs();
      //String text = "Click to show or hide the tool window bars";
      //if (vks.size() == 1) {
      //  Integer stroke = vks.iterator().next();
      //  String keystrokeText = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(stroke.intValue(), 0));
      //  text += ".\nDouble-press and hold " + keystrokeText + " to show tool window bars when hidden.";
      //}
      //if (!text.equals(getToolTipText())) {
      //  setToolTipText(text);
      //  changes = true;
      //}

      if (changes) {
        // FIXME [VISTALL] desktop hack
        if (Application.get().isSwingApplication()) {
          Component to = TargetAWT.to(myLabel);

          to.revalidate();
          to.repaint();
        }
      }
    }
    else {
      myLabel.setVisible(false);
      myLabel.setToolTipText(null);
    }
  }

  private boolean isActive() {
    return myStatusBar != null && myStatusBar.getProject() != null && Registry.is("ide.windowSystem.showTooWindowButtonsSwitcher");
  }

  @Nullable
  @Override
  public consulo.ui.Component getUIComponent() {
    return myLabel;
  }

  @Override
  public boolean isUnified() {
    return true;
  }

  @Nonnull
  @Override
  public String ID() {
    return "ToolWindows Widget";
  }

  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    myStatusBar = statusBar;
    updateIcon();
  }

  @Override
  public void dispose() {
    Disposer.dispose(this);
    myStatusBar = null;
    popup = null;
  }
}
