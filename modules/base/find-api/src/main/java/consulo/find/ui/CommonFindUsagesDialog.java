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

package consulo.find.ui;

import consulo.annotation.DeprecationInfo;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.find.FindUsagesUtil;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.usage.UsageViewUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * @author max
 */
@Deprecated
@DeprecationInfo("Use CommonFindUsagesDialogDescriptor")
public class CommonFindUsagesDialog extends AbstractFindUsagesDialog {
    
    protected final PsiElement myPsiElement;
    private final FindUsagesHandler myHandler;

    public CommonFindUsagesDialog(PsiElement element,
                                  Project project,
                                  FindUsagesOptions findUsagesOptions,
                                  boolean toShowInNewTab,
                                  boolean mustOpenInNewTab,
                                  boolean isSingleFile,
                                  FindUsagesHandler handler) {
        super(project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, isTextSearch(element, isSingleFile, handler),
            !isSingleFile && !element.getManager().isInProject(element));
        myPsiElement = element;
        myHandler = handler;
        init();
    }

    private static boolean isTextSearch(PsiElement element, boolean isSingleFile, FindUsagesHandler handler) {
        return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, isSingleFile, handler);
    }

    @Override
    protected boolean isInFileOnly() {
        return super.isInFileOnly() || PsiSearchScopeUtil.getUseScope(myPsiElement) instanceof LocalSearchScope;
    }

    @Override
    protected JPanel createFindWhatPanel() {
        return null;
    }

    @Override
    public void configureLabelComponent(SimpleColoredComponent coloredComponent) {
        coloredComponent.append(StringUtil.capitalize(UsageViewUtil.getType(myPsiElement)));
        coloredComponent.append(" ");
        coloredComponent.append(DescriptiveNameUtil.getDescriptiveName(myPsiElement), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    @Nullable
    @Override
    protected String getHelpId() {
        return myHandler.getHelpId();
    }
}
