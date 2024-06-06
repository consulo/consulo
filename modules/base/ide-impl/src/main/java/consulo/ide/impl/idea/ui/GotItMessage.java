/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.disposer.Disposable;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.BalloonBuilder;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Konstantin Bulenkov
 */
public class GotItMessage {
  @Nonnull
  private final String myTitle;
  @Nonnull
  private final String myMessage;
  private Runnable myCallback;
  private Disposable myDisposable;
  private boolean myShowCallout = true;

  private GotItMessage(@Nonnull String title, @Nonnull String message) {
    myTitle = title;

    StringBuilder builder = new StringBuilder();
    builder.append("<html><body><div align='center' style=\"font-family: ");
    builder.append(UIUtil.getLabelFont().getFontName());
    builder.append("; ");
    builder.append("font-size: ");
    builder.append(JBUI.scale(12));
    builder.append("pt;\">");
    builder.append(StringUtil.replace(message, "\n", "<br>"));
    builder.append("</div></body></html>");
    myMessage = builder.toString();
  }

  public static GotItMessage createMessage(@Nonnull String title, @Nonnull String message) {
    return new GotItMessage(title, message);
  }

  public GotItMessage setDisposable(Disposable disposable) {
    myDisposable = disposable;
    return this;
  }

  public GotItMessage setCallback(Runnable callback) {
    myCallback = callback;
    return this;
  }

  public GotItMessage setShowCallout(boolean showCallout) {
    myShowCallout = showCallout;
    return this;
  }

  public void show(RelativePoint point, Balloon.Position position) {
    final GotItPanel panel = new GotItPanel();
    panel.myTitle.setText(myTitle);
    panel.myMessage.setText(myMessage);

    panel.myButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    final BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(panel.myRoot);
    if (myDisposable != null) {
      builder.setDisposable(myDisposable);
    }

    builder.setFillColor(UIUtil.getListBackground());
    builder.setHideOnClickOutside(false);
    builder.setHideOnAction(false);
    builder.setHideOnFrameResize(false);
    builder.setHideOnKeyOutside(false);
    builder.setShowCallout(myShowCallout);
    builder.setBlockClicksThroughBalloon(true);
    final Balloon balloon = builder.createBalloon();
    panel.myButton.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        balloon.hide();
        if (myCallback != null) {
          myCallback.run();
        }
      }
    });

    balloon.show(point, position);
  }

}
