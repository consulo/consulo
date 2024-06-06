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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.application.util.function.Computable;
import consulo.ide.impl.idea.openapi.util.NotNullComputable;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.SearchTextField;
import consulo.ui.ex.awt.SearchTextFieldWithStoredHistory;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.*;
import consulo.ide.impl.idea.vcs.log.data.*;
import consulo.ide.impl.idea.vcs.log.impl.*;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogFilterCollectionImpl.VcsLogFilterCollectionBuilder;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogActionPlaces;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.util.*;

/**
 */
public class VcsLogClassicFilterUi implements VcsLogFilterUi {
  private static final String VCS_LOG_TEXT_FILTER_HISTORY = "Vcs.Log.Text.Filter.History";

  private static final String HASH_PATTERN = "[a-fA-F0-9]{7,}";
  private static final Logger LOG = Logger.getInstance(VcsLogClassicFilterUi.class);

  @Nonnull
  private final VcsLogUiImpl myUi;

  @Nonnull
  private final VcsLogDataImpl myLogData;
  @Nonnull
  private final MainVcsLogUiProperties myUiProperties;

  @Nonnull
  private VcsLogDataPack myDataPack;

  @Nonnull
  private final BranchFilterModel myBranchFilterModel;
  @Nonnull
  private final FilterModel<VcsLogUserFilter> myUserFilterModel;
  @Nonnull
  private final FilterModel<VcsLogDateFilter> myDateFilterModel;
  @Nonnull
  private final FilterModel<VcsLogFileFilter> myStructureFilterModel;
  @Nonnull
  private final TextFilterModel myTextFilterModel;

  public VcsLogClassicFilterUi(@Nonnull VcsLogUiImpl ui,
                               @Nonnull VcsLogDataImpl logData,
                               @Nonnull MainVcsLogUiProperties uiProperties,
                               @Nonnull VcsLogDataPack initialDataPack) {
    myUi = ui;
    myLogData = logData;
    myUiProperties = uiProperties;
    myDataPack = initialDataPack;

    NotNullComputable<VcsLogDataPack> dataPackGetter = () -> myDataPack;
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

  public void updateDataPack(@Nonnull VcsLogDataPack dataPack) {
    myDataPack = dataPack;
  }

  @Nonnull
  public SearchTextField createTextFilter() {
    return new TextFilterField(myTextFilterModel);
  }

  /**
   * Returns filter components which will be added to the Log toolbar.
   */
  @Nonnull
  public ActionGroup createActionGroup() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(new FilterActionComponent(() -> new BranchFilterPopupComponent(myUi, myUiProperties, myBranchFilterModel).initUi()));
    actionGroup.add(new FilterActionComponent(() -> new UserFilterPopupComponent(myUiProperties, myLogData, myUserFilterModel).initUi()));
    actionGroup.add(new FilterActionComponent(() -> new DateFilterPopupComponent(myDateFilterModel).initUi()));
    actionGroup.add(new FilterActionComponent(
            () -> new StructureFilterPopupComponent(myStructureFilterModel, myUi.getColorManager()).initUi()));
    return actionGroup;
  }

