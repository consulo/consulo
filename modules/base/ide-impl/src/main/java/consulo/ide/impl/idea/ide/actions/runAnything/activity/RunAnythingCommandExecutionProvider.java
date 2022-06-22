// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@ExtensionImpl(order = "last")
public class RunAnythingCommandExecutionProvider extends RunAnythingCommandProvider {

  @Nullable
  @Override
  public String findMatchingValue(@Nonnull DataContext dataContext, @Nonnull String pattern) {
    return pattern;
  }

  @Nullable
  @Override
  public String getHelpGroupTitle() {
    return null;
  }
}