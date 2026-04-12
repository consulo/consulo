// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.find.impl;

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.content.scope.ScopeDescriptor;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.find.FindManager;
import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.searchEverywhere.*;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.ComboBoxButton;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.usage.FindUsagesProcessPresentation;
import consulo.usage.UsageInfo2UsageAdapter;
import consulo.usage.UsageViewPresentation;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Search Everywhere contributor that provides full-text search (like Find in Files) in the SE popup.
 */
public class TextSearchContributor implements WeightedSearchEverywhereContributor<SearchEverywhereItem>,
    SearchFieldActionsContributor,
    PossibleSlowContributor,
    SearchEverywhereEmptyTextProvider,
    ScopeSupporting,
    Disposable {

    public static final String ID = "TextSearchContributor";
    private static final Key<String> SE_TEXT_SELECTED_SCOPE = Key.create("SE_TEXT_SELECTED_SCOPE");

    private final Project myProject;
    private final FindModel myModel;
    private final GlobalSearchScope myEverywhereScope;
    private final @Nullable GlobalSearchScope myProjectScope;
    private ScopeDescriptor mySelectedScopeDescriptor;
    private final @Nullable SmartPsiElementPointer<PsiElement> myPsiContext;

    private Runnable myOnDispose;

    public TextSearchContributor(AnActionEvent event) {
        myProject = event.getRequiredData(Project.KEY);
        myModel = FindManager.getInstance(myProject).getFindInProjectModel();

        myEverywhereScope = GlobalSearchScope.everythingScope(myProject);
        List<ScopeDescriptor> scopes = createScopes();
        myProjectScope = getProjectScope(scopes);
        mySelectedScopeDescriptor = getInitialSelectedScope(scopes);
        myPsiContext = getPsiContext(event);
    }

    private @Nullable SmartPsiElementPointer<PsiElement> getPsiContext(AnActionEvent event) {
        PsiElement context = GotoActionBase.getPsiContext(event);
        if (context != null) {
            return SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(context);
        }
        return null;
    }

    private @Nullable GlobalSearchScope getProjectScope(List<ScopeDescriptor> descriptors) {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(myProject);
        if (!projectScope.equals(myEverywhereScope)) {
            return projectScope;
        }
        // Fall back to second available scope
        for (ScopeDescriptor descriptor : descriptors) {
            if (!descriptor.scopeEquals(myEverywhereScope) && !descriptor.scopeEquals(null)) {
                return descriptor.getScope() instanceof GlobalSearchScope gss ? gss : null;
            }
        }
        return myEverywhereScope;
    }

    @Override
    public String getSearchProviderId() {
        return ID;
    }

    @Override
    public String getGroupName() {
        return FindLocalize.searchEverywhereGroupName().get();
    }

    @Override
    public int getSortWeight() {
        return 1500;
    }

    @Override
    public boolean showInFindResults() {
        return true;
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    @Override
    public void fetchWeightedElements(
        String pattern,
        ProgressIndicator indicator,
        Predicate<? super FoundItemDescriptor<SearchEverywhereItem>> consumer
    ) {
        FindModel.initStringToFind(myModel, pattern);

        FindUsagesProcessPresentation presentation =
            FindInProjectUtil.setupProcessPresentation(myProject, new UsageViewPresentation());

        GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
        ThreadLocal<Reference<SearchEverywhereItem>> recentItemRef = new ThreadLocal<>();

        FindInProjectUtil.findUsages(myModel, myProject, usageInfo -> {
            UsageInfo2UsageAdapter usage = new UsageInfo2UsageAdapter(usageInfo);
            indicator.checkCanceled();

            SearchEverywhereItem recentItem = deref(recentItemRef.get());
            if (recentItem != null && recentItem.getUsage().merge(usage)) {
                // recompute merged presentation
                SearchEverywhereItem newItem = recentItem.withPresentation(
                    SearchEverywhereItem.usagePresentation(myProject, scope, recentItem.getUsage())
                );
                recentItemRef.set(new WeakReference<>(newItem));
            }
            else {
                SearchEverywhereItem.UsagePresentation usagePresentation =
                    SearchEverywhereItem.usagePresentation(myProject, scope, usage);
                SearchEverywhereItem newItem = new SearchEverywhereItem(usage, usagePresentation);
                recentItemRef.set(new WeakReference<>(newItem));
                if (!consumer.test(new FoundItemDescriptor<>(newItem, 0))) {
                    return false;
                }
            }
            return true;
        }, presentation);
    }

    private static @Nullable SearchEverywhereItem deref(@Nullable Reference<SearchEverywhereItem> ref) {
        return ref != null ? ref.get() : null;
    }

    @Override
    public ListCellRenderer<? super SearchEverywhereItem> getElementsRenderer() {
        return new TextSearchRenderer();
    }

    @Override
    public boolean processSelectedItem(SearchEverywhereItem selected, int modifiers, String searchText) {
        UsageInfo2UsageAdapter info = selected.getUsage();
        if (!info.canNavigate()) return false;
        info.navigate(true);
        return true;
    }

    @Override
    public List<AnAction> getActions(Runnable onChanged) {
        JComboboxAction comboboxAction = new JComboboxAction(myProject, this, mask -> onChanged.run());
        myOnDispose = comboboxAction.saveMask;
        return List.of(
            new ScopeAction(() -> onChanged.run()),
            comboboxAction
        );
    }

    @Override
    public List<AnAction> createRightActions(Consumer<AnAction> registerShortcut, Runnable onChanged) {
        AtomicBoolean word = new AtomicBoolean(myModel.isWholeWordsOnly());
        AtomicBoolean caseSensitive = new AtomicBoolean(myModel.isCaseSensitive());
        AtomicBoolean regexp = new AtomicBoolean(myModel.isRegularExpressions());

        Runnable syncAndChanged = () -> {
            myModel.setWholeWordsOnly(word.get());
            myModel.setCaseSensitive(caseSensitive.get());
            myModel.setRegularExpressions(regexp.get());
            onChanged.run();
        };

        return List.of(
            new TextSearchRightActionAction.CaseSensitiveAction(caseSensitive, registerShortcut, syncAndChanged),
            new TextSearchRightActionAction.WordAction(word, registerShortcut, syncAndChanged),
            new TextSearchRightActionAction.RegexpAction(regexp, registerShortcut, syncAndChanged)
        );
    }

    @Override
    public @Nullable Object getDataForItem(SearchEverywhereItem element, Key dataId) {
        if (PsiElement.KEY == dataId) {
            return element.getUsage().getElement();
        }
        return null;
    }

    // -- ScopeSupporting --

    private ScopeDescriptor getInitialSelectedScope(List<ScopeDescriptor> scopeDescriptors) {
        String savedScope = SE_TEXT_SELECTED_SCOPE.get(myProject);
        if (savedScope != null) {
            for (ScopeDescriptor desc : scopeDescriptors) {
                if (savedScope.equals(desc.getDisplayName()) && !desc.scopeEquals(null)) {
                    return desc;
                }
            }
        }
        return new ScopeDescriptor(myProjectScope);
    }

    private void setSelectedScope(ScopeDescriptor scope) {
        mySelectedScopeDescriptor = scope;
        SE_TEXT_SELECTED_SCOPE.set(
            myProject,
            (scope.scopeEquals(myEverywhereScope) || scope.scopeEquals(myProjectScope)) ? null : scope.getDisplayName()
        );
        FindSettings.getInstance().setCustomScope(
            mySelectedScopeDescriptor.getScope() != null ? mySelectedScopeDescriptor.getScope().getDisplayName() : null
        );
        myModel.setCustomScopeName(
            mySelectedScopeDescriptor.getScope() != null ? mySelectedScopeDescriptor.getScope().getDisplayName() : null
        );
        myModel.setCustomScope(mySelectedScopeDescriptor.getScope());
        myModel.setCustomScope(true);
    }

    private List<ScopeDescriptor> createScopes() {
        List<ScopeDescriptor> scopes = new ArrayList<>();
        ScopeChooserCombo.processScopes(
            myProject,
            DataContext.EMPTY_CONTEXT,
            ScopeChooserCombo.OPT_LIBRARIES | ScopeChooserCombo.OPT_EMPTY_SCOPES,
            descriptor -> {
                scopes.add(descriptor);
                return true;
            }
        );
        return scopes;
    }

    @Override
    public ScopeDescriptor getScope() {
        return mySelectedScopeDescriptor;
    }

    @Override
    public List<ScopeDescriptor> getSupportedScopes() {
        return createScopes();
    }

    @Override
    public void setScope(ScopeDescriptor scope) {
        setSelectedScope(scope);
    }

    // -- SearchEverywhereEmptyTextProvider --

    @Override
    public void updateEmptyStatus(StatusText statusText, Runnable rebuild) {
        statusText.appendText(FindLocalize.messageNothingfound());

        if (!(myModel.isCaseSensitive() || myModel.isWholeWordsOnly() || myModel.isRegularExpressions()
            || (myModel.getFileFilter() != null && !myModel.getFileFilter().isBlank()))) {
            return;
        }

        statusText.appendSecondaryText(" " + FindLocalize.messageNothingfoundUsedOptions().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);

        if (myModel.isCaseSensitive()) {
            statusText.appendSecondaryText(" " + FindLocalize.findPopupCaseSensitive().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        }
        if (myModel.isWholeWordsOnly()) {
            statusText.appendSecondaryText(" " + FindLocalize.findWholeWords().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        }
        if (myModel.isRegularExpressions()) {
            statusText.appendSecondaryText(" " + FindLocalize.findRegex().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        }
        if (myModel.getFileFilter() != null && !myModel.getFileFilter().isBlank()) {
            statusText.appendSecondaryText(" " + FindLocalize.findPopupFilemask().get(), SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
        }

        statusText.appendSecondaryText(
            " " + FindLocalize.messageNothingfoundClearall().get(),
            SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES,
            e -> {
                myModel.setCaseSensitive(false);
                myModel.setWholeWordsOnly(false);
                myModel.setRegularExpressions(false);
                myModel.setFileFilter(null);
                rebuild.run();
            }
        );
    }

    // -- ScopeChooserAction inner class --

    private class ScopeAction extends ComboBoxAction implements DumbAware, SearchEverywhereToggleAction {
        private final Runnable myOnChanged;

        ScopeAction(Runnable onChanged) {
            myOnChanged = onChanged;
        }

        @Override
        protected ComboBoxButton createComboBoxButton(Presentation presentation) {
            ComboBoxButton button = super.createComboBoxButton(presentation);
            if (button instanceof JComponent jc) {
                jc.setBorder(JBUI.Borders.empty());
                jc.setOpaque(false);
            }
            return button;
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        public void update(AnActionEvent e) {
            ScopeDescriptor selection = mySelectedScopeDescriptor;
            String name = StringUtil.trimMiddle(selection.getDisplayName(), 30);
            e.getPresentation().setText(LocalizeValue.of(name));
            e.getPresentation().setIcon(selection.getIcon());
        }

        @Override
        public JBPopup createPopup(
            JComponent component,
            DataContext context,
            Presentation presentation,
            Runnable onDispose
        ) {
            List<ScopeDescriptor> items = new ArrayList<>();
            ScopeChooserCombo.processScopes(
                myProject,
                DataContext.EMPTY_CONTEXT,
                ScopeChooserCombo.OPT_LIBRARIES | ScopeChooserCombo.OPT_EMPTY_SCOPES,
                o -> {
                    if (!o.scopeEquals(null) && o.getScope() instanceof GlobalSearchScope) {
                        items.add(o);
                    }
                    return true;
                }
            );

            BaseListPopupStep<ScopeDescriptor> step = new BaseListPopupStep<>("", items) {
                @Override
                public @Nullable PopupStep onChosen(ScopeDescriptor selectedValue, boolean finalChoice) {
                    setSelectedScope(selectedValue);
                    myOnChanged.run();
                    return FINAL_CHOICE;
                }

                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                @Override
                public String getTextFor(ScopeDescriptor value) {
                    return value.getScope() instanceof GlobalSearchScope ? value.getDisplayName() : "";
                }

                @Override
                public boolean isSelectable(ScopeDescriptor value) {
                    return value.getScope() instanceof GlobalSearchScope;
                }
            };

            ScopeDescriptor selection = mySelectedScopeDescriptor;
            step.setDefaultOptionIndex(
                ContainerUtil.indexOf(items, o -> Comparing.equal(o.getDisplayName(), selection.getDisplayName()))
            );
            ListPopupImpl popup = new ListPopupImpl(myProject, step);
            popup.setMaxRowCount(10);
            return popup;
        }

        @Override
        protected ActionGroup createPopupActionGroup(JComponent button) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEverywhere() {
            return mySelectedScopeDescriptor.scopeEquals(myEverywhereScope);
        }

        @Override
        public void setEverywhere(boolean everywhere) {
            setSelectedScope(new ScopeDescriptor(everywhere ? myEverywhereScope : myProjectScope));
            myOnChanged.run();
        }

        @Override
        public boolean canToggleEverywhere() {
            if (myEverywhereScope.equals(myProjectScope)) return false;
            return mySelectedScopeDescriptor.scopeEquals(myEverywhereScope)
                || mySelectedScopeDescriptor.scopeEquals(myProjectScope);
        }
    }

    // -- Disposable --

    @Override
    public void dispose() {
        if (myOnDispose != null) {
            myOnDispose.run();
        }
    }
}
