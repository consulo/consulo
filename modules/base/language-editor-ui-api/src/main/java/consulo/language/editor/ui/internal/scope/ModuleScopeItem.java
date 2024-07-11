// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.internal.scope;

import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.scope.AnalysisScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import jakarta.annotation.Nullable;

public final class ModuleScopeItem implements ModelScopeItem {
  public final Module Module;

  @Nullable
  public static ModelScopeItem tryCreate(@Nullable Module module) {
    if (module != null) {
      Project project = module.getProject();
      if (ModuleManager.getInstance(project).getModules().length > 1)
        return new ModuleScopeItem(module);
    }
    return null;
  }

  public ModuleScopeItem(Module module) {
    Module = module;
  }

  @Override
  public AnalysisScope getScope() {
    return new AnalysisScope(Module);
  }
}