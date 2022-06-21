/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.plugin;

import consulo.util.nodep.xml.node.SimpleXmlElement;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-07-17
 */
public interface PluginDescriptor {
  PluginDescriptor[] EMPTY_ARRAY = new PluginDescriptor[0];

  public interface UserDataCalculator<Param, Result> {
    Result calc(Param param);
  }

  String EXPERIMENTAL_TAG = "experimental";

  PluginId getPluginId();

  ClassLoader getPluginClassLoader();

  File getPath();

  String getDescription();

  String getChangeNotes();

  String getName();

  PluginId[] getDependentPluginIds();

  PluginId[] getOptionalDependentPluginIds();

  PluginId[] getIncompatibleWithPlugindIds();

  String getVendor();

  String getVersion();

  String getPlatformVersion();

  String getResourceBundleBaseName();

  String getLocalize();

  @Deprecated
  //@DeprecationInfo("Use #getTags()")
  String getCategory();

  Set<String> getTags();

  List<SimpleXmlElement> getActionsDescriptionElements();

  String getVendorEmail();

  String getVendorUrl();

  String getUrl();

  Collection<HelpSetPath> getHelpSets();

  String getDownloads();

  List<SimpleExtension> getSimpleExtensions();

  PluginPermissionDescriptor getPermissionDescriptor(PluginPermissionType permissionType);

  /**
   * If return not null, plugin loaded in module mode.
   */
  ModuleLayer getModuleLayer();

  @Deprecated
  //@DeprecationInfo("use #getIconBytes(darkOrLight)")
  byte[] getIconBytes();

  byte[] getIconBytes(boolean isDarkTheme);

  @Deprecated
  //@DeprecationInfo("This method is obsolete now. Bundled plugin is always platform modules - it can't load plugins")
  boolean isBundled();

  boolean isEnabled();

  void setEnabled(boolean enabled);

  boolean isLoaded();

  boolean isDeleted();

  boolean isExperimental();

  String getChecksumSHA3_256();

  <K, V> V getUserData(K key);

  /**
   * Put user data data
   */
  <K, V> V computeUserData(K key, UserDataCalculator<K, V> function);
}
