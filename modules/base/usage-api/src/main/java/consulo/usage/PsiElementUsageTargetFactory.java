/*
 * Copyright 2013-2022 consulo.io
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
package consulo.usage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-04-20
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PsiElementUsageTargetFactory {
    static PsiElementUsageTargetFactory getInstance() {
        return Application.get().getInstance(PsiElementUsageTargetFactory.class);
    }

    @Nonnull
    PsiElementUsageTarget create(PsiElement element);

    @Nonnull
    default PsiElementUsageTarget[] create(PsiElement[] elements) {
        PsiElementUsageTarget[] targets = new PsiElementUsageTarget[elements.length];
        for (int i = 0; i < elements.length; i++) {
            targets[i] = create(elements[i]);
        }
        return targets;
    }
}
