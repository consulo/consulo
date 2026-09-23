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
package consulo.language.psi.search;

import consulo.language.psi.scope.GlobalSearchScope;
import consulo.content.scope.SearchScope;
import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
public class PsiSearchRequest {
    public final SearchScope searchScope;
    public final String word;
    public final short searchContext;
    public final boolean caseSensitive;
    public final RequestResultProcessor processor;
    public final @Nullable String containerName;

    public PsiSearchRequest(
        SearchScope searchScope,
        String word,
        short searchContext,
        boolean caseSensitive,
        RequestResultProcessor processor
    ) {
        this(searchScope, word, searchContext, caseSensitive, null, processor);
    }

    public PsiSearchRequest(
        SearchScope searchScope,
        String word,
        short searchContext,
        boolean caseSensitive,
        @Nullable String containerName,
        RequestResultProcessor processor
    ) {
        this.containerName = containerName;
        if (word.isEmpty()) {
            throw new IllegalArgumentException("Cannot search for elements with empty text");
        }
        this.searchScope = searchScope;
        this.word = word;
        this.searchContext = searchContext;
        this.caseSensitive = caseSensitive;
        this.processor = processor;
        if (searchScope instanceof GlobalSearchScope && ((GlobalSearchScope) searchScope).getProject() == null) {
            throw new AssertionError("Every search scope must be associated with a project");
        }
    }

    @Override
    public String toString() {
        return word + " -> " + processor;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof PsiSearchRequest that
            && caseSensitive == that.caseSensitive
            && searchContext == that.searchContext
            && processor.equals(that.processor)
            && searchScope.equals(that.searchScope)
            && word.equals(that.word);
    }

    @Override
    public int hashCode() {
        int result = searchScope.hashCode();
        result = 31 * result + word.hashCode();
        result = 31 * result + (int) searchContext;
        result = 31 * result + Boolean.hashCode(caseSensitive);
        return 31 * result + processor.hashCode();
    }
}
