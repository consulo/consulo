/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author anna
 * @since 2005-03-17
 */
@ExtensionImpl
public class CustomizationConfigurable implements SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  private CustomizableActionsPanel myPanel;

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    if (myPanel == null) {
      myPanel = new CustomizableActionsPanel();
    }
    return myPanel.getPanel();
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return IdeLocalize.titleCustomizations();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if(myPanel != null) {
      Disposer.dispose(myPanel);
      myPanel = null;
    }
  }

  @Override
  @Nonnull
  public String getId() {
    return "preferences.customizations";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }
}
