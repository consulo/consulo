// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard;

import consulo.project.Project;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class RunDashboardComponentWrapper extends NonOpaquePanel {
  public static final Key<Integer> CONTENT_ID_KEY = Key.create("RunDashboardContentId");
  private final Project myProject;

  private Integer myContentId;

  public RunDashboardComponentWrapper(@Nonnull Project project) {
    myProject = project;
  }

  public @Nullable Integer getContentId() {
    return myContentId;
  }

  public void setContentId(@Nullable Integer contentId) {
    myContentId = contentId;
  }

  public @Nonnull Project getProject() {
    return myProject;
  }
}
