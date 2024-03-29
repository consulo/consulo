/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.ui.ex.action.*;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.TestOnly;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

/**
 * @author Konstantin Bulenkov
 */
public class ActionLink extends LinkLabel implements DataProvider {
  private final AnAction myAction;
  private String myPlace = ActionPlaces.UNKNOWN;
  private InputEvent myEvent;
  private Color myVisitedColor;
  private Color myActiveColor;
  private Color myNormalColor;

  public ActionLink(String text, @Nonnull AnAction action) {
    this(text, Image.empty(0, 12), action);
  }

  public ActionLink(String text, Image icon, @Nonnull AnAction action) {
    this(text, icon, action, null);
  }

  public ActionLink(String text, Image icon, @Nonnull AnAction action, @Nullable final Runnable onDone) {
    super(text, icon);
    setListener(new LinkListener() {
      @Override
      public void linkSelected(LinkLabel aSource, Object aLinkData) {
        final Presentation presentation = myAction.getTemplatePresentation().clone();
        final AnActionEvent event = new AnActionEvent(myEvent,
                                                      DataManager.getInstance().getDataContext(ActionLink.this),
                                                      myPlace,
                                                      presentation,
                                                      ActionManager.getInstance(),
                                                      0);
        ActionManagerEx actionManagerEx = ActionManagerEx.getInstanceEx();
        actionManagerEx.performDumbAwareUpdate(myAction, event, true);
        if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
          myAction.actionPerformed(event);
          if (onDone != null) {
            onDone.run();
          }
        }
      }
    }, null);
    myAction = action;
  }

  @Override
  public void doClick(InputEvent e) {
    myEvent = e;
    super.doClick();
  }

  @Override
  protected Color getVisited() {
    return myVisitedColor == null ? super.getVisited() : myVisitedColor;
  }

  public Color getActiveColor() {
    return myActiveColor == null ? super.getActive() : myActiveColor;
  }

  protected Color getTextColor() {
    return myUnderline ? getActiveColor() : getNormal();
  }

  @Override
  protected Color getNormal() {
    return myNormalColor == null ? super.getNormal() : myNormalColor;
  }

  public void setVisitedColor(Color visitedColor) {
    myVisitedColor = visitedColor;
  }

  public void setActiveColor(Color activeColor) {
    myActiveColor = activeColor;
  }

  public void setNormalColor(Color normalColor) {
    myNormalColor = normalColor;
  }

  @Override
  public Object getData(@Nonnull Key dataId) {
    if (UIExAWTDataKey.DOMINANT_HINT_AREA_RECTANGLE == dataId) {
      final Point p = SwingUtilities.getRoot(this).getLocationOnScreen();
      return new Rectangle(p.x, p.y + getHeight(), 0, 0);
    }
    if (UIExAWTDataKey.CONTEXT_MENU_POINT == dataId) {
      return SwingUtilities.convertPoint(this, 0, getHeight(), UIUtil.getRootPane(this));
    }

    return null;
  }

  @TestOnly
  public AnAction getAction() {
    return myAction;
  }
}
