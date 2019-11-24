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

package com.intellij.execution;

import com.intellij.BundleBase;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author spleaner
 */
public abstract class Executor {
  public static final ExtensionPointName<Executor> EP_NAME = ExtensionPointName.create("com.intellij.executor");

  public abstract String getToolWindowId();

  public abstract Image getToolWindowIcon();

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public Image getDisabledIcon() {
    return null;
  }

  public abstract String getDescription();

  @Nonnull
  public abstract String getActionName();

  @Nonnull
  @NonNls
  public abstract String getId();

  @Nonnull
  public abstract String getStartActionText();

  @NonNls
  public String getContextActionId() {
    return "Context" + getId();
  }

  @NonNls
  public abstract String getHelpId();

  /**
   * Override this method and return {@code false} to hide executor from panel
   */
  public boolean isApplicable(@Nonnull Project project) {
    return true;
  }

  @Nonnull
  public String getActionText(@javax.annotation.Nullable String configurationName) {
    return BundleBase.format(getStartActionText(StringUtil.isEmpty(configurationName)), escapeMnemonicsInConfigurationName(configurationName));
  }

  private static String escapeMnemonicsInConfigurationName(String configurationName) {
    return configurationName.replace("_", "__");
  }

  @Nonnull
  public String getStartActionText(boolean emptyName) {
    return getStartActionText() + (emptyName ? "" : " ''{0}''");
  }

  @Deprecated
  @DeprecationInfo("Use #getStartActionText(emptyName)")
  @Nonnull
  public String getStartActionText(String configurationName) {
    return getStartActionText(StringUtil.isEmpty(configurationName));
  }
}
