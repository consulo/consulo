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
package com.intellij.psi.search.scope.packageSet;

import com.intellij.icons.AllIcons;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedScope {
  private final String myScopeId;
  @Nonnull
  private final LocalizeValue myPresentableName;
  @Nonnull
  private final Image myIcon;
  private final PackageSet myValue;

  @Deprecated
  public NamedScope(@Nonnull String scopeId, @Nonnull Image icon, @Nullable PackageSet value) {
    this(scopeId, LocalizeValue.of(scopeId), icon, value);
  }

  @Deprecated
  public NamedScope(@Nonnull String scopeId, @Nullable PackageSet value) {
    this(scopeId, LocalizeValue.of(scopeId), AllIcons.Ide.LocalScope, value);
  }

  public NamedScope(@Nonnull String scopeId, @Nonnull LocalizeValue presentableName, @Nonnull Image icon, @Nullable PackageSet value) {
    myScopeId = scopeId;
    myIcon = icon;
    myValue = value;
    myPresentableName = presentableName;
  }

  public NamedScope(@Nonnull String scopeId, @Nonnull LocalizeValue presentableName, @Nullable PackageSet value) {
    this(scopeId, presentableName, AllIcons.Ide.LocalScope, value);
  }

  @Nonnull
  public Image getIcon() {
    return myIcon;
  }

  @Nonnull
  public Image getIconForProjectView() {
    return myIcon;
  }

  @Nonnull
  protected Image createOffsetIcon() {
    return ImageEffects.appendRight(Image.empty(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE), myIcon);
  }

  @Nonnull
  public String getScopeId() {
    return myScopeId;
  }

  @Nonnull
  public LocalizeValue getPresentableName() {
    return myPresentableName;
  }

  /**
   * @deprecated please use {@link NamedScope#getScopeId()} for search/serialization/mappings and {@link #getPresentableName()} to display in UI
   */
  @Nonnull
  public String getName() {
    return myScopeId;
  }

  @Nullable
  public PackageSet getValue() {
    return myValue;
  }

  public NamedScope createCopy() {
    return new NamedScope(myScopeId, myPresentableName, myIcon, myValue != null ? myValue.createCopy() : null);
  }

  @Nullable
  public String getDefaultColorName() {
    return null;
  }

  public static class UnnamedScope extends NamedScope {
    public UnnamedScope(@Nonnull PackageSet value) {
      super(value.getText(), LocalizeValue.of(value.getText()), value);
    }
  }

  @Override
  public String toString() {
    return "Scope '" + myScopeId + "'; set:" + (myValue == null ? null : myValue.getText());
  }
}