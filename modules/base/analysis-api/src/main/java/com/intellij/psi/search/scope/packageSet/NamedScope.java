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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NamedScope {
  private final String myName;
  @Nonnull
  private final Image myIcon;
  private final PackageSet myValue;

  public NamedScope(@Nonnull String name, @Nonnull Image icon, @Nullable PackageSet value) {
    myName = name;
    myIcon = icon;
    myValue = value;
  }

  public NamedScope(@Nonnull String name, @Nullable PackageSet value) {
    this(name, AllIcons.Ide.LocalScope, value);
  }

  @Nonnull
  public Image getIcon() {
    return myIcon;
  }

  @Nonnull
  public String getScopeId() {
    return myName;
  }

  @Nonnull
  public LocalizeValue getPresentableName() {
    return LocalizeValue.of(myName);
  }

  /**
   * @deprecated please use {@link NamedScope#getScopeId()} for search/serialization/mappings and {@link #getPresentableName()} to display in UI
   */
  @Nonnull
  public String getName() {
    return myName;
  }

  @Nullable
  public PackageSet getValue() {
    return myValue;
  }

  public NamedScope createCopy() {
    return new NamedScope(myName, myIcon, myValue != null ? myValue.createCopy() : null);
  }

  @Nullable
  public String getDefaultColorName() {
    return null;
  }

  public static class UnnamedScope extends NamedScope {
    public UnnamedScope(@Nonnull PackageSet value) {
      super(value.getText(), value);
    }
  }

  @Override
  public String toString() {
    return "Scope '" + myName + "'; set:" + (myValue == null ? null : myValue.getText());
  }
}