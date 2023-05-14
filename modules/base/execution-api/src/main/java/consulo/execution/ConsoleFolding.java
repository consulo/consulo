// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ConsoleFolding {
  public static final ExtensionPointName<ConsoleFolding> EP_NAME = ExtensionPointName.create(ConsoleFolding.class);

  /**
   * @param project current project
   * @param line    line to check whether it should be folded or not
   * @return {@code true} if line should be folded, {@code false} if not
   */
  public boolean shouldFoldLine(@Nonnull Project project, @Nonnull String line) {
    return false;
  }

  /**
   * Return true if folded lines should not have dedicated line and should be attached to
   * the end of the line above instead
   */
  public boolean shouldBeAttachedToThePreviousLine() {
    return true;
  }

  /**
   * @param project current project
   * @param lines   lines to be folded
   * @return placeholder for lines or {@code null} if these lines should not be folded
   */
  @Nullable
  public String getPlaceholderText(@Nonnull Project project, @Nonnull List<String> lines) {
    return null;
  }
}
