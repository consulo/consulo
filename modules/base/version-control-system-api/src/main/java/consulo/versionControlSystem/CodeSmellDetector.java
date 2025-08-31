/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ProcessCanceledException;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author yole
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class CodeSmellDetector {
  public static CodeSmellDetector getInstance(Project project) {
    return project.getInstance(CodeSmellDetector.class);
  }

  /**
   * Performs pre-checkin code analysis on the specified files.
   *
   * @param files the files to analyze.
   * @return the list of problems found during the analysis.
   * @throws ProcessCanceledException if the analysis was cancelled by the user.
   */
  @Nonnull
  public abstract List<CodeSmellInfo> findCodeSmells(@Nonnull List<VirtualFile> files) throws ProcessCanceledException;

  /**
   * Shows the specified list of problems found during pre-checkin code analysis in a Messages pane.
   *
   * @param smells the problems to show.
   */
  public abstract void showCodeSmellErrors(@Nonnull List<CodeSmellInfo> smells);

}