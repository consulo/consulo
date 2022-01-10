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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * from kotlin
 */
public class PreserveSelection implements SelectionPolicy {
  public static final PreserveSelection INSTANCE = new PreserveSelection();

  @Nonnull
  @Override
  public List<Integer> performSelection(ChooseByNameBase popup, SmartPointerListModel<?> model) {
    Set<Object> chosenElements = popup.currentChosenInfo == null ? Collections.emptySet() : popup.currentChosenInfo.getChosenElements();

    List<?> items = model.getItems();

    List<Integer> preserved = new ArrayList<>();
    for(int i = 0; i < items.size(); i ++) {
      Object o = items.get(i);

      if(chosenElements.contains(o)) {
        preserved.add(i);
      }
    }

    return !preserved.isEmpty() ? preserved : SelectMostRelevant.INSTANCE.performSelection(popup, model);
  }
}
