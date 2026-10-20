// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.localize.VcsLocalize;

public enum ManageIgnoredOption {
    AlwaysAsk(VcsLocalize.ignoredFileManageAlwaysAskOption()),
    AllProjectsManage(VcsLocalize.ignoredFileManageAllProjectsOption()),
    CurrentProjectManage(VcsLocalize.ignoredFileManageThisProjectOption()),
    DoNotManageForCurrentProject(VcsLocalize.ignoredFileNotManageThisProjectOption()),
    DoNotManageForAllProject(VcsLocalize.ignoredFileNotManageOption());

    private final LocalizeValue myDisplayName;

    ManageIgnoredOption(LocalizeValue displayName) {
        myDisplayName = displayName;
    }

    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }
}
