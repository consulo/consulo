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

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.NameUtil;
import consulo.codeEditor.impl.ComplementaryFontsRegistry;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.FontPreferences;
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
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.SingleAlarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.FList;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author peter
 * @author Konstantin Bulenkov
 */
public class LookupCellRenderer implements ListCellRenderer {
  private static final Logger LOG = Logger.getInstance(LookupCellRenderer.class);

  private Image myEmptyIcon = Image.empty(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
  private static final Key<Font> CUSTOM_NAME_FONT = Key.create("CustomLookupElementNameFont");
  private static final Key<Font> CUSTOM_TAIL_FONT = Key.create("CustomLookupElementTailFont");
  private static final Key<Font> CUSTOM_TYPE_FONT = Key.create("CustomLookupElementTypeFont");

  private final Font myNormalFont;
  private final Font myBoldFont;
  private final FontMetrics myNormalMetrics;
  private final FontMetrics myBoldMetrics;

  //TODO[kb]: move all these awesome constants to Editor's Fonts & Colors settings
  public static final Color BACKGROUND_COLOR = MorphColor.of(UIUtil::getPanelBackground);

  public static final Color SELECTED_BACKGROUND_COLOR =
    JBColor.namedColor("CompletionPopup.selectionBackground", new JBColor(0xc5dffc, 0x113a5c));
  public static final Color SELECTED_NON_FOCUSED_BACKGROUND_COLOR =
    JBColor.namedColor("CompletionPopup.selectionInactiveBackground", new JBColor(0xE0E0E0, 0x515457));

  private static final Color NON_FOCUSED_MASK_COLOR = JBColor.namedColor("CompletionPopup.nonFocusedMask", Gray._0.withAlpha(0));

  public static final Color MATCHED_FOREGROUND_COLOR =
    JBColor.namedColor("CompletionPopup.matchForeground", JBCurrentTheme.Link.Foreground.ENABLED);

  private final LookupImpl myLookup;
  private int myMaxWidth = -1;
  private volatile int myLookupTextWidth = 50;

  private final SimpleColoredComponent myNameComponent;
  private final SimpleColoredComponent myTailComponent;
  private final SimpleColoredComponent myTypeLabel;
  private final LookupPanel myPanel;
  private final Map<Integer, Boolean> mySelected = new HashMap<>();

  private static final String ELLIPSIS = "\u2026";

  private final AsyncRendering myAsyncRendering;
  private final Runnable myLookupWidthUpdater;
  @Deprecated
  private final boolean myShrinkLookup = false;
  private final Object myWidthLock = ObjectUtil.sentinel("lookup width lock");

  public LookupCellRenderer(LookupImpl lookup, JComponent editorComponent) {
    EditorColorsScheme scheme = lookup.getTopLevelEditor().getColorsScheme();
    myNormalFont = scheme.getFont(EditorFontType.PLAIN);
    myBoldFont = scheme.getFont(EditorFontType.BOLD);

    myLookup = lookup;
    myNameComponent = new MySimpleColoredComponent();
    myNameComponent.setOpaque(false);
    myNameComponent.setIconTextGap(JBUIScale.scale(4));
    myNameComponent.setIpad(JBUI.insetsLeft(1));

    myTailComponent = new MySimpleColoredComponent();
    myTailComponent.setOpaque(false);
    myTailComponent.setIpad(JBInsets.emptyInsets());
    myTailComponent.setBorder(JBUI.Borders.emptyRight(10));

    myTypeLabel = new MySimpleColoredComponent();
    myTypeLabel.setOpaque(false);
    myTypeLabel.setIpad(JBInsets.emptyInsets());
    myTypeLabel.setBorder(JBUI.Borders.emptyRight(10));

    myPanel = new LookupPanel();
    myPanel.add(myNameComponent, BorderLayout.WEST);
    myPanel.add(myTailComponent, BorderLayout.CENTER);
    myPanel.add(myTypeLabel, BorderLayout.EAST);

    myNormalMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myNormalFont);
    myBoldMetrics = myLookup.getTopLevelEditor().getComponent().getFontMetrics(myBoldFont);

    myAsyncRendering = new AsyncRendering(myLookup);
    SingleAlarm alarm = new SingleAlarm(this::updateLookupWidthFromVisibleItems, 50, Alarm.ThreadToUse.SWING_THREAD,
                                        IdeaModalityState.stateForComponent(editorComponent), lookup);
    myLookupWidthUpdater = () -> {
      synchronized (alarm) {
        if (!alarm.isDisposed()) {
          alarm.request();
        }
      }
    };
  }

