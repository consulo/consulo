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

package consulo.desktop.awt.codeInsight.lookup;

import consulo.application.AccessRule;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.NameUtil;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.FontPreferences;
import consulo.component.ProcessCanceledException;
import consulo.ide.impl.idea.codeInsight.lookup.impl.EmptyLookupItem;
import consulo.ide.impl.idea.codeInsight.lookup.impl.LookupIconUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUIUtil;
import consulo.language.editor.completion.lookup.*;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.style.StandardColors;
import consulo.util.collection.FList;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author peter
 * @author Konstantin Bulenkov
 */
public class LookupCellRenderer implements ListCellRenderer {
  private static final Logger LOG = Logger.getInstance(LookupCellRenderer.class);
  private Image myEmptyIcon = Image.empty(Image.DEFAULT_ICON_SIZE * 2, Image.DEFAULT_ICON_SIZE);

  private final Font myNormalFont;
  private final Font myBoldFont;
  private final FontMetrics myNormalMetrics;
  private final FontMetrics myBoldMetrics;

  //TODO[kb]: move all these awesome constants to Editor's Fonts & Colors settings
  public static final Color BACKGROUND_COLOR = MorphColor.of(UIUtil::getPanelBackground);
  public static final Color FOREGROUND_COLOR = JBColor.foreground();
  private static final Color GRAYED_FOREGROUND_COLOR = new JBColor(Gray._160, Gray._110);
  public static final Color SELECTED_NON_FOCUSED_BACKGROUND_COLOR = new JBColor(0x6e8ea2, 0x55585a);
  public static final Color SELECTED_FOREGROUND_COLOR = MorphColor.of(() -> (UIUtil.isUnderDarkTheme() ? JBColor.foreground() : JBColor.WHITE));
  private static final Color SELECTED_GRAYED_FOREGROUND_COLOR = MorphColor.of((() -> (UIUtil.isUnderDarkTheme()) ? JBColor.foreground() : JBColor.WHITE));
  private static final Color NON_FOCUSED_MASK_COLOR = JBColor.namedColor("CompletionPopup.nonFocusedMask", Gray._0.withAlpha(0));

  static final Color PREFIX_FOREGROUND_COLOR = new JBColor(0xb000b0, 0xd17ad6);
  private static final Color SELECTED_PREFIX_FOREGROUND_COLOR = new JBColor(0xf9eccc, 0xd17ad6);

  private final LookupImpl myLookup;

  private final SimpleColoredComponent myNameComponent;
  private final SimpleColoredComponent myTailComponent;
  private final SimpleColoredComponent myTypeLabel;
  private final LookupPanel myPanel;
  private final Map<Integer, Boolean> mySelected = new HashMap<>();

  private static final String ELLIPSIS = "\u2026";
  private int myMaxWidth = -1;

