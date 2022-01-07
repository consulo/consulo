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
package com.intellij.openapi.actionSystem;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * The main (and single) purpose of this class is provide lazy initialization
 * of the actions. ClassLoader eats a lot of time on startup to load the actions' classes.
 *
 * @author Vladimir Kondratyev
 */
public class ActionStub extends AnAction implements ActionStubBase {
  private static final Logger LOG = Logger.getInstance(ActionStub.class);

  private final String myClassName;
  private final String myId;
  private final String myIconPath;
  private final Supplier<Presentation> myTemplatePresentation;
  @Nonnull
  private final PluginDescriptor myPluginDescriptor;

  public ActionStub(@Nonnull String actionClass,
                    @Nonnull String id,
                    @Nonnull PluginDescriptor pluginDescriptor,
                    String iconPath,
                    @Nonnull Supplier<Presentation> templatePresentation) {
    LOG.assertTrue(id.length() > 0);
    myPluginDescriptor = pluginDescriptor;
    myClassName = actionClass;
    myId = id;
    myIconPath = iconPath;
    myTemplatePresentation = templatePresentation;
  }

  @Nonnull
  @Override
  Presentation createTemplatePresentation() {
    return myTemplatePresentation.get();
  }

  public String getClassName() {
    return myClassName;
  }

  @Override
  public String getId() {
    return myId;
  }

  public ClassLoader getLoader() {
    return myPluginDescriptor.getPluginClassLoader();
  }

  @Override
  public PluginId getPluginId() {
    return myPluginDescriptor.getPluginId();
  }

  @Override
  public String getIconPath() {
    return myIconPath;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    throw new UnsupportedOperationException();
  }

  public final void initAction(@Nonnull AnAction targetAction) {
    copyTemplatePresentation(getTemplatePresentation(), targetAction.getTemplatePresentation());

    targetAction.setShortcutSet(getShortcutSet());
    targetAction.setCanUseProjectAsDefault(isCanUseProjectAsDefault());
    targetAction.setModuleExtensionIds(getModuleExtensionIds());
  }

  /**
   * Copies template presentation and shortcuts set to <code>targetAction</code>.
   *
   * @param targetAction cannot be <code>null</code>
   */
  public static void copyTemplatePresentation(@Nonnull Presentation sourcePresentation, @Nonnull Presentation targetPresentation) {
    if (targetPresentation.getIcon() == null && sourcePresentation.getIcon() != null) {
      targetPresentation.setIcon(sourcePresentation.getIcon());
    }
    if (targetPresentation.getText() == null && sourcePresentation.getText() != null) {
      targetPresentation.setText(sourcePresentation.getText());
    }
    if (targetPresentation.getDescription() == null && sourcePresentation.getDescription() != null) {
      targetPresentation.setDescription(sourcePresentation.getDescription());
    }
  }
}
