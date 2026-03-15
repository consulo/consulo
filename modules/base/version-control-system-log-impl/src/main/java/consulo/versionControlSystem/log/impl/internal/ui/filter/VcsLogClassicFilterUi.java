/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.application.util.function.Computable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.SearchTextField;
import consulo.ui.ex.awt.SearchTextFieldWithStoredHistory;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogHashFilterImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogUserFilterImpl;
import consulo.versionControlSystem.log.impl.internal.data.*;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogRootFilterImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.util.*;
import java.util.function.Supplier;

public class VcsLogClassicFilterUi implements VcsLogFilterUi {
    private static final String VCS_LOG_TEXT_FILTER_HISTORY = "Vcs.Log.Text.Filter.History";

    private static final String HASH_PATTERN = "[a-fA-F0-9]{7,}";
    private static final Logger LOG = Logger.getInstance(VcsLogClassicFilterUi.class);

    
    private final VcsLogUiImpl myUi;

    
    private final VcsLogDataImpl myLogData;
    
    private final MainVcsLogUiProperties myUiProperties;

    
    private VcsLogDataPack myDataPack;

    
    private final BranchFilterModel myBranchFilterModel;
    
    private final FilterModel<VcsLogUserFilter> myUserFilterModel;
    
    private final FilterModel<VcsLogDateFilter> myDateFilterModel;
    
    private final FilterModel<VcsLogFileFilter> myStructureFilterModel;
    
    private final TextFilterModel myTextFilterModel;

    public VcsLogClassicFilterUi(
        VcsLogUiImpl ui,
        VcsLogDataImpl logData,
        MainVcsLogUiProperties uiProperties,
        VcsLogDataPack initialDataPack
    ) {
        myUi = ui;
        myLogData = logData;
        myUiProperties = uiProperties;
        myDataPack = initialDataPack;

        Supplier<VcsLogDataPack> dataPackGetter = () -> myDataPack;
        myBranchFilterModel = new BranchFilterModel(dataPackGetter, myUiProperties);
        myUserFilterModel = new UserFilterModel(dataPackGetter, uiProperties);
        myDateFilterModel = new DateFilterModel(dataPackGetter, uiProperties);
        myStructureFilterModel = new FileFilterModel(dataPackGetter, myLogData.getLogProviders().keySet(), uiProperties);
        myTextFilterModel = new TextFilterModel(dataPackGetter, myUiProperties);

        updateUiOnFilterChange();
        myUi.applyFiltersAndUpdateUi(getFilters());
    }

    private void updateUiOnFilterChange() {
        FilterModel[] models = {myBranchFilterModel, myUserFilterModel, myDateFilterModel, myStructureFilterModel, myTextFilterModel};
        for (FilterModel<?> model : models) {
            model.addSetFilterListener(() -> {
                myUi.applyFiltersAndUpdateUi(getFilters());
                myBranchFilterModel
                    .onStructureFilterChanged(new HashSet<>(myLogData.getRoots()), myStructureFilterModel.getFilter());
            });
        }
    }

    public void updateDataPack(VcsLogDataPack dataPack) {
        myDataPack = dataPack;
    }

    
    public SearchTextField createTextFilter() {
        return new TextFilterField(myTextFilterModel);
    }

    /**
     * Returns filter components which will be added to the Log toolbar.
     */
    
