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

import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
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
import consulo.versionControlSystem.log.impl.internal.ui.AbstractVcsLogUi;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogRootFilterImpl;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class VcsLogClassicFilterUi implements VcsLogFilterUi {
    private static final String HASH_PATTERN = "[a-fA-F0-9]{7,}";
    private static final Logger LOG = Logger.getInstance(VcsLogClassicFilterUi.class);

    private final AbstractVcsLogUi myUi;

    
    private final VcsLogDataImpl myLogData;
    
    private final MainVcsLogUiProperties myUiProperties;

    
    private VcsLogDataPack myDataPack;

    
    private final BranchFilterModel myBranchFilterModel;
    
    private final FilterModel<VcsLogUserFilter> myUserFilterModel;
    
    private final FilterModel<VcsLogDateFilter> myDateFilterModel;
    
    private final FilterModel<VcsLogFileFilter> myStructureFilterModel;
    
    private final TextFilterModel myTextFilterModel;

    public VcsLogClassicFilterUi(
        AbstractVcsLogUi ui,
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

    /**
     * The filters of the Log toolbar, without widgets: each frontend shows them as it can.
     */
    public List<FilterPopup<?>> createFilterPopups() {
        return List.of(
            new BranchFilterPopup(myUiProperties, myBranchFilterModel),
            new UserFilterPopup(myUiProperties, myLogData, myUserFilterModel),
            new DateFilterPopup(myDateFilterModel),
            new StructureFilterPopup(myStructureFilterModel, myUi.getColorManager())
        );
    }

    public String getTextFilterText() {
        return myTextFilterModel.getText();
    }

    public void setUnsavedTextFilterText(String text) {
        myTextFilterModel.setUnsavedText(text);
    }

    public boolean hasUnsavedTextFilterChanges() {
        return myTextFilterModel.hasUnsavedChanges();
    }

    @RequiredUIAccess
    public void applyTextFilter(String text) {
        if (StringUtil.isEmptyOrSpaces(text)) {
            myTextFilterModel.setFilter(null);
            return;
        }
        myTextFilterModel.setFilter(new VcsLogTextFilterImpl(
            text,
            myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
            myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)
        ));
    }

    public void addTextFilterListener(Runnable listener) {
        myTextFilterModel.addSetFilterListener(listener);
    }

    public void addFilterListener(Runnable listener) {
        myBranchFilterModel.addSetFilterListener(listener);
        myUserFilterModel.addSetFilterListener(listener);
        myDateFilterModel.addSetFilterListener(listener);
        myStructureFilterModel.addSetFilterListener(listener);
        myTextFilterModel.addSetFilterListener(listener);
    }

    @RequiredUIAccess
    public void setFilters(VcsLogFilterCollection collection) {
        UIAccess.assertIsUIThread();
        myBranchFilterModel.setFilters(collection.getBranchFilter(), collection.getRangeFilter());
        myUserFilterModel.setFilter(collection.getUserFilter());
        myDateFilterModel.setFilter(collection.getDateFilter());

        VcsLogHashFilter hashFilter = collection.getHashFilter();
        if (hashFilter != null) {
            myTextFilterModel.setFilter(new VcsLogTextFilterImpl(String.join(" ", hashFilter.getHashes()), false, false));
        }
        else {
            myTextFilterModel.setFilter(collection.getTextFilter());
        }

        VcsLogStructureFilter structureFilter = collection.getStructureFilter();
        VcsLogRootFilter rootFilter = collection.getRootFilter();
        myStructureFilterModel.setFilter(
            structureFilter == null && rootFilter == null ? null : new VcsLogFileFilter(structureFilter, rootFilter)
        );
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
            .with(myBranchFilterModel.getRangeFilter())
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
        else if (filter instanceof VcsLogRootFilter rootFilter) {
            myStructureFilterModel.setFilter(new VcsLogFileFilter(null, rootFilter));
        }
    }

    public static class BranchFilterModel extends FilterModel<VcsLogBranchFilter> {
        private @Nullable Collection<VirtualFile> myVisibleRoots;
        private @Nullable VcsLogRangeFilter myRangeFilter;

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

        public @Nullable Collection<VirtualFile> getVisibleRoots() {
            return myVisibleRoots;
        }

        @Override
        void setFilter(@Nullable VcsLogBranchFilter filter) {
            setFilters(filter, null);
        }

        void setFilters(@Nullable VcsLogBranchFilter branchFilter, @Nullable VcsLogRangeFilter rangeFilter) {
            myRangeFilter = rangeFilter;
            super.setFilter(branchFilter);
        }

        public @Nullable VcsLogRangeFilter getRangeFilter() {
            return myRangeFilter;
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
        private @Nullable String myText;

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

        @Override
        protected @Nullable VcsLogFileFilter getLastFilter() {
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

        private @Nullable VcsLogRootFilter createRootsFilter(List<String> values) {
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

        @Override
        protected @Nullable VcsLogDateFilter createFilter(List<String> values) {
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
}
