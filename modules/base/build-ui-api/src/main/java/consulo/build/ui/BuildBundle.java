// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.build.ui;

import consulo.component.util.localize.AbstractBundle;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;

public final class BuildBundle extends AbstractBundle {
  private static final String BUNDLE = "consulo.build.ui.BuildBundle";
  private static final BuildBundle INSTANCE = new BuildBundle();

  private BuildBundle() {
    super(BUNDLE);
  }

  @Nonnull
  public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
    return INSTANCE.getMessage(key, params);
  }
}