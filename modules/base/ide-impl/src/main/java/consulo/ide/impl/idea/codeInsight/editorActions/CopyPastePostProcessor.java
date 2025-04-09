/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;

import java.awt.datatransfer.Transferable;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CopyPastePostProcessor<T extends TextBlockTransferableData> {
  public static final ExtensionPointName<CopyPastePostProcessor> EP_NAME = ExtensionPointName.create(CopyPastePostProcessor.class);

  @Nonnull
  public abstract List<T> collectTransferableData(final PsiFile file, final Editor editor, final int[] startOffsets, final int[] endOffsets);

  @Nonnull
  public List<T> extractTransferableData(final Transferable content) {
    return Collections.emptyList();
  }

  public void processTransferableData(final Project project, final Editor editor, final RangeMarker bounds, int caretOffset,
                                      SimpleReference<Boolean> indented, final List<T> values) {
  }
}
