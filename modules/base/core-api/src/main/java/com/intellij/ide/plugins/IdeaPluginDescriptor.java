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
package com.intellij.ide.plugins;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.extensions.PluginId;
import consulo.ide.plugins.SimpleExtension;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public interface IdeaPluginDescriptor {
  PluginId getPluginId();

  ClassLoader getPluginClassLoader();

  @Nullable
  File getPath();

  @Nullable
  String getDescription();

  String getChangeNotes();

  String getName();

  @Nonnull
  PluginId[] getDependentPluginIds();

  @Nonnull
  PluginId[] getOptionalDependentPluginIds();

  String getVendor();

  @Nullable
  String getVersion();

  @Nullable
  String getPlatformVersion();

  String getResourceBundleBaseName();

  String getCategory();

  @Nullable
  List<Element> getActionsDescriptionElements();

  @Nonnull
  ComponentConfig[] getAppComponents();

  @Nonnull
  ComponentConfig[] getProjectComponents();

  String getVendorEmail();

  String getVendorUrl();

  String getUrl();

  @Nonnull
  HelpSetPath[] getHelpSets();

  String getDownloads();

  @Nonnull
  default List<SimpleExtension> getSimpleExtensions() {
    return Collections.emptyList();
  }

  default boolean isBundled() {
    return false;
  }

  boolean isEnabled();

  void setEnabled(boolean enabled);
}
