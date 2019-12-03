/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes;

import com.intellij.ide.impl.dataRules.GetDataRule;
import com.intellij.openapi.actionSystem.DataProvider;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.util.ArrayUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;

public class VcsChangesSelectionRule implements GetDataRule<ChangesSelection> {
  @Nonnull
  @Override
  public Key<ChangesSelection> getKey() {
    return VcsDataKeys.CHANGES_SELECTION;
  }

  @Nullable
  @Override
  public ChangesSelection getData(@Nonnull DataProvider dataProvider) {
    return getChangesSelection(dataProvider);
  }

  @javax.annotation.Nullable
  public ChangesSelection getChangesSelection(@Nonnull DataProvider dataProvider) {
    Change currentChange = dataProvider.getDataUnchecked(VcsDataKeys.CURRENT_CHANGE);

    Change[] selectedChanges = dataProvider.getDataUnchecked(VcsDataKeys.SELECTED_CHANGES);
    if (selectedChanges != null) {
      int index = Math.max(ArrayUtil.indexOf(selectedChanges, currentChange), 0);
      return new ChangesSelection(Arrays.asList(selectedChanges), index);
    }

    Change[] changes = dataProvider.getDataUnchecked(VcsDataKeys.CHANGES);
    if (changes != null) {
      int index = Math.max(ArrayUtil.indexOf(changes, currentChange), 0);
      return new ChangesSelection(Arrays.asList(changes), index);
    }

    if (currentChange != null) {
      return new ChangesSelection(Collections.singletonList(currentChange), 0);
    }
    return null;
  }
}
