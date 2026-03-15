/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.externalSystem.importing;

import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.task.ProgressExecutionMode;
import consulo.project.Project;

/**
 * @author Vladislav.Soroka
 * @since 2014-05-29
 */
public class ImportSpec {
  
  private final Project myProject;
  
  private final ProjectSystemId myExternalSystemId;
  
  private ProgressExecutionMode myProgressExecutionMode;
  private boolean forceWhenUptodate;
  private boolean whenAutoImportEnabled;
  //private boolean isPreviewMode;
  //private boolean isReportRefreshError;

  public ImportSpec(Project project, ProjectSystemId id) {
    myProject = project;
    myExternalSystemId = id;
    myProgressExecutionMode = ProgressExecutionMode.MODAL_SYNC;
  }

  
  public Project getProject() {
    return myProject;
  }

  
  public ProjectSystemId getExternalSystemId() {
    return myExternalSystemId;
  }

  
  public ProgressExecutionMode getProgressExecutionMode() {
    return myProgressExecutionMode;
  }

  public void setProgressExecutionMode(ProgressExecutionMode progressExecutionMode) {
    myProgressExecutionMode = progressExecutionMode;
  }

  public boolean isForceWhenUptodate() {
    return forceWhenUptodate;
  }

  public void setForceWhenUptodate(boolean forceWhenUptodate) {
    this.forceWhenUptodate = forceWhenUptodate;
  }

  public boolean isWhenAutoImportEnabled() {
    return whenAutoImportEnabled;
  }

  public void setWhenAutoImportEnabled(boolean whenAutoImportEnabled) {
    this.whenAutoImportEnabled = whenAutoImportEnabled;
  }

  //public boolean isPreviewMode() {
  //  return isPreviewMode;
  //}
  //
  //public void setPreviewMode(boolean isPreviewMode) {
  //  this.isPreviewMode = isPreviewMode;
  //}
  //
  //public boolean isReportRefreshError() {
  //  return isReportRefreshError;
  //}
  //
  //public void setReportRefreshError(boolean isReportRefreshError) {
  //  this.isReportRefreshError = isReportRefreshError;
  //}
}
