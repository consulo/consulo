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

import consulo.annotation.access.RequiredReadAction;
import consulo.language.util.ProcessingContext;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author peter
 */
public class FilterPattern extends ObjectPattern<Object, FilterPattern> {
    private final @Nullable ElementFilter myFilter;

    public FilterPattern(final @Nullable ElementFilter filter) {
        super(new InitialPatternCondition<>(Object.class) {
            @Override
            @RequiredReadAction
            public boolean accepts(@Nullable Object o, ProcessingContext context) {
                if (filter == null) {
                    return true;
                }
                return o != null
                    && filter.isClassAcceptable(o.getClass())
                    && filter.isAcceptable(o, o instanceof PsiElement elem ? elem : null);
            }
        });
        myFilter = filter;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof FilterPattern that && Objects.equals(myFilter, that.myFilter);
    }

    @Override
    public int hashCode() {
        return (myFilter != null ? myFilter.hashCode() : 0);
    }

    @Override
    public String toString() {
        return super.toString() + " & " + myFilter;
    }
}
