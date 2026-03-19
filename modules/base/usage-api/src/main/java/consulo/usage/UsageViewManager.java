/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.rule.PsiElementUsage;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class UsageViewManager {
    public static UsageViewManager getInstance(Project project) {
        return project.getInstance(UsageViewManager.class);
    }

    
    public abstract UsageView createUsageView(
        UsageTarget[] targets,
        Usage[] usages,
        UsageViewPresentation presentation,
        Supplier<UsageSearcher> usageSearcherFactory
    );

    
    public abstract UsageView showUsages(
        UsageTarget[] searchedFor,
        Usage[] foundUsages,
        UsageViewPresentation presentation,
        Supplier<UsageSearcher> factory
    );

    
    public abstract UsageView showUsages(
        UsageTarget[] searchedFor,
        Usage[] foundUsages,
        UsageViewPresentation presentation
    );

    /**
     * @return returns null in case of no usages found or usage view not shown for one usage
     */
    public abstract @Nullable UsageView searchAndShowUsages(
        UsageTarget[] searchFor,
        Supplier<UsageSearcher> searcherFactory,
        boolean showPanelIfOnlyOneUsage,
        boolean showNotFoundMessage,
        UsageViewPresentation presentation,
        @Nullable UsageViewStateListener listener
    );

    public interface UsageViewStateListener {
        void usageViewCreated(UsageView usageView);

        @RequiredUIAccess
        void findingUsagesFinished(UsageView usageView);
    }

    public abstract void searchAndShowUsages(
        UsageTarget[] searchFor,
        Supplier<UsageSearcher> searcherFactory,
        FindUsagesProcessPresentation processPresentation,
        UsageViewPresentation presentation,
        @Nullable UsageViewStateListener listener
    );

    public abstract @Nullable UsageView getSelectedUsageView();

    public static boolean isSelfUsage(Usage usage, UsageTarget[] searchForTarget) {
        if (!(usage instanceof PsiElementUsage elementUsage)) {
            return false;
        }
        return Application.get().runReadAction((Supplier<Boolean>)() -> {
            PsiElement element = elementUsage.getElement();
            if (element == null) {
                return false;
            }

            for (UsageTarget ut : searchForTarget) {
                if (ut instanceof PsiElementUsageTarget elementUt && isSelfUsage(element, elementUt.getElement())) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean isSelfUsage(PsiElement element, PsiElement psiElement) {
        return element.getParent() == psiElement; // self usage might be configurable
    }
}
