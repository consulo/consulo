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
package consulo.versionControlSystem.log.impl.internal.ui;

import consulo.application.util.DateFormatUtil;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.font.FontFeature;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesBrowserUtil;
import consulo.versionControlSystem.history.TextRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.log.CommitId;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.RefGroup;
import consulo.versionControlSystem.log.VcsCommitStyleFactory;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogCommitSelection;
import consulo.versionControlSystem.log.VcsLogDataKeys;
import consulo.versionControlSystem.log.VcsLogDataProvider;
import consulo.versionControlSystem.log.VcsLogHighlighter;
import consulo.versionControlSystem.log.VcsLogHighlighterFactory;
import consulo.versionControlSystem.log.VcsLogSidePanelProvider;
import consulo.versionControlSystem.log.VcsLogRefManager;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsShortCommitDetails;
import consulo.versionControlSystem.log.graph.PrintElement;
import consulo.versionControlSystem.log.graph.RowInfo;
import consulo.versionControlSystem.log.graph.RowType;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.versionControlSystem.log.graph.action.GraphAction;
import consulo.versionControlSystem.log.graph.action.GraphAnswer;
import consulo.versionControlSystem.log.graph.paint.VcsGraphMouseListener;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRowImageFactory;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDataImpl;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import consulo.versionControlSystem.log.impl.internal.graph.DefaultColorGenerator;
import consulo.versionControlSystem.log.impl.internal.ui.action.ChangesBrowserPresentationSettingsGroup;
import consulo.versionControlSystem.log.impl.internal.ui.action.GraphOptionsChooserAction;
import consulo.versionControlSystem.log.impl.internal.ui.action.RightCornerToolbarGroup;
import consulo.versionControlSystem.log.impl.internal.ui.filter.FilterPopup;
import consulo.versionControlSystem.log.impl.internal.ui.filter.VcsLogClassicFilterUi;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.paint.VcsRefLabel;
import consulo.versionControlSystem.log.paint.VcsRefLabelImageFactory;
import consulo.versionControlSystem.log.ui.VcsLogColorManager;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import consulo.versionControlSystem.versionBrowser.CommittedChangeListForRevision;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class UnifiedVcsLogUi extends AbstractVcsLogUi {
    private static final int ROOT_COLUMN_WIDTH = 8;
    private static final int REF_LABEL_SIZE = 16;
    private static final int TEXT_FILTER_LENGTH = 20;

    private final VcsLogCommitModel myCommitModel;
    private final UnifiedVcsLogRowModel myRowModel;
    private final VcsLogClassicFilterUi myFilterUi;
    private final Table<Integer> myTable;
    private final HtmlLabel myDetails;
    private final VcsChangesBrowser myChangesBrowser;
    private final DockLayout myRoot;
    private final List<VcsLogHighlighter> myHighlighters = new ArrayList<>();
    private final Map<VcsRefLabel, Image> myRefLabelImages = new HashMap<>();
    private final VcsGraphRowBuilder myGraphRowBuilder = new VcsGraphRowBuilder(new DefaultColorGenerator());
    private final VcsGraphRowImageFactory myGraphImageFactory;
    private final VcsRefLabelImageFactory myRefLabelImageFactory;

    private final Runnable myMiniDetailsLoadedListener;
    private final Runnable myContainingBranchesListener;

    private @Nullable VcsLogManager myManager;
    private @Nullable Object mySelectionRequest;
    private @Nullable TextBox myTextFilter;
    private final DockLayout myRightArea;
    private final TwoComponentSplitLayout myDetailsSplitter;
    private final Component myDetailsScroll;
    private int myHoveredRow = -1;
    private @Nullable PrintElement myHoveredElement;

    @RequiredUIAccess
    public UnifiedVcsLogUi(
        String id,
        VcsLogDataImpl logData,
        Project project,
        VcsLogColorManager manager,
        MainVcsLogUiProperties uiProperties,
        VcsLogFilterer filterer
    ) {
        super(id, logData, project, manager, uiProperties, filterer);

        myCommitModel = new VcsLogCommitModel(myVisiblePack, logData, filterer, () -> {
        });
        myRowModel = new UnifiedVcsLogRowModel(myCommitModel);
        myGraphImageFactory = project.getApplication().getInstance(VcsGraphRowImageFactory.class);
        myRefLabelImageFactory = project.getApplication().getInstance(VcsRefLabelImageFactory.class);
        myFilterUi = new VcsLogClassicFilterUi(this, logData, uiProperties, myVisiblePack);

        myTable = Table.create(myRowModel);
        myTable.setAllowMultipleSelect(true);
        myTable.setShowHeader(false);
        if (manager.isMultipleRoots()) {
            myTable.addColumn(LocalizeValue.empty(), row -> row)
                .setWidth(ROOT_COLUMN_WIDTH)
                .setResizable(false)
                .setRenderer((presentation, item) -> renderRoot(presentation, item.getValue()));
        }
        myTable.addColumn(VersionControlSystemLogLocalize.columnSubject(), row -> row)
            .setRenderer((presentation, item) -> renderSubject(presentation, item.getValue(), item.isSelected()));
        myTable.addColumn(VersionControlSystemLogLocalize.columnAuthor(), row -> row)
            .setWidth(160)
            .setRenderer((presentation, item) -> renderAuthor(presentation, item.getValue(), item.isSelected()));
        myTable.addColumn(VersionControlSystemLogLocalize.columnDate(), row -> row)
            .setWidth(150)
            .setFontBuilder(font -> font.withFeatures(FontFeature.TABULAR_NUMBERS))
            .setRenderer((presentation, item) -> renderDate(presentation, item.getValue(), item.isSelected()));
        myTable.addSelectListener(event -> onSelectionChanged());
        myTable.addContextMenuListener(this::showContextMenu);
        myTable.setSpeedSearchConverter(row -> row < myCommitModel.getRowCount() ? myCommitModel.getShortDetails(row).getSubject() : "");

        myDetails = HtmlLabel.create(LocalizeValue.empty());
        VerticalLayout detailsContent = VerticalLayout.create();
        detailsContent.add(myDetails);
        detailsContent.paddingBuilder().allSet(Space.LARGE).apply();

        myChangesBrowser = project.getApplication().getInstance(VcsChangesDialogFactory.class)
            .changesBrowser(project)
            .emptyText(VersionControlSystemLogLocalize.textNoCommitsSelected())
            .viewOptions(ActionManager.getInstance().getAction(ChangesBrowserPresentationSettingsGroup.class))
            .build(this);

        myDetailsSplitter = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL).withProportion(70);
        myDetailsScroll = ScrollableLayout.create(detailsContent);
        myRightArea = DockLayout.create(Space.NONE);

        DockLayout tableArea = DockLayout.create();
        tableArea.top(createToolbar());
        tableArea.center(ScrollableLayout.create(myTable));

        TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL).withProportion(70);
        splitter.setFirstComponent(tableArea);
        splitter.setSecondComponent(myRightArea);
        applyShowDetails();

        myRoot = DockLayout.create(Space.NONE);
        VcsLogSidePanelProvider sidePanelProvider = project.getExtensionPoint(VcsLogSidePanelProvider.class)
            .findFirstSafe(VcsLogSidePanelProvider::isAvailable);
        if (sidePanelProvider != null) {
            VcsLogSidePanelHost sidePanel = new VcsLogSidePanelHost(project, this, sidePanelProvider, splitter, this);
            myRoot.left(sidePanel.getStripe());
            myRoot.center(sidePanel.getComponent());
        }
        else {
            myRoot.center(splitter);
        }
        myRoot.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);

        project.getExtensionPoint(VcsLogHighlighterFactory.class)
            .forEach(factory -> myHighlighters.add(factory.createHighlighter(logData, this)));

        myMiniDetailsLoadedListener = () -> myProject.getUIAccess().giveIfNeed(myRowModel::fireAllUpdated);
        myContainingBranchesListener = () -> myProject.getUIAccess().giveIfNeed(myRowModel::fireAllUpdated);
        logData.getMiniDetailsGetter().addDetailsLoadedListener(myMiniDetailsLoadedListener);
        logData.getContainingBranchesGetter().addTaskCompletedListener(myContainingBranchesListener);

        startListening();
    }

    @RequiredUIAccess
    private Component createToolbar() {
        ActionManager actionManager = ActionManager.getInstance();

        ActionGroup.Builder mainGroup = ActionGroup.newImmutableBuilder();
        List<FilterPopup<?>> filters = myFilterUi.createFilterPopups();
        for (FilterPopup<?> filter : filters) {
            mainGroup.add(new VcsLogFilterComboBoxAction(filter));
        }
        mainGroup.add(actionManager.getAction(GraphOptionsChooserAction.class));
        mainGroup.addSeparator();
        AnAction toolbarGroup = actionManager.getAction(VcsLogActionPlaces.TOOLBAR_ACTION_GROUP);
        if (toolbarGroup != null) {
            mainGroup.add(toolbarGroup);
        }

        ActionToolbar main = ActionToolbarFactory.getInstance()
            .createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, mainGroup.build(), ActionToolbar.Style.HORIZONTAL);
        main.setTargetUIComponent(myTable);
        for (FilterPopup<?> filter : filters) {
            filter.addChangeListener(main::updateActionsAsync);
        }

        ActionToolbar rightCorner = ActionToolbarFactory.getInstance().createActionToolbar(
            ActionPlaces.CHANGES_VIEW_TOOLBAR,
            actionManager.getAction(RightCornerToolbarGroup.class),
            ActionToolbar.Style.HORIZONTAL
        );
        rightCorner.setTargetUIComponent(myTable);

        DockLayout row = DockLayout.create();
        row.left(createTextFilter());
        row.center(main.getUIComponent());
        row.right(rightCorner.getUIComponent());
        return row;
    }

    @RequiredUIAccess
    public void toggleTextFilterFocus() {
        TextBox textFilter = myTextFilter;
        if (textFilter == null) {
            return;
        }
        if (textFilter.hasFocus()) {
            if (myTable instanceof HasFocus table) {
                table.focus();
            }
        }
        else {
            textFilter.focus();
        }
    }

    @RequiredUIAccess
    private TextBox createTextFilter() {
        TextBox textFilter = TextBox.create(myFilterUi.getTextFilterText());
        myTextFilter = textFilter;
        textFilter.setPlaceholder(VersionControlSystemLogLocalize.filterTextPlaceholder());
        textFilter.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        textFilter.setVisibleLength(TEXT_FILTER_LENGTH);

        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        textFilter.setPrefixComponent(searchIcon);

        AnAction textSettingsGroup = ActionManager.getInstance().getAction(VcsLogActionPlaces.TEXT_FILTER_SETTINGS_ACTION_GROUP);
        if (textSettingsGroup instanceof ActionGroup group) {
            ActionToolbar textSettings = ActionToolbarFactory.getInstance()
                .createActionToolbar(ActionPlaces.CHANGES_VIEW_TOOLBAR, group, ActionToolbar.Style.INPLACE);
            textSettings.setTargetUIComponent(textFilter);
            textFilter.setSuffixComponent(textSettings.getUIComponent());
        }

        textFilter.addValueListener(event -> {
            String text = StringUtil.notNullize(event.getValue());
            myFilterUi.setUnsavedTextFilterText(text);
            if (text.isEmpty() && myFilterUi.hasUnsavedTextFilterChanges()) {
                myFilterUi.applyTextFilter(text);
            }
        });
        textFilter.addKeyPressedListener(event -> {
            KeyCode keyCode = event.getInputDetails().getKeyCode();
            if (keyCode != null && keyCode.key() == KeyCode.ENTER.key()) {
                myFilterUi.applyTextFilter(StringUtil.notNullize(textFilter.getValue()));
            }
        });
        textFilter.addBlurListener(event -> {
            if (myFilterUi.hasUnsavedTextFilterChanges()) {
                myFilterUi.applyTextFilter(StringUtil.notNullize(textFilter.getValue()));
            }
        });
        return textFilter;
    }

    private void renderRoot(TextItemPresentation presentation, @Nullable Integer row) {
        if (row == null || row >= myCommitModel.getRowCount()) {
            return;
        }
        presentation.withBackgroundColor(myColorManager.getRootColor(myCommitModel.getRoot(row)));
        presentation.append("");
    }

    private void renderSubject(TextItemPresentation presentation, @Nullable Integer row, boolean selected) {
        if (row == null || row >= myCommitModel.getRowCount()) {
            return;
        }
        if (row >= myCommitModel.getRowCount() - 1 && myCommitModel.canRequestMore()) {
            myCommitModel.requestToLoadMore(() -> {
            });
        }

        VcsLogHighlighter.VcsCommitStyle commitStyle = commitStyleOf(row, selected);
        ColorValue background = rowBackgroundOf(commitStyle, selected);

        VisibleGraph<Integer> graph = myVisiblePack.getVisibleGraph();
        RowInfo<Integer> rowInfo = graph.getRowInfo(row);
        VcsGraphRow graphRow =
            myGraphRowBuilder.build(rowInfo.getPrintElements(), graph.getRecommendedWidth(), background, new GraphMouseHandler(row));
        presentation.withIcon(myGraphImageFactory.createImage(graphRow));

        presentation.append(myCommitModel.getShortDetails(row).getSubject(), applyStyle(presentation, commitStyle, selected));

        Collection<VcsRef> refs = myCommitModel.getRefsAtRow(row);
        if (!refs.isEmpty()) {
            VcsLogRefManager refManager = myLogData.getLogProvider(myCommitModel.getRoot(row)).getReferenceManager();
            List<String> names = new ArrayList<>();
            List<ColorValue> colors = new ArrayList<>();
            for (RefGroup group : refManager.groupForTable(refs, isCompactReferencesView(), isShowTagNames())) {
                names.add(group.getName());
                colors.addAll(group.getColors());
            }
            if (!colors.isEmpty()) {
                presentation.withSuffix(LocalizeValue.of(String.join(", ", names)), refLabelImage(colors, background));
            }
        }
    }

    private void renderAuthor(TextItemPresentation presentation, @Nullable Integer row, boolean selected) {
        if (row == null || row >= myCommitModel.getRowCount()) {
            return;
        }
        VcsShortCommitDetails details = myCommitModel.getShortDetails(row);
        String author = VcsUserUtil.getShortPresentation(details.getAuthor());
        String committerMark = VcsUserUtil.isSamePerson(details.getAuthor(), details.getCommitter()) ? "" : "*";
        presentation.append(author + committerMark, styleOf(presentation, row, selected));
    }

    private void renderDate(TextItemPresentation presentation, @Nullable Integer row, boolean selected) {
        if (row == null || row >= myCommitModel.getRowCount()) {
            return;
        }
        long time = myCommitModel.getShortDetails(row).getAuthorTime();
        presentation.append(time < 0 ? "" : DateFormatUtil.formatDateTime(time), styleOf(presentation, row, selected));
    }

    private TextAttribute styleOf(TextItemPresentation presentation, int row, boolean selected) {
        return applyStyle(presentation, commitStyleOf(row, selected), selected);
    }

    private VcsLogHighlighter.VcsCommitStyle commitStyleOf(int row, boolean selected) {
        RowInfo<Integer> rowInfo = myVisiblePack.getVisibleGraph().getRowInfo(row);
        ColorValue defaultForeground = rowInfo.getRowType() == RowType.UNMATCHED ? ComponentColors.DISABLED_TEXT : null;

        VcsShortCommitDetails details = myLogData.getMiniDetailsGetter().getCommitDataIfAvailable(rowInfo.getCommit());
        List<VcsLogHighlighter.VcsCommitStyle> styles = new ArrayList<>();
        if (details != null) {
            for (VcsLogHighlighter highlighter : myHighlighters) {
                styles.add(highlighter.getStyle(details, selected));
            }
        }
        styles.add(VcsCommitStyleFactory.createStyle(defaultForeground, null, VcsLogHighlighter.TextStyle.NORMAL));
        return VcsCommitStyleFactory.combine(styles);
    }

    private static TextAttribute applyStyle(TextItemPresentation presentation, VcsLogHighlighter.VcsCommitStyle style, boolean selected) {
        int fontStyle = switch (style.getTextStyle() == null ? VcsLogHighlighter.TextStyle.NORMAL : style.getTextStyle()) {
            case BOLD -> TextAttribute.STYLE_BOLD;
            case ITALIC -> TextAttribute.STYLE_ITALIC;
            case NORMAL -> TextAttribute.STYLE_PLAIN;
        };
        if (!selected && style.getBackground() != null) {
            presentation.withBackgroundColor(style.getBackground());
        }
        return new TextAttribute(fontStyle, selected ? null : style.getForeground(), null);
    }

    private static ColorValue rowBackgroundOf(VcsLogHighlighter.VcsCommitStyle commitStyle, boolean selected) {
        if (selected) {
            return ComponentColors.SELECTION_BACKGROUND;
        }
        ColorValue background = commitStyle.getBackground();
        return background != null ? background : ComponentColors.COMPONENT_BACKGROUND;
    }

    private Image refLabelImage(List<ColorValue> colors, ColorValue background) {
        VcsRefLabel label = new VcsRefLabel(REF_LABEL_SIZE, colors, background);
        return myRefLabelImages.computeIfAbsent(label, myRefLabelImageFactory::createImage);
    }

    @RequiredUIAccess
    private void onSelectionChanged() {
        List<Integer> rows = getSelectedRows();
        Object request = new Object();
        mySelectionRequest = request;

        if (rows.isEmpty()) {
            myDetails.setText(LocalizeValue.empty());
            myChangesBrowser.setEmptyText(VersionControlSystemLogLocalize.textNoCommitsSelected());
            myChangesBrowser.setChanges(List.of());
            return;
        }

        myChangesBrowser.setEmptyText(LocalizeValue.empty());
        myChangesBrowser.setChanges(List.of());

        List<Integer> limited = rows.size() > VcsLogUtil.MAX_SELECTED_COMMITS ? rows.subList(0, VcsLogUtil.MAX_SELECTED_COMMITS) : rows;
        List<Integer> commitIds = myCommitModel.convertToCommitIds(limited);
        UIAccess uiAccess = UIAccess.current();
        myLogData.getCommitDetailsGetter().loadCommitsData(commitIds, details -> uiAccess.giveIfNeed(() -> {
            if (mySelectionRequest != request || Disposer.isDisposed(this)) {
                return;
            }
            showDetails(details);
        }), null);
    }

    @RequiredUIAccess
    private void showDetails(List<? extends VcsFullCommitDetails> details) {
        List<Change> changes = new ArrayList<>();
        StringBuilder html = new StringBuilder();
        for (VcsFullCommitDetails detail : ContainerUtil.reverse(details)) {
            changes.addAll(detail.getChanges());
        }
        for (VcsFullCommitDetails detail : details) {
            if (!html.isEmpty()) {
                html.append("<hr>");
            }
            appendDetails(html, detail);
        }

        myChangesBrowser.setChanges(ChangesBrowserUtil.zipChanges(changes));
        myDetails.setText(LocalizeValue.of(html.toString()));
    }

    private static void appendDetails(StringBuilder html, VcsFullCommitDetails detail) {
        String subject = detail.getSubject();
        String fullMessage = detail.getFullMessage();
        String body = fullMessage.startsWith(subject) ? fullMessage.substring(subject.length()).strip() : fullMessage.strip();

        html.append("<b>").append(escape(subject)).append("</b>");
        if (!body.isEmpty()) {
            html.append("<br><br>").append(escape(body));
        }
        html.append("<br><br>")
            .append(escape(detail.getId().toShortString()))
            .append(" ")
            .append(escape(VcsUserUtil.getShortPresentation(detail.getAuthor())))
            .append(" ")
            .append(escape(DateFormatUtil.formatDateTime(detail.getAuthorTime())));
    }

    private static String escape(String text) {
        return StringUtil.escapeXmlEntities(text).replace("\n", "<br>");
    }

    @RequiredUIAccess
    @Override
    protected void onVisiblePackUpdated(boolean permGraphChanged) {
        Set<Integer> selectedCommits = new HashSet<>(myCommitModel.convertToCommitIds(getSelectedRows()));

        myFilterUi.updateDataPack(myVisiblePack);
        myCommitModel.setVisiblePack(myVisiblePack);
        for (VcsLogHighlighter highlighter : myHighlighters) {
            highlighter.update(myVisiblePack, permGraphChanged);
        }
        myRowModel.fireReset();
        restoreSelection(selectedCommits);
    }

    @RequiredUIAccess
    private void restoreSelection(Set<Integer> selectedCommits) {
        if (selectedCommits.isEmpty()) {
            return;
        }
        for (int row = 0; row < myCommitModel.getRowCount(); row++) {
            if (selectedCommits.contains(myCommitModel.getIdAtRow(row))) {
                myTable.select(row);
                return;
            }
        }
    }

    @Override
    public VcsLogCommitModel getCommitModel() {
        return myCommitModel;
    }

    @Override
    public List<Integer> getSelectedRows() {
        return myTable.getSelectedItems();
    }

    @RequiredUIAccess
    @Override
    public Component createPanel(VcsLogManager manager) {
        myManager = manager;
        return myRoot;
    }

    @RequiredUIAccess
    @Override
    public void requestFocus() {
        if (myCommitModel.getRowCount() > 0 && myTable.getSelectedItem() == null) {
            myTable.select(0);
        }
    }

    @RequiredUIAccess
    @Override
    protected void jumpToRow(int row) {
        if (row >= 0 && row < myCommitModel.getRowCount()) {
            myTable.select(row);
            myTable.scrollTo(row);
        }
    }

    @RequiredUIAccess
    @Override
    protected void handleAnswer(GraphAnswer<Integer> answer) {
        myRowModel.fireAllUpdated();
    }

    @RequiredUIAccess
    private void onGraphMouse(int row, float x, float y, int rowHeight, GraphAction.Type type) {
        if (row >= myCommitModel.getRowCount()) {
            return;
        }

        VisibleGraph<Integer> graph = myVisiblePack.getVisibleGraph();
        Collection<? extends PrintElement> elements = graph.getRowInfo(row).getPrintElements();
        VcsGraphRow graphRow = myGraphRowBuilder.build(elements, graph.getRecommendedWidth(), ComponentColors.COMPONENT_BACKGROUND, null);
        PrintElement element = VcsGraphRowBuilder.findElement(elements, graphRow, x, y, rowHeight);

        if (type == GraphAction.Type.MOUSE_OVER) {
            if (row == myHoveredRow && Objects.equals(element, myHoveredElement)) {
                return;
            }
            myHoveredRow = row;
            myHoveredElement = element;
        }
        else if (element == null) {
            return;
        }

        GraphAnswer<Integer> answer = graph.getActionController().performAction(new GraphAction.GraphActionImpl(element, type));
        applyGraphAnswer(graph, answer, type == GraphAction.Type.MOUSE_CLICK);
    }

    @RequiredUIAccess
    private void onGraphMouseExited() {
        if (myHoveredElement == null) {
            return;
        }
        myHoveredRow = -1;
        myHoveredElement = null;

        VisibleGraph<Integer> graph = myVisiblePack.getVisibleGraph();
        GraphAnswer<Integer> answer =
            graph.getActionController().performAction(new GraphAction.GraphActionImpl(null, GraphAction.Type.MOUSE_OVER));
        applyGraphAnswer(graph, answer, false);
    }

    @RequiredUIAccess
    private void applyGraphAnswer(VisibleGraph<Integer> graph, @Nullable GraphAnswer<Integer> answer, boolean dataCouldChange) {
        if (dataCouldChange) {
            Set<Integer> selectedCommits = new HashSet<>(myCommitModel.convertToCommitIds(getSelectedRows()));
            myRowModel.fireReset();
            restoreSelection(selectedCommits);
        }
        else {
            myRowModel.fireAllUpdated();
        }

        if (answer == null) {
            return;
        }

        myTable.setCursor(answer.getCursorToSet());
        Integer commit = answer.getCommitToJump();
        if (commit != null && answer.doJump()) {
            Integer row = graph.getVisibleRowIndex(commit);
            if (row != null && row >= 0) {
                jumpToRow(row);
            }
        }
    }

    private final class GraphMouseHandler implements VcsGraphMouseListener {
        private final int myRow;

        private GraphMouseHandler(int row) {
            myRow = row;
        }

        @Override
        @RequiredUIAccess
        public void mouseMoved(float x, float y, int rowHeight) {
            onGraphMouse(myRow, x, y, rowHeight, GraphAction.Type.MOUSE_OVER);
        }

        @Override
        @RequiredUIAccess
        public void mouseClicked(float x, float y, int rowHeight) {
            onGraphMouse(myRow, x, y, rowHeight, GraphAction.Type.MOUSE_CLICK);
        }

        @Override
        @RequiredUIAccess
        public void mouseExited() {
            onGraphMouseExited();
        }
    }

    @Override
    public void repaintUI() {
        myRowModel.fireAllUpdated();
    }

    @RequiredUIAccess
    @Override
    protected void onShowDetailsChanged() {
        applyShowDetails();
    }

    @RequiredUIAccess
    private void applyShowDetails() {
        Component changes = myChangesBrowser.getComponent();
        if (myUiProperties.get(MainVcsLogUiProperties.SHOW_DETAILS)) {
            myRightArea.center(myDetailsSplitter);
            myDetailsSplitter.setFirstComponent(changes);
            myDetailsSplitter.setSecondComponent(myDetailsScroll);
        }
        else {
            myDetailsSplitter.setFirstComponent(null);
            myDetailsSplitter.setSecondComponent(null);
            myRightArea.center(changes);
        }
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        if (!(ActionManager.getInstance().getAction(VcsLogActionPlaces.POPUP_ACTION_GROUP) instanceof ActionGroup group)) {
            return;
        }
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(VcsLogActionPlaces.POPUP_ACTION_GROUP, group);
        menu.setTargetComponent(myTable);
        InputDetails details = event.getInputDetails();
        menu.show(event.getComponent(), details.getX(), details.getY());
    }

    private final class LogCopyProvider implements CopyProvider {
        @Override
        public void performCopy(DataContext dataContext) {
            StringBuilder text = new StringBuilder();
            for (int row : getSelectedRows()) {
                if (row >= myCommitModel.getRowCount()) {
                    continue;
                }
                VcsShortCommitDetails details = myCommitModel.getShortDetails(row);
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(details.getId().toShortString())
                    .append(' ')
                    .append(details.getSubject())
                    .append(' ')
                    .append(VcsUserUtil.getShortPresentation(details.getAuthor()))
                    .append(' ')
                    .append(DateFormatUtil.formatDateTime(details.getAuthorTime()));
            }
            myProject.getUIAccess().getClipboard().setText(text.toString());
        }

        @Override
        public boolean isCopyEnabled(DataContext dataContext) {
            return !getSelectedRows().isEmpty();
        }

        @Override
        public boolean isCopyVisible(DataContext dataContext) {
            return true;
        }
    }

    @RequiredUIAccess
    @Override
    protected void onCompactReferencesViewChanged() {
        myRowModel.fireAllUpdated();
    }

    @RequiredUIAccess
    @Override
    protected void onShowTagNamesChanged() {
        myRowModel.fireAllUpdated();
    }

    @Override
    public VcsLogClassicFilterUi getFilterUi() {
        return myFilterUi;
    }

    private void uiDataSnapshot(DataSink sink) {
        sink.set(VcsLog.KEY, myLog);
        sink.set(VcsLogCommitSelection.KEY, new VcsLogCommitSelectionImpl(myLog, getSelectedRows(), getCommitModel()));
        sink.set(CopyProvider.KEY, new LogCopyProvider());
        sink.set(VcsLogUi.KEY, this);
        sink.set(VcsLogDataProvider.KEY, myLogData);
        sink.set(VcsLogInternalDataKeys.LOG_UI_PROPERTIES, myUiProperties);
        VcsLogManager manager = myManager;
        if (manager != null) {
            sink.set(VcsLogInternalDataKeys.LOG_MANAGER, manager);
        }
        sink.set(VcsDataKeys.CHANGES, myChangesBrowser.getChanges().toArray(Change[]::new));
        sink.lazy(VcsDataKeys.CHANGE_LISTS, () -> {
            List<VcsFullCommitDetails> details = myLog.getSelectedDetails();
            if (details.size() > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            return ContainerUtil.map2Array(
                details,
                CommittedChangeListForRevision.class,
                detail -> new CommittedChangeListForRevision(
                    detail.getSubject(),
                    detail.getFullMessage(),
                    VcsUserUtil.getShortPresentation(detail.getCommitter()),
                    new Date(detail.getCommitTime()),
                    detail.getChanges(),
                    convertToRevisionNumber(detail.getId())
                )
            );
        });
        sink.lazy(VcsDataKeys.VCS_REVISION_NUMBERS, () -> {
            List<CommitId> hashes = myLog.getSelectedCommits();
            if (hashes.size() > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            return ArrayUtil.toObjectArray(
                ContainerUtil.map(hashes, commitId -> convertToRevisionNumber(commitId.getHash())),
                VcsRevisionNumber.class
            );
        });
        sink.lazy(VcsDataKeys.VCS, () -> {
            List<Integer> rows = getSelectedRows();
            if (rows.isEmpty() || rows.size() > VcsLogUtil.MAX_SELECTED_COMMITS) {
                return null;
            }
            Set<VirtualFile> roots = ContainerUtil.map2Set(rows, myCommitModel::getRoot);
            if (roots.size() == 1) {
                return myLogData.getLogProvider(Objects.requireNonNull(ContainerUtil.getFirstItem(roots))).getSupportedVcs();
            }
            return null;
        });
        sink.lazy(VcsLogDataKeys.VCS_LOG_BRANCHES, () -> {
            List<Integer> rows = getSelectedRows();
            if (rows.size() != 1) {
                return null;
            }
            return myCommitModel.getBranchesAtRow(rows.get(0));
        });
    }

    private static TextRevisionNumber convertToRevisionNumber(Hash hash) {
        return new TextRevisionNumber(hash.asString(), hash.toShortString());
    }

    @Override
    public void dispose() {
        super.dispose();
        myLogData.getMiniDetailsGetter().removeDetailsLoadedListener(myMiniDetailsLoadedListener);
        myLogData.getContainingBranchesGetter().removeTaskCompletedListener(myContainingBranchesListener);
        myHighlighters.clear();
    }
}
