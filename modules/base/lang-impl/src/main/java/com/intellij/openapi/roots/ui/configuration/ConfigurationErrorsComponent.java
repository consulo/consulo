/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.ui.MessageType;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.*;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * User: spLeaner
 */
public class ConfigurationErrorsComponent extends JPanel {
  @NonNls private static final String FIX_ACTION_NAME = "FIX";
  @NonNls private static final String NAVIGATE_ACTION_NAME = "NAVIGATE";

  private final JBList myErrorList;
  private final CollectionListModel<ConfigurationError> myModel;

  public ConfigurationErrorsComponent(@Nonnull final Project project) {
    super(new BorderLayout());
    myModel = new CollectionListModel<ConfigurationError>();
    final JLabel label = new JLabel("<html><body><b>Problems:</b></body></html>");
    label.setVisible(false);
    add(label, BorderLayout.NORTH);
    project.getMessageBus().connect().subscribe(ConfigurationErrors.TOPIC, new ConfigurationErrors() {
      @Override
      public void addError(@Nonnull ConfigurationError error) {
        int elementIndex = myModel.getElementIndex(error);
        if (elementIndex != -1) {
          return;
        }

        myModel.add(error);
        label.setVisible(myModel.getSize() != 0);
      }

      @Override
      public void removeError(@Nonnull ConfigurationError error) {
        myModel.remove(error);
        label.setVisible(myModel.getSize() != 0);
      }
    });

    myErrorList = new JBList(myModel);
    myErrorList.setEmptyText("");
    myErrorList.setBackground(UIUtil.getPanelBackground());
    myErrorList.setCellRenderer(new ErrorListRenderer(myErrorList));
    myErrorList.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (!e.isPopupTrigger()) {
          processListMouseEvent(e, true);
        }
      }
    });

    myErrorList.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        if (!e.isPopupTrigger()) {
          processListMouseEvent(e, false);
        }
      }
    });

    add(myErrorList, BorderLayout.CENTER);
  }

  private abstract static class ToolbarAlikeButton extends JComponent {
    private BaseButtonBehavior myBehavior;
    private Image myIcon;
    private String myName;

    ToolbarAlikeButton(@Nonnull final Image icon, @Nonnull final String name) {
      this(icon);
      myName = name;
    }

    ToolbarAlikeButton(@Nonnull final Image icon) {
      myIcon = icon;

      myBehavior = new BaseButtonBehavior(this, TimedDeadzone.NULL) {
        @Override
        protected void execute(MouseEvent e) {
          onClick(e);
        }
      };

      setOpaque(false);
    }

    public String getButtonName() {
      return myName;
    }

    public void onClick(MouseEvent e) {
    }

    @Override
    public Insets getInsets() {
      return JBUI.insets(2);
    }

    @Override
    public Dimension getPreferredSize() {
      return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
      final Insets insets = getInsets();
      return new Dimension(myIcon.getWidth() + insets.left + insets.right, myIcon.getHeight() + insets.top + insets.bottom);
    }

    @Override
    public void paint(final Graphics g) {
      final Insets insets = getInsets();
      final Dimension d = getSize();

      int x = (d.width - myIcon.getWidth() - insets.left - insets.right) / 2;
      int y = (d.height - myIcon.getHeight() - insets.top - insets.bottom) / 2;

      if (myBehavior.isHovered()) {
        // todo
      }

      if (myBehavior.isPressedByMouse()) {
        x += 1;
        y += 1;
      }

      TargetAWT.to(myIcon).paintIcon(this, g, x + insets.left, y + insets.top);
    }
  }

  private void processListMouseEvent(final MouseEvent e, final boolean click) {
    final int index = myErrorList.locationToIndex(e.getPoint());
    if (index > -1) {
      final Object value = myErrorList.getModel().getElementAt(index);
      if (value != null && value instanceof ConfigurationError) {
        final ConfigurationError error = (ConfigurationError)value;
        final Component renderer = myErrorList.getCellRenderer().getListCellRendererComponent(myErrorList, value, index, false, false);
        if (renderer instanceof ErrorListRenderer) {
          final Rectangle bounds = myErrorList.getCellBounds(index, index);
          renderer.setBounds(bounds);
          renderer.doLayout();

          final Point point = e.getPoint();
          point.translate(-bounds.x, -bounds.y);

          final Component deepestComponentAt = SwingUtilities.getDeepestComponentAt(renderer, point.x, point.y);
          if (deepestComponentAt instanceof ToolbarAlikeButton) {
            final String name = ((ToolbarAlikeButton)deepestComponentAt).getButtonName();
            if (click) {
              if (FIX_ACTION_NAME.equals(name)) {
                onClickFix(error, (JComponent)deepestComponentAt, e);
              }
              else if (NAVIGATE_ACTION_NAME.equals(name)) {
                error.navigate();
              }
              else {
                error.ignore(!error.isIgnored());
                myModel.contentsChanged(error);
              }
            }
            else {
              myErrorList.setToolTipText(FIX_ACTION_NAME.equals(name)
                                         ? "Fix"
                                         : NAVIGATE_ACTION_NAME.equals(name)
                                           ? "Navigate to the problem"
                                           : error.isIgnored() ? "Not ignore this error" : "Ignore this error");
              return;
            }
          }
          else {
            if (e.getClickCount() == 2) {
              error.navigate();
            }
          }
        }
      }
    }

    myErrorList.setToolTipText(null);
  }

  private void onClickFix(@Nonnull final ConfigurationError error, JComponent component, MouseEvent e) {
    error.fix(component, new RelativePoint(e));
  }


  private static class ErrorListRenderer extends JComponent implements ListCellRenderer {
    private JTextPane myText;
    private JTextPane myFakeTextPane;
    private JViewport myFakeViewport;
    private JList myList;
    private JPanel myButtonsPanel;
    private JPanel myFixGroup;

    private ErrorListRenderer(@Nonnull final JList list) {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      setOpaque(false);

      myList = list;

      myText = new JTextPane();

      myButtonsPanel = new JPanel(new BorderLayout());
      myButtonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 3, 5, 3));
      myButtonsPanel.setOpaque(false);
      final JPanel buttons = new JPanel();
      buttons.setOpaque(false);
      buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
      myButtonsPanel.add(buttons, BorderLayout.NORTH);
      add(myButtonsPanel, BorderLayout.EAST);

      myFixGroup = new JPanel();
      myFixGroup.setOpaque(false);
      myFixGroup.setLayout(new BoxLayout(myFixGroup, BoxLayout.Y_AXIS));

      myFixGroup.add(new ToolbarAlikeButton(AllIcons.Actions.QuickfixBulb, FIX_ACTION_NAME) {
      });
      myFixGroup.add(Box.createHorizontalStrut(3));
      buttons.add(myFixGroup);

      buttons.add(new ToolbarAlikeButton(AllIcons.General.AutoscrollToSource, NAVIGATE_ACTION_NAME) {
      });
      buttons.add(Box.createHorizontalStrut(3));

      buttons.add(new ToolbarAlikeButton(AllIcons.Actions.Cancel, "IGNORE") {
      });

      myFakeTextPane = new JTextPane();
      myText.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      myFakeTextPane.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
      myText.setOpaque(false);
      if (UIUtil.isUnderNimbusLookAndFeel()) {
        myText.setBackground(UIUtil.TRANSPARENT_COLOR);
      }

      myText.setEditable(false);
      myFakeTextPane.setEditable(false);
      myText.setEditorKit(JBHtmlEditorKit.create());
      myFakeTextPane.setEditorKit(JBHtmlEditorKit.create());

      myFakeViewport = new JViewport();
      myFakeViewport.setView(myFakeTextPane);

      add(myText, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
      final Container parent = myList.getParent();
      if (parent != null) {
        myFakeTextPane.setText(myText.getText());
        final Dimension size = parent.getSize();
        myFakeViewport.setSize(size);
        final Dimension preferredSize = myFakeTextPane.getPreferredSize();

        final Dimension buttonsPrefSize = myButtonsPanel.getPreferredSize();
        final int maxHeight = Math.max(buttonsPrefSize.height, preferredSize.height);

        final Insets insets = getInsets();
        return new Dimension(Math.min(size.width, preferredSize.width), maxHeight + insets.top + insets.bottom);
      }

      return super.getPreferredSize();
    }

    @Override
    public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
      final ConfigurationError error = (ConfigurationError)value;

      myList = list;

      myFixGroup.setVisible(error.canBeFixed());
      myText.setText(error.getDescription());

      setBackground(error.isIgnored() ? MessageType.WARNING.getPopupBackground() : MessageType.ERROR.getPopupBackground());
      return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
      final Graphics2D g2d = (Graphics2D)g;

      final Rectangle bounds = getBounds();
      final Insets insets = getInsets();

      final GraphicsConfig cfg = new GraphicsConfig(g);
      cfg.setAntialiasing(true);

      final Shape shape = new RoundRectangle2D.Double(insets.left, insets.top, bounds.width - 1 - insets.left - insets.right,
                                                      bounds.height - 1 - insets.top - insets.bottom, 6, 6);
      g2d.setColor(JBColor.WHITE);
      g2d.fill(shape);

      Color bgColor = getBackground();

      g2d.setColor(bgColor);
      g2d.fill(shape);

      g2d.setColor(getBackground().darker());
      g2d.draw(shape);
      cfg.restore();

      super.paintComponent(g);
    }
  }
}

