// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.webcore.packaging;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Objects;


public class InstalledPackage {
  @Nonnull
  private final String myName;
  @Nullable
  private final String myVersion;

  public InstalledPackage(@Nonnull String name, @Nullable String version) {
    myName = name;
    myVersion = version;
  }

  @Nonnull
  public String getName() {
    return myName;
  }

  @Nullable
  public String getVersion() {
    return myVersion;
  }

  @Nullable
  public String getTooltipText() {
    return null;
  }

  @Override
  public
  @Nonnull
  String toString() {
    return getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    InstalledPackage aPackage = (InstalledPackage)o;
    return myName.equals(aPackage.myName) && Objects.equals(myVersion, aPackage.myVersion);
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + (myVersion != null ? myVersion.hashCode() : 0);
    return result;
  }
}
