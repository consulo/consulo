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
package consulo.desktop.awt.execution.console.language;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.ui.console.language.LanguageConsoleBuilder;
import consulo.execution.ui.console.language.LanguageConsoleBuilderFactory;
import consulo.language.Language;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 05/06/2023
 */
@Singleton
@ServiceImpl
public class DesktopAWTLanguageConsoleBuilderFactory implements LanguageConsoleBuilderFactory {
  private final Project myProject;

  @Inject
  public DesktopAWTLanguageConsoleBuilderFactory(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public LanguageConsoleBuilder newBuilder(@Nonnull Language language) {
    return new DesktopLanguageConsoleBuilder(myProject, language);
  }
}
