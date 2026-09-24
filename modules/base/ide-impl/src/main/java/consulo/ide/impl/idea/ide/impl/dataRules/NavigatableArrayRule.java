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
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.dataContext.DataSnapshot;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.navigation.Navigatable;
import org.jspecify.annotations.Nullable;

public final class NavigatableArrayRule {
    static Navigatable[] getData(DataSnapshot dataProvider) {
        Navigatable[] navigatables = navigatablesFromSelectedItems(dataProvider);
        if (navigatables == null) {
            navigatables = nagigatablesFromKey(dataProvider);
        }
        return navigatables;
    }

    private static Navigatable @Nullable [] navigatablesFromSelectedItems(DataSnapshot dataProvider) {
        Object[] selectedItems = dataProvider.get(PlatformDataKeys.SELECTED_ITEMS);

        int nItems = selectedItems == null ? 0 : selectedItems.length;
        if (nItems == 0) {
            return null;
        }

        Navigatable[] navigatables = new Navigatable[nItems];
        for (int i = 0; i < nItems; i++) {
            // do not provide PSI in EDT, errors are already logged
            if (selectedItems[i] instanceof Navigatable selectedNav && !(selectedNav instanceof PsiElement)) {
                navigatables[i] = selectedNav;
            }
            else {
                return null;
            }
        }
        return navigatables;
    }

    private static Navigatable @Nullable [] nagigatablesFromKey(DataSnapshot dataProvider) {
        Navigatable element = dataProvider.get(Navigatable.KEY);
        return element == null ? null : new Navigatable[]{element};
    }

}
