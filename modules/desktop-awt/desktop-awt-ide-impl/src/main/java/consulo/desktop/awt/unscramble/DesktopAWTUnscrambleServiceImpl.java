/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.awt.unscramble;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.unscramble.StacktraceAnalyzer;
import consulo.execution.unscramble.UnscrambleService;
import consulo.ide.impl.idea.unscramble.UnscrambleDialog;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 04/02/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTUnscrambleServiceImpl implements UnscrambleService {
  private final Project myProject;

  @Inject
  public DesktopAWTUnscrambleServiceImpl(Project project) {
    myProject = project;
  }

  @RequiredUIAccess
  @Override
  public void showAsync(@Nullable String stackTrace, @Nullable StacktraceAnalyzer stacktraceAnalyzer) {
    UnscrambleDialog dialog = new UnscrambleDialog(myProject, stacktraceAnalyzer);

    if (stackTrace != null) {
      dialog.setText(stackTrace);
    }

    dialog.showAsync();
  }
}
