// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

/**
 * Project Service
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface BuildWorkspaceConfiguration {
  boolean isShowFirstErrorInEditor();
}
