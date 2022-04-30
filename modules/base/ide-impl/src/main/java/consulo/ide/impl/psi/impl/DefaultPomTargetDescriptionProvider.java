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
package consulo.ide.impl.psi.impl;

import consulo.application.util.TypePresentationService;
import com.intellij.openapi.util.text.StringUtil;
import consulo.language.pom.PomDescriptionProvider;
import consulo.language.pom.PomNamedTarget;
import consulo.language.pom.PomTarget;
import consulo.language.psi.ElementDescriptionLocation;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageViewNodeTextLocation;
import consulo.usage.UsageViewTypeLocation;
import consulo.language.editor.highlight.HighlightUsagesDescriptionLocation;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public class DefaultPomTargetDescriptionProvider extends PomDescriptionProvider {
  @Override
  public String getElementDescription(@Nonnull PomTarget element, @Nonnull ElementDescriptionLocation location) {
    if (element instanceof PsiElement) return null;

    if (location == UsageViewTypeLocation.INSTANCE) {
      return getTypeName(element);
    }
    if (location == UsageViewNodeTextLocation.INSTANCE) {
      return getTypeName(element) + " " + StringUtil.notNullize(element instanceof PomNamedTarget ? ((PomNamedTarget)element).getName() : null, "''");
    }
    if (location instanceof HighlightUsagesDescriptionLocation) {
      return getTypeName(element);
    }
    return null;
  }

  private static String getTypeName(PomTarget element) {

    final String s = TypePresentationService.getInstance().getTypePresentableName(element.getClass());
    return s == null ? "Element" : s;
  }
}