  private boolean myIsSelected = false;

  @Override
  public Component getListCellRendererComponent(final JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
    boolean nonFocusedSelection = isSelected && myLookup.getLookupFocusDegree() == LookupFocusDegree.SEMI_FOCUSED;
    if (!myLookup.isFocused()) {
      isSelected = false;
    }

    myIsSelected = isSelected;
    myPanel.setBackground(nonFocusedSelection ? SELECTED_NON_FOCUSED_BACKGROUND_COLOR : isSelected ? SELECTED_BACKGROUND_COLOR : null);

    final LookupElement item = (LookupElement)value;

    int allowedWidth =
      list.getWidth() - calcSpacing(myNameComponent, myEmptyIcon) - calcSpacing(myTailComponent, null) - calcSpacing(myTypeLabel, null);

    FontMetrics normalMetrics = getRealFontMetrics(item, false);

    LookupElementPresentation presentation = myAsyncRendering.getLastComputed(item);

    if (presentation.getIcon() != null) {
      setIconInsets(myNameComponent);
    }

    myNameComponent.clear();
    Color itemColor = TargetAWT.to(presentation.getItemTextForeground());
    allowedWidth -= setItemTextLabel(item,
                                     itemColor,
                                     isSelected,
                                     presentation,
                                     allowedWidth);

    Font font = myLookup.getCustomFont(item, false);
    if (font == null) {
      font = myNormalFont;
    }
    myTailComponent.setFont(font);
    myTypeLabel.setFont(font);
    myNameComponent.setIcon(LookupIconUtil.augmentIcon(myLookup.getEditor(), presentation.getIcon(), myEmptyIcon));

    final Color grayedForeground = getGrayedForeground(isSelected);

    myTypeLabel.clear();
    if (allowedWidth > 0) {
      allowedWidth -= setTypeTextLabel(item,
                                       grayedForeground,
                                       presentation,
                                       isSelected ? getMaxWidth() : allowedWidth,
                                       isSelected,
                                       nonFocusedSelection,
                                       normalMetrics);
    }

    myTailComponent.clear();
    if (isSelected || allowedWidth >= 0) {
      setTailTextLabel(isSelected, presentation,
                       grayedForeground, isSelected ? getMaxWidth() : allowedWidth, nonFocusedSelection, normalMetrics);
    }

    if (mySelected.containsKey(index)) {
      if (!isSelected && mySelected.get(index)) {
        myPanel.setUpdateExtender(true);
      }
    }
    mySelected.put(index, isSelected);

    final double w = myNameComponent.getPreferredSize().getWidth() +
      myTailComponent.getPreferredSize().getWidth() +
      myTypeLabel.getPreferredSize().getWidth();

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
    Insets insets = component.getInsets();
    if (insets != null) {
      width += insets.left + insets.right;
    }
    if (icon != null) {
      width += icon.getWidth() + component.getIconTextGap();
    }
    return width;
  }

  private int getMaxWidth() {
    if (myMaxWidth < 0) {
      final Point p = myLookup.getComponent().getLocationOnScreen();
      final Rectangle rectangle = ScreenUtil.getScreenRectangle(p);
      myMaxWidth = rectangle.x + rectangle.width - p.x - 111;
    }
    return myMaxWidth;
  }

  private void setTailTextLabel(boolean isSelected,
                                LookupElementPresentation presentation,
                                Color foreground,
                                int allowedWidth,
                                boolean nonFocusedSelection,
                                FontMetrics fontMetrics) {
    int style = getStyle(false, presentation.isStrikeout(), false, false);

    for (LookupElementPresentation.TextFragment fragment : presentation.getTailFragments()) {
      if (allowedWidth < 0) {
        return;
      }

      String trimmed = trimLabelText(fragment.text, allowedWidth, fontMetrics);
      int fragmentStyle = fragment.isItalic() ? style | SimpleTextAttributes.STYLE_ITALIC : style;
      myTailComponent.append(trimmed,
                             new SimpleTextAttributes(fragmentStyle,
                                                      getTailTextColor(isSelected, fragment, foreground, nonFocusedSelection)));
      allowedWidth -= getStringWidth(trimmed, fontMetrics);
    }
  }

