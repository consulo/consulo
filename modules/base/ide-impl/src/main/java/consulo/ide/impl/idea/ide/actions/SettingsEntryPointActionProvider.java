// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.ui.ex.action.AnAction;
import consulo.dataContext.DataContext;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nonnull;
import java.util.Collection;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface SettingsEntryPointActionProvider {
  ExtensionPointName<SettingsEntryPointActionProvider> EP_NAME = ExtensionPointName.create(SettingsEntryPointActionProvider.class);

  String ICON_KEY = "Update_Type_Icon_Key";

  @Nonnull
  Collection<AnAction> getUpdateActions(@Nonnull DataContext context);
}