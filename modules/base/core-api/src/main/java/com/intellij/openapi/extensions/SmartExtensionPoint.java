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
package com.intellij.openapi.extensions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peter
 */
public abstract class SmartExtensionPoint<Extension, V> {
  private final Collection<V> myExplicitExtensions;
  private ExtensionPoint<Extension> myExtensionPoint;
  private List<V> myCache;

  protected SmartExtensionPoint(@Nonnull final Collection<V> explicitExtensions) {
    myExplicitExtensions = explicitExtensions;
  }

  @Nonnull
  protected abstract ExtensionPoint<Extension> getExtensionPoint();

  public final void addExplicitExtension(@Nonnull V extension) {
    synchronized (myExplicitExtensions) {
      myExplicitExtensions.add(extension);
      myCache = null;
    }
  }

  public final void removeExplicitExtension(@Nonnull V extension) {
    synchronized (myExplicitExtensions) {
      myExplicitExtensions.remove(extension);
      myCache = null;
    }
  }

  @Nullable
  protected abstract V getExtension(@Nonnull final Extension extension);

  @Nonnull
  public final List<V> getExtensions() {
    synchronized (myExplicitExtensions) {
      if (myCache == null) {
        myExtensionPoint = getExtensionPoint();
        List<V> list = new ArrayList<>();
        list.addAll(myExtensionPoint.getExtensionList().stream().map(this::getExtension).collect(Collectors.toList()));
        list.addAll(myExplicitExtensions);

        myCache = list;
        return list;
      }
      return myCache;
    }
  }
}