  private String trimLabelText(@Nullable String text, int maxWidth, FontMetrics metrics) {
    if (text == null || StringUtil.isEmpty(text)) {
      return "";
    }

    final int strWidth = getStringWidth(text, metrics);
    if (strWidth <= maxWidth || myIsSelected) {
      return text;
    }

    if (getStringWidth(ELLIPSIS, metrics) > maxWidth) {
      return "";
    }

    int i = 0;
    int j = text.length();
    while (i + 1 < j) {
      int mid = (i + j) / 2;
      final String candidate = text.substring(0, mid) + ELLIPSIS;
      final int width = getStringWidth(candidate, metrics);
      if (width <= maxWidth) {
        i = mid;
      }
      else {
        j = mid;
      }
    }

    return text.substring(0, i) + ELLIPSIS;
  }

  private static Color getTypeTextColor(LookupElement item,
                                        Color foreground,
                                        LookupElementPresentation presentation,
                                        boolean selected,
                                        boolean nonFocusedSelection) {
    if (nonFocusedSelection) {
      return foreground;
    }

    return presentation.isTypeGrayed() ? getGrayedForeground(selected) : item instanceof EmptyLookupItem ? JBColor.foreground() : foreground;
  }

  private static Color getTailTextColor(boolean isSelected,
                                        LookupElementPresentation.TextFragment fragment,
                                        Color defaultForeground,
                                        boolean nonFocusedSelection) {
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
    return UIUtil.getContextHelpForeground();
  }

