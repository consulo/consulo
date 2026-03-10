// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.issue;

import consulo.build.ui.issue.BuildIssue;
import consulo.build.ui.issue.BuildIssueProvider;
import consulo.build.ui.issue.BuildIssueQuickFix;
import consulo.externalSystem.rt.model.ExternalSystemException;
import jakarta.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
public class BuildIssueException extends ExternalSystemException implements BuildIssueProvider {
    private final BuildIssue myBuildIssue;

    public BuildIssueException(@Nonnull BuildIssue issue) {
        super(issue.getDescription(), getQuickfixIds(issue));
        myBuildIssue = issue;
    }

    @Override
    public @Nonnull BuildIssue getBuildIssue() {
        return myBuildIssue;
    }

    private static String[] getQuickfixIds(@Nonnull BuildIssue issue) {
        return issue.getQuickFixes().stream().map(BuildIssueQuickFix::getId).toArray(String[]::new);
    }
}
