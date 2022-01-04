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
package com.intellij.notification.impl;

import com.intellij.notification.impl.ui.NotificationsConfigurablePanel;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author spleaner
 */
public class NotificationsConfigurable implements Configurable, SearchableConfigurable, Configurable.NoScroll {
  public static final String DISPLAY_NAME = "Notifications";
  private NotificationsConfigurablePanel myComponent;

  @Override
  @Nls
  public String getDisplayName() {
    return DISPLAY_NAME;
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
  @Nonnull
  public String getId() {
    return "notifcations";
  }

  @Override
  public Runnable enableSearch(final String option) {
    return () -> myComponent.selectGroup(option);
  }
}