    public ActionGroup createActionGroup() {
        ActionGroup.Builder actionGroup = ActionGroup.newImmutableBuilder();
        actionGroup.add(new FilterActionComponent(() -> new BranchFilterPopupComponent(
            myUi,
            myUiProperties,
            myBranchFilterModel
        ).initUi()));
        actionGroup.add(new FilterActionComponent(() -> new UserFilterPopupComponent(
            myUiProperties,
            myLogData,
            myUserFilterModel
        ).initUi()));
        actionGroup.add(new FilterActionComponent(() -> new DateFilterPopupComponent(myDateFilterModel).initUi()));
        actionGroup.add(
            new FilterActionComponent(() -> new StructureFilterPopupComponent(myStructureFilterModel, myUi.getColorManager()).initUi())
        );
        return actionGroup.build();
    }

    
    @Override
    @RequiredUIAccess
    public VcsLogFilterCollection getFilters() {
        UIAccess.assertIsUIThread();
        Pair<VcsLogTextFilter, VcsLogHashFilter> filtersFromText = getFiltersFromTextArea(
            myTextFilterModel.getFilter(),
            myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
            myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)
        );
        return new VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder()
            .with(myBranchFilterModel.getFilter())
            .with(myUserFilterModel.getFilter())
            .with(filtersFromText.second)
            .with(myDateFilterModel.getFilter())
            .with(filtersFromText.first)
            .with(myStructureFilterModel.getFilter() == null ? null : myStructureFilterModel.getFilter().getStructureFilter())
            .with(myStructureFilterModel.getFilter() == null ? null : myStructureFilterModel.getFilter().getRootFilter())
            .build();
    }

    
    private static Pair<VcsLogTextFilter, VcsLogHashFilter> getFiltersFromTextArea(
        @Nullable VcsLogTextFilter filter,
        boolean isRegexAllowed,
        boolean matchesCase
    ) {
        if (filter == null) {
            return Pair.empty();
        }
        String text = filter.getText().trim();
        if (StringUtil.isEmptyOrSpaces(text)) {
            return Pair.empty();
        }
        List<String> hashes = new ArrayList<>();
        for (String word : StringUtil.split(text, " ")) {
            if (!StringUtil.isEmptyOrSpaces(word) && word.matches(HASH_PATTERN)) {
                hashes.add(word);
            }
            else {
                break;
            }
        }

        VcsLogTextFilter textFilter;
        VcsLogHashFilterImpl hashFilter;
        if (!hashes.isEmpty()) { // text is ignored if there are hashes in the text
            textFilter = null;
            hashFilter = new VcsLogHashFilterImpl(hashes);
        }
        else {
            textFilter = new VcsLogTextFilterImpl(text, isRegexAllowed, matchesCase);
            hashFilter = null;
        }
        return Pair.<VcsLogTextFilter, VcsLogHashFilter>create(textFilter, hashFilter);
    }

    @Override
    @RequiredUIAccess
    public void setFilter(VcsLogFilter filter) {
        UIAccess.assertIsUIThread();
        if (filter instanceof VcsLogBranchFilter) {
            myBranchFilterModel.setFilter((VcsLogBranchFilter) filter);
        }
        else if (filter instanceof VcsLogStructureFilter) {
            myStructureFilterModel.setFilter(new VcsLogFileFilter((VcsLogStructureFilter) filter, null));
        }
        JComponent toolbar = myUi.getToolbar();
        toolbar.revalidate();
        toolbar.repaint();
    }

    private static class FilterActionComponent extends DumbAwareAction implements CustomComponentAction {
        
        private final Computable<JComponent> myComponentCreator;

        public FilterActionComponent(Computable<JComponent> componentCreator) {
            myComponentCreator = componentCreator;
        }

        
        @Override
        public JComponent createCustomComponent(Presentation presentation, String place) {
            return myComponentCreator.compute();
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
        }
    }

    public static class BranchFilterModel extends FilterModel<VcsLogBranchFilter> {
        @Nullable
        private Collection<VirtualFile> myVisibleRoots;

        BranchFilterModel(Supplier<VcsLogDataPack> provider, MainVcsLogUiProperties properties) {
            super("branch", provider, properties);
        }

        public void onStructureFilterChanged(Set<VirtualFile> roots, @Nullable VcsLogFileFilter filter) {
            if (filter == null) {
                myVisibleRoots = null;
            }
            else {
                myVisibleRoots = VcsLogUtil.getAllVisibleRoots(roots, filter.getRootFilter(), filter.getStructureFilter());
            }
        }

        @Nullable
        public Collection<VirtualFile> getVisibleRoots() {
            return myVisibleRoots;
        }

        
        @Override
        protected VcsLogBranchFilter createFilter(List<String> values) {
            return VcsLogBranchFilterImpl
                .fromTextPresentation(values, ContainerUtil.map2Set(getDataPack().getRefs().getBranches(), VcsRef::getName));
        }

        
        @Override
        protected List<String> getFilterValues(VcsLogBranchFilter filter) {
            return ContainerUtil.newArrayList(ContainerUtil.sorted(filter.getTextPresentation()));
        }
    }

    private static class TextFilterModel extends FilterModel<VcsLogTextFilter> {
        @Nullable
        private String myText;

        public TextFilterModel(Supplier<VcsLogDataPack> dataPackProvider, MainVcsLogUiProperties properties) {
            super("text", dataPackProvider, properties);
        }

        
        String getText() {
            if (myText != null) {
                return myText;
            }
            else if (getFilter() != null) {
                return getFilter().getText();
            }
            else {
                return "";
            }
        }

        void setUnsavedText(String text) {
            myText = text;
        }

        boolean hasUnsavedChanges() {
            return myText != null && (getFilter() == null || !myText.equals(getFilter().getText()));
        }

        @Override
        void setFilter(@Nullable VcsLogTextFilter filter) {
            super.setFilter(filter);
            myText = null;
        }

        
        @Override
        protected VcsLogTextFilter createFilter(List<String> values) {
            return new VcsLogTextFilterImpl(
                ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(values)),
                myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
                myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)
            );
        }

        
        @Override
        protected List<String> getFilterValues(VcsLogTextFilter filter) {
            return Collections.singletonList(filter.getText());
        }
    }

    private static class FileFilterModel extends FilterModel<VcsLogFileFilter> {
        
        private static final String ROOTS = "roots";
        
        private static final String STRUCTURE = "structure";
        
        private final Set<VirtualFile> myRoots;

        public FileFilterModel(
            Supplier<VcsLogDataPack> dataPackGetter,
            Set<VirtualFile> roots,
            MainVcsLogUiProperties uiProperties
        ) {
            super("file", dataPackGetter, uiProperties);
            myRoots = roots;
        }

        @Override
        protected void saveFilter(@Nullable VcsLogFileFilter filter) {
            if (filter == null) {
                myUiProperties.saveFilterValues(ROOTS, null);
                myUiProperties.saveFilterValues(STRUCTURE, null);
            }
            else if (filter.getStructureFilter() != null) {
                myUiProperties.saveFilterValues(STRUCTURE, getFilterValues(filter.getStructureFilter()));
            }
            else if (filter.getRootFilter() != null) {
                myUiProperties.saveFilterValues(ROOTS, getFilterValues(filter.getRootFilter()));
            }
        }

        
        private static List<String> getFilterValues(VcsLogStructureFilter filter) {
            return ContainerUtil.map(filter.getFiles(), FilePath::getPath);
        }

        
        private static List<String> getFilterValues(VcsLogRootFilter filter) {
            return ContainerUtil.map(filter.getRoots(), VirtualFile::getPath);
        }

        @Nullable
        @Override
        protected VcsLogFileFilter getLastFilter() {
            List<String> values = myUiProperties.getFilterValues(STRUCTURE);
            if (values != null) {
                return new VcsLogFileFilter(createStructureFilter(values), null);
            }
            values = myUiProperties.getFilterValues(ROOTS);
            if (values != null) {
                return new VcsLogFileFilter(null, createRootsFilter(values));
            }
            return null;
        }

        @Nullable
        private VcsLogRootFilter createRootsFilter(List<String> values) {
            List<VirtualFile> selectedRoots = new ArrayList<>();
            for (String path : values) {
                VirtualFile root = LocalFileSystem.getInstance().findFileByPath(path);
                if (root != null) {
                    if (myRoots.contains(root)) {
                        selectedRoots.add(root);
                    }
                    else {
                        LOG.warn("Can not find VCS root for filtering " + root);
                    }
                }
                else {
                    LOG.warn("Can not filter by file that does not exist " + path);
                }
            }
            if (selectedRoots.isEmpty()) {
                return null;
            }
            return new VcsLogRootFilterImpl(selectedRoots);
        }

        
        private static VcsLogStructureFilter createStructureFilter(List<String> values) {
            return new VcsLogStructureFilterImpl(ContainerUtil.map(values, VcsUtil::getFilePath));
        }

        
        @Override
        protected VcsLogFileFilter createFilter(List<String> values) {
            throw new UnsupportedOperationException("Can not create file filter from list of strings");
        }

        
        @Override
        protected List<String> getFilterValues(VcsLogFileFilter filter) {
            throw new UnsupportedOperationException("Can not save file filter to a list of strings");
        }
    }

    private static class DateFilterModel extends FilterModel<VcsLogDateFilter> {
        public DateFilterModel(Supplier<VcsLogDataPack> dataPackGetter, MainVcsLogUiProperties uiProperties) {
            super("date", dataPackGetter, uiProperties);
        }

        @Nullable
        @Override
        protected VcsLogDateFilter createFilter(List<String> values) {
            if (values.size() != 2) {
                LOG.warn("Can not create date filter from " + values + " before and after dates are required.");
                return null;
            }
            String after = values.get(0);
            String before = values.get(1);
            try {
                return new VcsLogDateFilterImpl(
                    after.isEmpty() ? null : new Date(Long.parseLong(after)),
                    before.isEmpty() ? null : new Date(Long.parseLong(before))
                );
            }
            catch (NumberFormatException e) {
                LOG.warn("Can not create date filter from " + values);
            }
            return null;
        }

        
        @Override
        protected List<String> getFilterValues(VcsLogDateFilter filter) {
            Date after = filter.getAfter();
            Date before = filter.getBefore();
            return Arrays.asList(
                after == null ? "" : Long.toString(after.getTime()),
                before == null ? "" : Long.toString(before.getTime())
            );
        }
    }

    private class UserFilterModel extends FilterModel<VcsLogUserFilter> {
        public UserFilterModel(Supplier<VcsLogDataPack> dataPackGetter, MainVcsLogUiProperties uiProperties) {
            super("user", dataPackGetter, uiProperties);
        }

        
        @Override
        protected VcsLogUserFilter createFilter(List<String> values) {
            return new VcsLogUserFilterImpl(values, myLogData.getCurrentUser(), myLogData.getAllUsers());
        }

        
        @Override
        protected List<String> getFilterValues(VcsLogUserFilter filter) {
            return ContainerUtil.newArrayList(((VcsLogUserFilterImpl) filter).getUserNamesForPresentation());
        }
    }

    private static class TextFilterField extends SearchTextFieldWithStoredHistory {
        
        private final TextFilterModel myTextFilterModel;

        public TextFilterField(TextFilterModel model) {
            super(VCS_LOG_TEXT_FILTER_HISTORY);
            myTextFilterModel = model;
            setText(myTextFilterModel.getText());
            getTextEditor().addActionListener(e -> applyFilter());
            setPlaceholder(LocalizeValue.localizeTODO("Text or Hash"));

            addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(DocumentEvent e) {
                    try {
                        myTextFilterModel.setUnsavedText(e.getDocument().getText(0, e.getDocument().getLength()));
                    }
                    catch (BadLocationException ex) {
                        LOG.error(ex);
                    }
                }
            });
            
            String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(VcsLogActionPlaces.VCS_LOG_FOCUS_TEXT_FILTER);
            if (!shortcutText.isEmpty()) {
                getTextEditor().setToolTipText("Use " + shortcutText + " to switch between text filter and commits list");
            }
        }

        protected void applyFilter() {
            myTextFilterModel.setFilter(new VcsLogTextFilterImpl(
                getText(),
                myTextFilterModel.myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
                myTextFilterModel.myUiProperties
                    .get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)
            ));
            addCurrentTextToHistory();
        }

        @Override
        protected void onFieldCleared() {
            myTextFilterModel.setFilter(null);
        }

        @Override
        protected void onFocusLost() {
            if (myTextFilterModel.hasUnsavedChanges()) {
                applyFilter();
            }
        }
    }
}
