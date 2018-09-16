/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.completion;

import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 02-May-17
 * <p>
 * from kotlin platform\lang-impl\src\com\intellij\codeInsight\completion\PresentationInvariant.kt
 */
public class PresentationInvariant implements Comparable<PresentationInvariant> {
  @Nullable
  private String itemText;
  @Nullable
  private String tail;
  @Nullable
  private String type;

  public PresentationInvariant(@Nullable String itemText, @Nullable String tail, @Nullable String type) {
    this.itemText = itemText;
    this.tail = tail;
    this.type = type;
  }

  @Override
  public int compareTo(@Nonnull PresentationInvariant other) {
    int result = StringUtil.naturalCompare(itemText, other.itemText);
    if (result != 0) return result;

    result = Integer.compare(getLenght(tail), getLenght(other.tail));
    if (result != 0) return result;

    result = StringUtil.naturalCompare(StringUtil.notNullize(tail), StringUtil.notNullize(tail));
    if (result != 0) return result;

    return StringUtil.naturalCompare(StringUtil.notNullize(type), StringUtil.notNullize(other.type));
  }

  private static int getLenght(String str) {
    return str == null ? 0 : str.length();
  }
}
