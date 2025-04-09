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
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 01.11.2006
 * Time: 17:15:24
 */
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface UsageTypeProvider {
    ExtensionPointName<UsageTypeProvider> EP_NAME = ExtensionPointName.create(UsageTypeProvider.class);

    @Nullable
    UsageType getUsageType(PsiElement element);

    @Nullable
    default UsageType getUsageType(PsiElement element, @Nonnull UsageTarget[] targets) {
        return getUsageType(element);
    }
}