/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.application.ui.impl;

import consulo.application.ui.UIFontManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 21-Feb-22
 */
@Singleton
@State(name = "UIFontManager", storages = @Storage(value = "ui.font.xml", roamingType = RoamingType.PER_OS))
public class UIFontManagerImpl implements UIFontManager, PersistentStateComponent<UIFontManagerImpl.State> {
  public static class State {
    public String fontName;
    public int fontSize;
    public boolean overrideFont;
  }

  private final State myState = new State();

  private Pair<String, Integer> mySystemFontInfo;

  @Override
  public boolean isOverrideFont() {
    return myState.overrideFont;
  }

  @Nonnull
  @Override
  public String getFontName() {
    String fontName = myState.fontName;
    if (fontName == null) {
      return initSystemFontInfo().getFirst();
    }
    return fontName;
  }

  @Override
  public int getFontSize() {
    if (myState.fontSize == 0) {
      return initSystemFontInfo().getSecond();
    }
    return myState.fontSize;
  }

  @Override
  public void setFontName(@Nullable String fontName) {
    myState.fontName = fontName;
  }

  @Override
  public void setFontSize(int fontSize) {
    myState.fontSize = fontSize;
  }

  @Override
  public void setOverrideFont(boolean overrideFont) {
    myState.overrideFont = overrideFont;
  }

  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState.fontName = state.fontName;
    myState.fontSize = state.fontSize;
    myState.overrideFont = state.overrideFont;
  }

  @Override
  public void afterLoadState() {

    // 1. Sometimes system font cannot display standard ASCII symbols. If so we have
    // find any other suitable font withing "preferred" fonts first.
    boolean fontIsValid = UIUtil.isValidFont(new Font(getFontName(), Font.PLAIN, getFontSize()));
    if (!fontIsValid) {
      final String[] preferredFonts = {"dialog", "Arial", "Tahoma"};
      for (String preferredFont : preferredFonts) {
        if (UIUtil.isValidFont(new Font(preferredFont, Font.PLAIN, getFontSize()))) {
          setFontName(preferredFont);
          fontIsValid = true;
          break;
        }
      }

      // 2. If all preferred fonts are not valid in current environment
      // we have to find first valid font (if any)
      if (!fontIsValid) {
        String[] fontNames = UIUtil.getValidFontNames(false);
        if (fontNames.length > 0) {
          setFontName(fontNames[0]);
        }
      }
    }
  }

  @Nonnull
  private Pair<String, Integer> initSystemFontInfo() {
    if(mySystemFontInfo == null) {
      mySystemFontInfo = JBUIScale.getSystemFontData();
    }
    return mySystemFontInfo;
  }
}
