// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build.events;

import consulo.ide.impl.idea.build.issue.BuildIssue;

import javax.annotation.Nonnull;

/**
 * @author Vladislav.Soroka
 */
//@ApiStatus.Experimental
public interface BuildIssueEvent extends MessageEvent {
  @Nonnull
  BuildIssue getIssue();
}
