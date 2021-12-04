/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.container.plugin.*;
import consulo.ide.plugins.PluginJsonNode;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author stathik
 */
public class PluginNode extends PluginDescriptorStub {
  public static final int STATUS_UNKNOWN = 0;
  public static final int STATUS_INSTALLED = 1;
  public static final int STATUS_MISSING = 2;
  public static final int STATUS_CURRENT = 3;
  public static final int STATUS_NEWEST = 4;
  public static final int STATUS_DOWNLOADED = 5;
  public static final int STATUS_DELETED = 6;

  private PluginId id;
  private String name;
  private String version;
  private String platformVersion;
  private String vendor;
  private String description;

  private String changeNotes;
  private String downloads;
  private String category;
  private String size;
  private String vendorEmail;
  private String vendorUrl;
  private String url;
  private long date = Long.MAX_VALUE;
  private List<PluginId> myDependencies = Collections.emptyList();
  private List<PluginId> myOptionalDependencies = Collections.emptyList();

  private int status = STATUS_UNKNOWN;

  private String myInstalledVersion;

  private boolean myEnabled = true;
  private String myRating;
  private boolean myExperimental;

  private byte[] myIconBytes = ArrayUtil.EMPTY_BYTE_ARRAY;
  private byte[] myIconDarkBytes = ArrayUtil.EMPTY_BYTE_ARRAY;

  private List<SimpleExtension> mySimpleExtensions = Collections.emptyList();
  private Map<PluginPermissionType, PluginPermissionDescriptor> myPermissions = Collections.emptyMap();

  private String myChecksumSha3_256;

  private final Set<String> myTags = new TreeSet<>();

  public PluginNode() {
  }

  public PluginNode(PluginId id) {
    this.id = id;
  }

  public PluginNode(PluginJsonNode jsonPlugin) {
    setId(jsonPlugin.id);
    setName(jsonPlugin.name);
    setDescription(jsonPlugin.description);
    setDate(jsonPlugin.date);
    setVendor(jsonPlugin.vendor);
    setUrl(jsonPlugin.url);
    setVersion(jsonPlugin.version);
    setPlatformVersion(jsonPlugin.platformVersion);
    setDownloads(String.valueOf(jsonPlugin.downloads));
    setCategory(jsonPlugin.category);
    myIconBytes = decodeIconBytes(jsonPlugin.iconBytes);
    myIconDarkBytes = decodeIconBytes(jsonPlugin.iconDarkBytes);

    if (jsonPlugin.dependencies != null) {
      addDependency(Arrays.stream(jsonPlugin.dependencies).map(PluginId::getId).toArray(PluginId[]::new));
    }

    if (jsonPlugin.optionalDependencies != null) {
      addOptionalDependency(Arrays.stream(jsonPlugin.optionalDependencies).map(PluginId::getId).toArray(PluginId[]::new));
    }

    myChecksumSha3_256 = jsonPlugin.checksum.sha3_256;
    myExperimental = jsonPlugin.experimental;
    PluginJsonNode.Extension[] extensions = jsonPlugin.extensionsV2;
    if (extensions != null) {
      mySimpleExtensions = new ArrayList<>(extensions.length);
      for (PluginJsonNode.Extension extension : extensions) {
        mySimpleExtensions.add(new SimpleExtension(extension.key, extension.values));
      }
    }

    PluginJsonNode.Permission[] permissions = jsonPlugin.permissions;
    if (permissions != null) {
      myPermissions = new HashMap<>();
      for (PluginJsonNode.Permission permission : permissions) {
        try {
          PluginPermissionType type = PluginPermissionType.valueOf(permission.type);
          Set<String> options = permission.options == null ? Set.of() : new TreeSet<>(Arrays.asList(permission.options));
          myPermissions.put(type, new PluginPermissionDescriptor(type, options));
        }
        catch (IllegalArgumentException e) {
          // ignored unknown permission
        }
      }
    }

    String[] tags = jsonPlugin.tags;
    if (tags != null) {
      Collections.addAll(myTags, tags);
    }

    if (jsonPlugin.experimental) {
      myTags.add(EXPERIMENTAL_TAG);
    }
  }

  private static byte[] decodeIconBytes(String iconBytes) {
    if (StringUtil.isEmptyOrSpaces(iconBytes)) {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }
    else {
      try {
        return Base64.getDecoder().decode(iconBytes);
      }
      catch (Exception e) {
        Logger.getInstance(PluginNode.class).warn(e);
      }
    }
    return ArrayUtil.EMPTY_BYTE_ARRAY;
  }

