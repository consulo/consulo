// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.build.ui.impl.internal.event;

import consulo.build.ui.BuildNotificationsGroups;
import consulo.build.ui.event.BuildIssueEvent;
import consulo.build.ui.event.MessageEventResult;
import consulo.build.ui.issue.BuildIssue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;

import org.jspecify.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class BuildIssueEventImpl extends AbstractBuildEvent implements BuildIssueEvent {
  private final BuildIssue myIssue;
  private final Kind myKind;

  public BuildIssueEventImpl(Object parentId, BuildIssue buildIssue, Kind kind) {
    super(new Object(), parentId, System.currentTimeMillis(), buildIssue.getTitle());
    myIssue = buildIssue;
    myKind = kind;
  }

  
  @Override
  public final String getDescription() {
    return myIssue.getDescription();
  }

  
  @Override
  public BuildIssue getIssue() {
    return myIssue;
  }

  
  @Override
  public Kind getKind() {
    return myKind;
  }

  
  @Override
  public NotificationGroup getGroup() {
    return BuildNotificationsGroups.BUILD_ISSUES;
  }

  @Nullable
  @Override
  public Navigatable getNavigatable(Project project) {
    return myIssue.getNavigatable(project);
  }

  @Override
  public MessageEventResult getResult() {
    return new MessageEventResult() {
      @Override
      public Kind getKind() {
        return myKind;
      }

      
      @Override
      public String getDetails() {
        return myIssue.getDescription();
      }
    };
  }
}
