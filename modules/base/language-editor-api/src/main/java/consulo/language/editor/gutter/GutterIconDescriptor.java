/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.editor.gutter;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Allows user to configure visible gutter icons.
 *
 * @author Dmitry Avdeev
 * @since 144
 */
public abstract class GutterIconDescriptor {

  protected static final Option[] NO_OPTIONS = new Option[0];

  /**
   * Human-readable provider name for UI.
   *
   * @return {@link LocalizeValue#empty()} if no configuration needed
   */
  @Nonnull
  public abstract LocalizeValue getName();

  @Nullable
  public Image getIcon() {
    return null;
  }

  public boolean isEnabledByDefault() {
    return true;
  }

  public String getId() {
    return getClass().getName();
  }

  public Option[] getOptions() {
    return NO_OPTIONS;
  }

  public static class Option extends GutterIconDescriptor {
    private final String myId;
    private final LocalizeValue myName;
    private final Image myIcon;

    public Option(@Nonnull String id, @Nonnull LocalizeValue name, Image icon) {
      myId = id;
      myName = name;
      myIcon = icon;
    }

    public boolean isEnabled() {
      return LineMarkerSettings.getInstance().isEnabled(this);
    }

    @Nullable
    @Override
    public Image getIcon() {
      return myIcon;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
      return myName;
    }

    @Override
    public String getId() {
      return myId;
    }
  }
}
