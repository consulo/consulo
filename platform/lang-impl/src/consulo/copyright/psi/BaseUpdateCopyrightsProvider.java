/*
 * Copyright 2013-2015 must-be.org
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
package consulo.copyright.psi;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.maddyhome.idea.copyright.psi.UpdateCopyrightsProvider;
import com.maddyhome.idea.copyright.ui.TemplateCommentPanel;
import org.jetbrains.annotations.NotNull;
import consulo.copyright.config.CopyrightFileConfig;

/**
 * @author VISTALL
 * @since 13.02.15
 */
public abstract class BaseUpdateCopyrightsProvider extends UpdateCopyrightsProvider<CopyrightFileConfig> {
  @NotNull
  @Override
  public CopyrightFileConfig createDefaultOptions() {
    return new CopyrightFileConfig();
  }

  @NotNull
  @Override
  public TemplateCommentPanel createConfigurable(@NotNull Project project, @NotNull TemplateCommentPanel parentPane, @NotNull FileType fileType) {
    return new TemplateCommentPanel(fileType, parentPane, project);
  }
}
