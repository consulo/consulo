/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.popup;

import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseListPopupStep<T> extends BaseStep<T> implements ListPopupStep<T> {
  private String myTitle;
  private List<T> myValues;
  private List<Image> myIcons;
  private int myDefaultOptionIndex = -1;

  public BaseListPopupStep(@Nullable String title, T... values) {
    this(title, values, new Image[0]);
  }

  public BaseListPopupStep(@Nullable String title, List<? extends T> values) {
    this(title, values, new ArrayList<>());
  }

  public BaseListPopupStep(@Nullable String title, T[] values, Image[] icons) {
    this(title, Arrays.asList(values), Arrays.asList(icons));
  }

  public BaseListPopupStep(@Nullable String aTitle, @Nonnull List<? extends T> aValues, Image aSameIcon) {
    List<Image> icons = new ArrayList<>(aValues.size());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < aValues.size(); i++) {
      icons.add(aSameIcon);
    }
    init(aTitle, aValues, icons);
  }

  public BaseListPopupStep(@Nullable String title, @Nonnull List<? extends T> values, List<Image> icons) {
    init(title, values, icons);
  }

  protected BaseListPopupStep() { }

  protected final void init(@Nullable String title, @Nonnull List<? extends T> values, @Nullable List<Image> icons) {
    myTitle = title;
    myValues = new ArrayList<>(values);
    myIcons = icons;
  }

  @Override
  @Nullable
  public final String getTitle() {
    return myTitle;
  }

  @Override
  @Nonnull
  public final List<T> getValues() {
    return myValues;
  }

  @Override
  public PopupStep onChosen(T selectedValue, boolean finalChoice) {
    return FINAL_CHOICE;
  }

  @Override
  public Image getIconFor(T value) {
    int index = myValues.indexOf(value);
    if (index != -1 && myIcons != null && index < myIcons.size()) {
      return myIcons.get(index);
    }
    else {
      return null;
    }
  }

  @Nullable
  public Color getBackgroundFor(T value) {
    return null;
  }

  @Nullable
  public Color getForegroundFor(T value) {
    return null;
  }

  @Override
  @Nonnull
  public String getTextFor(T value) {
    return value.toString();
  }

  @Override
  @Nullable
  public ListSeparator getSeparatorAbove(T value) {
    return null;
  }

  @Override
  public boolean isSelectable(T value) {
    return true;
  }

  @Override
  public boolean hasSubstep(T selectedValue) {
    return false;
  }

  @Override
  public void canceled() {
  }

  public void setDefaultOptionIndex(int aDefaultOptionIndex) {
    myDefaultOptionIndex = aDefaultOptionIndex;
  }

  @Override
  public int getDefaultOptionIndex() {
    return myDefaultOptionIndex;
  }
}
