// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.ui.awt;

import consulo.application.AllIcons;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.editor.hint.HintColorUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.Html;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.IdeTooltipManager;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.ref.Ref;
import org.intellij.lang.annotations.JdkConstants;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

public class HintUtil {
  /**
   * @deprecated use getInformationColor()
   */
  @Deprecated
  public static final Color INFORMATION_COLOR = HintColorUtil.INFORMATION_COLOR;
  @Deprecated
  public static final Color INFORMATION_BORDER_COLOR = HintColorUtil.INFORMATION_BORDER_COLOR;
  /**
   * @deprecated use getErrorColor()
   */
  @Deprecated
  public static final Color ERROR_COLOR = HintColorUtil.ERROR_COLOR;

  @Deprecated
  public static final EditorColorKey INFORMATION_COLOR_KEY = HintColorUtil.INFORMATION_COLOR_KEY;
  @Deprecated
  public static final EditorColorKey QUESTION_COLOR_KEY = HintColorUtil.QUESTION_COLOR_KEY;
  @Deprecated
  public static final EditorColorKey ERROR_COLOR_KEY = HintColorUtil.ERROR_COLOR_KEY;
  @Deprecated
  public static final Color QUESTION_UNDERSCORE_COLOR = HintColorUtil.QUESTION_UNDERSCORE_COLOR;
  @Deprecated
  public static final EditorColorKey RECENT_LOCATIONS_SELECTION_KEY = HintColorUtil.RECENT_LOCATIONS_SELECTION_KEY;

  private HintUtil() {
  }

  @Nonnull
  @Deprecated
  public static ColorValue getInformationColor() {
    return HintColorUtil.getInformationColor();
  }

  @Nonnull
  @Deprecated
  public static ColorValue getQuestionColor() {
    return HintColorUtil.getQuestionColor();
  }

  @Nonnull
  @Deprecated
  public static ColorValue getErrorColor() {
    return HintColorUtil.getErrorColor();
  }

  @Nonnull
  @Deprecated
  public static ColorValue getRecentLocationsSelectionColor(EditorColorsScheme colorsScheme) {
    return HintColorUtil.getRecentLocationsSelectionColor(colorsScheme);
  }

  public static JComponent createInformationLabel(@Nonnull String text) {
    return createInformationLabel(text, null, null, null);
  }

  public static JComponent createInformationLabel(@Nonnull String text,
                                                  @Nullable HyperlinkListener hyperlinkListener,
                                                  @Nullable MouseListener mouseListener,
                                                  @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    HintHint hintHint = getInformationHint();
    HintLabel label = createLabel(text, null, hintHint.getTextBackground(), hintHint);
    configureLabel(label, hyperlinkListener, mouseListener, updatedTextConsumer);
    return label;
  }

  @Nonnull
  public static HintHint getInformationHint() {
    return new HintHint().setBorderColor(INFORMATION_BORDER_COLOR).setTextBg(TargetAWT.to(getInformationColor())).setTextFg(UIUtil.isUnderDarcula() ? UIUtil.getLabelForeground() : Color.black)
            .setFont(getBoldFont()).setAwtTooltip(true);
  }

  public static CompoundBorder createHintBorder() {
    //noinspection UseJBColor
    return BorderFactory.createCompoundBorder(new ColoredSideBorder(Color.white, Color.white, Color.gray, Color.gray, 1), BorderFactory.createEmptyBorder(2, 2, 2, 2));
  }

  @Nonnull
  public static JComponent createInformationLabel(SimpleColoredText text) {
    return createInformationLabel(text, null);
  }

  public static JComponent createQuestionLabel(String text) {
    final Image icon = AllIcons.General.ContextHelp;
    return createQuestionLabel(text, icon);
  }

  public static JComponent createQuestionLabel(String text, Image icon) {
    Color bg = TargetAWT.to(getQuestionColor());
    HintHint hintHint = new HintHint().setTextBg(bg).setTextFg(JBColor.foreground()).setFont(getBoldFont()).setAwtTooltip(true);

    return createLabel(text, icon, bg, hintHint);
  }

  @Nullable
  public static String getHintLabel(JComponent hintComponent) {
    if (hintComponent instanceof HintLabel) {
      return ((HintLabel)hintComponent).getText();
    }
    return null;
  }

  @Nullable
  public static Icon getHintIcon(JComponent hintComponent) {
    if (hintComponent instanceof HintLabel) {
      return ((HintLabel)hintComponent).getIcon();
    }
    return null;
  }

  @Nonnull
  public static SimpleColoredComponent createInformationComponent() {
    SimpleColoredComponent component = new SimpleColoredComponent();
    component.setBackground(TargetAWT.to(getInformationColor()));
    component.setForeground(JBColor.foreground());
    component.setFont(getBoldFont());
    return component;
  }

  @Nonnull
  public static JComponent createInformationLabel(@Nonnull SimpleColoredText text, @Nullable Image icon) {
    SimpleColoredComponent component = createInformationComponent();
    component.setIcon(icon);
    text.appendToComponent(component);
    return new HintLabel(component);
  }

