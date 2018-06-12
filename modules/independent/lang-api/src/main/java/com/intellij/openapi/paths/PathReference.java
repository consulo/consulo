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

package com.intellij.openapi.paths;

import consulo.awt.TargetAWT;
import consulo.ide.IconDescriptorUpdaters;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.NullableConstantFunction;
import com.intellij.util.NullableFunction;
import javax.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class PathReference {
  public static final NullableFunction<PathReference, Icon> NULL_ICON = new NullableConstantFunction<PathReference, Icon>((Icon)null);

  private final String myPath;
  private final NullableLazyValue<Icon> myIcon;

  public PathReference(@Nonnull String path, final @Nonnull Function<PathReference, Icon> icon) {
    myPath = path;
    myIcon = new NullableLazyValue<Icon>() {
      @Override
      protected Icon compute() {
        return icon.fun(PathReference.this);
      }
    };
  }

  @Nonnull
  public String getPath() {
    return myPath;
  }

  @Nonnull
  public String getTrimmedPath() {
    return trimPath(myPath);
  }

  @javax.annotation.Nullable
  public Icon getIcon() {
    return myIcon.getValue();
  }

  @javax.annotation.Nullable
  public PsiElement resolve() {
    return null;
  }

  public static String trimPath(final String url) {
    for (int i = 0; i < url.length(); i++) {
      switch (url.charAt(i)) {
        case '?':
        case '#':
          return url.substring(0, i);
      }
    }
    return url;
  }

  public static class ResolveFunction implements NullableFunction<PathReference, Icon> {
    public static final ResolveFunction NULL_RESOLVE_FUNCTION = new ResolveFunction(null);
    private final Icon myDefaultIcon;

    public ResolveFunction(@javax.annotation.Nullable final Icon defaultValue) {
      myDefaultIcon = defaultValue;
    }

    @Override
    public Icon fun(final PathReference pathReference) {
      final PsiElement element = pathReference.resolve();
      return element == null ? myDefaultIcon : TargetAWT.to(IconDescriptorUpdaters.getIcon(element, Iconable.ICON_FLAG_READ_STATUS));
    }
  }
}
