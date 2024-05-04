/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.application.AllIcons;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.util.ui.BaseButtonBehavior;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.AnimatedIconComponent;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

class ActionMacroWidget implements CustomStatusBarWidget, Consumer<MouseEvent> {
  private static final String TYPING_SAMPLE = "WWWWWWWWWWWWWWWWWWWW";
  private static final String RECORDED = "Recorded: ";

  private AnimatedIconComponent myIcon = new AnimatedIconComponent("Macro recording",
                                                                   new Image[]{
                                                                     AllIcons.Ide.Macro.Recording_1,
                                                                     AllIcons.Ide.Macro.Recording_2,
                                                                     AllIcons.Ide.Macro.Recording_3,
                                                                     AllIcons.Ide.Macro.Recording_4
                                                                   },
                                                                   AllIcons.Ide.Macro.Recording_1,
                                                                   1000);
  private final WidgetPresentation myPresentation;

  private JPanel myBalloonComponent;
  private Balloon myBalloon;
  private final JLabel myText;
  private String myLastTyping = "";

  ActionMacroWidget() {
    myPresentation = new WidgetPresentation() {
      @Override
      public String getTooltipText() {
        return "Macro is being recorded now";
      }

      @Override
      public Consumer<MouseEvent> getClickConsumer() {
        return ActionMacroWidget.this;
      }
    };


    new BaseButtonBehavior(myIcon) {
      @Override
      protected void execute(MouseEvent e) {
        showBalloon();
      }
    };

    myBalloonComponent = new NonOpaquePanel(new BorderLayout());

    final AnAction stopAction = ActionManager.getInstance().getAction("StartStopMacroRecording");
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(stopAction);
    final ActionToolbar tb = ActionManager.getInstance().createActionToolbar(ActionPlaces.STATUS_BAR_PLACE, group, true);
    tb.setMiniMode(true);

    final NonOpaquePanel top = new NonOpaquePanel(new BorderLayout());
    top.add(tb.getComponent(), BorderLayout.WEST);
    myText = new JLabel(RECORDED + "..." + TYPING_SAMPLE, SwingConstants.LEFT);
    final Dimension preferredSize = myText.getPreferredSize();
    myText.setPreferredSize(preferredSize);
    myText.setText("Macro recording started...");
    myLastTyping = "";
    top.add(myText, BorderLayout.CENTER);
    myBalloonComponent.add(top, BorderLayout.CENTER);
  }

  private void showBalloon() {
    if (myBalloon != null) {
      Disposer.dispose(myBalloon);
      return;
    }

    myBalloon = JBPopupFactory.getInstance()
                              .createBalloonBuilder(myBalloonComponent)
                              .setAnimationCycle(200)
                              .setCloseButtonEnabled(true)
                              .setHideOnAction(false)
                              .setHideOnClickOutside(false)
                              .setHideOnFrameResize(false)
                              .setHideOnKeyOutside(false)
                              .setSmallVariant(true)
                              .setShadow(true)
                              .createBalloon();

    Disposer.register(myBalloon, () -> myBalloon = null);

    myBalloon.addListener(new JBPopupAdapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        if (myBalloon != null) {
          Disposer.dispose(myBalloon);
        }
      }
    });

    myBalloon.show(new PositionTracker<>(myIcon) {
      @Override
      public RelativePoint recalculateLocation(Balloon object) {
        return new RelativePoint(myIcon, new Point(myIcon.getSize().width / 2, 4));
      }
    }, Balloon.Position.above);
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myIcon;
  }

  @Nonnull
  @Override
  public String getId() {
    return "MacroRecording";
  }

  @Override
  public void accept(MouseEvent mouseEvent) {
  }

  @Override
  public WidgetPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    showBalloon();
  }

  @Override
  public void dispose() {
    myIcon.dispose();
    if (myBalloon != null) {
      Disposer.dispose(myBalloon);
    }
  }

  public void notifyUser(String text, boolean typing) {
    String actualText = text;
    if (typing) {
      int maxLength = TYPING_SAMPLE.length();
      myLastTyping += text;
      if (myLastTyping.length() > maxLength) {
        myLastTyping = "..." + myLastTyping.substring(myLastTyping.length() - maxLength);
      }
      actualText = myLastTyping;
    }
    else {
      myLastTyping = "";
    }

    notifyUser(RECORDED + actualText);
  }

  private void notifyUser(String text) {
    myText.setText(text);
    myText.revalidate();
    myText.repaint();
  }
}
