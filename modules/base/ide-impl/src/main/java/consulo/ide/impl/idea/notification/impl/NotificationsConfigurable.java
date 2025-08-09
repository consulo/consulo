/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.notification.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.notification.impl.ui.NotificationsConfigurablePanel;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author spleaner
 */
@ExtensionImpl
public class NotificationsConfigurable implements Configurable, SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  private NotificationsConfigurablePanel myComponent;

  @Override
  @Nonnull
  public String getId() {
    return "notifications";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Notifications");
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent(@Nonnull Disposable uiDisposable) {
    if (myComponent == null) {
      myComponent = new NotificationsConfigurablePanel();
      Disposer.register(uiDisposable, myComponent);
    }

    return myComponent;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myComponent != null && myComponent.isModified();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myComponent.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myComponent.reset();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myComponent = null;
  }

  @Override
  public Runnable enableSearch(String option) {
    return () -> myComponent.selectGroup(option);
  }
}
