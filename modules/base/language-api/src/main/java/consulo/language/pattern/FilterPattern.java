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
package consulo.language.pattern;

import consulo.language.util.ProcessingContext;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
public class FilterPattern extends ObjectPattern<Object,FilterPattern> {
  @Nullable private final ElementFilter myFilter;

  public FilterPattern(@Nullable final ElementFilter filter) {
    super(new InitialPatternCondition<Object>(Object.class) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        return filter == null ||
               o != null &&
               filter.isClassAcceptable(o.getClass()) &&
               filter.isAcceptable(o, o instanceof PsiElement ? (PsiElement)o : null);
      }
    });
    myFilter = filter;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FilterPattern)) return false;

    FilterPattern that = (FilterPattern)o;

    if (myFilter != null ? !myFilter.equals(that.myFilter) : that.myFilter != null) return false;

    return true;
  }

  public int hashCode() {
    return (myFilter != null ? myFilter.hashCode() : 0);
  }

  @Override
  public String toString() {
    return super.toString() + " & " + myFilter;
  }
}
