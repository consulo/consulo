// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.extensions.ExtensionPointName;

import javax.annotation.Nonnull;
import java.util.Collection;

public interface SettingsEntryPointActionProvider {
  ExtensionPointName<SettingsEntryPointActionProvider> EP_NAME = ExtensionPointName.create("com.intellij.settingsEntryPointActionProvider");

  String ICON_KEY = "Update_Type_Icon_Key";

  @Nonnull
  Collection<AnAction> getUpdateActions(@Nonnull DataContext context);
}