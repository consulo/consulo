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

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface HighlightRangeExtension {
  ExtensionPointName<HighlightRangeExtension> EP_NAME = ExtensionPointName.create(HighlightRangeExtension.class);

  /**
   * @return true if this file structure is so peculiar and irregular that it's needed to highlight the parents of the PSI element with an error inside.
   * In particular, {@link consulo.language.editor.annotation.Annotator}s will be called for all PSI elements irrespective of children with errors.
   * (Regular highlighting doesn't analyze parents of PSI elements with an error).
   * Please be aware that returning true may decrease highlighting performance/increase latency.
   */
  boolean isForceHighlightParents(PsiFile file);
}
