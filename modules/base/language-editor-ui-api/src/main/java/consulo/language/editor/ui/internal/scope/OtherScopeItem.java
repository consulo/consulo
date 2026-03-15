// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.scope.AnalysisScope;
import org.jspecify.annotations.Nullable;

public final class OtherScopeItem implements ModelScopeItem {
  private final AnalysisScope myScope;

  @Nullable
  public static OtherScopeItem tryCreate(AnalysisScope scope) {
    if (scope.getScopeType() != AnalysisScope.PROJECT
      && scope.getScopeType() != AnalysisScope.MODULE
      && scope.getScopeType() != AnalysisScope.UNCOMMITTED_FILES
      && scope.getScopeType() != AnalysisScope.CUSTOM) {
      return new OtherScopeItem(scope);
    }
    return null;
  }

  public OtherScopeItem(AnalysisScope scope) {
    myScope = scope;
  }

  @Override
  public AnalysisScope getScope() {
    return myScope;
  }
}