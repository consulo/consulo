// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Shares a project on a hosting service. The commit result offers it for a fresh repository without a remote.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ShareProjectActionProvider {
    LocalizeValue getHostServiceName();

    AnAction getAction();

    boolean isApplicableForRoot(Project project, VirtualFile root);
}