  public LookupCellRenderer(LookupImpl lookup) {
    EditorColorsScheme scheme = lookup.getTopLevelEditor().getColorsScheme();
    myNormalFont = scheme.getFont(EditorFontType.PLAIN);
    myBoldFont = scheme.getFont(EditorFontType.BOLD);

    myLookup = lookup;
    myNameComponent = new MySimpleColoredComponent();
    myNameComponent.setIpad(JBUI.insetsLeft(2));
    myNameComponent.setMyBorder(null);

    myTailComponent = new MySimpleColoredComponent();
    myTailComponent.setIpad(JBUI.emptyInsets());
    myTailComponent.setBorder(JBUI.Borders.emptyRight(10));

    myTypeLabel = new MySimpleColoredComponent();
    myTypeLabel.setIpad(JBUI.emptyInsets());
    myTypeLabel.setBorder(JBUI.Borders.emptyRight(6));

    myPanel = new LookupPanel();
    myPanel.add(myNameComponent, BorderLayout.WEST);
    myPanel.add(myTailComponent, BorderLayout.CENTER);
    myPanel.add(myTypeLabel, BorderLayout.EAST);

    myNormalMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myNormalFont);
    myBoldMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myBoldFont);
  }

  private boolean myIsSelected = false;

  @Override
  public Component getListCellRendererComponent(final JList list, Object value, int index, boolean isSelected, boolean hasFocus) {


    boolean nonFocusedSelection = isSelected && myLookup.getLookupFocusDegree() == LookupFocusDegree.SEMI_FOCUSED;
    if (!myLookup.isFocused()) {
      isSelected = false;
    }

    myIsSelected = isSelected;
    final LookupElement item = (LookupElement)value;
    final Color foreground = getForegroundColor(isSelected);
    final Color background = nonFocusedSelection ? UIUtil.getListSelectionBackground(false) : isSelected ? UIUtil.getListSelectionBackground(true) : BACKGROUND_COLOR;

    int allowedWidth = list.getWidth() - calcSpacing(myNameComponent, myEmptyIcon) - calcSpacing(myTailComponent, null) - calcSpacing(myTypeLabel, null);

    FontMetrics normalMetrics = getRealFontMetrics(item, false);
    FontMetrics boldMetrics = getRealFontMetrics(item, true);
    final LookupElementPresentation presentation = new RealLookupElementPresentation(isSelected ? getMaxWidth() : allowedWidth, normalMetrics, boldMetrics, myLookup);
    AccessRule.read(() -> {
      if (item.isValid()) {
        try {
          item.renderElement(presentation);
        }
        catch (ProcessCanceledException e) {
          LOG.info(e);
          presentation.setItemTextForeground(StandardColors.RED);
          presentation.setItemText("Error occurred, see the log in Help | Show Log");
        }
        catch (Exception | Error e) {
          LOG.error(e);
        }
      }
      else {
        presentation.setItemTextForeground(StandardColors.RED);
        presentation.setItemText("Invalid");
      }
    });

    myNameComponent.clear();
    myNameComponent.setBackground(background);
    Color itemTextForeground = TargetAWT.to(presentation.getItemTextForeground());
    allowedWidth -= setItemTextLabel(item, new JBColor(isSelected ? SELECTED_FOREGROUND_COLOR : itemTextForeground, itemTextForeground), isSelected, presentation, allowedWidth);

    Font font = myLookup.getCustomFont(item, false);
    if (font == null) {
      font = myNormalFont;
    }
    myTailComponent.setFont(font);
    myTypeLabel.setFont(font);
    myNameComponent.setIcon(LookupIconUtil.augmentIcon(myLookup.getEditor(), presentation.getIcon(), myEmptyIcon));


    myTypeLabel.clear();
    if (allowedWidth > 0) {
      allowedWidth -= setTypeTextLabel(item, background, foreground, presentation, isSelected ? getMaxWidth() : allowedWidth, isSelected, nonFocusedSelection, normalMetrics);
    }

    myTailComponent.clear();
    myTailComponent.setBackground(background);
    if (isSelected || allowedWidth >= 0) {
      setTailTextLabel(isSelected, presentation, foreground, isSelected ? getMaxWidth() : allowedWidth, nonFocusedSelection, normalMetrics);
    }

    if (mySelected.containsKey(index)) {
      if (!isSelected && mySelected.get(index)) {
        myPanel.setUpdateExtender(true);
      }
    }
    mySelected.put(index, isSelected);

    final double w = myNameComponent.getPreferredSize().getWidth() + myTailComponent.getPreferredSize().getWidth() + myTypeLabel.getPreferredSize().getWidth();

    boolean useBoxLayout = isSelected && w > list.getWidth() && ((JBList)list).getExpandableItemsHandler().isEnabled();
    if (useBoxLayout != myPanel.getLayout() instanceof BoxLayout) {
      myPanel.removeAll();
      if (useBoxLayout) {
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
        myPanel.add(myNameComponent);
        myPanel.add(myTailComponent);
        myPanel.add(myTypeLabel);
      }
      else {
        myPanel.setLayout(new BorderLayout());
        myPanel.add(myNameComponent, BorderLayout.WEST);
        myPanel.add(myTailComponent, BorderLayout.CENTER);
        myPanel.add(myTypeLabel, BorderLayout.EAST);
      }
    }

    AccessibleContextUtil.setCombinedName(myPanel, myNameComponent, "", myTailComponent, " - ", myTypeLabel);
    AccessibleContextUtil.setCombinedDescription(myPanel, myNameComponent, "", myTailComponent, " - ", myTypeLabel);
    return myPanel;
  }

  private static int calcSpacing(@Nonnull SimpleColoredComponent component, @Nullable Image icon) {
    Insets iPad = component.getIpad();
    int width = iPad.left + iPad.right;
    Border myBorder = component.getMyBorder();
    if (myBorder != null) {
      Insets insets = myBorder.getBorderInsets(component);
      width += insets.left + insets.right;
    }
    Insets insets = component.getInsets();
    if (insets != null) {
      width += insets.left + insets.right;
    }
    if (icon != null) {
      width += icon.getWidth() + component.getIconTextGap();
    }
    return width;
  }

  private static Color getForegroundColor(boolean isSelected) {
    return isSelected ? SELECTED_FOREGROUND_COLOR : FOREGROUND_COLOR;
  }

  private int getMaxWidth() {
    if (myMaxWidth < 0) {
      final Point p = myLookup.getComponent().getLocationOnScreen();
      final Rectangle rectangle = ScreenUtil.getScreenRectangle(p);
      myMaxWidth = rectangle.x + rectangle.width - p.x - 111;
    }
    return myMaxWidth;
  }

  private void setTailTextLabel(boolean isSelected, LookupElementPresentation presentation, Color foreground, int allowedWidth, boolean nonFocusedSelection, FontMetrics fontMetrics) {
    int style = getStyle(false, presentation.isStrikeout(), false, false);

    for (LookupElementPresentation.TextFragment fragment : presentation.getTailFragments()) {
      if (allowedWidth < 0) {
        return;
      }

      String trimmed = trimLabelText(fragment.text, allowedWidth, fontMetrics);
      int fragmentStyle = fragment.isItalic() ? style | SimpleTextAttributes.STYLE_ITALIC : style;
      myTailComponent.append(trimmed, new SimpleTextAttributes(fragmentStyle, getTailTextColor(isSelected, fragment, foreground, nonFocusedSelection)));
      allowedWidth -= RealLookupElementPresentation.getStringWidth(trimmed, fontMetrics);
    }
  }

  private String trimLabelText(@Nullable String text, int maxWidth, FontMetrics metrics) {
    if (text == null || StringUtil.isEmpty(text)) {
      return "";
    }

    final int strWidth = RealLookupElementPresentation.getStringWidth(text, metrics);
    if (strWidth <= maxWidth || myIsSelected) {
      return text;
    }

    if (RealLookupElementPresentation.getStringWidth(ELLIPSIS, metrics) > maxWidth) {
      return "";
    }

    int i = 0;
    int j = text.length();
    while (i + 1 < j) {
      int mid = (i + j) / 2;
      final String candidate = text.substring(0, mid) + ELLIPSIS;
      final int width = RealLookupElementPresentation.getStringWidth(candidate, metrics);
      if (width <= maxWidth) {
        i = mid;
      }
      else {
        j = mid;
      }
    }

    return text.substring(0, i) + ELLIPSIS;
  }

  private static Color getTypeTextColor(LookupElement item, Color foreground, LookupElementPresentation presentation, boolean selected, boolean nonFocusedSelection) {
    if (nonFocusedSelection) {
      return foreground;
    }

    return presentation.isTypeGrayed() ? getGrayedForeground(selected) : item instanceof EmptyLookupItem ? JBColor.foreground() : foreground;
  }

  private static Color getTailTextColor(boolean isSelected, LookupElementPresentation.TextFragment fragment, Color defaultForeground, boolean nonFocusedSelection) {
    if (nonFocusedSelection) {
      return defaultForeground;
    }

    if (fragment.isGrayed()) {
      return getGrayedForeground(isSelected);
    }

    if (!isSelected) {
      final ColorValue tailForeground = fragment.getForegroundColor();
      if (tailForeground != null) {
        return TargetAWT.to(tailForeground);
      }
    }

    return defaultForeground;
  }

  public static Color getGrayedForeground(boolean isSelected) {
    return isSelected ? SELECTED_GRAYED_FOREGROUND_COLOR : GRAYED_FOREGROUND_COLOR;
  }

  private int setItemTextLabel(LookupElement item, final Color foreground, final boolean selected, LookupElementPresentation presentation, int allowedWidth) {
    boolean bold = presentation.isItemTextBold();

    Font customItemFont = myLookup.getCustomFont(item, bold);
    myNameComponent.setFont(customItemFont != null ? customItemFont : bold ? myBoldFont : myNormalFont);
    int style = getStyle(bold, presentation.isStrikeout(), presentation.isItemTextUnderlined(), presentation.isItemTextItalic());

    final FontMetrics metrics = getRealFontMetrics(item, bold);
    final String name = trimLabelText(presentation.getItemText(), allowedWidth, metrics);
    int used = RealLookupElementPresentation.getStringWidth(name, metrics);

    renderItemName(item, foreground, selected, style, name, myNameComponent);
    return used;
  }

  private FontMetrics getRealFontMetrics(LookupElement item, boolean bold) {
    Font customFont = myLookup.getCustomFont(item, bold);
    if (customFont != null) {
      return myLookup.getTopLevelEditor().getComponent().getFontMetrics(customFont);
    }

    return bold ? myBoldMetrics : myNormalMetrics;
  }

  @SimpleTextAttributes.StyleAttributeConstant
  private static int getStyle(boolean bold, boolean strikeout, boolean underlined, boolean italic) {
    int style = bold ? SimpleTextAttributes.STYLE_BOLD : SimpleTextAttributes.STYLE_PLAIN;
    if (strikeout) {
      style |= SimpleTextAttributes.STYLE_STRIKEOUT;
    }
    if (underlined) {
      style |= SimpleTextAttributes.STYLE_UNDERLINE;
    }
    if (italic) {
      style |= SimpleTextAttributes.STYLE_ITALIC;
    }
    return style;
  }

  private void renderItemName(LookupElement item, Color foreground, boolean selected, @SimpleTextAttributes.StyleAttributeConstant int style, String name, final SimpleColoredComponent nameComponent) {
    final SimpleTextAttributes base = new SimpleTextAttributes(style, foreground);

    final String prefix = item instanceof EmptyLookupItem ? "" : myLookup.itemPattern(item);
    if (prefix.length() > 0) {
      Iterable<MatcherTextRange> ranges = getMatchingFragments(prefix, name);
      if (ranges != null) {
        SimpleTextAttributes highlighted = new SimpleTextAttributes(style, selected ? SELECTED_PREFIX_FOREGROUND_COLOR : PREFIX_FOREGROUND_COLOR);
        SpeedSearchUtil.appendColoredFragments(nameComponent, name, ranges, base, highlighted);
        return;
      }
    }
    nameComponent.append(name, base);
  }

  public static FList<MatcherTextRange> getMatchingFragments(String prefix, String name) {
    return NameUtil.buildMatcher("*" + prefix).build().matchingFragments(name);
  }

  private int setTypeTextLabel(LookupElement item,
                               final Color background,
                               Color foreground,
                               final LookupElementPresentation presentation,
                               int allowedWidth,
                               boolean selected,
                               boolean nonFocusedSelection,
                               FontMetrics normalMetrics) {
    final String givenText = presentation.getTypeText();
    final String labelText = trimLabelText(StringUtil.isEmpty(givenText) ? "" : " " + givenText, allowedWidth, normalMetrics);

    int used = RealLookupElementPresentation.getStringWidth(labelText, normalMetrics);

    final Image icon = presentation.getTypeIcon();
    if (icon != null) {
      myTypeLabel.setIcon(icon);
      used += icon.getWidth();
    }

    Color sampleBackground = background;

    Object o = item.isValid() ? item.getObject() : null;
    //noinspection deprecation
    if (o instanceof LookupValueWithUIHint && StringUtil.isEmpty(labelText)) {
      //noinspection deprecation
      Color proposedBackground = ((LookupValueWithUIHint)o).getColorHint();
      if (proposedBackground != null) {
        sampleBackground = proposedBackground;
      }
      myTypeLabel.append("  ");
      used += normalMetrics.stringWidth("WW");
    }
    else {
      myTypeLabel.append(labelText);
    }

    myTypeLabel.setBackground(sampleBackground);
    myTypeLabel.setForeground(getTypeTextColor(item, foreground, presentation, selected, nonFocusedSelection));
    return used;
  }

  @Nullable
  Font getFontAbleToDisplay(LookupElementPresentation p) {
    String sampleString = p.getItemText() + p.getTailText() + p.getTypeText();

    // assume a single font can display all lookup item chars
    Set<Font> fonts = new HashSet<>();
    FontPreferences fontPreferences = myLookup.getFontPreferences();
    for (int i = 0; i < sampleString.length(); i++) {
      fonts.add(ComplementaryFontsRegistry.getFontAbleToDisplay(sampleString.charAt(i), Font.PLAIN, fontPreferences, null).getFont());
    }

    eachFont:
    for (Font font : fonts) {
      if (font.equals(myNormalFont)) continue;

      for (int i = 0; i < sampleString.length(); i++) {
        if (!font.canDisplay(sampleString.charAt(i))) {
          continue eachFont;
        }
      }
      return font;
    }
    return null;
  }


  int updateMaximumWidth(final LookupElementPresentation p, LookupElement item) {
    final Image icon = p.getIcon();
    if (icon != null && (icon.getWidth() > myEmptyIcon.getWidth() || icon.getHeight() > myEmptyIcon.getHeight())) {
      // get width return scaled and image creating also scaled - it will be double scale
      //myEmptyIcon = Image.empty(Math.max(icon.getWidth(), myEmptyIcon.getWidth()), Math.max(icon.getHeight(), myEmptyIcon.getHeight()));
    }

    return RealLookupElementPresentation.calculateWidth(p, getRealFontMetrics(item, false), getRealFontMetrics(item, true)) + calcSpacing(myTailComponent, null) + calcSpacing(myTypeLabel, null);
  }

  public int getTextIndent() {
    return myNameComponent.getIpad().left + myEmptyIcon.getWidth() + myNameComponent.getIconTextGap();
  }

  private static class MySimpleColoredComponent extends SimpleColoredComponent {
    private MySimpleColoredComponent() {
      setFocusBorderAroundIcon(true);
    }

    @Override
    protected void applyAdditionalHints(@Nonnull Graphics2D g) {
      EditorUIUtil.setupAntialiasing(g);
    }
  }

  private class LookupPanel extends JPanel {
    boolean myUpdateExtender;

    public LookupPanel() {
      super(new BorderLayout());
    }

    public void setUpdateExtender(boolean updateExtender) {
      myUpdateExtender = updateExtender;
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      if (NON_FOCUSED_MASK_COLOR.getAlpha() > 0 && !myLookup.isFocused() && myLookup.isCompletion()) {
        g = g.create();
        try {
          g.setColor(NON_FOCUSED_MASK_COLOR);
          g.fillRect(0, 0, getWidth(), getHeight());
        }
        finally {
          g.dispose();
        }
      }
    }
  }
}
