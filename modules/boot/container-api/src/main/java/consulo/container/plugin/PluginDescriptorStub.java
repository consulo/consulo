/* * Copyright 2013-2019 consulo.io * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * * http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */
package consulo.container.plugin;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author VISTALL
 * @since 2019-07-17
 */
public abstract class PluginDescriptorStub implements PluginDescriptor {
  private final ConcurrentMap<Object, Object> myUserData = new ConcurrentHashMap<Object, Object>();

  @Override
  public ClassLoader getPluginClassLoader() {
    throw new UnsupportedOperationException();
  }

  @Override
  public File getPath() {
    return null;
  }

  @Override
  public Path getNioPath() {
    return null;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public String getChangeNotes() {
    return null;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public PluginId[] getDependentPluginIds() {
    return PluginId.EMPTY_ARRAY;
  }

  @Override
  public PluginId[] getOptionalDependentPluginIds() {
    return PluginId.EMPTY_ARRAY;
  }

  @Override
  public PluginId[] getIncompatibleWithPlugindIds() {
    return PluginId.EMPTY_ARRAY;
  }

  @Override
  public String getVendor() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public String getPlatformVersion() {
    return null;
  }

  @Override
  public String getResourceBundleBaseName() {
    return null;
  }

  @Override
  public String getLocalize() {
    return null;
  }

  @Override
  public Set<String> getTags() {
    return Collections.emptySet();
  }

  @Override
  public String getCategory() {
    return normalizeCategory(null);
  }

  protected static String normalizeCategory(String cagetory) {
    if (cagetory == null || cagetory.trim().length() == 0) {
      return "Uncategorized";
    }
    return cagetory;
  }

  @Override
  public List<SimpleXmlElement> getActionsDescriptionElements() {
    return Collections.emptyList();
  }

  @Override
  public PluginPermissionDescriptor getPermissionDescriptor(PluginPermissionType permissionType) {
    return null;
  }

  @Override
  public ModuleLayer getModuleLayer() {
    return null;
  }

  @Override
  public byte[] getIconBytes() {
    return getIconBytes(false);
  }

  @Override
  public byte[] getIconBytes(boolean isDarkTheme) {
    return ArrayUtilRt.EMPTY_BYTE_ARRAY;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> V getUserData(K key) {
    return (V)myUserData.get(key);
  }

  @Override
  public <K, V> V computeUserData(K key, UserDataCalculator<K, V> function) {
    V data = getUserData(key);
    if (data != null) {
      return data;
    }
    V value = function.calc(key);
    myUserData.putIfAbsent(key, value);
    return value;
  }

  @Override
  public String getVendorEmail() {
    return null;
  }

  @Override
  public String getVendorUrl() {
    return null;
  }

  @Override
  public String getUrl() {
    return null;
  }

  @Override
  public String getDownloads() {
    return null;
  }

  @Override
  public boolean isBundled() {
    return false;
  }

  @Override
  public boolean isEnabled() {
    return getStatus() == PluginDescriptorStatus.OK;
  }

  @Override
  public PluginDescriptorStatus getStatus() {
    return PluginDescriptorStatus.OK;
  }

  @Override
  public boolean isLoaded() {
    return false;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public boolean isExperimental() {
    return false;
  }

  @Override
  public String getChecksumSHA3_256() {
    return null;
  }
}
