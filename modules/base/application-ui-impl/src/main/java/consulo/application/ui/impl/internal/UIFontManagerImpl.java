/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.ui.impl.internal;

import consulo.application.ui.UIFontManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 26/01/2023
 */
@State(name = "UIFontManager", storages = @Storage(value = "ui.font.xml", roamingType = RoamingType.PER_OS))
public abstract class UIFontManagerImpl implements UIFontManager, PersistentStateComponent<UIFontManagerImpl.State> {
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

  @Nonnull
  private Pair<String, Integer> initSystemFontInfo() {
    if (mySystemFontInfo == null) {
      mySystemFontInfo = resolveSystemFontData();
    }
    return mySystemFontInfo;
  }

  @Nonnull
  protected abstract Pair<String, Integer> resolveSystemFontData();
}