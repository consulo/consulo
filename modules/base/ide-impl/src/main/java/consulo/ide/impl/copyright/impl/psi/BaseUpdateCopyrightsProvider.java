/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.copyright.impl.psi;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.ide.impl.copyright.impl.ui.TemplateCommentPanel;
import javax.annotation.Nonnull;
import consulo.ide.impl.copyright.impl.config.CopyrightFileConfig;

/**
 * @author VISTALL
 * @since 13.02.15
 */
public abstract class BaseUpdateCopyrightsProvider extends UpdateCopyrightsProvider<CopyrightFileConfig> {
  @Nonnull
  @Override
  public CopyrightFileConfig createDefaultOptions() {
    return new CopyrightFileConfig();
  }

  @Nonnull
  @Override
  public TemplateCommentPanel createConfigurable(@Nonnull Project project, @Nonnull TemplateCommentPanel parentPane, @Nonnull FileType fileType) {
    return new TemplateCommentPanel(fileType, parentPane, project);
  }
}
