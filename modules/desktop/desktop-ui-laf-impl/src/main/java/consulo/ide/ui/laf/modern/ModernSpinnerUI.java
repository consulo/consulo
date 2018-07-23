/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.ui.laf.modern;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.laf.DPIAwareArrowButton;
import org.intellij.lang.annotations.MagicConstant;
import javax.annotation.Nonnull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 18.08.14
 * <p/>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaSpinnerUI}
 */
public class ModernSpinnerUI extends BasicSpinnerUI implements ModernTextBorder.ModernTextUI {
  public static ComponentUI createUI(JComponent c) {
    return new ModernSpinnerUI(c);
  }

  private boolean myFocused;

  private FocusAdapter myFocusListener = new FocusAdapter() {
    @Override
    public void focusGained(FocusEvent e) {
      myFocused = true;
      spinner.repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
      myFocused = false;
      spinner.repaint();
    }
  };

  private MouseEnterHandler myMouseEnterHandler;

  public ModernSpinnerUI(JComponent c) {
    myFocused = false;
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    myMouseEnterHandler.replace(null, c);
  }

  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);

    JComponent editor = ((JSpinner)c).getEditor();
    Component component = editor.getComponents()[0];
    myMouseEnterHandler.replace(component, null);
  }

  @Override
  protected void replaceEditor(JComponent oldEditor, JComponent newEditor) {
    super.replaceEditor(oldEditor, newEditor);
    if (oldEditor != null) {
      oldEditor.getComponents()[0].removeFocusListener(myFocusListener);
    }
    if (newEditor != null) {
      newEditor.getComponents()[0].addFocusListener(myFocusListener);
    }
    myMouseEnterHandler.replace(oldEditor, newEditor);
  }

  @Override
  protected JComponent createEditor() {
    final JComponent editor = super.createEditor();
    Component component = editor.getComponents()[0];
    component.addFocusListener(myFocusListener);
    myMouseEnterHandler.replace(null, component);
    return editor;
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    super.paint(g, c);
    final Border border = spinner.getBorder();
    if (border != null) {
      border.paintBorder(c, g, 0, 0, spinner.getWidth(), spinner.getHeight());
    }
  }

  @Override
  protected Component createPreviousButton() {
    JButton button = createArrow(SwingConstants.SOUTH);
    button.setName("Spinner.previousButton");
    button.setBorder(JBUI.Borders.empty(1));
    installPreviousButtonListeners(button);
    return button;
  }

  @Override
  protected Component createNextButton() {
    JButton button = createArrow(SwingConstants.NORTH);
    button.setName("Spinner.nextButton");
    button.setBorder(JBUI.Borders.empty(1));
    installNextButtonListeners(button);
    return button;
  }


  @Override
  protected LayoutManager createLayout() {
    return new LayoutManagerDelegate(super.createLayout()) {
      @Override
      public void layoutContainer(Container parent) {
        super.layoutContainer(parent);
        final JComponent editor = spinner.getEditor();
        if (editor != null) {
          final Rectangle bounds = editor.getBounds();
          editor.setBounds(bounds.x, bounds.y, bounds.width - JBUI.scale(6), bounds.height);
        }
      }
    };
  }

  private JButton createArrow(@MagicConstant(intValues = {SwingConstants.NORTH, SwingConstants.SOUTH}) int direction) {
    final Color shadow = UIUtil.getPanelBackground();
    final Color darkShadow = UIUtil.getLabelForeground();
    JButton b = new DPIAwareArrowButton(direction, shadow, shadow, darkShadow, shadow) {
      @Override
      public void paint(Graphics g) {
        int y = direction == NORTH ? getHeight() - JBUI.scale(6) : JBUI.scale(2);
        paintTriangle(g, 0, y, 0, direction, ModernSpinnerUI.this.spinner.isEnabled());
      }

      @Override
      public boolean isOpaque() {
        return false;
      }

      @Override
      public void paintTriangle(Graphics g, int x, int y, int size, int direction, boolean isEnabled) {
        final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
        int mid;
        final int w = JBUI.scale(8);
        final int h = JBUI.scale(6);
        mid = w / 2;

        g.setColor(isEnabled ? darkShadow : darkShadow.darker());

        g.translate(x, y);
        switch (direction) {
          case SOUTH:
            g.fillPolygon(new int[]{0, w, mid}, new int[]{JBUI.scale(1), JBUI.scale(1), h}, 3);
            break;
          case NORTH:
            g.fillPolygon(new int[]{0, w, mid}, new int[]{h - JBUI.scale(1), h - JBUI.scale(1), 0}, 3);
            break;
          case WEST:
          case EAST:
        }
        g.translate(-x, -y);
        config.restore();
      }
    };
    Border buttonBorder = UIManager.getBorder("Spinner.arrowButtonBorder");
    if (buttonBorder instanceof UIResource) {
      // Wrap the border to avoid having the UIResource be replaced by
      // the ButtonUI. This is the opposite of using BorderUIResource.
      b.setBorder(new CompoundBorder(buttonBorder, null));
    }
    else {
      b.setBorder(buttonBorder);
    }
    b.setInheritsPopupMenu(true);
    return b;
  }

  @Override
  public boolean isFocused() {
    return myFocused;
  }

  @Nonnull
  @Override
  public MouseEnterHandler getMouseEnterHandler() {
    return myMouseEnterHandler;
  }

  static class LayoutManagerDelegate implements LayoutManager {
    protected final LayoutManager myDelegate;

    LayoutManagerDelegate(LayoutManager delegate) {
      myDelegate = delegate;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
      myDelegate.addLayoutComponent(name, comp);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
      myDelegate.removeLayoutComponent(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return myDelegate.preferredLayoutSize(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      return myDelegate.minimumLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
      myDelegate.layoutContainer(parent);
    }
  }
}
