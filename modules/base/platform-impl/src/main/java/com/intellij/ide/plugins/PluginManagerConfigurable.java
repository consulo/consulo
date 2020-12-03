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
package com.intellij.ide.plugins;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.Messages;
import consulo.container.plugin.PluginDescriptor;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author stathik
 * @since 9:30:44 PM Oct 26, 2003
 */
public class PluginManagerConfigurable implements SearchableConfigurable, Configurable.NoScroll, Configurable.HoldPreferredFocusedComponent {
  private static final String POSTPONE = "&Postpone";
  public static final String ID = "preferences.pluginManager";
  public static final String DISPLAY_NAME = IdeBundle.message("title.plugins");

  private PluginManagerMain myPluginManagerMain;
  protected final PluginManagerUISettings myUISettings;
  protected boolean myAvailable;

  @Inject
  public PluginManagerConfigurable(final PluginManagerUISettings uiSettings) {
    myUISettings = uiSettings;
  }

  public PluginManagerConfigurable(final PluginManagerUISettings uiSettings, boolean available) {
    myUISettings = uiSettings;
    myAvailable = available;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myPluginManagerMain.getPluginTable();
  }

  @Override
  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPluginManagerMain.reset();
    myPluginManagerMain.myPluginsModel.sort();
  }

  @Override
  public String getHelpTopic() {
    return ID;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPluginManagerMain != null) {
      Disposer.dispose(myPluginManagerMain);
      myPluginManagerMain = null;
    }
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    return getOrCreatePanel().getMainPanel();
  }

  protected PluginManagerMain createPanel() {
    return new InstalledPluginsManagerMain();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    final String applyMessage = myPluginManagerMain.apply();
    if (applyMessage != null) {
      throw new ConfigurationException(applyMessage);
    }

    if (myPluginManagerMain.isRequireShutdown()) {
      final ApplicationEx app = (ApplicationEx)Application.get();

      int response = app.isRestartCapable() ? showRestartIDEADialog() : showShutDownIDEADialog();
      if (response == Messages.YES) {
        app.restart(true);
      }
      else {
        myPluginManagerMain.ignoreChanges();
      }
    }
  }

  public PluginManagerMain getOrCreatePanel() {
    if (myPluginManagerMain == null) {
      myPluginManagerMain = createPanel();
    }
    return myPluginManagerMain;
  }

  @Messages.YesNoResult
  public static int showShutDownIDEADialog() {
    return showShutDownIDEADialog(IdeBundle.message("title.plugins.changed"));
  }

  @Messages.YesNoResult
  private static int showShutDownIDEADialog(final String title) {
    String message = IdeBundle.message("message.idea.shutdown.required", ApplicationNamesInfo.getInstance().getFullProductName());
    return Messages.showYesNoDialog(message, title, "Shut Down", POSTPONE, Messages.getQuestionIcon());
  }

  @Messages.YesNoResult
  public static int showRestartIDEADialog() {
    return showRestartIDEADialog(IdeBundle.message("title.plugins.changed"));
  }

  @Messages.YesNoResult
  private static int showRestartIDEADialog(final String title) {
    String message = IdeBundle.message("message.idea.restart.required", ApplicationNamesInfo.getInstance().getFullProductName());
    return Messages.showYesNoDialog(message, title, "Restart", POSTPONE, Messages.getQuestionIcon());
  }

  public static void shutdownOrRestartApp(String title) {
    final ApplicationEx app = (ApplicationEx)Application.get();
    int response = app.isRestartCapable() ? showRestartIDEADialog(title) : showShutDownIDEADialog(title);
    if (response == Messages.YES) app.restart(true);
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPluginManagerMain != null && myPluginManagerMain.isModified();
  }

  @Override
  @Nonnull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  @Nullable
  public Runnable enableSearch(final String option) {
    return new Runnable() {
      @Override
      public void run() {
        if (myPluginManagerMain == null) return;
        myPluginManagerMain.filter(option);
      }
    };
  }

  public void select(PluginDescriptor... descriptors) {
    myPluginManagerMain.select(descriptors);
  }
}