  public static JComponent createErrorLabel(@Nonnull String text,
                                            @Nullable HyperlinkListener hyperlinkListener,
                                            @Nullable MouseListener mouseListener,
                                            @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    Color bg = TargetAWT.to(getErrorColor());
    HintHint hintHint = new HintHint().setTextBg(bg).setTextFg(JBColor.foreground()).setFont(getBoldFont()).setAwtTooltip(true);

    HintLabel label = createLabel(text, null, bg, hintHint);
    configureLabel(label, hyperlinkListener, mouseListener, updatedTextConsumer);
    return label;
  }

  @Nonnull
  public static JComponent createErrorLabel(@Nonnull String text) {
    return createErrorLabel(text, null, null, null);
  }

  @Nonnull
  private static HintLabel createLabel(String text, @Nullable Image icon, @Nonnull Color color, @Nonnull HintHint hintHint) {
    HintLabel label = new HintLabel();
    label.setText(text, hintHint);
    label.setIcon(TargetAWT.to(icon));

    if (!hintHint.isAwtTooltip()) {
      label.setBorder(createHintBorder());
      label.setForeground(JBColor.foreground());
      label.setFont(getBoldFont());
      label.setBackground(color);
      label.setOpaque(true);
    }
    return label;
  }

  private static Font getBoldFont() {
    return UIUtil.getLabelFont().deriveFont(Font.BOLD);
  }

  @Nonnull
  public static JLabel createAdComponent(final String bottomText, final Border border, @JdkConstants.HorizontalAlignment int alignment) {
    JLabel label = new JLabel();
    label.setText(bottomText);
    label.setHorizontalAlignment(alignment);
    label.setForeground(JBCurrentTheme.Advertiser.foreground());
    label.setBackground(JBCurrentTheme.Advertiser.background());
    label.setOpaque(true);
    label.setFont(label.getFont().deriveFont((float)(label.getFont().getSize() - 2)));
    if (bottomText != null) {
      label.setBorder(border);
    }
    return label;
  }

  @Nonnull
  public static String prepareHintText(@Nonnull String text, @Nonnull HintHint hintHint) {
    return prepareHintText(new Html(text), hintHint);
  }

  public static String prepareHintText(@Nonnull Html text, @Nonnull HintHint hintHint) {
    String htmlBody = UIUtil.getHtmlBody(text);
    return String.format("<html><head>%s</head><body>%s</body></html>",
                         UIUtil.getCssFontDeclaration(hintHint.getTextFont(), hintHint.getTextForeground(), hintHint.getLinkForeground(), hintHint.getUlImg()), htmlBody);
  }

  private static void configureLabel(@Nonnull HintLabel label,
                                     @Nullable HyperlinkListener hyperlinkListener,
                                     @Nullable MouseListener mouseListener,
                                     @Nullable Ref<? super Consumer<? super String>> updatedTextConsumer) {
    if (hyperlinkListener != null) {
      label.myPane.addHyperlinkListener(hyperlinkListener);
    }
    if (mouseListener != null) {
      label.myPane.addMouseListener(mouseListener);
    }
    if (updatedTextConsumer != null) {
      Consumer<? super String> consumer = s -> {
        label.myPane.setText(s);

        // Force preferred size recalculation.
        label.setPreferredSize(null);
        label.myPane.setPreferredSize(null);
      };
      updatedTextConsumer.set(consumer);
    }
  }

  private static class HintLabel extends JPanel {
    private JEditorPane myPane;
    private SimpleColoredComponent myColored;
    private JLabel myIcon;

    private HintLabel() {
      setLayout(new BorderLayout());
    }

    private HintLabel(@Nonnull SimpleColoredComponent component) {
      this();
      setText(component);
    }

    @Override
    public boolean requestFocusInWindow() {
      // Forward the focus to the tooltip contents so that screen readers announce
      // the tooltip contents right away.
      if (myPane != null) {
        return myPane.requestFocusInWindow();
      }
      if (myColored != null) {
        return myColored.requestFocusInWindow();
      }
      if (myIcon != null) {
        return myIcon.requestFocusInWindow();
      }
      return super.requestFocusInWindow();
    }

    public void setText(@Nonnull SimpleColoredComponent colored) {
      clearText();

      myColored = colored;
      add(myColored, BorderLayout.CENTER);

      setOpaque(true);
      setBackground(colored.getBackground());

      revalidate();
      repaint();
    }

    public void setText(String s, HintHint hintHint) {
      clearText();

      if (s != null) {
        myPane = IdeTooltipManager.getInstance().initEditorPane(s, hintHint, null);
        add(myPane, BorderLayout.CENTER);
      }

      setOpaque(true);
      setBackground(hintHint.getTextBackground());

      revalidate();
      repaint();
    }

    private void clearText() {
      if (myPane != null) {
        remove(myPane);
        myPane = null;
      }

      if (myColored != null) {
        remove(myColored);
        myColored = null;
      }
    }

    public void setIcon(Icon icon) {
      if (myIcon != null) {
        remove(myIcon);
      }

      myIcon = new JLabel(icon, SwingConstants.CENTER);
      myIcon.setVerticalAlignment(SwingConstants.TOP);

      add(myIcon, BorderLayout.WEST);

      revalidate();
      repaint();
    }

    @Override
    public String toString() {
      return "Hint: text='" + getText() + "'";
    }

    public String getText() {
      return myPane != null ? myPane.getText() : "";
    }

    @Nullable
    public Icon getIcon() {
      return myIcon != null ? myIcon.getIcon() : null;
    }
  }
}
