/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.openapi.wm.impl.content;

import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.TabbedContent;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * from kotlin
 */
public class SelectContentTabStep extends BaseListPopupStep<Integer> {
  private final TabbedContent myContent;
  private final List<Pair<String, JComponent>> myTabs;

  public SelectContentTabStep(TabbedContent content) {
    super(null);
    myContent = content;
    myTabs = myContent.getTabs();

    List<Integer> indexes = new ArrayList<>();
    for (int i = 0; i < myTabs.size(); i++) {
      indexes.add(i);
    }

    init(null, indexes, null);

    setDefaultOptionIndex(content.getSelectedIndex());
  }

  @Override
  public boolean isSpeedSearchEnabled() {
    return true;
  }

  @Nonnull
  @Override
  public String getTextFor(Integer value) {
    return myTabs.get(value).getFirst();
  }

  @Override
  public PopupStep onChosen(Integer selectedValue, boolean finalChoice) {
    ContentManager manager = myContent.getManager();
    if(manager == null) {
      return FINAL_CHOICE;
    }
    myContent.selectContent(selectedValue);
    manager.setSelectedContent(myContent);
    return FINAL_CHOICE;
  }
}
