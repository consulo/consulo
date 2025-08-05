/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.ide.impl.idea.openapi.editor.ex.ErrorStripeAdapter;
import consulo.codeEditor.internal.ErrorStripeEvent;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public class ErrorStripeHandler extends ErrorStripeAdapter {
  private final Project myProject;

  public ErrorStripeHandler(Project project) {
    myProject = project;
  }

  @Override
  public void errorMarkerClicked(@Nonnull ErrorStripeEvent e) {
    RangeHighlighter highlighter = e.getHighlighter();
    if (!highlighter.isValid()) return;
    HighlightInfoImpl info = findInfo(highlighter);
    if (info != null) {
      GotoNextErrorHandler.navigateToError(myProject, e.getEditor(), info);
    }
  }

  private static HighlightInfoImpl findInfo(final RangeHighlighter highlighter) {
    Object o = highlighter.getErrorStripeTooltip();
    if (o instanceof HighlightInfoImpl) return (HighlightInfoImpl)o;
    return null;
  }
}
