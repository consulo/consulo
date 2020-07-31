/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.find.impl;

import com.intellij.find.FindModel;
import com.intellij.find.FindSettings;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Pair;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import javax.annotation.*;
import javax.swing.*;

public interface FindPopupScopeUI {
  @Nonnull
  Pair<ScopeType, JComponent>[] getComponents();

  @Nonnull
  ScopeType initByModel(@Nonnull FindModel findModel);
  void applyTo(@Nonnull FindSettings findSettings, @Nonnull FindPopupScopeUI.ScopeType selectedScope);
  void applyTo(@Nonnull FindModel findModel, @Nonnull FindPopupScopeUI.ScopeType selectedScope);

  /**
   *
   * @param model
   * @param selectedScope
   * @return null means OK
   */
  @Nullable
  default ValidationInfo validate(@Nonnull FindModel model, FindPopupScopeUI.ScopeType selectedScope) {
    return null;
  }

  boolean hideAllPopups();

  class ScopeType {
    public final String name;
    public final LocalizeValue text;
    public final Image icon;

    public ScopeType(@Nonnull String name, @Nonnull LocalizeValue text, @Nullable Image icon) {
      this.name = name;
      this.text = text;
      this.icon = icon;
    }
  }
}
