// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.startup.StartupActivity;

@ExtensionImpl(id = "DumbModeWhileScanningSubscriber")
public final class DumbModeWhileScanningSubscriber implements StartupActivity.RequiredForSmartMode {
    @Override
    public void runActivity(Project project) {
        DumbModeWhileScanningTrigger.getInstance(project).subscribe();
    }
}
