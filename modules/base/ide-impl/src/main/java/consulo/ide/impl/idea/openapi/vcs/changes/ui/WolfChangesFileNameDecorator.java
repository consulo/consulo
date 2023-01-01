/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.editor.wolfAnalyzer.WolfTheProblemSolver;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.inject.Inject;

import jakarta.inject.Singleton;
import java.awt.*;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class WolfChangesFileNameDecorator extends ChangesFileNameDecorator {
  private final WolfTheProblemSolver myProblemSolver;

  @Inject
  public WolfChangesFileNameDecorator(final WolfTheProblemSolver problemSolver) {
    myProblemSolver = problemSolver;
  }

  @Override
  public void appendFileName(final ChangesBrowserNodeRenderer renderer, final VirtualFile vFile, final String fileName, final Color color, final boolean highlightProblems) {
    int style = SimpleTextAttributes.STYLE_PLAIN;
    Color underlineColor = null;
    if (highlightProblems && vFile != null && !vFile.isDirectory() && myProblemSolver.isProblemFile(vFile)) {
      underlineColor = JBColor.RED;
      style = SimpleTextAttributes.STYLE_WAVED;
    }
    renderer.append(fileName, new SimpleTextAttributes(style, color, underlineColor));
  }
}
