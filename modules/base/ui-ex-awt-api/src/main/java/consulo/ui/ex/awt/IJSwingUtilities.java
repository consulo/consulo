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
package consulo.ui.ex.awt;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.awt.util.JBSwingUtilities;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.FilteringIterator;
import consulo.util.collection.primitive.ints.IntStack;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class IJSwingUtilities extends JBSwingUtilities {
  public static void invoke(Runnable runnable) {
    Application application = Application.get();
    if (application.isDispatchThread()) {
      runnable.run();
    }
    else {
      application.invokeLater(runnable, application.getNoneModalityState());
    }
  }

  /**
   * @return true if javax.swing.SwingUtilities.findFocusOwner(component) != null
   */
  public static boolean hasFocus(Component component) {
    Component focusOwner = findFocusOwner(component);
    return focusOwner != null;
  }

  private static Component findFocusOwner(Component c) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

    // verify focusOwner is a descendant of c
    for (Component temp = focusOwner; temp != null; temp = (temp instanceof Window) ? null : temp.getParent()) {
      if (temp == c) {
        return focusOwner;
      }
    }

    return null;
  }

  /**
   * @return true if window ancestor of component was most recent focused window and most recent focused component
   * in that window was descended from component
   */
  public static boolean hasFocus2(Component component) {
    WindowManager windowManager = WindowManager.getInstance();
    Window activeWindow = null;
    if (windowManager != null) {
      activeWindow = TargetAWT.to(windowManager.getMostRecentFocusedWindow());
    }

    if (activeWindow == null) {
      return false;
    }
    Component focusedComponent = windowManager.getFocusedComponent(activeWindow);
    if (focusedComponent == null) {
      return false;
    }

    return SwingUtilities.isDescendingFrom(focusedComponent, component);
  }

  @Nonnull
  public static Component getFocusedComponentInWindowOrSelf(@Nonnull Component component) {
    Window window = UIUtil.getWindow(component);
    Component focusedComponent = window == null ? null : WindowManager.getInstance().getFocusedComponent(window);
    return focusedComponent != null ? focusedComponent : component;
  }

  /**
   * This method is copied from <code>SwingUtilities</code>.
   * Returns index of the first occurrence of <code>mnemonic</code>
   * within string <code>text</code>. Matching algorithm is not
   * case-sensitive.
   *
   * @param text     The text to search through, may be null
   * @param mnemonic The mnemonic to find the character for.
   * @return index into the string if exists, otherwise -1
   */
  public static int findDisplayedMnemonicIndex(String text, int mnemonic) {
    if (text == null || mnemonic == '\0') {
      return -1;
    }

    char uc = Character.toUpperCase((char)mnemonic);
    char lc = Character.toLowerCase((char)mnemonic);

    int uci = text.indexOf(uc);
    int lci = text.indexOf(lc);

    if (uci == -1) {
      return lci;
    }
    else if (lci == -1) {
      return uci;
    }
    else {
      return (lci < uci) ? lci : uci;
    }
  }

  public static Iterator<Component> getParents(final Component component) {
    return new Iterator<Component>() {
      private Component myCurrent = component;

      public boolean hasNext() {
        return myCurrent != null && myCurrent.getParent() != null;
      }

      public Component next() {
        myCurrent = myCurrent.getParent();
        return myCurrent;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * @param component - parent component, won't be reached by iterator.
   * @return Component tree traverse {@link Iterator}.
   */
  public static Iterator<Component> getChildren(final Container component) {
    return new Iterator<Component>() {
      private Container myCurrentParent = component;
      private final IntStack myState = new IntStack();
      private int myCurrentIndex = 0;

      public boolean hasNext() {
        return hasNextChild();
      }

      public Component next() {
        Component next = myCurrentParent.getComponent(myCurrentIndex);
        myCurrentIndex++;
        if (next instanceof Container) {
          Container container = ((Container)next);
          if (container.getComponentCount() > 0) {
            myState.push(myCurrentIndex);
            myCurrentIndex = 0;
            myCurrentParent = container;
          }
        }
        while (!hasNextChild()) {
          if (myState.size() == 0) break;
          myCurrentIndex = myState.pop();
          myCurrentParent = myCurrentParent.getParent();
        }
        return next;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }

      private boolean hasNextChild() {
        return myCurrentParent.getComponentCount() > myCurrentIndex;
      }
    };
  }

  @Nullable
  public static <T extends Component> T findParentOfType(Component focusOwner, Class<T> aClass) {
    return (T)ContainerUtil.find(getParents(focusOwner), FilteringIterator.instanceOf(aClass));

  }

  @Nullable
  public static Component findParentByInterface(Component focusOwner, Class aClass) {
    return ContainerUtil.find(getParents(focusOwner), FilteringIterator.instanceOf(aClass));
  }

  public static HyperlinkEvent createHyperlinkEvent(@Nullable String href, @Nonnull Object source) {
    URL url = null;
    try {
      url = new URL(href);
    }
    catch (MalformedURLException ignored) {
    }
    return new HyperlinkEvent(source, HyperlinkEvent.EventType.ACTIVATED, url, href);
  }

  /**
   * A copy of javax.swing.SwingUtilities#updateComponentTreeUI that invokes children updateUI() first
   *
   * @param c component
   * @see javax.swing.SwingUtilities#updateComponentTreeUI
   */
  public static void updateComponentTreeUI(@Nullable Component c) {
    if (c == null) return;
    for (Component component : UIUtil.uiTraverser().withRoot(c).postOrderDfsTraversal()) {
      if (component instanceof JComponent) ((JComponent)component).updateUI();
    }
    c.invalidate();
    c.validate();
    c.repaint();
  }

  public static void moveMousePointerOn(Component component) {
    if (component != null && component.isShowing()) {
      UISettings settings = ApplicationManager.getApplication() == null ? null : UISettings.getInstance();
      if (settings != null && settings.MOVE_MOUSE_ON_DEFAULT_BUTTON) {
        Point point = component.getLocationOnScreen();
        int dx = component.getWidth() / 2;
        int dy = component.getHeight() / 2;
        try {
          new Robot().mouseMove(point.x + dx, point.y + dy);
        }
        catch (AWTException ignored) {
          // robot is not available
        }
      }
    }
  }
}