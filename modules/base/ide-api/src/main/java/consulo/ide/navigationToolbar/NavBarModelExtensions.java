/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.navigationToolbar;

import consulo.component.extension.ExtensionPoint;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-11-27
 */
public class NavBarModelExtensions {
    @Nullable
    public static PsiElement normalize(@Nullable PsiElement child) {
        if (child == null) {
            return null;
        }

        List<NavBarModelExtension> extensions = child.getApplication().getExtensionList(NavBarModelExtension.class);
        for (int i = extensions.size(); --i >= 0; ) {
            NavBarModelExtension modelExtension = extensions.get(i);
            child = modelExtension.adjustElement(child);
            if (child == null) {
                return null;
            }
        }
        return child;
    }
}
