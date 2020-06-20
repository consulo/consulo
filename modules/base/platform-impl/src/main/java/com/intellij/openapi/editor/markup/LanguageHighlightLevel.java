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

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Per language highlight level
 *
 * from kotlin
 */
public final class LanguageHighlightLevel {
  private final String myLangID;
  private final InspectionsLevel myLevel;

  public LanguageHighlightLevel(@Nonnull String langID, @Nonnull  InspectionsLevel level) {
    myLangID = langID;
    myLevel = level;
  }

  public LanguageHighlightLevel copy(@Nonnull String langID, @Nonnull InspectionsLevel level) {
    return new LanguageHighlightLevel(langID, level);
  }

  public String getLangID() {
    return myLangID;
  }

  public InspectionsLevel getLevel() {
    return myLevel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LanguageHighlightLevel that = (LanguageHighlightLevel)o;
    return Objects.equals(myLangID, that.myLangID) && myLevel == that.myLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(myLangID, myLevel);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("LanguageHighlightLevel{");
    sb.append("myLangId='").append(myLangID).append('\'');
    sb.append(", myInspectionsLevel=").append(myLevel);
    sb.append('}');
    return sb.toString();
  }
}
