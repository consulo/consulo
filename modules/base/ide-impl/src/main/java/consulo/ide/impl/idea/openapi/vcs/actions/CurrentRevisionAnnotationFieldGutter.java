/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.LineAnnotationAspect;
import consulo.ide.impl.idea.openapi.vcs.annotate.TextAnnotationPresentation;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ui.color.ColorValue;

import java.util.Map;
import java.util.function.Consumer;

/**
 * shown additionally only when merge
 *
 * @author Konstantin Bulenkov
 */
class CurrentRevisionAnnotationFieldGutter extends AspectAnnotationFieldGutter implements Consumer<AnnotationSource> {
  // merge source showing is turned on
  private boolean myTurnedOn;

  CurrentRevisionAnnotationFieldGutter(FileAnnotation annotation,
                                       LineAnnotationAspect aspect,
                                       TextAnnotationPresentation highlighting,
                                       Couple<Map<VcsRevisionNumber, ColorValue>> colorScheme) {
    super(annotation, aspect, highlighting, colorScheme);
  }

  @Override
  public EditorColorKey getColor(int line, Editor editor) {
    return AnnotationSource.LOCAL.getColor();
  }

  @Override
  public String getLineText(int line, Editor editor) {
    final String value = myAspect.getValue(line);
    if (String.valueOf(myAnnotation.getLineRevisionNumber(line)).equals(value)) {
      return "";
    }
    // shown in merge sources mode
    return myTurnedOn ? value : "";
  }

  @Override
  public String getToolTip(int line, Editor editor) {
    final String aspectTooltip = myAspect.getTooltipText(line);
    if (aspectTooltip != null) {
      return aspectTooltip;
    }
    final String text = getLineText(line, editor);
    return ((text == null) || (text.length() == 0)) ? "" : VcsBundle.message("annotation.original.revision.text", text);
  }

  @Override
  public void accept(final AnnotationSource annotationSource) {
    myTurnedOn = annotationSource.showMerged();
  }
}
