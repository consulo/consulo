// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.find.ui;

import consulo.application.util.function.Processor;
import consulo.component.util.WeighedItem;
import consulo.content.scope.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.find.internal.FindApiInternal;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import consulo.ui.ex.awt.JBUIScale;
import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.Promise;
import consulo.util.lang.BitUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ScopeChooserCombo extends ComboboxWithBrowseButton implements Disposable {
    public static final int OPT_LIBRARIES = 0x1;
    public static final int OPT_SEARCH_RESULTS = 0x2;
    public static final int OPT_FROM_SELECTION = 0x4;
    public static final int OPT_USAGE_VIEW = 0x8;
    public static final int OPT_EMPTY_SCOPES = 0x10;

    private Project myProject;
    private int myOptions = OPT_FROM_SELECTION | OPT_USAGE_VIEW;
    private Predicate<? super ScopeDescriptor> myScopeFilter;
    private BrowseListener myBrowseListener = null;

    public ScopeChooserCombo() {
        super(new MyComboBox());
    }

    public ScopeChooserCombo(final Project project, boolean suggestSearchInLibs, boolean prevSearchWholeFiles, String preselect) {
        this();
        init(project, suggestSearchInLibs, prevSearchWholeFiles, preselect);
    }

    public void init(final Project project, final String preselect) {
        init(project, false, true, preselect);
    }

    public void init(final Project project, final boolean suggestSearchInLibs, final boolean prevSearchWholeFiles, final String preselect) {
        init(project, suggestSearchInLibs, prevSearchWholeFiles, preselect, null);
    }

    public void init(
        final Project project,
        final boolean suggestSearchInLibs,
        final boolean prevSearchWholeFiles,
        final Object selection,
        @Nullable Predicate<? super ScopeDescriptor> scopeFilter
    ) {
        if (myProject != null) {
            throw new IllegalStateException("scope chooser combo already initialized");
        }
        myOptions = BitUtil.set(myOptions, OPT_LIBRARIES, suggestSearchInLibs);
        myOptions = BitUtil.set(myOptions, OPT_SEARCH_RESULTS, prevSearchWholeFiles);
        myProject = project;

        NamedScopesHolder.ScopeListener scopeListener = () -> {
            SearchScope selectedScope = getSelectedScope();
            rebuildModelAndSelectScopeOnSuccess(selectedScope);
        };
        myScopeFilter = scopeFilter;
        NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(project);
        for (NamedScopesHolder holder : holders) {
            holder.addScopeListener(scopeListener, this);
        }
        addActionListener(e -> handleScopeChooserAction());

        ComboBox<ScopeDescriptor> combo = getComboBox();
        combo.setMinimumAndPreferredWidth(JBUIScale.scale(300));
        combo.setRenderer(createDefaultRenderer());

        rebuildModelAndSelectScopeOnSuccess(selection);
    }

    @Nonnull
    public static ListCellRenderer<ScopeDescriptor> createDefaultRenderer() {
        return new MyRenderer();
    }

    @Override
    public ComboBox<ScopeDescriptor> getComboBox() {
        //noinspection unchecked
        return (ComboBox<ScopeDescriptor>)super.getComboBox();
    }

    public void setBrowseListener(BrowseListener browseListener) {
        myBrowseListener = browseListener;
    }

    public void setCurrentSelection(boolean currentSelection) {
        myOptions = BitUtil.set(myOptions, OPT_FROM_SELECTION, currentSelection);
    }

    public void setUsageView(boolean usageView) {
        myOptions = BitUtil.set(myOptions, OPT_USAGE_VIEW, usageView);
    }

    public void selectItem(@Nullable Object selection) {
        if (selection == null) {
            return;
        }
        JComboBox combo = getComboBox();
        DefaultComboBoxModel model = (DefaultComboBoxModel)combo.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            ScopeDescriptor descriptor = (ScopeDescriptor)model.getElementAt(i);
            if (selection instanceof String && selection.equals(descriptor.getDisplayName())
                || selection instanceof SearchScope && descriptor.scopeEquals((SearchScope)selection)) {
                combo.setSelectedIndex(i);
                break;
            }
        }
    }

    @RequiredUIAccess
    private void handleScopeChooserAction() {
        String selectedScopeName = getSelectedScopeName();
        SearchScope selectedScope = getSelectedScope();

        if (myBrowseListener != null) {
            myBrowseListener.onBeforeBrowseStarted();
        }

        FindApiInternal findApiInternal = myProject.getApplication().getInstance(FindApiInternal.class);
        findApiInternal.openScopeConfigurable(myProject, selectedScopeName).doWhenDone(() -> {
            rebuildModelAndSelectScopeOnSuccess(selectedScope);

            if (myBrowseListener != null) {
                myBrowseListener.onAfterBrowseFinished();
            }
        });
    }

    public static boolean processScopes(
        @Nonnull Project project,
        @Nonnull DataContext dataContext,
        @MagicConstant(flagsFromClass = ScopeChooserCombo.class) int options,
        @Nonnull Predicate<? super ScopeDescriptor> processor
    ) {
        List<SearchScope> predefinedScopes = PredefinedSearchScopeProvider.getInstance().getPredefinedScopes(
            project,
            dataContext,
            BitUtil.isSet(options, OPT_LIBRARIES),
            BitUtil.isSet(options, OPT_SEARCH_RESULTS),
            BitUtil.isSet(options, OPT_FROM_SELECTION),
            BitUtil.isSet(options, OPT_USAGE_VIEW),
            BitUtil.isSet(options, OPT_EMPTY_SCOPES)
        );
        for (SearchScope searchScope : predefinedScopes) {
            if (!processor.test(new ScopeDescriptor(searchScope))) {
                return false;
            }
        }
        for (ScopeDescriptorProvider provider : ScopeDescriptorProvider.EP_NAME.getExtensionList()) {
            for (ScopeDescriptor descriptor : provider.getScopeDescriptors(project)) {
                if (!processor.test(descriptor)) {
                    return false;
                }
            }
        }
        Comparator<SearchScope> comparator = (o1, o2) -> {
            int w1 = o1 instanceof WeighedItem ? ((WeighedItem)o1).getWeight() : Integer.MAX_VALUE;
            int w2 = o2 instanceof WeighedItem ? ((WeighedItem)o2).getWeight() : Integer.MAX_VALUE;
            if (w1 == w2) {
                return StringUtil.naturalCompare(o1.getDisplayName(), o2.getDisplayName());
            }
            return w1 - w2;
        };
        for (SearchScopeProvider each : SearchScopeProvider.EP_NAME.getExtensionList()) {
            if (StringUtil.isEmpty(each.getDisplayName())) {
                continue;
            }
            List<SearchScope> scopes = each.getSearchScopes(project);
            if (scopes.isEmpty()) {
                continue;
            }
            if (!processor.test(new ScopeSeparator(each.getDisplayName()))) {
                return false;
            }
            for (SearchScope scope : ContainerUtil.sorted(scopes, comparator)) {
                if (!processor.test(new ScopeDescriptor(scope))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void rebuildModelAndSelectScopeOnSuccess(@Nullable Object selection) {
        DefaultComboBoxModel<ScopeDescriptor> model = new DefaultComboBoxModel<>();
        Promise<DataContext> promise = DataManager.getInstance().getDataContextFromFocusAsync();
        promise.onSuccess(c -> {
            processScopes(myProject, c, myOptions, descriptor -> {
                if (myScopeFilter == null || myScopeFilter.test(descriptor)) {
                    model.addElement(descriptor);
                }
                return true;
            });
            getComboBox().setModel(model);
            selectItem(selection);
        });
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Dimension preferredSize = super.getPreferredSize();
        return new Dimension(Math.min(400, preferredSize.width), preferredSize.height);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        Dimension minimumSize = super.getMinimumSize();
        return new Dimension(Math.min(200, minimumSize.width), minimumSize.height);
    }

    public void setShowEmptyScopes(boolean showEmptyScopes) {
        myOptions = BitUtil.set(myOptions, OPT_EMPTY_SCOPES, showEmptyScopes);
    }

    @Nullable
    public SearchScope getSelectedScope() {
        ScopeDescriptor item = (ScopeDescriptor)getComboBox().getSelectedItem();
        return item == null ? null : item.getScope();
    }

    @Nullable
    public String getSelectedScopeName() {
        ScopeDescriptor item = (ScopeDescriptor)getComboBox().getSelectedItem();
        return item == null ? null : item.getDisplayName();
    }

    private static class ScopeSeparator extends ScopeDescriptor {
        final String text;

        ScopeSeparator(@Nonnull String text) {
            super(null);
            this.text = text;
        }

        @Override
        public String getDisplayName() {
            return text;
        }
    }

    private static class MyRenderer extends ColoredListCellRenderer<ScopeDescriptor> {
        @Override
        protected void customizeCellRenderer(
            @Nonnull JList<? extends ScopeDescriptor> list,
            ScopeDescriptor value,
            int index,
            boolean selected,
            boolean hasFocus
        ) {
            if (value instanceof ScopeSeparator) {
                setSeparator(value.getDisplayName());
            }
            else {
                if (value == null) {
                    return;
                }

                setIcon(value.getIcon());
                append(ObjectUtil.notNull(value.getDisplayName(), value.getClass().getSimpleName()));
            }
        }
    }

    public interface BrowseListener {
        void onBeforeBrowseStarted();

        void onAfterBrowseFinished();
    }

    private static class MyComboBox extends ComboBox {

        @Override
        public void setSelectedItem(Object item) {
            if (!(item instanceof ScopeSeparator)) {
                super.setSelectedItem(item);
            }
        }

        @Override
        public void setSelectedIndex(final int anIndex) {
            Object item = getItemAt(anIndex);
            if (!(item instanceof ScopeSeparator)) {
                super.setSelectedIndex(anIndex);
            }
        }
    }
}
