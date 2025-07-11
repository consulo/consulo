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
package consulo.language.editor.ui;

import consulo.codeEditor.EditorEx;

import jakarta.annotation.Nonnull;

/**
 * Editor customization that can make target editor soft wraps-aware.
 * <p/>
 * Thread-safe.
 *
 * @author Denis Zhdanov
 * @since 2010-08-20
 */
public class SoftWrapsEditorCustomization extends SimpleEditorCustomization {

  public static final SoftWrapsEditorCustomization ENABLED = new SoftWrapsEditorCustomization(true);
  public static final SoftWrapsEditorCustomization DISABLED = new SoftWrapsEditorCustomization(false);

  private SoftWrapsEditorCustomization(boolean enabled) {
    super(enabled);
  }

  @Override
  public void customize(@Nonnull EditorEx editor) {
    editor.getSettings().setUseSoftWraps(isEnabled());
  }
}
