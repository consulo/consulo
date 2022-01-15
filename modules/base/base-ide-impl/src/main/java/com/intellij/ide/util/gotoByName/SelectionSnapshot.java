/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.ide.util.gotoByName;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

/**
 * ffrom kotlin
 */
public class SelectionSnapshot {
  private final String pattern;
  private final Set<Object> chosenElements;

  public SelectionSnapshot(String pattern, Set<Object> chosenElements) {
    this.pattern = pattern;
    this.chosenElements = chosenElements;
  }

  @Nonnull
  public Set<Object> getChosenElements() {
    return chosenElements;
  }

  public boolean hasSamePattern(ChooseByNameBase popup) {
    return Objects.equals(popup.transformPattern(pattern), popup.transformPattern(popup.getTrimmedText()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SelectionSnapshot that = (SelectionSnapshot)o;
    return Objects.equals(pattern, that.pattern) && Objects.equals(chosenElements, that.chosenElements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pattern, chosenElements);
  }
}
