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

package consulo.language.psi.path;

import consulo.component.util.Iconable;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Dmitry Avdeev
 */
public class PathReference {
  public static final Function<PathReference, Image> NULL_ICON = (p) -> null;

  private final String myPath;
  private final Supplier<Image> myIcon;

  public PathReference(@Nonnull String path, final @Nonnull Function<PathReference, Image> icon) {
    myPath = path;
    myIcon = LazyValue.nullable(() -> icon.apply(PathReference.this));
  }

  @Nonnull
  public String getPath() {
    return myPath;
  }

  @Nonnull
  public String getTrimmedPath() {
    return trimPath(myPath);
  }

  @Nullable
  public Image getIcon() {
    return myIcon.get();
  }

  @Nullable
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

  public static class ResolveFunction implements Function<PathReference, Image> {
    public static final ResolveFunction NULL_RESOLVE_FUNCTION = new ResolveFunction(null);
    private final Image myDefaultIcon;

    public ResolveFunction(@Nullable final Image defaultValue) {
      myDefaultIcon = defaultValue;
    }

    @Override
    public Image apply(final PathReference pathReference) {
      final PsiElement element = pathReference.resolve();
      return element == null ? myDefaultIcon : IconDescriptorUpdaters.getIcon(element, Iconable.ICON_FLAG_READ_STATUS);
    }
  }
}
