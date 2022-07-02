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

/*
 * @author max
 */
package consulo.ide.impl.idea.lang;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.Language;
import consulo.language.editor.annotation.ExternalAnnotator;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;
import java.util.List;

public class ExternalLanguageAnnotators {
  @Nonnull
  public static List<ExternalAnnotator> allForFile(Language language, final PsiFile file) {
    List<ExternalAnnotator> annotators = ExternalAnnotator.forLanguage(language);
    final List<ExternalAnnotatorsFilter> filters = ExternalAnnotatorsFilter.EXTENSION_POINT_NAME.getExtensionList();
    return ContainerUtil.findAll(annotators, annotator -> {
      for (ExternalAnnotatorsFilter filter : filters) {
        if (filter.isProhibited(annotator, file)) {
          return false;
        }
      }
      return true;
    });
  }
}