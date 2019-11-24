/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.navigation;

import com.intellij.pom.Navigatable;
import com.intellij.util.ArrayFactory;
import consulo.annotation.DeprecationInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface NavigationItem extends Navigatable {
  public static final NavigationItem[] EMPTY_ARRAY = new NavigationItem[0];

  public static ArrayFactory<NavigationItem> ARRAY_FACTORY = new ArrayFactory<NavigationItem>() {
    @Nonnull
    @Override
    public NavigationItem[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new NavigationItem[count];
    }
  };

  @Deprecated
  @DeprecationInfo("Use NavigationItem#EMPTY_ARRAY")
  NavigationItem[] EMPTY_NAVIGATION_ITEM_ARRAY = new NavigationItem[0];

  @Nullable
  String getName();

  @Nullable
  ItemPresentation getPresentation();
}