/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.find.actions;

import consulo.application.AccessRule;
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.function.Processor;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorLocation;
import consulo.fileEditor.TextEditor;
import consulo.find.FindManager;
import consulo.find.FindSettings;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesOptions;
import consulo.find.ui.AbstractFindUsagesDialog;
import consulo.find.ui.AbstractFindUsagesDialogDescriptor;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.ide.impl.idea.find.findUsages.FindUsagesManager;
import consulo.ide.impl.idea.find.impl.FindManagerImpl;
import consulo.ide.impl.idea.ide.util.gotoByName.ModelDiff;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.AsyncEditorLoader;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.usages.impl.*;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.ui.popup.HintUpdateSupply;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.SpeedSearchBase;
import consulo.ui.ex.awt.speedSearch.SpeedSearchComparator;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.table.ListTableModel;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awt.util.TableUtil;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.image.Image;
import consulo.usage.*;
import consulo.usage.rule.UsageFilteringRuleListener;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ShowUsagesAction extends AnAction implements PopupAction {
    public static final String ID = "ShowUsages";

    public static int getUsagesPageSize() {
        return Math.max(1, Registry.intValue("ide.usages.page.size", 100));
    }

    static final Usage MORE_USAGES_SEPARATOR = NullUsage.INSTANCE;
    static final Usage USAGES_OUTSIDE_SCOPE_SEPARATOR = new UsageAdapter();

    private static final UsageNode MORE_USAGES_SEPARATOR_NODE = UsageViewImpl.NULL_NODE;
    private static final UsageNode USAGES_OUTSIDE_SCOPE_NODE = new UsageNode(null, USAGES_OUTSIDE_SCOPE_SEPARATOR);

    private static final Comparator<UsageNode> USAGE_NODE_COMPARATOR = (c1, c2) -> {
        if (c1 instanceof StringNode || c2 instanceof StringNode) {
            if (c1 instanceof StringNode && c2 instanceof StringNode) {
                return Comparing.compare(c1.toString(), c2.toString());
            }
            return c1 instanceof StringNode ? 1 : -1;
        }

        Usage o1 = c1.getUsage();
        Usage o2 = c2.getUsage();
        int weight1 = o1 == USAGES_OUTSIDE_SCOPE_SEPARATOR ? 2 : o1 == MORE_USAGES_SEPARATOR ? 1 : 0;
        int weight2 = o2 == USAGES_OUTSIDE_SCOPE_SEPARATOR ? 2 : o2 == MORE_USAGES_SEPARATOR ? 1 : 0;
        if (weight1 != weight2) {
            return weight1 - weight2;
        }

        if (o1 instanceof Comparable && o2 instanceof Comparable) {
            //noinspection unchecked
            return ((Comparable) o1).compareTo(o2);
        }

        VirtualFile v1 = UsageListCellRenderer.getVirtualFile(o1);
        VirtualFile v2 = UsageListCellRenderer.getVirtualFile(o2);
        String name1 = v1 == null ? null : v1.getName();
        String name2 = v2 == null ? null : v2.getName();
        int i = Comparing.compare(name1, name2);
        if (i != 0) {
            return i;
        }
        if (Comparing.equal(v1, v2)) {
            FileEditorLocation loc1 = o1.getLocation();
            FileEditorLocation loc2 = o2.getLocation();
            return Comparing.compare(loc1, loc2);
        }
        else {
            String path1 = v1 == null ? null : v1.getPath();
            String path2 = v2 == null ? null : v2.getPath();
            return Comparing.compare(path1, path2);
        }
    };

    private final boolean myShowSettingsDialogBefore;
    private final UsageViewSettings myUsageViewSettings;
    private Runnable mySearchEverywhereRunnable;

    // used from plugin.xml
    @SuppressWarnings("UnusedDeclaration")
    public ShowUsagesAction() {
        this(false);
    }

    private ShowUsagesAction(boolean showDialogBefore) {
        setInjectedContext(true);
        myShowSettingsDialogBefore = showDialogBefore;

        final UsageViewSettings usageViewSettings = UsageViewSettings.getInstance();
        myUsageViewSettings = new UsageViewSettings();
        myUsageViewSettings.loadState(usageViewSettings);
        myUsageViewSettings.GROUP_BY_FILE_STRUCTURE = false;
        myUsageViewSettings.GROUP_BY_MODULE = false;
        myUsageViewSettings.GROUP_BY_PACKAGE = false;
        myUsageViewSettings.GROUP_BY_USAGE_TYPE = false;
        myUsageViewSettings.GROUP_BY_SCOPE = false;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        FindUsagesInFileAction.updateFindUsagesAction(e);

        if (e.getPresentation().isEnabled()) {
            UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
            if (usageTargets != null && !(ArrayUtil.getFirstElement(usageTargets) instanceof PsiElementUsageTarget)) {
                e.getPresentation().setEnabled(false);
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Runnable searchEverywhere = mySearchEverywhereRunnable;
        mySearchEverywhereRunnable = null;
        hideHints();

        if (searchEverywhere != null) {
            searchEverywhere.run();
            return;
        }

        final RelativePoint popupPosition = JBPopupFactory.getInstance().guessBestPopupLocation(e.getDataContext());
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.goto.usages");

        UsageTarget[] usageTargets = e.getData(UsageView.USAGE_TARGETS_KEY);
        final Editor editor = e.getData(Editor.KEY);
        if (usageTargets == null) {
            FindUsagesAction.chooseAmbiguousTargetAndPerform(project, editor, element -> {
                startFindUsages(element, popupPosition, editor, getUsagesPageSize());
                return false;
            });
        }
        else if (ArrayUtil.getFirstElement(usageTargets) instanceof PsiElementUsageTarget) {
            PsiElement element = ((PsiElementUsageTarget) usageTargets[0]).getElement();
            if (element != null) {
                startFindUsages(element, popupPosition, editor, getUsagesPageSize());
            }
        }
    }

    private static void hideHints() {
        HintManager.getInstance().hideHints(HintManager.HIDE_BY_ANY_KEY, false, false);
    }

    @RequiredUIAccess
    public void startFindUsages(@Nonnull PsiElement element, @Nonnull RelativePoint popupPosition, Editor editor, int maxUsages) {
        Project project = element.getProject();
        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
        if (handler == null) {
            return;
        }

        DataContext context = DataManager.getInstance().getDataContext();
        if (myShowSettingsDialogBefore) {
            showDialogAndFindUsagesAsync(context, handler, popupPosition, editor, maxUsages);
            return;
        }
        showElementUsages(editor, popupPosition, handler, maxUsages, handler.getFindUsagesOptions(context));
    }

    @RequiredUIAccess
    private void showElementUsages(
        Editor editor,
        @Nonnull RelativePoint popupPosition,
        @Nonnull FindUsagesHandler handler,
        int maxUsages,
        @Nonnull FindUsagesOptions options
    ) {
        UIAccess.assertIsUIThread();

        final UsageViewSettings usageViewSettings = UsageViewSettings.getInstance();
        final UsageViewSettings savedGlobalSettings = new UsageViewSettings();

        savedGlobalSettings.loadState(usageViewSettings);
        usageViewSettings.loadState(myUsageViewSettings);

        final Project project = handler.getProject();
        UsageViewManager manager = UsageViewManager.getInstance(project);
        FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(project)).getFindUsagesManager();
        final UsageViewPresentation presentation = findUsagesManager.createPresentation(handler, options);
        presentation.setDetachedMode(true);
        UsageViewImpl usageView = (UsageViewImpl) manager.createUsageView(UsageTarget.EMPTY_ARRAY, Usage.EMPTY_ARRAY, presentation, null);
        if (editor != null) {
            PsiReference reference = TargetElementUtil.findReference(editor);
            if (reference != null) {
                UsageInfo2UsageAdapter origin = new UsageInfo2UsageAdapter(new UsageInfo(reference));
                usageView.setOriginUsage(origin);
            }
        }

        Disposer.register(usageView, () -> {
            myUsageViewSettings.loadState(usageViewSettings);
            usageViewSettings.loadState(savedGlobalSettings);
        });

        final MyTable table = new MyTable();
        final AsyncProcessIcon processIcon = new AsyncProcessIcon("xxx");

        addUsageNodes(usageView.getRoot(), usageView, new ArrayList<>());

        final List<Usage> usages = new ArrayList<>();
        final Set<UsageNode> visibleNodes = new LinkedHashSet<>();
        final List<UsageNode> data = collectData(usages, visibleNodes, usageView, presentation);
        final AtomicInteger outOfScopeUsages = new AtomicInteger();
        setTableModel(table, usageView, data, outOfScopeUsages, options.searchScope);

        Runnable itemChosenCallback = prepareTable(table, editor, popupPosition, handler, maxUsages, options, false);

        @Nullable final JBPopup popup = createUsagePopup(usages, visibleNodes, handler, editor, popupPosition, maxUsages, usageView, options, table, itemChosenCallback, presentation, processIcon);
        if (popup != null) {
            Disposer.register(popup, usageView);

            // show popup only if find usages takes more than 300ms, otherwise it would flicker needlessly
            Alarm alarm = new Alarm(usageView);
            alarm.addRequest(() -> showPopupIfNeedTo(popup, popupPosition), 300);
        }

        final PingEDT pingEDT = new PingEDT(
            "Rebuild popup in EDT",
            o -> popup != null && popup.isDisposed(),
            100,
            () -> {
                if (popup != null && popup.isDisposed()) {
                    return;
                }

                final List<UsageNode> nodes = new ArrayList<>();
                List<Usage> copy;
                synchronized (usages) {
                    // open up popup as soon as several usages 've been found
                    if (popup != null && !popup.isVisible() && (usages.size() <= 1 || !showPopupIfNeedTo(popup, popupPosition))) {
                        return;
                    }
                    addUsageNodes(usageView.getRoot(), usageView, nodes);
                    copy = new ArrayList<>(usages);
                }

                rebuildTable(usageView, copy, nodes, table, popup, presentation, popupPosition, !processIcon.isDisposed(), outOfScopeUsages, options.searchScope);
            }
        );

        final MessageBusConnection messageBusConnection = project.getMessageBus().connect(usageView);
        messageBusConnection.subscribe(UsageFilteringRuleListener.class, pingEDT::ping);

        final UsageTarget[] myUsageTarget = {new PsiElement2UsageTargetAdapter(handler.getPsiElement())};
        Processor<Usage> collect = usage -> {
            if (!UsageViewManagerImpl.isInScope(usage, options.searchScope)) {
                if (outOfScopeUsages.getAndIncrement() == 0) {
                    visibleNodes.add(USAGES_OUTSIDE_SCOPE_NODE);
                    usages.add(USAGES_OUTSIDE_SCOPE_SEPARATOR);
                }
                return true;
            }
            synchronized (usages) {
                if (visibleNodes.size() >= maxUsages) {
                    return false;
                }
                if (UsageViewManager.isSelfUsage(usage, myUsageTarget)) {
                    return true;
                }
                ThrowableComputable<UsageNode, RuntimeException> action = () -> usageView.doAppendUsage(usage);
                UsageNode node = AccessRule.read(action);
                usages.add(usage);
                if (node != null) {
                    visibleNodes.add(node);
                    boolean continueSearch = true;
                    if (visibleNodes.size() == maxUsages) {
                        visibleNodes.add(MORE_USAGES_SEPARATOR_NODE);
                        usages.add(MORE_USAGES_SEPARATOR);
                        continueSearch = false;
                    }
                    pingEDT.ping();

                    return continueSearch;
                }
            }

            return true;
        };

        final ProgressIndicator indicator = FindUsagesManager.startProcessUsages(
            handler,
            handler.getPrimaryElements(),
            handler.getSecondaryElements(),
            collect,
            options,
            () -> Application.get().invokeLater(
                () -> {
                    Disposer.dispose(processIcon);
                    Container parent = processIcon.getParent();
                    if (parent != null) {
                        parent.remove(processIcon);
                        parent.repaint();
                    }
                    pingEDT.ping(); // repaint title
                    synchronized (usages) {
                        if (visibleNodes.isEmpty()) {
                            if (usages.isEmpty()) {
                                String text = UsageViewBundle.message("no.usages.found.in", searchScopePresentableName(options));
                                hint(editor, text, handler, popupPosition, maxUsages, options, false);
                                cancel(popup);
                            }
                            // else all usages filtered out
                        }
                        else if (visibleNodes.size() == 1) {
                            if (usages.size() == 1) {
                                //the only usage
                                Usage usage = visibleNodes.iterator().next().getUsage();
                                if (usage == USAGES_OUTSIDE_SCOPE_SEPARATOR) {
                                    hint(editor, UsageViewManagerImpl.outOfScopeMessage(outOfScopeUsages.get(), options.searchScope), handler, popupPosition, maxUsages, options, true);
                                }
                                else {
                                    String message = UsageViewBundle.message("show.usages.only.usage", searchScopePresentableName(options));
                                    navigateAndHint(usage, message, handler, popupPosition, maxUsages, options);
                                }
                                cancel(popup);
                            }
                            else {
                                assert usages.size() > 1 : usages;
                                // usage view can filter usages down to one
                                Usage visibleUsage = visibleNodes.iterator().next().getUsage();
                                if (areAllUsagesInOneLine(visibleUsage, usages)) {
                                    String hint = UsageViewBundle.message("all.usages.are.in.this.line", usages.size(), searchScopePresentableName(options));
                                    navigateAndHint(visibleUsage, hint, handler, popupPosition, maxUsages, options);
                                    cancel(popup);
                                }
                            }
                        }
                        else {
                            if (popup != null) {
                                String title = presentation.getTabText();
                                boolean shouldShowMoreSeparator = visibleNodes.contains(MORE_USAGES_SEPARATOR_NODE);
                                String fullTitle = getFullTitle(
                                    usages,
                                    title,
                                    shouldShowMoreSeparator,
                                    visibleNodes.size() - (shouldShowMoreSeparator ? 1 : 0),
                                    false
                                );
                                popup.setCaption(fullTitle);
                            }
                        }
                    }
                },
                project.getDisposed()
            )
        );
        if (popup != null) {
            Disposer.register(popup, indicator::cancel);
        }
    }

    @Nonnull
    private static UsageNode createStringNode(@Nonnull final Object string) {
        return new StringNode(string);
    }

    private static class MyModel extends ListTableModel<UsageNode> implements ModelDiff.Model<Object> {
        private MyModel(@Nonnull List<UsageNode> data, int cols) {
            super(cols(cols), data, 0);
        }

        @Nonnull
        private static ColumnInfo[] cols(int cols) {
            ColumnInfo<UsageNode, UsageNode> o = new ColumnInfo<UsageNode, UsageNode>("") {
                @Nullable
                @Override
                public UsageNode valueOf(UsageNode node) {
                    return node;
                }
            };
            List<ColumnInfo<UsageNode, UsageNode>> list = Collections.nCopies(cols, o);
            return list.toArray(new ColumnInfo[list.size()]);
        }

        @Override
        public void addToModel(int idx, Object element) {
            UsageNode node = element instanceof UsageNode ? (UsageNode) element : createStringNode(element);

            if (idx < getRowCount()) {
                insertRow(idx, node);
            }
            else {
                addRow(node);
            }
        }

        @Override
        public void removeRangeFromModel(int start, int end) {
            for (int i = end; i >= start; i--) {
                removeRow(i);
            }
        }
    }

    private static boolean showPopupIfNeedTo(@Nonnull JBPopup popup, @Nonnull RelativePoint popupPosition) {
        if (!popup.isDisposed() && !popup.isVisible()) {
            popup.show(popupPosition);
            return true;
        }
        else {
            return false;
        }
    }


    @Nonnull
    private JComponent createHintComponent(
        @Nonnull String text,
        @Nonnull final FindUsagesHandler handler,
        @Nonnull final RelativePoint popupPosition,
        final Editor editor,
        @Nonnull final Runnable cancelAction,
        final int maxUsages,
        @Nonnull final FindUsagesOptions options,
        boolean isWarning
    ) {
        JComponent label = HintUtil.createInformationLabel(suggestSecondInvocation(options, handler, text));
        label.setBorder(JBUI.Borders.emptyTop(4));
        if (isWarning) {
            label.setBackground(MessageType.WARNING.getPopupBackground());
        }
        AnAction settingAction = createSettingsAction(handler, popupPosition, editor, maxUsages, cancelAction);

        ActionToolbar hintToolbar = ActionToolbarFactory.getInstance().createActionToolbar("HintToolbar",
            ActionGroup.of(settingAction),
            ActionToolbar.Style.INPLACE
        );
        hintToolbar.setTargetComponent(null);

        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public void addNotify() {
                mySearchEverywhereRunnable = () -> searchEverywhere(options, handler, editor, popupPosition, maxUsages);
                super.addNotify();
            }

            @Override
            public void removeNotify() {
                mySearchEverywhereRunnable = null;
                super.removeNotify();
            }
        };
        panel.setBackground(label.getBackground());
        label.setOpaque(false);
        panel.add(label, BorderLayout.CENTER);
        panel.add(hintToolbar.getComponent(), BorderLayout.EAST);
        return panel;
    }

    @Nonnull
    private AnAction createSettingsAction(
        @Nonnull final FindUsagesHandler handler,
        @Nonnull final RelativePoint popupPosition,
        final Editor editor,
        final int maxUsages,
        @Nonnull final Runnable cancelAction
    ) {
        return new DumbAwareAction("Settings", null, PlatformIconGroup.generalGearplain()) {
            {
                KeyboardShortcut shortcut = UsageViewUtil.getShowUsagesWithSettingsShortcut();
                if (shortcut != null) {
                    setShortcutSet(new CustomShortcutSet(shortcut));
                }
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                Application.get().invokeLater(() -> {
                    DataContext context = DataManager.getInstance().getDataContext();

                    showDialogAndFindUsagesAsync(context, handler, popupPosition, editor, maxUsages);
                });
                cancelAction.run();
            }
        };
    }

    @RequiredUIAccess
    private void showDialogAndFindUsagesAsync(@Nonnull DataContext dataContext,
                                              @Nonnull FindUsagesHandler handler,
                                              @Nonnull RelativePoint popupPosition,
                                              Editor editor,
                                              int maxUsages) {
        if (handler.supportConsuloUI()) {
            showDialogAndFindUsagesAsyncNew(dataContext, handler, popupPosition, editor, maxUsages);
        }
        else {
            showDialogAndFindUsagesAsyncOld(dataContext, handler, popupPosition, editor, maxUsages);
        }
    }

    @RequiredUIAccess
    private void showDialogAndFindUsagesAsyncOld(@Nonnull DataContext dataContext,
                                                 @Nonnull FindUsagesHandler handler,
                                                 @Nonnull RelativePoint popupPosition,
                                                 Editor editor,
                                                 int maxUsages) {
        AbstractFindUsagesDialog dialog = handler.getFindUsagesDialog(false, false, false);
        if (dialog.showAndGet()) {
            dialog.calcFindUsagesOptions();
            FindUsagesOptions options = handler.getFindUsagesOptions(DataManager.getInstance().getDataContext());
            showElementUsages(editor, popupPosition, handler, maxUsages, options);
        }
    }

    @RequiredUIAccess
    private void showDialogAndFindUsagesAsyncNew(@Nonnull DataContext dataContext,
                                                 @Nonnull FindUsagesHandler handler,
                                                 @Nonnull RelativePoint popupPosition,
                                                 Editor editor,
                                                 int maxUsages) {
        DialogService dialogService = Application.get().getInstance(DialogService.class);
        AbstractFindUsagesDialogDescriptor descriptor = handler.createFindUsagesDialogDescriptor(dataContext, false, false, false);

        Dialog dialog = dialogService.build(handler.getProject(), descriptor);

        dialog.showAsync().whenComplete((dialogValue, throwable) -> {
            if (dialogValue != null) {
                descriptor.calcFindUsagesOptions();

                FindUsagesOptions options = handler.getFindUsagesOptions(dataContext);

                showElementUsages(editor, popupPosition, handler, maxUsages, options);
            }
        });
    }

    @Nonnull
    private static String searchScopePresentableName(@Nonnull FindUsagesOptions options) {
        return options.searchScope.getDisplayName();
    }

    @Nonnull
    private Runnable prepareTable(
        final MyTable table,
        final Editor editor,
        final RelativePoint popupPosition,
        final FindUsagesHandler handler,
        final int maxUsages,
        @Nonnull final FindUsagesOptions options,
        final boolean previewMode
    ) {

        SpeedSearchBase<JTable> speedSearch = new MySpeedSearch(table);
        speedSearch.setComparator(new SpeedSearchComparator(false));

        table.setRowHeight(AllIcons.Nodes.Class.getHeight() + 2);
        table.setShowGrid(false);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setTableHeader(null);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setIntercellSpacing(new Dimension(0, 0));

        final AtomicReference<List<Object>> selectedUsages = new AtomicReference<>();
        final AtomicBoolean moreUsagesSelected = new AtomicBoolean();
        final AtomicBoolean outsideScopeUsagesSelected = new AtomicBoolean();
        table.getSelectionModel().addListSelectionListener(e -> {
            selectedUsages.set(null);
            outsideScopeUsagesSelected.set(false);
            moreUsagesSelected.set(false);
            List<Object> usages = null;

            for (int i : table.getSelectedRows()) {
                Object value = table.getValueAt(i, 0);
                if (value instanceof UsageNode) {
                    Usage usage = ((UsageNode) value).getUsage();
                    if (usage == USAGES_OUTSIDE_SCOPE_SEPARATOR) {
                        outsideScopeUsagesSelected.set(true);
                        usages = null;
                        break;
                    }
                    else if (usage == MORE_USAGES_SEPARATOR) {
                        moreUsagesSelected.set(true);
                        usages = null;
                        break;
                    }
                    else {
                        if (usages == null) {
                            usages = new ArrayList<>();
                        }
                        usages.add(usage instanceof UsageInfo2UsageAdapter ? ((UsageInfo2UsageAdapter) usage).getUsageInfo().copy() : usage);
                    }
                }
            }

            selectedUsages.set(usages);
        });

        final Runnable itemChosenCallback = () -> {
            if (moreUsagesSelected.get()) {
                appendMoreUsages(editor, popupPosition, handler, maxUsages, options);
                return;
            }

            if (outsideScopeUsagesSelected.get()) {
                options.searchScope = GlobalSearchScope.projectScope(handler.getProject());
                showElementUsages(editor, popupPosition, handler, maxUsages, options);
                return;
            }

            List<Object> usages = selectedUsages.get();
            if (usages != null) {
                for (Object usage : usages) {
                    if (usage instanceof UsageInfo) {
                        UsageViewUtil.navigateTo((UsageInfo) usage, true);
                    }
                    else if (usage instanceof Navigatable) {
                        ((Navigatable) usage).navigate(true);
                    }
                }
            }
        };

        if (previewMode) {
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED) && !UIUtil.isSelectionButtonDown(e) && !e.isConsumed()) {
                        itemChosenCallback.run();
                    }
                }
            });
            table.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        itemChosenCallback.run();
                    }
                }
            });
        }

        return itemChosenCallback;
    }

    @RequiredUIAccess
    @Nonnull
    private JBPopup createUsagePopup(
        @Nonnull final List<Usage> usages,
        @Nonnull Set<UsageNode> visibleNodes,
        @Nonnull final FindUsagesHandler handler,
        final Editor editor,
        @Nonnull final RelativePoint popupPosition,
        final int maxUsages,
        @Nonnull final UsageViewImpl usageView,
        @Nonnull final FindUsagesOptions options,
        @Nonnull final JTable table,
        @Nonnull final Runnable itemChoseCallback,
        @Nonnull final UsageViewPresentation presentation,
        @Nonnull final AsyncProcessIcon processIcon
    ) {
        UIAccess.assertIsUIThread();
        Project project = handler.getProject();

        PopupChooserBuilder builder = new PopupChooserBuilder(table);
        final String title = presentation.getTabText();
        if (title != null) {
            String result = getFullTitle(usages, title, false, visibleNodes.size() - 1, true);
            builder.setTitle(result);
            builder.setAdText(getSecondInvocationTitle(options, handler));
        }

        builder.setMovable(true).setResizable(true);
        builder.setMovable(true).setResizable(true);
        builder.setItemChoosenCallback(itemChoseCallback);
        final JBPopup[] popup = new JBPopup[1];

        KeyboardShortcut shortcut = UsageViewUtil.getShowUsagesWithSettingsShortcut();
        if (shortcut != null) {
            new DumbAwareAction() {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    cancel(popup[0]);

                    showDialogAndFindUsagesAsync(e.getDataContext(), handler, popupPosition, editor, maxUsages);
                }
            }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
        }
        shortcut = getShowUsagesShortcut();
        if (shortcut != null) {
            new DumbAwareAction() {
                @RequiredUIAccess
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    cancel(popup[0]);
                    searchEverywhere(options, handler, editor, popupPosition, maxUsages);
                }
            }.registerCustomShortcutSet(new CustomShortcutSet(shortcut.getFirstKeyStroke()), table);
        }

        AnAction settingsButton = createSettingsAction(handler, popupPosition, editor, maxUsages, () -> cancel(popup[0]));

        final AnAction pin = createPinButton(project, handler, usageView, options, popup);

        List<AnAction> leftActions = new ArrayList<>();
        usageView.addFilteringActions(leftActions::add);
        leftActions.add(UsageGroupingRuleProviderImpl.createGroupByFileStructureAction(usageView));

        builder.setHeaderLeftActions(leftActions);
        builder.setHeaderRightActions(List.of(new ProgressIconAsAction(processIcon), settingsButton, pin));

        builder.setCancelKeyEnabled(false);

        return builder.createPopup();
    }

    private AnAction createPinButton(
        @Nonnull Project project,
        @Nonnull final FindUsagesHandler handler,
        @Nonnull final UsageViewImpl usageView,
        @Nonnull final FindUsagesOptions options,
        @Nonnull final JBPopup[] popup
    ) {
        Image icon = ToolWindowManager.getInstance(project).getLocationIcon(ToolWindowId.FIND, AllIcons.General.Pin_tab);
        DumbAwareAction pinAction = new DumbAwareAction("Open Find Usages Toolwindow", "Show all usages in a separate toolwindow", icon) {
            {
                AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES);
                setShortcutSet(action.getShortcutSet());
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                hideHints();
                cancel(popup[0]);
                FindUsagesManager findUsagesManager = ((FindManagerImpl) FindManager.getInstance(usageView.getProject())).getFindUsagesManager();
                findUsagesManager.findUsages(
                    handler.getPrimaryElements(),
                    handler.getSecondaryElements(),
                    handler,
                    options,
                    FindSettings.getInstance().isSkipResultsWithOneUsage()
                );
            }
        };
        return pinAction;
    }

    private static void cancel(@Nullable JBPopup popup) {
        if (popup != null) {
            popup.cancel();
        }
    }

    @Nonnull
    private static String getFullTitle(@Nonnull List<Usage> usages, @Nonnull String title, boolean hadMoreSeparator, int visibleNodesCount, boolean findUsagesInProgress) {
        String s;
        String soFarSuffix = findUsagesInProgress ? " so far" : "";
        if (hadMoreSeparator) {
            s = "<b>Some</b> " + title + " " + "<b>(Only " + visibleNodesCount + " usages shown" + soFarSuffix + ")</b>";
        }
        else {
            s = title + " (" + UsageViewBundle.message("usages.n", usages.size()) + soFarSuffix + ")";
        }
        return "<html><nobr>" + s + "</nobr></html>";
    }

    @Nonnull
    private static String suggestSecondInvocation(@Nonnull FindUsagesOptions options, @Nonnull FindUsagesHandler handler, @Nonnull String text) {
        final String title = getSecondInvocationTitle(options, handler);

        if (title != null) {
            text += "<br><small> " + title + "</small>";
        }
        return XmlStringUtil.wrapInHtml(UIUtil.convertSpace2Nbsp(text));
    }

    @Nullable
    private static String getSecondInvocationTitle(@Nonnull FindUsagesOptions options, @Nonnull FindUsagesHandler handler) {
        if (getShowUsagesShortcut() != null) {
            GlobalSearchScope maximalScope = FindUsagesManager.getMaximalScope(handler);
            if (!options.searchScope.equals(maximalScope)) {
                return "Press " + KeymapUtil.getShortcutText(getShowUsagesShortcut()) + " again to search in " + maximalScope.getDisplayName();
            }
        }
        return null;
    }

    private void searchEverywhere(@Nonnull FindUsagesOptions options, @Nonnull FindUsagesHandler handler, Editor editor, @Nonnull RelativePoint popupPosition, int maxUsages) {
        FindUsagesOptions cloned = options.clone();
        cloned.searchScope = FindUsagesManager.getMaximalScope(handler);
        showElementUsages(editor, popupPosition, handler, maxUsages, cloned);
    }

    @Nullable
    private static KeyboardShortcut getShowUsagesShortcut() {
        return ActionManager.getInstance().getKeyboardShortcut(ID);
    }

    private static int filtered(@Nonnull List<Usage> usages, @Nonnull UsageViewImpl usageView) {
        return (int) usages.stream().filter(usage -> !usageView.isVisible(usage)).count();
    }

    private static int getUsageOffset(@Nonnull Usage usage) {
        if (!(usage instanceof UsageInfo2UsageAdapter)) {
            return -1;
        }
        PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
        if (element == null) {
            return -1;
        }
        return element.getTextRange().getStartOffset();
    }

    private static boolean areAllUsagesInOneLine(@Nonnull Usage visibleUsage, @Nonnull List<Usage> usages) {
        Editor editor = getEditorFor(visibleUsage);
        if (editor == null) {
            return false;
        }
        int offset = getUsageOffset(visibleUsage);
        if (offset == -1) {
            return false;
        }
        int lineNumber = editor.getDocument().getLineNumber(offset);
        for (Usage other : usages) {
            Editor otherEditor = getEditorFor(other);
            if (otherEditor != editor) {
                return false;
            }
            int otherOffset = getUsageOffset(other);
            if (otherOffset == -1) {
                return false;
            }
            int otherLine = otherEditor.getDocument().getLineNumber(otherOffset);
            if (otherLine != lineNumber) {
                return false;
            }
        }
        return true;
    }

    @Nonnull
    @RequiredUIAccess
    private static MyModel setTableModel(
        @Nonnull JTable table,
        @Nonnull UsageViewImpl usageView,
        @Nonnull final List<UsageNode> data,
        @Nonnull AtomicInteger outOfScopeUsages,
        @Nonnull SearchScope searchScope
    ) {
        UIAccess.assertIsUIThread();
        final int columnCount = calcColumnCount(data);
        MyModel model = table.getModel() instanceof MyModel ? (MyModel) table.getModel() : null;
        if (model == null || model.getColumnCount() != columnCount) {
            model = new MyModel(data, columnCount);
            table.setModel(model);

            ShowUsagesTableCellRenderer renderer = new ShowUsagesTableCellRenderer(usageView, outOfScopeUsages, searchScope);
            for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
                TableColumn column = table.getColumnModel().getColumn(i);
                column.setPreferredWidth(0);
                column.setCellRenderer(renderer);
            }
        }
        return model;
    }

    private static int calcColumnCount(@Nonnull List<UsageNode> data) {
        return data.isEmpty() || data.get(0) instanceof StringNode ? 1 : 3;
    }

    @Nonnull
    private static List<UsageNode> collectData(@Nonnull List<Usage> usages, @Nonnull Collection<UsageNode> visibleNodes, @Nonnull UsageViewImpl usageView, @Nonnull UsageViewPresentation presentation) {
        @Nonnull List<UsageNode> data = new ArrayList<>();
        int filtered = filtered(usages, usageView);
        if (filtered != 0) {
            data.add(createStringNode(UsageViewBundle.message("usages.were.filtered.out", filtered)));
        }
        data.addAll(visibleNodes);
        if (data.isEmpty()) {
            String progressText = StringUtil.escapeXml(UsageViewManagerImpl.getProgressTitle(presentation));
            data.add(createStringNode(progressText));
        }
        Collections.sort(data, USAGE_NODE_COMPARATOR);
        return data;
    }

    private static int calcMaxWidth(JTable table) {
        int colsNum = table.getColumnModel().getColumnCount();

        int totalWidth = 0;
        for (int col = 0; col < colsNum - 1; col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int preferred = column.getPreferredWidth();
            int width = Math.max(preferred, columnMaxWidth(table, col));
            totalWidth += width;
            column.setMinWidth(width);
            column.setMaxWidth(width);
            column.setWidth(width);
            column.setPreferredWidth(width);
        }

        totalWidth += columnMaxWidth(table, colsNum - 1);

        return totalWidth;
    }

    private static int columnMaxWidth(@Nonnull JTable table, int col) {
        TableColumn column = table.getColumnModel().getColumn(col);
        int width = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            Component component = table.prepareRenderer(column.getCellRenderer(), row, col);

            int rendererWidth = component.getPreferredSize().width;
            width = Math.max(width, rendererWidth + table.getIntercellSpacing().width);
        }
        return width;
    }

    private int myWidth = -1;

    @RequiredUIAccess
    private void rebuildTable(
        @Nonnull final UsageViewImpl usageView,
        @Nonnull final List<Usage> usages,
        @Nonnull List<UsageNode> nodes,
        @Nonnull final JTable table,
        @Nullable final JBPopup popup,
        @Nonnull final UsageViewPresentation presentation,
        @Nonnull final RelativePoint popupPosition,
        boolean findUsagesInProgress,
        @Nonnull AtomicInteger outOfScopeUsages,
        @Nonnull SearchScope searchScope
    ) {
        UIAccess.assertIsUIThread();

        boolean shouldShowMoreSeparator = usages.contains(MORE_USAGES_SEPARATOR);
        if (shouldShowMoreSeparator) {
            nodes.add(MORE_USAGES_SEPARATOR_NODE);
        }
        boolean hasOutsideScopeUsages = usages.contains(USAGES_OUTSIDE_SCOPE_SEPARATOR);
        if (hasOutsideScopeUsages && !shouldShowMoreSeparator) {
            nodes.add(USAGES_OUTSIDE_SCOPE_NODE);
        }

        String title = presentation.getTabText();
        String fullTitle = getFullTitle(usages, title, shouldShowMoreSeparator || hasOutsideScopeUsages, nodes.size() - (shouldShowMoreSeparator || hasOutsideScopeUsages ? 1 : 0), findUsagesInProgress);
        if (popup != null) {
            popup.setCaption(fullTitle);
        }

        List<UsageNode> data = collectData(usages, nodes, usageView, presentation);
        MyModel tableModel = setTableModel(table, usageView, data, outOfScopeUsages, searchScope);
        List<UsageNode> existingData = tableModel.getItems();

        int row = table.getSelectedRow();

        int newSelection = updateModel(tableModel, existingData, data, row == -1 ? 0 : row);
        if (newSelection < 0 || newSelection >= tableModel.getRowCount()) {
            ScrollingUtil.ensureSelectionExists(table);
            newSelection = table.getSelectedRow();
        }
        else {
            // do not pre-select the usage under caret by default
            if (newSelection == 0 && table.getModel().getRowCount() > 1) {
                Object valueInTopRow = table.getModel().getValueAt(0, 0);
                if (valueInTopRow instanceof UsageNode && usageView.isOriginUsage(((UsageNode) valueInTopRow).getUsage())) {
                    newSelection++;
                }
            }
            table.getSelectionModel().setSelectionInterval(newSelection, newSelection);
        }
        ScrollingUtil.ensureIndexIsVisible(table, newSelection, 0);

        if (popup != null) {
            setSizeAndDimensions(table, popup, popupPosition, data);
        }
    }

    // returns new selection
    private static int updateModel(@Nonnull MyModel tableModel, @Nonnull List<UsageNode> listOld, @Nonnull List<UsageNode> listNew, int oldSelection) {
        UsageNode[] oa = listOld.toArray(new UsageNode[listOld.size()]);
        UsageNode[] na = listNew.toArray(new UsageNode[listNew.size()]);
        List<ModelDiff.Cmd> cmds = ModelDiff.createDiffCmds(tableModel, oa, na);
        int selection = oldSelection;
        if (cmds != null) {
            for (ModelDiff.Cmd cmd : cmds) {
                selection = cmd.translateSelection(selection);
                cmd.apply();
            }
        }
        return selection;
    }

    private void setSizeAndDimensions(@Nonnull JTable table, @Nonnull JBPopup popup, @Nonnull RelativePoint popupPosition, @Nonnull List<UsageNode> data) {
        JComponent content = popup.getContent();
        Window window = SwingUtilities.windowForComponent(content);
        Dimension d = window.getSize();

        int width = calcMaxWidth(table);
        width = (int) Math.max(d.getWidth(), width);
        Dimension headerSize = ((AbstractPopup) popup).getHeaderPreferredSize();
        width = Math.max((int) headerSize.getWidth(), width);
        width = Math.max(myWidth, width);

        if (myWidth == -1) {
            myWidth = width;
        }
        int newWidth = Math.max(width, d.width + width - myWidth);

        myWidth = newWidth;

        int rowsToShow = Math.min(30, data.size());
        Dimension dimension = new Dimension(newWidth, table.getRowHeight() * rowsToShow);
        Rectangle rectangle = fitToScreen(dimension, popupPosition, table);
        if (!data.isEmpty()) {
            ScrollingUtil.ensureSelectionExists(table);
        }
        table.setSize(rectangle.getSize());
        //table.setPreferredSize(dimension);
        //table.setMaximumSize(dimension);
        //table.setPreferredScrollableViewportSize(dimension);


        Dimension footerSize = ((AbstractPopup) popup).getFooterPreferredSize();

        int footer = footerSize.height;
        int footerBorder = footer == 0 ? 0 : 1;
        Insets insets = ((AbstractPopup) popup).getPopupBorder().getBorderInsets(content);
        rectangle.height += headerSize.height + footer + footerBorder + insets.top + insets.bottom;
        ScreenUtil.fitToScreen(rectangle);
        Dimension newDim = rectangle.getSize();
        window.setBounds(rectangle);
        window.setMinimumSize(newDim);
        window.setMaximumSize(newDim);

        window.validate();
        window.repaint();
    }

    private static Rectangle fitToScreen(@Nonnull Dimension newDim, @Nonnull RelativePoint popupPosition, JTable table) {
        Rectangle rectangle = new Rectangle(popupPosition.getScreenPoint(), newDim);
        ScreenUtil.fitToScreen(rectangle);
        if (rectangle.getHeight() != newDim.getHeight()) {
            int newHeight = (int) rectangle.getHeight();
            int roundedHeight = newHeight - newHeight % table.getRowHeight();
            rectangle.setSize((int) rectangle.getWidth(), Math.max(roundedHeight, table.getRowHeight()));
        }
        return rectangle;

    }

    private void appendMoreUsages(Editor editor, @Nonnull RelativePoint popupPosition, @Nonnull FindUsagesHandler handler, int maxUsages, @Nonnull FindUsagesOptions options) {
        showElementUsages(editor, popupPosition, handler, maxUsages + getUsagesPageSize(), options);
    }

    private static void addUsageNodes(@Nonnull GroupNode root, @Nonnull final UsageViewImpl usageView, @Nonnull List<UsageNode> outNodes) {
        for (UsageNode node : root.getUsageNodes()) {
            Usage usage = node.getUsage();
            if (usageView.isVisible(usage)) {
                node.setParent(root);
                outNodes.add(node);
            }
        }
        for (GroupNode groupNode : root.getSubGroups()) {
            groupNode.setParent(root);
            addUsageNodes(groupNode, usageView, outNodes);
        }
    }

    private void navigateAndHint(@Nonnull Usage usage,
                                 @Nullable final String hint,
                                 @Nonnull final FindUsagesHandler handler,
                                 @Nonnull final RelativePoint popupPosition,
                                 final int maxUsages,
                                 @Nonnull final FindUsagesOptions options) {
        usage.navigate(true);
        if (hint == null) {
            return;
        }
        final Editor newEditor = getEditorFor(usage);
        if (newEditor == null) {
            return;
        }
        hint(newEditor, hint, handler, popupPosition, maxUsages, options, false);
    }

    private void showHint(@Nullable final Editor editor,
                          @Nonnull String hint,
                          @Nonnull FindUsagesHandler handler,
                          @Nonnull final RelativePoint popupPosition,
                          int maxUsages,
                          @Nonnull FindUsagesOptions options,
                          boolean isWarning) {
        Runnable runnable = () -> {
            if (!handler.getPsiElement().isValid()) {
                return;
            }

            JComponent label = createHintComponent(hint, handler, popupPosition, editor, ShowUsagesAction::hideHints, maxUsages, options, isWarning);
            if (editor == null || editor.isDisposed() || !editor.getComponent().isShowing()) {
                HintManager.getInstance().showHint(label, popupPosition, HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING, 0);
            }
            else {
                HintManager.getInstance().showInformationHint(editor, label);
            }
        };
        if (editor == null) {
            runnable.run();
        }
        else {
            AsyncEditorLoader.performWhenLoaded(editor, runnable);
        }
    }

    private void hint(@Nullable final Editor editor,
                      @Nonnull final String hint,
                      @Nonnull final FindUsagesHandler handler,
                      @Nonnull final RelativePoint popupPosition,
                      final int maxUsages,
                      @Nonnull final FindUsagesOptions options,
                      final boolean isWarning) {
        final Project project = handler.getProject();
        //opening editor is performing in invokeLater
        ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> {
            Runnable runnable = () -> {
                // after new editor created, some editor resizing events are still bubbling. To prevent hiding hint, invokeLater this
                ProjectIdeFocusManager.getInstance(project).doWhenFocusSettlesDown(() -> showHint(editor, hint, handler, popupPosition, maxUsages, options, isWarning));
            };
            if (editor == null) {
                runnable.run();
            }
            else {
                editor.getScrollingModel().runActionOnScrollingFinished(runnable);
            }
        });
    }

    @Nullable
    private static Editor getEditorFor(@Nonnull Usage usage) {
        FileEditorLocation location = usage.getLocation();
        FileEditor newFileEditor = location == null ? null : location.getEditor();
        return newFileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : null;
    }

    private static class MyTable extends JBTable implements DataProvider {
        private static final int MARGIN = 2;

        public MyTable() {
            ScrollingUtil.installActions(this);
            HintUpdateSupply.installDataContextHintUpdateSupply(this);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public Object getData(@Nonnull @NonNls Key<?> dataId) {
            if (PsiElement.KEY == dataId) {
                final int[] selected = getSelectedRows();
                if (selected.length == 1) {
                    return getPsiElementForHint(getValueAt(selected[0], 0));
                }
            }
            else if (LangDataKeys.POSITION_ADJUSTER_POPUP == dataId) {
                return PopupUtil.getPopupContainerFor(this);
            }
            return null;
        }

        @Override
        public int getRowHeight() {
            return super.getRowHeight() + 2 * MARGIN;
        }

        @Nonnull
        @Override
        public Component prepareRenderer(@Nonnull TableCellRenderer renderer, int row, int column) {
            Component component = super.prepareRenderer(renderer, row, column);
            if (component instanceof JComponent) {
                ((JComponent) component).setBorder(IdeBorderFactory.createEmptyBorder(MARGIN, MARGIN, MARGIN, 0));
            }
            return component;
        }

        @Nullable
        private static PsiElement getPsiElementForHint(Object selectedValue) {
            if (selectedValue instanceof UsageNode) {
                final Usage usage = ((UsageNode) selectedValue).getUsage();
                if (usage instanceof UsageInfo2UsageAdapter) {
                    final PsiElement element = ((UsageInfo2UsageAdapter) usage).getElement();
                    if (element != null) {
                        final PsiElement view = UsageToPsiElementProvider.findAppropriateParentFrom(element);
                        return view == null ? element : view;
                    }
                }
            }
            return null;
        }
    }

    static class StringNode extends UsageNode {
        @Nonnull
        private final Object myString;

        StringNode(@Nonnull Object string) {
            super(null, NullUsage.INSTANCE);
            myString = string;
        }

        @Override
        public String toString() {
            return myString.toString();
        }
    }

    private static class MySpeedSearch extends SpeedSearchBase<JTable> {
        MySpeedSearch(@Nonnull MyTable table) {
            super(table);
        }

        @Override
        protected int getSelectedIndex() {
            return getTable().getSelectedRow();
        }

        @Override
        protected int convertIndexToModel(int viewIndex) {
            return getTable().convertRowIndexToModel(viewIndex);
        }

        @Nonnull
        @Override
        protected Object[] getAllElements() {
            return ((MyModel) getTable().getModel()).getItems().toArray();
        }

        @Override
        protected String getElementText(@Nonnull Object element) {
            if (!(element instanceof UsageNode)) {
                return element.toString();
            }
            UsageNode node = (UsageNode) element;
            if (node instanceof StringNode) {
                return "";
            }
            Usage usage = node.getUsage();
            if (usage == MORE_USAGES_SEPARATOR || usage == USAGES_OUTSIDE_SCOPE_SEPARATOR) {
                return "";
            }
            GroupNode group = (GroupNode) node.getParent();
            String groupText = group == null ? "" : group.getGroup().getText(null);
            return groupText + usage.getPresentation().getPlainText();
        }

        @Override
        protected void selectElement(Object element, String selectedText) {
            List<UsageNode> data = ((MyModel) getTable().getModel()).getItems();
            int i = data.indexOf(element);
            if (i == -1) {
                return;
            }
            final int viewRow = getTable().convertRowIndexToView(i);
            getTable().getSelectionModel().setSelectionInterval(viewRow, viewRow);
            TableUtil.scrollSelectionToVisible(getTable());
        }

        private MyTable getTable() {
            return (MyTable) myComponent;
        }
    }
}