  @Nonnull
  @Override
  public Set<String> getTags() {
    return myTags;
  }

  @Override
  @Nonnull
  public List<SimpleExtension> getSimpleExtensions() {
    return mySimpleExtensions;
  }

  @Nullable
  @Override
  public PluginPermissionDescriptor getPermissionDescriptor(@Nonnull PluginPermissionType permissionType) {
    return myPermissions.get(permissionType);
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (id == null) {
      id = PluginId.getId(name);
    }
    this.name = name;
  }

  public void setId(String id) {
    this.id = PluginId.getId(id);
  }

  @Nonnull
  @Override
  public String getCategory() {
    return normalizeCategory(category);
  }

  /**
   * Be carefull when comparing Plugins versions. Use
   * PluginManagerColumnInfo.compareVersion() for version comparing.
   *
   * @return Return plugin version
   */
  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String getPlatformVersion() {
    return platformVersion;
  }

  public void setPlatformVersion(String platformVersion) {
    this.platformVersion = platformVersion;
  }

  @Override
  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Nonnull
  @Override
  public byte[] getIconBytes(boolean isDarkTheme) {
    if (isDarkTheme && myIconDarkBytes.length > 0) {
      return myIconDarkBytes;
    }
    return myIconBytes;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getChangeNotes() {
    return changeNotes;
  }

  public void setChangeNotes(String changeNotes) {
    this.changeNotes = changeNotes;
  }

  /**
   * In complex environment use PluginManagerColumnInfo.getRealNodeState () method instead.
   *
   * @return Status of plugin
   */
  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public String getDownloads() {
    return downloads;
  }

  public void setDownloads(String downloads) {
    this.downloads = downloads;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  @Override
  public String getVendorEmail() {
    return vendorEmail;
  }

  public void setVendorEmail(String vendorEmail) {
    this.vendorEmail = vendorEmail;
  }

  @Override
  public String getVendorUrl() {
    return vendorUrl;
  }

  public void setVendorUrl(String vendorUrl) {
    this.vendorUrl = vendorUrl;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setDate(Long date) {
    this.date = date == null ? 0 : date;
  }

  public long getDate() {
    return date;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof PluginNode && id.equals(((PluginNode)object).getPluginId());
  }

  public void addDependency(PluginId... depends) {
    if (myDependencies.isEmpty()) {
      myDependencies = new ArrayList<PluginId>();
    }

    Collections.addAll(myDependencies, depends);
  }

  public void addOptionalDependency(PluginId... depends) {
    if (myOptionalDependencies.isEmpty()) {
      myOptionalDependencies = new ArrayList<PluginId>();
    }

    Collections.addAll(myOptionalDependencies, depends);
  }

  /**
   * Methods below implement PluginDescriptor and IdeaPluginDescriptor interface
   */
  @Nonnull
  @Override
  public PluginId getPluginId() {
    return id;
  }


  @Override
  @Nonnull
  public PluginId[] getDependentPluginIds() {
    return myDependencies.isEmpty() ? PluginId.EMPTY_ARRAY : myDependencies.toArray(new PluginId[myDependencies.size()]);
  }

  @Override
  @Nonnull
  public PluginId[] getOptionalDependentPluginIds() {
    return myOptionalDependencies.isEmpty() ? PluginId.EMPTY_ARRAY : myOptionalDependencies.toArray(new PluginId[myOptionalDependencies.size()]);
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }

  @Override
  public boolean isExperimental() {
    return myExperimental;
  }

  @Nullable
  @Override
  public String getChecksumSHA3_256() {
    return myChecksumSha3_256;
  }

  public void setExperimental(boolean experimental) {
    myExperimental = experimental;
  }

  @Nullable
  public String getStatusText() {
    switch (status) {
      case STATUS_UNKNOWN:
        return "Available";
      case STATUS_INSTALLED:
        return "Installed";
      case STATUS_NEWEST:
        return "Ready to update";
      default:
        return null;
    }
  }

  public void setInstalledVersion(String installedVersion) {
    myInstalledVersion = installedVersion;
  }

  public String getInstalledVersion() {
    return myInstalledVersion;
  }

  public void setRating(String rating) {
    myRating = rating;
  }

  public String getRating() {
    return myRating;
  }
}
