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
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.language.psi.PsiElement;
import consulo.language.psi.search.FindSymbolParameters;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

public interface ChooseByNameModelEx extends ChooseByNameModel {
    /**
     * @deprecated use {@link #processNames(Predicate, FindSymbolParameters)} instead
     */
    @Deprecated
    default void processNames(Predicate<? super String> processor, boolean inLibraries) {
    }

    default void processNames(Predicate<? super String> processor, FindSymbolParameters parameters) {
        processNames(processor, parameters.isSearchInLibraries());
    }

    
    default ChooseByNameItemProvider getItemProvider(@Nullable PsiElement context) {
        return new DefaultChooseByNameItemProvider(context);
    }

    
    static ChooseByNameItemProvider getItemProvider(ChooseByNameModel model, @Nullable PsiElement context) {
        return model instanceof ChooseByNameModelEx chooseByNameModelEx
            ? chooseByNameModelEx.getItemProvider(context)
            : new DefaultChooseByNameItemProvider(context);
    }
}
