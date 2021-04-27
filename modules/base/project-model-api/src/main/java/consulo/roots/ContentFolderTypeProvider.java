/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots;

import com.google.common.base.Predicate;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiDirectory;
import consulo.annotation.access.RequiredReadAction;
import consulo.ide.IconDescriptor;
import consulo.psi.PsiPackage;
import consulo.psi.PsiPackageManager;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 22:32/31.10.13
 */
public abstract class ContentFolderTypeProvider {
  public static final ExtensionPointName<ContentFolderTypeProvider> EP_NAME = ExtensionPointName.create("com.intellij.contentFolderTypeProvider");

  private final String myId;

  protected ContentFolderTypeProvider(String id) {
    myId = id;
  }

  @Nonnull
  public String getId() {
    return myId;
  }

  public int getWeight() {
    return Integer.MAX_VALUE;
  }

  /**
   * Return child directory icon
   * If psiDirectory is null it require force package support if this provider is supported it
   * @param psiDirectory child directory
   * @return icon of child directory
   */
  @Nullable
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable PsiDirectory psiDirectory) {
    return getChildDirectoryIcon(psiDirectory, null);
  }

  @Nullable
  @RequiredReadAction
  public final Image getChildDirectoryIcon(@Nullable PsiDirectory psiDirectory, @Nullable PsiPackageManager oldPsiPackageManager) {
    Image packageIcon = getChildPackageIcon();
    if (packageIcon == null) {
      return getChildDirectoryIcon();
    }

    if (psiDirectory != null) {
      PsiPackageManager psiPackageManager = oldPsiPackageManager == null ? PsiPackageManager.getInstance(psiDirectory.getProject()) : oldPsiPackageManager;
      PsiPackage anyPackage = psiPackageManager.findAnyPackage(psiDirectory);
      if (anyPackage != null) {
        return packageIcon;
      }
      else {
        return getChildDirectoryIcon();
      }
    }
    else {
      //
      return packageIcon;
    }
  }

  public Image getChildDirectoryIcon() {
    return AllIcons.Nodes.TreeOpen;
  }

  @Nullable
  public Image getChildPackageIcon() {
    return null;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public Image getIcon(@Nonnull Map<Key, Object> map) {
    if(map.isEmpty()) {
      return getIcon();
    }

    IconDescriptor iconDescriptor = new IconDescriptor(getIcon());
    for (ContentFolderPropertyProvider propertyProvider : ContentFolderPropertyProvider.EP_NAME.getExtensionList()) {
      Object value = propertyProvider.getKey().get(map);
      if(value == null) {
        continue;
      }

      Image layerIcon = propertyProvider.getLayerIcon(value);
      if(layerIcon == null) {
        continue;
      }
      iconDescriptor.addLayerIcon(layerIcon);
    }
    return iconDescriptor.toIcon();
  }
  
  @Nonnull
  public abstract Image getIcon();

  @Nonnull
  public abstract String getName();

  @Nonnull
  public abstract ColorValue getGroupColor();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContentFolderTypeProvider that = (ContentFolderTypeProvider)o;

    if (!myId.equals(that.myId)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }

  @Nonnull
  public static List<ContentFolderTypeProvider> filter(@Nonnull Predicate<ContentFolderTypeProvider> predicate){
    List<ContentFolderTypeProvider> providers = new ArrayList<>();
    for (ContentFolderTypeProvider contentFolderTypeProvider : EP_NAME.getExtensionList()) {
      if(predicate.apply(contentFolderTypeProvider)) {
        providers.add(contentFolderTypeProvider);
      }
    }
    return providers;
  }

  @Nullable
  public static ContentFolderTypeProvider byId(String attributeValue) {
    for (ContentFolderTypeProvider contentFolderTypeProvider : EP_NAME.getExtensionList()) {
      if (Comparing.equal(attributeValue, contentFolderTypeProvider.getId())) {
        return contentFolderTypeProvider;
      }
    }
    return null;
  }
}
