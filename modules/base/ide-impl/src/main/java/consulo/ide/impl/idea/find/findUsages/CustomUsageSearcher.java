/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.findUsages;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.find.FindUsagesOptions;
import consulo.language.psi.PsiElement;
import consulo.usage.Usage;
import consulo.application.util.function.Processor;

/**
 * @author gregsh
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CustomUsageSearcher {
    public static final ExtensionPointName<CustomUsageSearcher> EP_NAME = ExtensionPointName.create(CustomUsageSearcher.class);

    public abstract void processElementUsages(PsiElement element, Processor<Usage> processor, FindUsagesOptions options);
}
