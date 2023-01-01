/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.colorScheme.EditorColorsManager;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.ui.SimpleEditorCustomization;

import javax.annotation.Nonnull;

public class RightMarginEditorCustomization extends SimpleEditorCustomization {

  private int myRightMarginColumns;

  public RightMarginEditorCustomization(boolean enabled, int rightMarginColumns) {
    super(enabled);
    myRightMarginColumns = rightMarginColumns;
  }

  public int getRightMarginColumns() {
    return myRightMarginColumns;
  }

  @Override
  public void customize(@Nonnull EditorEx editor) {
    if (isEnabled()) {
      editor.getSettings().setRightMarginShown(true);
      editor.getSettings().setRightMargin(getRightMarginColumns());
      // ensure we've got a monospace font by loading up the global editor scheme
      editor.setColorsScheme(EditorColorsManager.getInstance().getGlobalScheme());
    } else {
      editor.getSettings().setRightMarginShown(false);
    }
  }
}
