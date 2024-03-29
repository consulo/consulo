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

package consulo.language.editor.completion.lookup;

import consulo.component.util.Iconable;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * Use {@link LookupElementBuilder}
 */
@Deprecated(forRemoval = true)
public class LookupValueFactory {
  
  private LookupValueFactory() {
  }

  @Nonnull
  public static Object createLookupValue(@Nonnull String name, @Nullable Image icon) {
    return icon == null ? name : new LookupValueWithIcon(name, icon);
  }

  @Nonnull
  public static Object createLookupValueWithHint(@Nonnull String name, @Nullable Image icon, String hint) {
    return new LookupValueWithIconAndHint(name, icon, hint);
  }

  public static class LookupValueWithIcon implements PresentableLookupValue, Iconable {
    private final String myName;
    private final Image myIcon;

    protected LookupValueWithIcon(@Nonnull String name, @Nullable Image icon) {
      myName = name;
      myIcon = icon;
    }
    @Override
    public String getPresentation() {
      return myName;
    }

    @Override
    public Image getIcon(int flags) {
      return myIcon;
    }

    @Override
    public int hashCode() {
      return getPresentation().hashCode();
    }

    public boolean equals(Object a) {
      return a.getClass() == getClass() && a instanceof PresentableLookupValue && ((PresentableLookupValue)a).getPresentation().equals(getPresentation());
    }
  }

  public static class LookupValueWithIconAndHint extends LookupValueWithIcon implements LookupValueWithUIHint {

    private final String myHint;

    protected LookupValueWithIconAndHint(final String name, final Image icon, String hint) {
      super(name, icon);
      myHint = hint;
    }

    @Override
    public String getTypeHint() {
      return myHint;
    }
  }
}
