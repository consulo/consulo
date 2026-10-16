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
package consulo.project.ui.impl.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * @author spleaner
 */
@ExtensionImpl
public class NotificationsConfigurable implements Configurable, SearchableConfigurable, Configurable.NoScroll, ApplicationConfigurable {
  private NotificationsConfigurablePanel myComponent;

  @Override
  
  public String getId() {
    return "notifications";
  }

  @Override
  public @Nullable String getParentId() {
    return StandardConfigurableIds.GENERAL_GROUP;
  }

  
  @Override
  public LocalizeValue getDisplayName() {
    return ProjectUILocalize.toolwindowNotificationsDisplayName();
  }

  @RequiredUIAccess
  @Override
  public Component createUIComponent(Disposable uiDisposable) {
    if (myComponent == null) {
      myComponent = new NotificationsConfigurablePanel(NotificationsConfigurationImpl.getInstanceImpl());
    }
    return myComponent.getComponent();
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
    return () -> {
      if (myComponent != null) {
        myComponent.selectGroup(option);
      }
    };
  }
}
