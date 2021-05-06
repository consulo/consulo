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
import com.intellij.ui.content.Content;
import com.intellij.ui.content.TabbedContent;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.annotation.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

/**
 * from kotlin
 */
public class SelectContentStep extends BaseListPopupStep<Content> {
  public SelectContentStep(Content[] contents) {
    super(null, contents);
  }

  public SelectContentStep(List<Content> contents) {
    super(null, contents);
  }

  @Override
  public boolean isSpeedSearchEnabled() {
    return true;
  }

  @Override
  public Image getIconFor(Content value) {
    return value.getIcon();
  }

  @Nonnull
  @Override
  public String getTextFor(Content value) {
    TabbedContent tabbedContent = asMultiTabbed(value);
    if(tabbedContent != null) {
      String titlePrefix = tabbedContent.getTitlePrefix();
      if(titlePrefix != null) {
        return titlePrefix;
      }
    }

    String displayName = value.getDisplayName();
    if(displayName != null) {
      return displayName;
    }
    return super.getTextFor(value);
  }

  @Override
  public PopupStep onChosen(Content selectedValue, boolean finalChoice) {
    TabbedContent tabbed = asMultiTabbed(selectedValue);
    if(tabbed == null) {
      selectedValue.getManager().setSelectedContentCB(selectedValue, true, true);
      return PopupStep.FINAL_CHOICE;
    }

    return new SelectContentTabStep(tabbed);
  }

  @Override
  public boolean hasSubstep(Content selectedValue) {
    return asMultiTabbed(selectedValue) != null;
  }

  @Nullable
  private static TabbedContent asMultiTabbed(Content content) {
    if(content instanceof TabbedContent && ((TabbedContent)content).hasMultipleTabs()) {
      return (TabbedContent)content;
    }
    return null;
  }
}
