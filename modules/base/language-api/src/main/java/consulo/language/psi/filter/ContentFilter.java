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
package consulo.language.psi.filter;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.filter.position.PositionElementFilter;
import org.jspecify.annotations.Nullable;

/**
 * @author ik
 * @since 2003-02-13
 */
public class ContentFilter extends PositionElementFilter {
  public ContentFilter(ElementFilter filter){
    setFilter(filter);
  }

  @Override
  @RequiredReadAction
  public boolean isAcceptable(Object element, @Nullable PsiElement scope){
    if (!(element instanceof PsiElement psiElement)) {
      return false;
    }
    PsiElement currentChild = psiElement.getFirstChild();
    while (currentChild != null) {
      if (getRequiredFilter().isAcceptable(currentChild, psiElement)) {
        return true;
      }
      currentChild = currentChild.getNextSibling();
    }
    return false;
  }

  @Override
  public String toString(){
    return "content(" + getFilter() + ")";
  }
}
