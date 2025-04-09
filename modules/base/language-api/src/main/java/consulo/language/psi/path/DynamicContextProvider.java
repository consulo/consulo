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

package consulo.language.psi.path;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;

/**
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DynamicContextProvider {
    ExtensionPointName<DynamicContextProvider> EP_NAME = ExtensionPointName.create(DynamicContextProvider.class);

    /**
     * Returns starting position for file references
     *
     * @param element
     * @param offset      initial offset
     * @param elementText
     * @return -1 to suppress file references generation
     */
    int getOffset(final PsiElement element, final int offset, final String elementText);
}
