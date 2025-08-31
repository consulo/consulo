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

package consulo.ide.impl.idea.codeInsight.highlighting;

import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.lang.StringUtil;

/**
 * @author msk
 */
public abstract class HighlightHandlerBase {
  static void setupFindModel(Project project) {
    FindManager findManager = FindManager.getInstance(project);
    FindModel model = findManager.getFindNextModel();
    if (model == null) {
      model = findManager.getFindInFileModel();
    }
    model.setSearchHighlighters(true);
    findManager.setFindWasPerformed();
    findManager.setFindNextModel(model);
  }

  public static String getLineTextErrorStripeTooltip(Document document, int offset, boolean escape) {
    int lineNumber = document.getLineNumber(offset);
    int lineStartOffset = document.getLineStartOffset(lineNumber);
    int lineEndOffset = document.getLineEndOffset(lineNumber);
    int lineFragmentEndOffset = Math.min(lineStartOffset + 140, lineEndOffset);
    String lineText = document.getText().substring(lineStartOffset, lineFragmentEndOffset);
    if (lineFragmentEndOffset != lineEndOffset) {
      lineText = lineText.trim() + "...";
    }
    return "  " + (escape ? StringUtil.escapeXml(lineText.trim()) : lineText.trim()) + "  ";
  }
}
