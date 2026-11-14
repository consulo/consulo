// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.compiler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.issue.BuildIssue;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Turns a compiler message into a {@link BuildIssue} (with quick fixes) shown in the Build tool window instead of the plain
 * message. The first contributor returning an issue wins.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface BuildIssueContributor {
    @Nullable
    BuildIssue createBuildIssue(
        Project project,
        Collection<String> moduleNames,
        LocalizeValue title,
        LocalizeValue message,
        MessageEvent.Kind kind,
        @Nullable Path file,
        @Nullable Navigatable navigatable
    );
}
