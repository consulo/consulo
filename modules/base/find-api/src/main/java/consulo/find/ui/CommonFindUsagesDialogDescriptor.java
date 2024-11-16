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
package consulo.find.ui;

import consulo.disposer.Disposable;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.find.FindUsagesUtil;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.project.Project;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2024-11-16
 */
public class CommonFindUsagesDialogDescriptor extends AbstractFindUsagesDialogDescriptor {
    @Nonnull
    protected final PsiElement myPsiElement;
    private final FindUsagesHandler myHandler;

    @RequiredUIAccess
    public CommonFindUsagesDialogDescriptor(@Nonnull PsiElement element,
                                            @Nonnull Project project,
                                            @Nonnull FindUsagesOptions findUsagesOptions,
                                            boolean toShowInNewTab,
                                            boolean mustOpenInNewTab,
                                            boolean isSingleFile,
                                            FindUsagesHandler handler) {
        super(project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, isTextSearch(element, isSingleFile, handler),
            !isSingleFile && !element.getManager().isInProject(element));
        myPsiElement = element;
        myHandler = handler;
    }

    private static boolean isTextSearch(PsiElement element, boolean isSingleFile, FindUsagesHandler handler) {
        return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, isSingleFile, handler);
    }

    @Nullable
    @Override
    public String getHelpId() {
        return myHandler.getHelpId();
    }

    @RequiredUIAccess
    @Override
    protected void configureLabelComponent(@Nonnull TextItemPresentation presentation, @Nonnull Disposable uiDisposable) {
        presentation.append(StringUtil.capitalize(UsageViewUtil.getType(myPsiElement)));
        presentation.append(" ");
        presentation.append(DescriptiveNameUtil.getDescriptiveName(myPsiElement), TextAttribute.REGULAR_BOLD);
    }

    @Override
    protected boolean isInFileOnly() {
        return super.isInFileOnly() || PsiSearchScopeUtil.getUseScope(myPsiElement) instanceof LocalSearchScope;
    }
}
