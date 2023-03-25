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
package consulo.language.editor.impl.internal.rawHighlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.rawHighlight.HighlightVisitor;
import consulo.language.editor.rawHighlight.HighlightVisitorFactory;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25/03/2023
 */
@ExtensionImpl(order = "last")
public class DefaultHighlightVisitorFactory implements HighlightVisitorFactory, DumbAware {
  private final Project myProject;

  @Inject
  public DefaultHighlightVisitorFactory(Project project) {
    myProject = project;
  }

  @Override
  public boolean suitableForFile(@Nonnull PsiFile file) {
    return true;
  }

  @Nonnull
  @Override
  public HighlightVisitor createVisitor() {
    return new DefaultHighlightVisitor(myProject);
  }
}
