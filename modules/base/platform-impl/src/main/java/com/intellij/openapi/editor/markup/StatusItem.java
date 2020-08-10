/*
 * Copyright 2013-2020 consulo.io
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
package com.intellij.openapi.editor.markup;

import consulo.ui.image.Image;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Severity status item containing text (not necessarily a number) possible icon and severity type
 * <p>
 * from kotlin
 */
public final class StatusItem {
  private final String myText;
  private final Image myIcon;
  private final String myType;

  public StatusItem(String text) {
    this(text, null, null);
  }

  public StatusItem(String text, @Nullable Image icon) {
    this(text, icon, null);
  }

  public StatusItem(String text, @Nullable Image icon, @Nullable String type) {
    myText = text;
    myIcon = icon;
    myType = type;
  }

  public Image getIcon() {
    return myIcon;
  }

  public String getText() {
    return myText;
  }

  public String getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StatusItem that = (StatusItem)o;
    return Objects.equals(myText, that.myText) && Objects.equals(myIcon, that.myIcon) && Objects.equals(myType, that.myType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myText, myIcon, myType);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("StatusItem{");
    sb.append("myText='").append(myText).append('\'');
    sb.append(", myIcon=").append(myIcon);
    sb.append(", myType='").append(myType).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
