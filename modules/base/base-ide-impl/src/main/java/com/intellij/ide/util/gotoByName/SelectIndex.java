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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * from kotlin
 */
public class SelectIndex implements SelectionPolicy {
  private final int mySelectIndex;

  public SelectIndex(int selectIndex) {
    mySelectIndex = selectIndex;
  }

  @Nonnull
  @Override
  public List<Integer> performSelection(ChooseByNameBase popup, SmartPointerListModel<?> model) {
    return Collections.singletonList(mySelectIndex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SelectIndex that = (SelectIndex)o;
    return mySelectIndex == that.mySelectIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mySelectIndex);
  }
}