  @Nonnull
  @Override
  public VcsLogFilterCollection getFilters() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Pair<VcsLogTextFilter, VcsLogHashFilter> filtersFromText =
            getFiltersFromTextArea(myTextFilterModel.getFilter(),
                                   myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
                                   myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE));
    return new VcsLogFilterCollectionBuilder().with(myBranchFilterModel.getFilter())
            .with(myUserFilterModel.getFilter())
            .with(filtersFromText.second)
            .with(myDateFilterModel.getFilter())
            .with(filtersFromText.first)
            .with(myStructureFilterModel.getFilter() == null
                  ? null
                  : myStructureFilterModel.getFilter().getStructureFilter())
            .with(myStructureFilterModel.getFilter() == null
                  ? null
                  : myStructureFilterModel.getFilter().getRootFilter()).build();
  }

  @Nonnull
  private static Pair<VcsLogTextFilter, VcsLogHashFilter> getFiltersFromTextArea(@Nullable VcsLogTextFilter filter,
                                                                                 boolean isRegexAllowed,
                                                                                 boolean matchesCase) {
    if (filter == null) {
      return Pair.empty();
    }
    String text = filter.getText().trim();
    if (StringUtil.isEmptyOrSpaces(text)) {
      return Pair.empty();
    }
    List<String> hashes = ContainerUtil.newArrayList();
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
  public void setFilter(@Nonnull VcsLogFilter filter) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (filter instanceof VcsLogBranchFilter) {
      myBranchFilterModel.setFilter((VcsLogBranchFilter)filter);
    }
    else if (filter instanceof VcsLogStructureFilter) {
      myStructureFilterModel.setFilter(new VcsLogFileFilter((VcsLogStructureFilter)filter, null));
    }
    JComponent toolbar = myUi.getToolbar();
    toolbar.revalidate();
    toolbar.repaint();
  }

  private static class FilterActionComponent extends DumbAwareAction implements CustomComponentAction {

    @Nonnull
    private final Computable<JComponent> myComponentCreator;

    public FilterActionComponent(@Nonnull Computable<JComponent> componentCreator) {
      myComponentCreator = componentCreator;
    }

    @Override
    public JComponent createCustomComponent(Presentation presentation, String place) {
      return myComponentCreator.compute();
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }
  }

  public static class BranchFilterModel extends FilterModel<VcsLogBranchFilter> {
    @Nullable
    private Collection<VirtualFile> myVisibleRoots;

    BranchFilterModel(@Nonnull Computable<VcsLogDataPack> provider, @Nonnull MainVcsLogUiProperties properties) {
      super("branch", provider, properties);
    }

    public void onStructureFilterChanged(@Nonnull Set<VirtualFile> roots, @Nullable VcsLogFileFilter filter) {
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

    @Nonnull
    @Override
    protected VcsLogBranchFilter createFilter(@Nonnull List<String> values) {
      return VcsLogBranchFilterImpl
              .fromTextPresentation(values, ContainerUtil.map2Set(getDataPack().getRefs().getBranches(), VcsRef::getName));
    }

    @Nonnull
    @Override
    protected List<String> getFilterValues(@Nonnull VcsLogBranchFilter filter) {
      return ContainerUtil.newArrayList(ContainerUtil.sorted(filter.getTextPresentation()));
    }
  }

  private static class TextFilterModel extends FilterModel<VcsLogTextFilter> {
    @Nullable private String myText;

    public TextFilterModel(NotNullComputable<VcsLogDataPack> dataPackProvider, @Nonnull MainVcsLogUiProperties properties) {
      super("text", dataPackProvider, properties);
    }

    @Nonnull
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

    void setUnsavedText(@Nonnull String text) {
      myText = text;
    }

    boolean hasUnsavedChanges() {
      if (myText == null) return false;
      return getFilter() == null || !myText.equals(getFilter().getText());
    }

    @Override
    void setFilter(@Nullable VcsLogTextFilter filter) {
      super.setFilter(filter);
      myText = null;
    }

    @Nonnull
    @Override
    protected VcsLogTextFilter createFilter(@Nonnull List<String> values) {
      return new VcsLogTextFilterImpl(ObjectUtil.assertNotNull(ContainerUtil.getFirstItem(values)),
                                      myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
                                      myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE));
    }

    @Nonnull
    @Override
    protected List<String> getFilterValues(@Nonnull VcsLogTextFilter filter) {
      return Collections.singletonList(filter.getText());
    }
  }

  private static class FileFilterModel extends FilterModel<VcsLogFileFilter> {
    @Nonnull
    private static final String ROOTS = "roots";
    @Nonnull
    private static final String STRUCTURE = "structure";
    @Nonnull
    private final Set<VirtualFile> myRoots;

    public FileFilterModel(NotNullComputable<VcsLogDataPack> dataPackGetter,
                           @Nonnull Set<VirtualFile> roots,
                           MainVcsLogUiProperties uiProperties) {
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

    @Nonnull
    private static List<String> getFilterValues(@Nonnull VcsLogStructureFilter filter) {
      return ContainerUtil.map(filter.getFiles(), FilePath::getPath);
    }

    @Nonnull
    private static List<String> getFilterValues(@Nonnull VcsLogRootFilter filter) {
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
    private VcsLogRootFilter createRootsFilter(@Nonnull List<String> values) {
      List<VirtualFile> selectedRoots = ContainerUtil.newArrayList();
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
      if (selectedRoots.isEmpty()) return null;
      return new VcsLogRootFilterImpl(selectedRoots);
    }

    @Nonnull
    private static VcsLogStructureFilter createStructureFilter(@Nonnull List<String> values) {
      return new VcsLogStructureFilterImpl(ContainerUtil.map(values, VcsUtil::getFilePath));
    }

    @Nonnull
    @Override
    protected VcsLogFileFilter createFilter(@Nonnull List<String> values) {
      throw new UnsupportedOperationException("Can not create file filter from list of strings");
    }

    @Nonnull
    @Override
    protected List<String> getFilterValues(@Nonnull VcsLogFileFilter filter) {
      throw new UnsupportedOperationException("Can not save file filter to a list of strings");
    }
  }

  private static class DateFilterModel extends FilterModel<VcsLogDateFilter> {
    public DateFilterModel(NotNullComputable<VcsLogDataPack> dataPackGetter, MainVcsLogUiProperties uiProperties) {
      super("date", dataPackGetter, uiProperties);
    }

    @Nullable
    @Override
    protected VcsLogDateFilter createFilter(@Nonnull List<String> values) {
      if (values.size() != 2) {
        LOG.warn("Can not create date filter from " + values + " before and after dates are required.");
        return null;
      }
      String after = values.get(0);
      String before = values.get(1);
      try {
        return new VcsLogDateFilterImpl(after.isEmpty() ? null : new Date(Long.parseLong(after)),
                                        before.isEmpty() ? null : new Date(Long.parseLong(before)));
      }
      catch (NumberFormatException e) {
        LOG.warn("Can not create date filter from " + values);
      }
      return null;
    }

    @Nonnull
    @Override
    protected List<String> getFilterValues(@Nonnull VcsLogDateFilter filter) {
      Date after = filter.getAfter();
      Date before = filter.getBefore();
      return Arrays.asList(after == null ? "" : Long.toString(after.getTime()),
                           before == null ? "" : Long.toString(before.getTime()));
    }
  }

  private class UserFilterModel extends FilterModel<VcsLogUserFilter> {
    public UserFilterModel(NotNullComputable<VcsLogDataPack> dataPackGetter, MainVcsLogUiProperties uiProperties) {
      super("user", dataPackGetter, uiProperties);
    }

    @Nonnull
    @Override
    protected VcsLogUserFilter createFilter(@Nonnull List<String> values) {
      return new VcsLogUserFilterImpl(values, myLogData.getCurrentUser(), myLogData.getAllUsers());
    }

    @Nonnull
    @Override
    protected List<String> getFilterValues(@Nonnull VcsLogUserFilter filter) {
      return ContainerUtil.newArrayList(((VcsLogUserFilterImpl)filter).getUserNamesForPresentation());
    }
  }

  private static class TextFilterField extends SearchTextFieldWithStoredHistory {
    @Nonnull
    private final TextFilterModel myTextFilterModel;

    public TextFilterField(@Nonnull TextFilterModel model) {
      super(VCS_LOG_TEXT_FILTER_HISTORY);
      myTextFilterModel = model;
      setText(myTextFilterModel.getText());
      getTextEditor().addActionListener(e -> applyFilter());
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
      myTextFilterModel.setFilter(new VcsLogTextFilterImpl(getText(),
                                                           myTextFilterModel.myUiProperties.get(MainVcsLogUiProperties.TEXT_FILTER_REGEX),
                                                           myTextFilterModel.myUiProperties
                                                                   .get(MainVcsLogUiProperties.TEXT_FILTER_MATCH_CASE)));
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
