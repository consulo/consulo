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
package consulo.usage;

import consulo.application.Application;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
public interface UsagePreviewPanelFactory {
  static UsagePreviewPanelFactory getInstance() {
    return Application.get().getInstance(UsagePreviewPanelFactory.class);
  }

  @Nonnull
  default UsagePreviewPanel createPreviewPanel(@Nonnull Project project, @Nonnull UsageViewPresentation presentation) {
    return createPreviewPanel(project, presentation, false);
  }

  @Nonnull
  UsagePreviewPanel createPreviewPanel(@Nonnull Project project, @Nonnull UsageViewPresentation presentation, boolean isEditor);
}
