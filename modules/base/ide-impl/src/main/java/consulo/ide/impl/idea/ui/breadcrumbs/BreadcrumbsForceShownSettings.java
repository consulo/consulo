// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.breadcrumbs;

import consulo.codeEditor.Editor;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class BreadcrumbsForceShownSettings {
  private BreadcrumbsForceShownSettings() {
  }

  private static final Key<Boolean> FORCED_BREADCRUMBS = new Key<>("FORCED_BREADCRUMBS");

  public static boolean setForcedShown(@Nullable Boolean selected, @Nonnull Editor editor) {
    Boolean old = getForcedShown(editor);
    editor.putUserData(FORCED_BREADCRUMBS, selected);
    return !Objects.equals(old, selected);
  }

  @Nullable
  public static Boolean getForcedShown(@Nonnull Editor editor) {
    return editor.getUserData(FORCED_BREADCRUMBS);
  }
}