  private int setItemTextLabel(LookupElement item,
                               final Color foreground,
                               final boolean selected,
                               LookupElementPresentation presentation,
                               int allowedWidth) {
    boolean bold = presentation.isItemTextBold();

    Font customItemFont = myLookup.getCustomFont(item, bold);
    myNameComponent.setFont(customItemFont != null ? customItemFont : bold ? myBoldFont : myNormalFont);
    int style = getStyle(bold, presentation.isStrikeout(), presentation.isItemTextUnderlined(), presentation.isItemTextItalic());

    final FontMetrics metrics = getRealFontMetrics(item, bold);
    final String name = trimLabelText(presentation.getItemText(), allowedWidth, metrics);
    int used = getStringWidth(name, metrics);

    renderItemName(item, foreground, style, name, myNameComponent);
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

  private void renderItemName(LookupElement item,
                              Color foreground,
                              @SimpleTextAttributes.StyleAttributeConstant int style,
                              String name,
                              final SimpleColoredComponent nameComponent) {
    final SimpleTextAttributes base = new SimpleTextAttributes(style, foreground);

    final String prefix = item instanceof EmptyLookupItem ? "" : myLookup.itemPattern(item);
    if (prefix.length() > 0) {
      Iterable<MatcherTextRange> ranges = getMatchingFragments(prefix, name);
      if (ranges != null) {
        SimpleTextAttributes highlighted = new SimpleTextAttributes(style, MATCHED_FOREGROUND_COLOR);
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
                               Color foreground,
                               final LookupElementPresentation presentation,
                               int allowedWidth,
                               boolean selected,
                               boolean nonFocusedSelection,
                               FontMetrics normalMetrics) {
    final String givenText = presentation.getTypeText();
    final String labelText = trimLabelText(StringUtil.isEmpty(givenText) ? "" : " " + givenText, allowedWidth, normalMetrics);

    int used = getStringWidth(labelText, normalMetrics);

    final Image icon = presentation.getTypeIcon();
    if (icon != null) {
      myTypeLabel.setIcon(icon);
      used += icon.getWidth();
    }

    Object o = item.isValid() ? item.getObject() : null;
    //noinspection deprecation
    if (o instanceof LookupValueWithUIHint && StringUtil.isEmpty(labelText)) {
      myTypeLabel.append("  ");
      used += normalMetrics.stringWidth("WW");
    }
    else {
      myTypeLabel.append(labelText);
    }

    myTypeLabel.setForeground(getTypeTextColor(item, foreground, presentation, selected, nonFocusedSelection));
    return used;
  }

  public int getLookupTextWidth() {
    return myLookupTextWidth;
  }

  int updateMaximumWidth(final LookupElementPresentation p, LookupElement item) {
    final Image icon = p.getIcon();
    if (icon != null && (icon.getWidth() > myEmptyIcon.getWidth() || icon.getHeight() > myEmptyIcon.getHeight())) {
      // get width return scaled and image creating also scaled - it will be double scale
      //myEmptyIcon = Image.empty(Math.max(icon.getWidth(), myEmptyIcon.getWidth()), Math.max(icon.getHeight(), myEmptyIcon.getHeight()));
    }

    FontMetrics normalMetrics = getRealFontMetrics(item, false);
    FontMetrics boldMetrics = getRealFontMetrics(item, true);
    int result = 0;
    result += getStringWidth(p.getItemText(), p.isItemTextBold() ? boldMetrics : normalMetrics);
    result += getStringWidth(p.getTailText(), normalMetrics);
    final String typeText = p.getTypeText();
    if (StringUtil.isNotEmpty(typeText)) {
      result += getStringWidth("W", normalMetrics); // nice tail-type separation
      result += getStringWidth(typeText, normalMetrics);
    }
    result += getStringWidth("W", boldMetrics); //for unforeseen Swing size adjustments
    final Image typeIcon = p.getTypeIcon();
    if (typeIcon != null) {
      result += typeIcon.getWidth();
    }
    return result + calcSpacing(myTailComponent, null) + calcSpacing(myTypeLabel, null);
  }

  public static int getStringWidth(@Nullable final String text, FontMetrics metrics) {
    return text != null ? metrics.stringWidth(text) : 0;
  }

  public int getTextIndent() {
    return myNameComponent.getIpad().left + myEmptyIcon.getWidth() + myNameComponent.getIconTextGap();
  }

  void scheduleUpdateLookupWidthFromVisibleItems() {
    myLookupWidthUpdater.run();
  }

  @Nullable
  private Font getFontAbleToDisplay(@Nullable String sampleString) {
    if (sampleString == null) return null;

    // assume a single font can display all chars
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

  void updateItemPresentation(@Nonnull LookupElement element) {
    LookupElementRenderer<? extends LookupElement> renderer = element.getExpensiveRenderer();
    if (renderer != null) {
      myAsyncRendering.scheduleRendering(element, renderer);
    }
  }

  /**
   * Update lookup width due to visible in lookup items
   */
  void updateLookupWidthFromVisibleItems() {
    List<LookupElement> visibleItems = myLookup.getVisibleItems();

    int maxWidth = myShrinkLookup ? 0 : myLookupTextWidth;
    for (var item : visibleItems) {
      LookupElementPresentation presentation = myAsyncRendering.getLastComputed(item);

      item.putUserData(CUSTOM_NAME_FONT, getFontAbleToDisplay(presentation.getItemText()));
      item.putUserData(CUSTOM_TAIL_FONT, getFontAbleToDisplay(presentation.getTailText()));
      item.putUserData(CUSTOM_TYPE_FONT, getFontAbleToDisplay(presentation.getTypeText()));

      int itemWidth = updateMaximumWidth(presentation, item);
      if (itemWidth > maxWidth) {
        maxWidth = itemWidth;
      }
    }

    synchronized (myWidthLock) {
      if (myShrinkLookup || maxWidth > myLookupTextWidth) {
        myLookupTextWidth = maxWidth;
        myLookup.requestResize();
        myLookup.refreshUi(false, false);
      }
    }
  }

  void itemAdded(@Nonnull LookupElement element, @Nonnull LookupElementPresentation fastPresentation) {
    updateIconWidth(fastPresentation.getIcon());
    scheduleUpdateLookupWidthFromVisibleItems();
    AsyncRendering.rememberPresentation(element, fastPresentation);

    updateItemPresentation(element);
  }

  private void updateIconWidth(@Nullable Image baseIcon) {
    Image icon = baseIcon;
    if (icon == null) {
      return;
    }

    icon = Image.empty(icon.getWidth(), icon.getHeight());

    if (icon.getWidth() > myEmptyIcon.getWidth() || icon.getHeight() > myEmptyIcon.getHeight()) {
      myEmptyIcon = Image.empty(Math.max(icon.getWidth(), myEmptyIcon.getWidth()),
                                Math.max(icon.getHeight(), myEmptyIcon.getHeight()));
      setIconInsets(myNameComponent);
    }
  }

  private static void setIconInsets(@Nonnull SimpleColoredComponent component) {
    component.setIpad(JBUI.insetsLeft(6));
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
      setBorder(JBCurrentTheme.listCellBorderFull());
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
