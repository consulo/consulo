// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.build.events.impl;

import consulo.ide.impl.idea.build.BuildBundle;
import consulo.ide.impl.idea.build.events.BuildIssueEvent;
import consulo.ide.impl.idea.build.events.MessageEventResult;
import consulo.ide.impl.idea.build.issue.BuildIssue;
import consulo.project.Project;
import consulo.navigation.Navigatable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 */
//@ApiStatus.Experimental
public class BuildIssueEventImpl extends AbstractBuildEvent implements BuildIssueEvent {
  private final BuildIssue myIssue;
  private final Kind myKind;

  public BuildIssueEventImpl(@Nonnull Object parentId, @Nonnull BuildIssue buildIssue, @Nonnull Kind kind) {
    super(new Object(), parentId, System.currentTimeMillis(), buildIssue.getTitle());
    myIssue = buildIssue;
    myKind = kind;
  }

  @Nonnull
  @Override
  public final String getDescription() {
    return myIssue.getDescription();
  }

  @Nonnull
  @Override
  public BuildIssue getIssue() {
    return myIssue;
  }

  @Nonnull
  @Override
  public Kind getKind() {
    return myKind;
  }

  @Nonnull
  @Override
  public String getGroup() {
    return BuildBundle.message("build.event.title.build.issues");
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(@Nonnull Project project) {
    return myIssue.getNavigatable(project);
  }

  @Override
  public MessageEventResult getResult() {
    return new MessageEventResult() {
      @Override
      public Kind getKind() {
        return myKind;
      }

      @Nonnull
      @Override
      public String getDetails() {
        return myIssue.getDescription();
      }
    };
  }
}
