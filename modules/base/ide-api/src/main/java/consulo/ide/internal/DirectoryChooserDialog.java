/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 10-Jul-22
 */
public interface DirectoryChooserDialog {
  void setTitle(String title);

  @RequiredReadAction
  void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, String postfixToShow);

  @RequiredReadAction
  void fillList(PsiDirectory[] directories, @Nullable PsiDirectory defaultSelection, Project project, Map<PsiDirectory, String> postfixes);

  @RequiredUIAccess
  void show();

  boolean isOK();

  PsiDirectory getSelectedDirectory();
}
