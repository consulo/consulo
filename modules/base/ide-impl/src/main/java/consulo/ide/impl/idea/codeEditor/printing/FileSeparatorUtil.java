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
package consulo.ide.impl.idea.codeEditor.printing;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.document.Document;
import consulo.ide.impl.idea.codeInsight.daemon.impl.LineMarkersPass;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author anna
 * @since 2008-01-25
 */
public class FileSeparatorUtil {
  @Nonnull
  @RequiredReadAction
  public static List<LineMarkerInfo> getFileSeparators(PsiFile file, Document document) {
    List<LineMarkerInfo> result = new ArrayList<>();
    for (LineMarkerInfo lineMarkerInfo : LineMarkersPass.queryLineMarkers(file, document)) {
      if (lineMarkerInfo.separatorColor != null) {
        result.add(lineMarkerInfo);
      }
    }

    result.sort((i1, i2) -> getDisplayLine(i1, document) - getDisplayLine(i2, document));
    return result;
  }

  public static int getDisplayLine(@Nonnull LineMarkerInfo lineMarkerInfo, @Nonnull Document document) {
    int offset = lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? lineMarkerInfo.startOffset : lineMarkerInfo.endOffset;
    return document.getLineNumber(Math.min(document.getTextLength(), Math.max(0, offset))) +
      (lineMarkerInfo.separatorPlacement == SeparatorPlacement.TOP ? 0 : 1);
  }
}