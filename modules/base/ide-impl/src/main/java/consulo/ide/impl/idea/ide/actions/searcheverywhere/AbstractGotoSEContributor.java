// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.application.internal.ProgressIndicatorUtils;
import consulo.application.progress.ProgressIndicator;
import consulo.content.scope.ScopeDescriptor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.ide.actions.SearchEverywherePsiRenderer;
import consulo.ide.impl.idea.ide.util.gotoByName.*;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.util.EditSourceUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationItem;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.searchEverywhere.FoundItemDescriptor;
import consulo.searchEverywhere.SearchEverywhereToggleAction;
import consulo.searchEverywhere.WeightedSearchEverywhereContributor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.action.ComboBoxButton;
import consulo.ui.ex.awt.action.ComboBoxButtonImpl;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGotoSEContributor implements WeightedSearchEverywhereContributor<Object> {
    private static final Logger LOG = Logger.getInstance(AbstractGotoSEContributor.class);
    private static final Key<Map<String, String>> SE_SELECTED_SCOPES = Key.create("SE_SELECTED_SCOPES");

    private static final Pattern ourPatternToDetectLinesAndColumns = Pattern.compile("(.+?)" + // name, non-greedy matching
        "(?::|@|,| |#|#L|\\?l=| on line | at line |:?\\(|:?\\[)" + // separator
        "(\\d+)?(?:\\W(\\d+)?)?" + // line + column
        "[)\\]]?" // possible closing paren/brace
    );

    protected final Project myProject;
    protected final PsiElement psiContext;
    protected ScopeDescriptor myScopeDescriptor;

    private final GlobalSearchScope myEverywhereScope;
    private final GlobalSearchScope myProjectScope;

    protected AbstractGotoSEContributor(@Nullable Project project, @Nullable PsiElement context) {
        myProject = project;
        psiContext = context;
        myEverywhereScope = myProject == null ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.everythingScope(myProject);
        GlobalSearchScope projectScope = myProject == null ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.projectScope(myProject);
        if (myProject == null) {
            myProjectScope = GlobalSearchScope.EMPTY_SCOPE;
        }
        else if (!myEverywhereScope.equals(projectScope)) {
            myProjectScope = projectScope;
        }
        else {
            // just get the second scope, i.e. Attached Directories in DataGrip
            SimpleReference<GlobalSearchScope> result = SimpleReference.create();
            processScopes(SimpleDataContext.getProjectContext(myProject), o -> {
                if (o.scopeEquals(myEverywhereScope) || o.scopeEquals(null)) {
                    return true;
                }
                result.set((GlobalSearchScope)o.getScope());
                return false;
            });
            myProjectScope = ObjectUtil.notNull(result.get(), myEverywhereScope);
        }
        myScopeDescriptor = getInitialSelectedScope();
    }

    @Nonnull
    @Override
    public String getSearchProviderId() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isShownInSeparateTab() {
        return true;
    }

    private static void processScopes(@Nonnull DataContext dataContext, @Nonnull Predicate<? super ScopeDescriptor> processor) {
        ScopeChooserCombo.processScopes(
            dataContext.getRequiredData(Project.KEY),
            dataContext,
            ScopeChooserCombo.OPT_LIBRARIES | ScopeChooserCombo.OPT_EMPTY_SCOPES,
            processor
        );
    }

    @Nonnull
    protected List<AnAction> doGetActions(
        @Nonnull LocalizeValue everywhereText,
        @Nullable PersistentSearchEverywhereContributorFilter<?> filter,
        @Nonnull Runnable onChanged
    ) {
        if (myProject == null || filter == null) {
            return Collections.emptyList();
        }
        ArrayList<AnAction> result = new ArrayList<>();
        result.add(new ScopeChooserAction() {
            boolean canToggleEverywhere = !myEverywhereScope.equals(myProjectScope);

            @Override
            void onScopeSelected(@Nonnull ScopeDescriptor o) {
                setSelectedScope(o);
                onChanged.run();
            }

            @Nonnull
            @Override
            ScopeDescriptor getSelectedScope() {
                return myScopeDescriptor;
            }

            @Override
            public boolean isEverywhere() {
                return myScopeDescriptor.scopeEquals(myEverywhereScope);
            }

            @Override
            public void setEverywhere(boolean everywhere) {
                setSelectedScope(new ScopeDescriptor(everywhere ? myEverywhereScope : myProjectScope));
                onChanged.run();
            }

            @Override
            public boolean canToggleEverywhere() {
                return canToggleEverywhere && (myScopeDescriptor.scopeEquals(myEverywhereScope) || myScopeDescriptor.scopeEquals(
                    myProjectScope));
            }
        });
        result.add(new SearchEverywhereUI.FiltersAction(filter, onChanged));
        return result;
    }

    @Nonnull
    private ScopeDescriptor getInitialSelectedScope() {
        String selectedScope = myProject == null ? null : getSelectedScopes(myProject).get(getClass().getSimpleName());
        if (StringUtil.isNotEmpty(selectedScope)) {
            SimpleReference<ScopeDescriptor> result = SimpleReference.create();
            processScopes(SimpleDataContext.getProjectContext(myProject), o -> {
                if (!selectedScope.equals(o.getDisplayName()) || o.scopeEquals(null)) {
                    return true;
                }
                result.set(o);
                return false;
            });
            return !result.isNull() ? result.get() : new ScopeDescriptor(myProjectScope);
        }
        else {
            return new ScopeDescriptor(myProjectScope);
        }
    }

    private void setSelectedScope(@Nonnull ScopeDescriptor o) {
        myScopeDescriptor = o;
        getSelectedScopes(myProject).put(
            getClass().getSimpleName(),
            o.scopeEquals(myEverywhereScope) || o.scopeEquals(myProjectScope) ? null : o.getDisplayName()
        );
    }

    @Nonnull
    private static Map<String, String> getSelectedScopes(@Nonnull Project project) {
        Map<String, String> map = SE_SELECTED_SCOPES.get(project);
        if (map == null) {
            SE_SELECTED_SCOPES.set(project, map = new HashMap<>(3));
        }
        return map;
    }

    @Override
    public void fetchWeightedElements(
        @Nonnull String pattern,
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super FoundItemDescriptor<Object>> predicate
    ) {
        if (myProject == null) {
            return; //nowhere to search
        }
        if (!isEmptyPatternSupported() && pattern.isEmpty()) {
            return;
        }

        ProgressIndicatorUtils.yieldToPendingWriteActions();
        ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(
            () -> {
                if (!isDumbAware() && DumbService.isDumb(myProject)) {
                    return;
                }

                FilteringGotoByModel<?> model = createModel(myProject);
                if (progressIndicator.isCanceled()) {
                    return;
                }

                PsiElement context = psiContext != null && psiContext.isValid() ? psiContext : null;
                ChooseByNamePopup popup = ChooseByNamePopup.createPopup(myProject, model, context);
                try {
                    ChooseByNameItemProvider provider = popup.getProvider();
                    GlobalSearchScope scope = (GlobalSearchScope)ObjectUtil.notNull(myScopeDescriptor.getScope());

                    boolean everywhere = scope.isSearchInLibraries();
                    if (provider instanceof ChooseByNameInScopeItemProvider chooseByNameInScopeItemProvider) {
                        FindSymbolParameters parameters = FindSymbolParameters.wrap(pattern, scope);
                        chooseByNameInScopeItemProvider.filterElementsWithWeights(
                            popup,
                            parameters,
                            progressIndicator,
                            item -> processElement(progressIndicator, predicate, model, item.getItem(), item.getWeight())
                        );
                    }
                    else if (provider instanceof ChooseByNameWeightedItemProvider chooseByNameWeightedItemProvider) {
                        chooseByNameWeightedItemProvider.filterElementsWithWeights(
                            popup,
                            pattern,
                            everywhere,
                            progressIndicator,
                            item -> processElement(progressIndicator, predicate, model, item.getItem(), item.getWeight())
                        );
                    }
                    else {
                        provider.filterElements(
                            popup,
                            pattern,
                            everywhere,
                            progressIndicator,
                            element -> processElement(progressIndicator, predicate, model, element, getElementPriority(element, pattern))
                        );
                    }
                }
                finally {
                    Disposer.dispose(popup);
                }
            },
            progressIndicator
        );
    }

    private boolean processElement(
        @Nonnull ProgressIndicator progressIndicator,
        @Nonnull Predicate<? super FoundItemDescriptor<Object>> predicate,
        FilteringGotoByModel<?> model,
        Object element,
        int degree
    ) {
        if (progressIndicator.isCanceled()) {
            return false;
        }

        if (element == null) {
            LOG.error("Null returned from " + model + " in " + this);
            return true;
        }

        return predicate.test(new FoundItemDescriptor<>(element, degree));
    }

    @Nonnull
    protected abstract FilteringGotoByModel<?> createModel(@Nonnull Project project);

    @Nonnull
    @Override
    public String filterControlSymbols(@Nonnull String pattern) {
        if (StringUtil.containsAnyChar(pattern, ":,;@[( #") || pattern.contains(" line ") || pattern.contains("?l=")) {
            // quick test if reg exp should be used
            return applyPatternFilter(pattern, ourPatternToDetectLinesAndColumns);
        }

        return pattern;
    }

    protected static String applyPatternFilter(String str, Pattern regex) {
        Matcher matcher = regex.matcher(str);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return str;
    }

    @Override
    public boolean showInFindResults() {
        return true;
    }

    @Override
    public boolean processSelectedItem(@Nonnull Object selected, int modifiers, @Nonnull String searchText) {
        if (selected instanceof PsiElement element) {
            if (!element.isValid()) {
                LOG.warn("Cannot navigate to invalid PsiElement");
                return true;
            }

            PsiElement psiElement = preparePsi(element, modifiers, searchText);
            Navigatable extNavigatable = createExtendedNavigatable(psiElement, searchText, modifiers);
            if (extNavigatable != null && extNavigatable.canNavigate()) {
                extNavigatable.navigate(true);
                return true;
            }

            PopupNavigationUtil.activateFileWithPsiElement(psiElement, openInCurrentWindow(modifiers));
        }
        else {
            EditSourceUtil.navigate(((NavigationItem)selected), true, openInCurrentWindow(modifiers));
        }

        return true;
    }

    @Override
    public Object getDataForItem(@Nonnull Object element, @Nonnull Key dataId) {
        if (PsiElement.KEY == dataId) {
            if (element instanceof PsiElement) {
                return element;
            }
            if (element instanceof DataProvider dataProvider) {
                return dataProvider.getData(dataId);
            }
        }

        if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION == dataId && element instanceof PsiElement psiElement) {
            return QualifiedNameProviderUtil.getQualifiedName(psiElement);
        }

        return null;
    }

    @Override
    public boolean isMultiSelectionSupported() {
        return true;
    }

    @Override
    public boolean isDumbAware() {
        return DumbService.isDumbAware(createModel(myProject));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public ListCellRenderer<Object> getElementsRenderer() {
        return (ListCellRenderer)new SERenderer();
    }

    @Override
    public int getElementPriority(@Nonnull Object element, @Nonnull String searchPattern) {
        return 50;
    }

    @Nullable
    protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
        VirtualFile file = PsiUtilCore.getVirtualFile(psi);
        Couple<Integer> position = getLineAndColumn(searchText);
        boolean positionSpecified = position.first >= 0 || position.second >= 0;
        if (file != null && positionSpecified) {
            OpenFileDescriptorImpl descriptor = new OpenFileDescriptorImpl(psi.getProject(), file, position.first, position.second);
            return descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
        }

        return null;
    }

    protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
        return psiElement.getNavigationElement();
    }

    protected static Couple<Integer> getLineAndColumn(String text) {
        int line = getLineAndColumnRegexpGroup(text, 2);
        int column = getLineAndColumnRegexpGroup(text, 3);

        if (line == -1 && column != -1) {
            line = 0;
        }

        return Couple.of(line, column);
    }

    private static int getLineAndColumnRegexpGroup(String text, int groupNumber) {
        Matcher matcher = ourPatternToDetectLinesAndColumns.matcher(text);
        if (matcher.matches()) {
            try {
                if (groupNumber <= matcher.groupCount()) {
                    String group = matcher.group(groupNumber);
                    if (group != null) {
                        return Integer.parseInt(group) - 1;
                    }
                }
            }
            catch (NumberFormatException ignored) {
            }
        }

        return -1;
    }

    protected static boolean openInCurrentWindow(int modifiers) {
        return (modifiers & InputEvent.SHIFT_MASK) == 0;
    }

    protected static class SERenderer extends SearchEverywherePsiRenderer {
        @Override
        @RequiredReadAction
        public String getElementText(PsiElement element) {
            if (element instanceof NavigationItem navigationItem) {
                return Optional.ofNullable(navigationItem.getPresentation())
                    .map(ItemPresentation::getPresentableText)
                    .orElse(super.getElementText(element));
            }
            return super.getElementText(element);
        }
    }

    abstract static class ScopeChooserAction extends ComboBoxAction implements DumbAware, SearchEverywhereToggleAction {
        static final char CHOOSE = 'O';
        static final char TOGGLE = 'P';

        abstract void onScopeSelected(@Nonnull ScopeDescriptor o);

        @Nonnull
        abstract ScopeDescriptor getSelectedScope();

        @Nonnull
        @Override
        protected ComboBoxButton createComboBoxButton(Presentation presentation) {
            ComboBoxButtonImpl button = (ComboBoxButtonImpl)super.createComboBoxButton(presentation);
            button.setBorder(JBUI.Borders.empty());
            button.setOpaque(false);
            return button;
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            ScopeDescriptor selection = getSelectedScope();
            String name = StringUtil.trimMiddle(selection.getDisplayName(), 30);
            String text = StringUtil.escapeMnemonics(name).replaceFirst("(?i)([" + TOGGLE + CHOOSE + "])", "_$1");
            e.getPresentation().setTextValue(LocalizeValue.of(text));
            e.getPresentation().setIcon(selection.getIcon());
            String shortcutText =
                KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(CHOOSE, MnemonicHelper.getFocusAcceleratorKeyMask(), true));
            String shortcutText2 =
                KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(TOGGLE, MnemonicHelper.getFocusAcceleratorKeyMask(), true));
            e.getPresentation().setDescription("Choose scope (" + shortcutText + ")\n" + "Toggle scope (" + shortcutText2 + ")");
            JComponent button = e.getPresentation().getClientProperty(CustomComponentAction.COMPONENT_KEY);
            if (button != null) {
                button.setBackground(TargetAWT.to(selection.getColor()));
            }
        }

        @Nonnull
        @Override
        public JBPopup createPopup(
            @Nonnull JComponent component,
            @Nonnull DataContext context,
            @Nonnull Presentation presentation,
            @Nonnull Runnable onDispose
        ) {
            JList<ScopeDescriptor> fakeList = new JBList<>();
            ListCellRenderer<ScopeDescriptor> renderer = new ListCellRenderer<>() {
                ListCellRenderer<ScopeDescriptor> delegate = ScopeChooserCombo.createDefaultRenderer();

                @Override
                public Component getListCellRendererComponent(
                    JList<? extends ScopeDescriptor> list,
                    ScopeDescriptor value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
                ) {
                    // copied from DarculaJBPopupComboPopup.customizeListRendererComponent()
                    Component component = delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (component instanceof JComponent jComponent
                        && !(component instanceof JSeparator || component instanceof TitledSeparator)) {
                        jComponent.setBorder(JBUI.Borders.empty(2, 8));
                    }
                    return component;
                }
            };
            List<ScopeDescriptor> items = new ArrayList<>();
            processScopes(context, o -> {
                Component c = renderer.getListCellRendererComponent(fakeList, o, -1, false, false);
                if (c instanceof JSeparator
                    || c instanceof TitledSeparator
                    || !o.scopeEquals(null) && o.getScope() instanceof GlobalSearchScope) {
                    items.add(o);
                }
                return true;
            });
            BaseListPopupStep<ScopeDescriptor> step = new BaseListPopupStep<ScopeDescriptor>("", items) {
                @Nullable
                @Override
                @RequiredUIAccess
                public PopupStep onChosen(ScopeDescriptor selectedValue, boolean finalChoice) {
                    onScopeSelected(selectedValue);
                    ActionToolbar toolbar = UIUtil.uiParents(component, true).filter(ActionToolbar.class).first();
                    if (toolbar != null) {
                        toolbar.updateActionsAsync();
                    }
                    return FINAL_CHOICE;
                }

                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                @Nonnull
                @Override
                public String getTextFor(ScopeDescriptor value) {
                    return value.getScope() instanceof GlobalSearchScope ? value.getDisplayName() : "";
                }

                @Override
                public boolean isSelectable(ScopeDescriptor value) {
                    return value.getScope() instanceof GlobalSearchScope;
                }
            };
            ScopeDescriptor selection = getSelectedScope();
            step.setDefaultOptionIndex(ContainerUtil.indexOf(items, o -> Comparing.equal(o.getDisplayName(), selection.getDisplayName())));
            ListPopupImpl popup = new ListPopupImpl(context.getData(Project.KEY), step);
            popup.setMaxRowCount(10);
            //noinspection unchecked
            popup.getList().setCellRenderer(renderer);
            return popup;
        }

        @Nonnull
        @Override
        protected ActionGroup createPopupActionGroup(JComponent button) {
            throw new UnsupportedOperationException();
        }
    }
}
