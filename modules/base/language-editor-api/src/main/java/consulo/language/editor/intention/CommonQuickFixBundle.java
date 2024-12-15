// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.intention;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.language.editor.localize.CommonQuickFixLocalize;
import org.jetbrains.annotations.PropertyKey;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

@Deprecated
@DeprecationInfo("Use CommonQuickFixLocalize")
@MigratedExtensionsTo(CommonQuickFixLocalize.class)
public final class CommonQuickFixBundle extends AbstractBundle {
  public static final String BUNDLE = "messages.CommonQuickFixBundle";
  private static final CommonQuickFixBundle INSTANCE = new CommonQuickFixBundle();

  private CommonQuickFixBundle() {
    super(BUNDLE);
  }

  @Nonnull
  public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
    return INSTANCE.getMessage(key, params);
  }

  @Nonnull
  public static Supplier<String> messagePointer(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull Object... params) {
    return () -> INSTANCE.getMessage(key, params);
  }
}