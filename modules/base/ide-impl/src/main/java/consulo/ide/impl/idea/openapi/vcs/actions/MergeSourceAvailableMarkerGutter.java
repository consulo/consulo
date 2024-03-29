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
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.ide.impl.idea.openapi.vcs.annotate.TextAnnotationPresentation;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.ui.color.ColorValue;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Konstantin Bulenkov
 */
class MergeSourceAvailableMarkerGutter extends AnnotationFieldGutter implements Consumer<AnnotationSource> {
  // merge source showing is turned on
  private boolean myTurnedOn;

  MergeSourceAvailableMarkerGutter(FileAnnotation annotation,
                                   TextAnnotationPresentation highlighting,
                                   Couple<Map<VcsRevisionNumber, ColorValue>> colorScheme) {
    super(annotation, highlighting, colorScheme);
  }

  @Override
  public EditorColorKey getColor(int line, Editor editor) {
    return AnnotationSource.LOCAL.getColor();
  }

  @Override
  public String getLineText(int line, Editor editor) {
    if (myTurnedOn) return "";
    final AnnotationSourceSwitcher switcher = myAnnotation.getAnnotationSourceSwitcher();
    if (switcher == null) return "";
    return switcher.mergeSourceAvailable(line) ? "M" : "";
  }

  @Override
  public void accept(final AnnotationSource annotationSource) {
    myTurnedOn = annotationSource.showMerged();
  }
}
