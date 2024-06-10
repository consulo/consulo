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
package consulo.language.editor.completion.lookup;

import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author peter
 */
public class LookupElementPresentation {
  private Image myIcon;
  private Image myTypeIcon;
  private boolean myTypeIconRightAligned;
  private String myItemText;
  private String myTypeText;
  private boolean myStrikeout;
  private ColorValue myItemTextForeground = ComponentColors.TEXT_FOREGROUND;
  private boolean myItemTextBold;
  private boolean myItemTextUnderlined;
  private boolean myItemTextItalic;
  private boolean myTypeGrayed;
  @Nullable
  private List<TextFragment> myTail;
  private volatile boolean myFrozen;

  public void setIcon(@Nullable Image icon) {
    myIcon = icon;
  }

  public void setItemText(@Nullable String text) {
    ensureMutable();
    myItemText = text;
  }

  public void setStrikeout(boolean strikeout) {
    ensureMutable();
    myStrikeout = strikeout;
  }

  public void setItemTextBold(boolean bold) {
    ensureMutable();
    myItemTextBold = bold;
  }

  public void setTailText(@Nullable String text) {
    setTailText(text, false);
  }

  public void clearTail() {
    ensureMutable();
    myTail = null;
  }

  public void appendTailText(@Nonnull String text, boolean grayed) {
    appendTailText(new TextFragment(text, grayed, false, null));
  }

  public void appendTailTextItalic(@Nonnull String text, boolean grayed) {
    appendTailText(new TextFragment(text, grayed, true, null));
  }

  private void appendTailText(@Nonnull TextFragment fragment) {
    if (myTail == null) {
      myTail = new SmartList<>();
    }
    myTail.add(fragment);
  }

  public void setTailText(@Nullable String text, boolean grayed) {
    clearTail();
    if (text != null) {
      appendTailText(new TextFragment(text, grayed, false, null));
    }
  }

  public void setTailText(@Nullable String text, @Nullable ColorValue foreground) {
    clearTail();
    if (text != null) {
      appendTailText(new TextFragment(text, false, false, foreground));
    }
  }

  public void setTypeText(@Nullable String text) {
    setTypeText(text, null);
  }

  public void setTypeText(@Nullable String text, @Nullable Image icon) {
    ensureMutable();
    myTypeText = text;
    myTypeIcon = icon;
  }

  public void setItemTextItalic(boolean itemTextItalic) {
    ensureMutable();
    myItemTextItalic = itemTextItalic;
  }

  @Deprecated
  public boolean isReal() {
    return false;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  @Nullable
  public Image getTypeIcon() {
    return myTypeIcon;
  }

  @Nullable
  public String getItemText() {
    return myItemText;
  }

  @Nonnull
  public List<TextFragment> getTailFragments() {
    return myTail == null ? Collections.emptyList() : Collections.unmodifiableList(myTail);
  }

  @Nullable
  public String getTailText() {
    if (myTail == null) return null;
    return StringUtil.join(myTail, fragment -> fragment.text, "");
  }

  @Nullable
  public String getTypeText() {
    return myTypeText;
  }

  public boolean isStrikeout() {
    return myStrikeout;
  }

  @Deprecated
  public boolean isTailGrayed() {
    return myTail != null && myTail.get(0).myGrayed;
  }

  @Nullable
  @Deprecated
  public ColorValue getTailForeground() {
    return myTail != null ? myTail.get(0).myFgColor : null;
  }

  public boolean isItemTextBold() {
    return myItemTextBold;
  }

  public boolean isItemTextUnderlined() {
    return myItemTextUnderlined;
  }

  public boolean isItemTextItalic() {
    return myItemTextItalic;
  }

  public void setItemTextUnderlined(boolean itemTextUnderlined) {
    ensureMutable();
    myItemTextUnderlined = itemTextUnderlined;
  }

  @Nonnull
  public ColorValue getItemTextForeground() {
    return myItemTextForeground;
  }

  public void setItemTextForeground(@Nonnull ColorValue itemTextForeground) {
    ensureMutable();
    myItemTextForeground = itemTextForeground;
  }

  public void copyFrom(@Nonnull LookupElementPresentation presentation) {
    myIcon = presentation.myIcon;
    myTypeIcon = presentation.myTypeIcon;
    myItemText = presentation.myItemText;

    List<TextFragment> thatTail = presentation.myTail;
    myTail = thatTail == null ? null : new SmartList<>(thatTail);

    myTypeText = presentation.myTypeText;
    myStrikeout = presentation.myStrikeout;
    myItemTextItalic = presentation.myItemTextItalic;
    myItemTextBold = presentation.myItemTextBold;
    myTypeGrayed = presentation.myTypeGrayed;
    myTypeIconRightAligned = presentation.myTypeIconRightAligned;
    myItemTextUnderlined = presentation.myItemTextUnderlined;
    myItemTextForeground = presentation.myItemTextForeground;
  }

  public boolean isTypeIconRightAligned() {
    return myTypeIconRightAligned;
  }

  public void setTypeIconRightAligned(boolean typeIconRightAligned) {
    ensureMutable();
    myTypeIconRightAligned = typeIconRightAligned;
  }

  public boolean isTypeGrayed() {
    return myTypeGrayed;
  }

  public void setTypeGrayed(boolean typeGrayed) {
    ensureMutable();
    myTypeGrayed = typeGrayed;
  }

  public static LookupElementPresentation renderElement(LookupElement element) {
    LookupElementPresentation presentation = new LookupElementPresentation();
    element.renderElement(presentation);
    return presentation;
  }

  @Override
  public String toString() {
    return "LookupElementPresentation{" + ", itemText='" + myItemText + '\'' + ", tail=" + myTail + ", typeText='" + myTypeText + '\'' + '}';
  }

  private void ensureMutable() {
    if (myFrozen) throw new IllegalStateException("This lookup element presentation can't be changed");
  }

  /**
   * Disallow any further changes to this presentation object.
   */
  public void freeze() {
    myFrozen = true;
  }

  public static class TextFragment {
    public final String text;
    private final boolean myGrayed;
    private final boolean myItalic;
    @Nullable
    private final ColorValue myFgColor;

    private TextFragment(String text, boolean grayed, boolean italic, @Nullable ColorValue fgColor) {
      this.text = text;
      myGrayed = grayed;
      myItalic = italic;
      myFgColor = fgColor;
    }

    @Override
    public String toString() {
      return "TextFragment{" + "text='" + text + '\'' + ", grayed=" + myGrayed + ", fgColor=" + myFgColor + '}';
    }

    public boolean isGrayed() {
      return myGrayed;
    }

    public boolean isItalic() {
      return myItalic;
    }

    @Nullable
    public ColorValue getForegroundColor() {
      return myFgColor;
    }
  }
}
