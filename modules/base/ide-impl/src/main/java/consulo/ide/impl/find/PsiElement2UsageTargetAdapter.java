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

package consulo.ide.impl.find;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataSink;
import consulo.dataContext.TypeSafeDataProvider;
import consulo.fileEditor.FileEditor;
import consulo.find.FindManager;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.find.localize.FindLocalize;
import consulo.ide.impl.idea.codeInsight.highlighting.HighlightUsagesHandler;
import consulo.ide.impl.idea.find.findUsages.FindUsagesManager;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.language.findUsage.DescriptiveNameUtil;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.meta.PsiMetaOwner;
import consulo.language.psi.meta.PsiPresentableMetaData;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author max
 */
public class PsiElement2UsageTargetAdapter implements PsiElementUsageTarget, TypeSafeDataProvider, PsiElementNavigationItem, ItemPresentation, ConfigurableUsageTarget {
    private final SmartPsiElementPointer myPointer;
    @Nonnull
    protected final FindUsagesOptions myOptions;

    private String myPresentableText;
    private Image myIcon;

    public PsiElement2UsageTargetAdapter(@Nonnull PsiElement element, @Nonnull FindUsagesOptions options) {
        myOptions = options;
        myPointer = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);

        if (!(element instanceof NavigationItem)) {
            throw new IllegalArgumentException("Element is not a navigation item: " + element);
        }
        update(element);
    }

    @RequiredReadAction
    public PsiElement2UsageTargetAdapter(@Nonnull PsiElement element) {
        this(element, new FindUsagesOptions(element.getProject()));
    }

    @Override
    @RequiredReadAction
    public String getName() {
        PsiElement element = getElement();
        return element instanceof NavigationItem navigationItem ? navigationItem.getName() : null;
    }

    @Override
    @Nonnull
    public ItemPresentation getPresentation() {
        return this;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        PsiElement element = getElement();
        if (element instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(requestFocus);
        }
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        PsiElement element = getElement();
        return element instanceof Navigatable navigatable && navigatable.canNavigate();
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        PsiElement element = getElement();
        return element instanceof Navigatable navigatable && navigatable.canNavigateToSource();
    }

    @Override
    @RequiredReadAction
    public PsiElement getTargetElement() {
        return getElement();
    }

    @Override
    public String toString() {
        return getPresentableText();
    }

    @Override
    @RequiredUIAccess
    public void findUsages() {
        PsiElement element = getElement();
        if (element == null) {
            return;
        }
        ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager()
            .startFindUsages(element, myOptions, null, null);
    }

    @Override
    @RequiredReadAction
    public PsiElement getElement() {
        return myPointer.getElement();
    }

    @Override
    @RequiredUIAccess
    public void findUsagesInEditor(@Nonnull FileEditor editor) {
        PsiElement element = getElement();
        FindManager.getInstance(element.getProject()).findUsagesInEditor(element, editor);
    }

    @Override
    @RequiredUIAccess
    public void highlightUsages(@Nonnull PsiFile file, @Nonnull Editor editor, boolean clearHighlights) {
        PsiElement target = getElement();

        if (file instanceof PsiCompiledFile compiledFile) {
            file = compiledFile.getDecompiledPsiFile();
        }

        Project project = target.getProject();
        final FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
        final FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(target, true);

        // in case of injected file, use host file to highlight all occurrences of the target in each injected file
        PsiFile context = InjectedLanguageManager.getInstance(project).getTopLevelFile(file);
        SearchScope searchScope = new LocalSearchScope(context);
        Collection<PsiReference> refs = handler == null
            ? ReferencesSearch.search(target, searchScope, false).findAll()
            : handler.findReferencesToHighlight(target, searchScope);

        new HighlightUsagesHandler.DoHighlightRunnable(new ArrayList<>(refs), project, target, editor, context, clearHighlights).run();
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return getElement() != null;
    }

    @Override
    @RequiredReadAction
    public boolean isReadOnly() {
        return isValid() && !getElement().isWritable();
    }

    @Override
    @RequiredReadAction
    public VirtualFile[] getFiles() {
        if (!isValid()) {
            return null;
        }

        final PsiFile psiFile = getElement().getContainingFile();
        if (psiFile == null) {
            return null;
        }

        final VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile == null ? null : new VirtualFile[]{virtualFile};
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement2UsageTargetAdapter[] convert(@Nonnull PsiElement[] psiElements) {
        PsiElement2UsageTargetAdapter[] targets = new PsiElement2UsageTargetAdapter[psiElements.length];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new PsiElement2UsageTargetAdapter(psiElements[i]);
        }

        return targets;
    }

    @Nonnull
    @RequiredReadAction
    public static PsiElement[] convertToPsiElements(PsiElement2UsageTargetAdapter[] adapters) {
        PsiElement[] targets = new PsiElement[adapters.length];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = adapters[i].getElement();
        }

        return targets;
    }

    @Override
    @RequiredReadAction
    public void calcData(final Key<?> key, final DataSink sink) {
        if (key == UsageView.USAGE_INFO_KEY) {
            PsiElement element = getElement();
            if (element != null && element.getTextRange() != null) {
                sink.put(UsageView.USAGE_INFO_KEY, new UsageInfo(element));
            }
        }
        else if (key == UsageView.USAGE_SCOPE) {
            sink.put(UsageView.USAGE_SCOPE, myOptions.searchScope);
        }
    }

    @Override
    public KeyboardShortcut getShortcut() {
        return UsageViewUtil.getShowUsagesWithSettingsShortcut();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getLongDescriptiveName() {
        SearchScope searchScope = myOptions.searchScope;
        String scopeString = searchScope.getDisplayName();
        PsiElement psiElement = getElement();

        return psiElement == null
            ? UsageViewBundle.message("node.invalid")
            : FindLocalize.recentFindUsagesActionPopup(
                StringUtil.capitalize(UsageViewUtil.getType(psiElement)),
                DescriptiveNameUtil.getDescriptiveName(psiElement),
                scopeString
            ).get();
    }

    @Override
    @RequiredUIAccess
    public void showSettings() {
        PsiElement element = getElement();
        if (element != null) {
            FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(myPointer.getProject())).getFindUsagesManager();
            findUsagesManager.findUsagesAsync(element, null, null, true, null);
        }
    }

    @Override
    @RequiredReadAction
    public void update() {
        update(getElement());
    }

    private void update(PsiElement element) {
        if (element != null && element.isValid()) {
            final ItemPresentation presentation = ((NavigationItem)element).getPresentation();
            myIcon = presentation == null ? null : presentation.getIcon();
            myPresentableText = presentation == null ? UsageViewUtil.createNodeText(element) : presentation.getPresentableText();
            if (myIcon == null) {
                if (element instanceof PsiMetaOwner metaOwner) {
                    if (metaOwner.getMetaData() instanceof PsiPresentableMetaData presentableMetaData) {
                        if (myIcon == null) {
                            myIcon = presentableMetaData.getIcon();
                        }
                    }
                }
                else if (element instanceof PsiFile file) {
                    final VirtualFile virtualFile = file.getVirtualFile();
                    if (virtualFile != null) {
                        myIcon = VirtualFilePresentation.getIcon(virtualFile);
                    }
                }
            }
        }
    }

    @Override
    public String getPresentableText() {
        return myPresentableText;
    }

    @Override
    public String getLocationString() {
        return null;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Nonnull
    public FindUsagesOptions getOptions() {
        return myOptions;
    }
}
