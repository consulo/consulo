// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.action;

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.execution.ExecutionUtil;
import consulo.execution.debug.attach.*;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.ProcessInfo;
import consulo.process.ExecutionException;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.StatusText;
import consulo.ui.ex.awt.popup.AWTListPopup;
import consulo.ui.ex.awt.popup.ListPopupStepEx;
import consulo.ui.ex.popup.*;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.InputEvent;
import java.util.*;
import java.util.function.Supplier;

public abstract class AttachToProcessActionBase extends AnAction {
    public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("Attach to Process");

    private static final Key<Map<XAttachHost, LinkedHashSet<RecentItem>>> RECENT_ITEMS_KEY =
        Key.create("AttachToProcessAction.RECENT_ITEMS_KEY");
    private static final Logger LOG = Logger.getInstance(AttachToProcessActionBase.class);

    @Nonnull
    private final Supplier<? extends List<XAttachDebuggerProvider>> myAttachProvidersSupplier;
    @Nonnull
    private final String myAttachActionsListTitle;
    @Nonnull
    private final Supplier<? extends List<XAttachHostProvider>> myAttachHostProviderSupplier;

    public AttachToProcessActionBase(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon,
        @Nonnull Supplier<? extends List<XAttachDebuggerProvider>> attachProvidersSupplier,
        @Nonnull Supplier<? extends List<XAttachHostProvider>> attachHostProviderSupplier,
        @Nonnull String attachActionsListTitle
    ) {
        super(text, description, icon);
        myAttachProvidersSupplier = attachProvidersSupplier;
        myAttachActionsListTitle = attachActionsListTitle;
        myAttachHostProviderSupplier = attachHostProviderSupplier;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        int attachDebuggerProvidersNumber = myAttachProvidersSupplier.get().size();
        boolean enabled = project != null && attachDebuggerProvidersNumber > 0;
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }


        new Task.Backgroundable(
            project,
            XDebuggerLocalize.xdebuggerAttachActionCollectingitems(),
            true,
            PerformInBackgroundOption.DEAF
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {

                List<AttachItem> allItems = List.copyOf(getTopLevelItems(indicator, project));

                Application.get().invokeLater(() -> {
                    AttachListStep step = new AttachListStep(
                        allItems,
                        XDebuggerLocalize.xdebuggerAttachPopupTitleDefault().get(),
                        project
                    );

                    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
                    JList mainList = ((AWTListPopup) popup).getList();

                    ListSelectionListener listener = event -> {
                        if (event.getValueIsAdjusting()) {
                            return;
                        }

                        Object item = ((JList) event.getSource()).getSelectedValue();

                        // if a sub-list is closed, fallback to the selected value from the main list
                        if (item == null) {
                            item = mainList.getSelectedValue();
                        }

                        if (item instanceof AttachToProcessItem attachToProcessItem) {
                            popup.setCaption(attachToProcessItem.getSelectedDebugger().getDebuggerSelectedTitle());
                        }

                        if (item instanceof AttachHostItem hostItem) {
                            String attachHostName = hostItem.getText(project);
                            attachHostName = StringUtil.shortenTextWithEllipsis(attachHostName, 50, 0);

                            popup.setCaption(XDebuggerLocalize.xdebuggerAttachHostPopupTitle(attachHostName).get());
                        }
                    };
                    popup.addListSelectionListener(listener);

                    // force first valueChanged event
                    listener.valueChanged(new ListSelectionEvent(
                        mainList,
                        mainList.getMinSelectionIndex(),
                        mainList.getMaxSelectionIndex(),
                        false
                    ));

                    popup.showCenteredInCurrentWindow(project);
                }, project.getDisposed());
            }
        }.queue();
    }

    @Nonnull
    protected List<? extends AttachItem> getTopLevelItems(@Nonnull ProgressIndicator indicator, @Nonnull Project project) {
        List<AttachItem> attachHostItems = collectAttachHostsItems(project, indicator);

        // If any of hosts available, fold local PIDs into "Local Host" subgroup
        if (!attachHostItems.isEmpty()) {
            AttachItem localHostGroupItem = new AttachHostItem(
                LocalAttachHostPresentationGroup.INSTANCE,
                false,
                LocalAttachHost.INSTANCE,
                project,
                new UserDataHolderBase()
            );
            attachHostItems.add(localHostGroupItem);
            doUpdateFirstInGroup(attachHostItems);
            return attachHostItems;
        }
        return collectAttachProcessItems(project, LocalAttachHost.INSTANCE, indicator);
    }

    private static void doUpdateFirstInGroup(@Nonnull List<? extends AttachItem> items) {
        if (items.isEmpty()) {
            return;
        }

        items.get(0).makeFirstInGroup();

        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).getGroup() != items.get(i - 1).getGroup()) {
                items.get(i).makeFirstInGroup();
            }
        }
    }

    @Nonnull
    public List<AttachItem> collectAttachHostsItems(@Nonnull Project project, @Nonnull ProgressIndicator indicator) {

        List<AttachItem> currentItems = new ArrayList<>();

        UserDataHolderBase dataHolder = new UserDataHolderBase();

        for (XAttachHostProvider hostProvider : myAttachHostProviderSupplier.get()) {
            indicator.checkCanceled();
            //noinspection unchecked
            Set<XAttachHost> hosts = new HashSet<>(hostProvider.getAvailableHosts(project));

            for (XAttachHost host : hosts) {
                //noinspection unchecked
                currentItems.add(new AttachHostItem(hostProvider.getPresentationGroup(), false, host, project, dataHolder));
            }
        }

        //noinspection unchecked
        Collections.sort(currentItems);

        doUpdateFirstInGroup(currentItems);
        return currentItems;
    }

    @Nonnull
    private static List<AttachToProcessItem> getRecentItems(
        @Nonnull List<? extends AttachToProcessItem> currentItems,
        @Nonnull XAttachHost host,
        @Nonnull Project project,
        @Nonnull UserDataHolder dataHolder
    ) {
        List<AttachToProcessItem> result = new ArrayList<>();
        List<RecentItem> recentItems = getRecentItems(host, project);

        for (int i = recentItems.size() - 1; i >= 0; i--) {
            RecentItem recentItem = recentItems.get(i);
            for (AttachToProcessItem currentItem : currentItems) {
                boolean isSuitableItem = recentItem.getGroup().equals(currentItem.getGroup()) && recentItem.getProcessInfo()
                    .getCommandLine()
                    .equals(currentItem.getProcessInfo().getCommandLine());

                if (!isSuitableItem) {
                    continue;
                }

                List<XAttachDebugger> debuggers = currentItem.getDebuggers();
                int selectedDebugger = -1;
                for (int j = 0; j < debuggers.size(); j++) {
                    XAttachDebugger debugger = debuggers.get(j);
                    if (debugger.getDebuggerDisplayName().equals(recentItem.getDebuggerName())) {
                        selectedDebugger = j;
                        break;
                    }
                }
                if (selectedDebugger == -1) {
                    continue;
                }

                result.add(AttachToProcessItem.createRecentAttachItem(
                    currentItem,
                    result.isEmpty(),
                    debuggers,
                    selectedDebugger,
                    project,
                    dataHolder
                ));
            }
        }
        return result;
    }

    @Nonnull
    private static Collection<ProcessInfo> getProcessInfos(@Nonnull XAttachHost host) {
        try {
            return host.getProcessList();
        }
        catch (ExecutionException e) {
            NotificationService.getInstance().newWarn(GROUP)
                .title(XDebuggerLocalize.xdebuggerAttachActionItemsErrorTitle())
                .content(XDebuggerLocalize.xdebuggerAttachActionItemsErrorMessage())
                .notify(null);
            LOG.warn("Error while getting attach items", e);

            return Collections.emptyList();
        }
    }

    @Nonnull
    private List<XAttachDebuggerProvider> getProvidersApplicableForHost(@Nonnull XAttachHost host) {
        return ContainerUtil.filter(
            myAttachProvidersSupplier.get(),
            provider -> provider.isAttachHostApplicable(host)
        );
    }

    @Nonnull
    public List<AttachToProcessItem> collectAttachProcessItems(
        @Nonnull Project project,
        @Nonnull XAttachHost host,
        @Nonnull ProgressIndicator indicator
    ) {
        return doCollectAttachProcessItems(project, host, getProcessInfos(host), indicator, getProvidersApplicableForHost(host));
    }

    @Nonnull
    static List<AttachToProcessItem> doCollectAttachProcessItems(
        @Nonnull Project project,
        @Nonnull XAttachHost host,
        @Nonnull Collection<? extends ProcessInfo> processInfos,
        @Nonnull ProgressIndicator indicator,
        @Nonnull List<? extends XAttachDebuggerProvider> providers
    ) {
        UserDataHolderBase dataHolder = new UserDataHolderBase();

        List<AttachToProcessItem> currentItems = new ArrayList<>();

        for (ProcessInfo process : processInfos) {

            MultiMap<XAttachPresentationGroup<ProcessInfo>, XAttachDebugger> groupsWithDebuggers = new MultiMap<>();

            for (XAttachDebuggerProvider provider : providers) {
                indicator.checkCanceled();

                groupsWithDebuggers.putValues(
                    provider.getPresentationGroup(),
                    provider.getAvailableDebuggers(project, host, process, dataHolder)
                );
            }

            for (XAttachPresentationGroup<ProcessInfo> group : groupsWithDebuggers.keySet()) {
                Collection<XAttachDebugger> debuggers = groupsWithDebuggers.get(group);
                if (!debuggers.isEmpty()) {
                    currentItems.add(new AttachToProcessItem(group, false, host, process, new ArrayList<>(debuggers), project, dataHolder));
                }
            }
        }

        Collections.sort(currentItems);

        doUpdateFirstInGroup(currentItems);

        List<AttachToProcessItem> result = getRecentItems(currentItems, host, project, dataHolder);

        result.addAll(currentItems);
        return result;
    }

    public static void addToRecent(@Nonnull Project project, @Nonnull AttachToProcessItem item) {
        Map<XAttachHost, LinkedHashSet<RecentItem>> recentItems = project.getUserData(RECENT_ITEMS_KEY);

        if (recentItems == null) {
            project.putUserData(RECENT_ITEMS_KEY, recentItems = new HashMap<>());
        }

        XAttachHost host = item.getHost();

        LinkedHashSet<RecentItem> hostRecentItems = recentItems.get(host);

        if (hostRecentItems == null) {
            recentItems.put(host, new LinkedHashSet<>());
            hostRecentItems = recentItems.get(host);
        }

        RecentItem newRecentItem = new RecentItem(host, item);

        hostRecentItems.remove(newRecentItem);

        hostRecentItems.add(newRecentItem);

        while (hostRecentItems.size() > 4) {
            hostRecentItems.remove(hostRecentItems.iterator().next());
        }
    }

    @Nonnull
    public static List<RecentItem> getRecentItems(@Nonnull XAttachHost host, @Nonnull Project project) {
        Map<XAttachHost, LinkedHashSet<RecentItem>> recentItems = project.getUserData(RECENT_ITEMS_KEY);
        return recentItems == null || !recentItems.containsKey(host) ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(
            recentItems.get(host)));
    }

    public static class RecentItem {
        @Nonnull
        private final XAttachHost myHost;
        @Nonnull
        private final ProcessInfo myProcessInfo;
        @Nonnull
        private final XAttachPresentationGroup myGroup;
        @Nonnull
        private final String myDebuggerName;

        public RecentItem(@Nonnull XAttachHost host, @Nonnull AttachToProcessItem item) {
            this(host, item.getProcessInfo(), item.getGroup(), item.getSelectedDebugger().getDebuggerDisplayName());
        }

        private RecentItem(
            @Nonnull XAttachHost host,
            @Nonnull ProcessInfo info,
            @Nonnull XAttachPresentationGroup group,
            @Nonnull String debuggerName
        ) {
            myHost = host;
            myProcessInfo = info;
            myGroup = group;
            myDebuggerName = debuggerName;
        }

        @TestOnly
        public static RecentItem createRecentItem(
            @Nonnull XAttachHost host,
            @Nonnull ProcessInfo info,
            @Nonnull XAttachPresentationGroup group,
            @Nonnull String debuggerName
        ) {
            return new RecentItem(host, info, group, debuggerName);
        }

        @Nonnull
        public XAttachHost getHost() {
            return myHost;
        }

        @Nonnull
        public ProcessInfo getProcessInfo() {
            return myProcessInfo;
        }

        @Nonnull
        public XAttachPresentationGroup getGroup() {
            return myGroup;
        }

        @Nonnull
        public String getDebuggerName() {
            return myDebuggerName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RecentItem item = (RecentItem) o;
            return Objects.equals(myProcessInfo.getCommandLine(), item.myProcessInfo.getCommandLine());
        }

        @Override
        public int hashCode() {

            return Objects.hash(myProcessInfo.getCommandLine());
        }
    }

    public static abstract class AttachItem<T> implements Comparable<AttachItem<T>> {

        @Nonnull
        XAttachPresentationGroup<T> myGroup;
        boolean myIsFirstInGroup;
        @Nonnull
        String myGroupName;
        @Nonnull
        Project myProject;
        @Nonnull
        UserDataHolder myDataHolder;
        @Nonnull
        T myInfo;

        public AttachItem(
            @Nonnull XAttachPresentationGroup<T> group,
            boolean isFirstInGroup,
            @Nonnull String groupName,
            @Nonnull T info,
            @Nonnull Project project,
            @Nonnull UserDataHolder dataHolder
        ) {
            myGroup = group;
            myIsFirstInGroup = isFirstInGroup;
            myInfo = info;
            myProject = project;
            myDataHolder = dataHolder;
            myGroupName = groupName;
        }

        void makeFirstInGroup() {
            myIsFirstInGroup = true;
        }

        @Nonnull
        XAttachPresentationGroup<T> getGroup() {
            return myGroup;
        }

        @Nullable
        String getSeparatorTitle() {
            return myIsFirstInGroup ? myGroupName : null;
        }

        @Nullable
        protected Image getIcon(@Nonnull Project project) {
            return myGroup.getItemIcon(project, myInfo, myDataHolder);
        }

        protected abstract boolean hasSubStep();

        protected abstract String getText(@Nonnull Project project);

        @Nullable
        protected abstract String getTooltipText(@Nonnull Project project);

        protected abstract List<AttachToProcessItem> getSubItems();

        @Override
        public int compareTo(AttachItem<T> compareItem) {
            int groupDifference = myGroup.getOrder() - compareItem.getGroup().getOrder();

            if (groupDifference != 0) {
                return groupDifference;
            }

            return myGroup.compare(myInfo, compareItem.myInfo);
        }
    }

    private class AttachHostItem extends AttachItem<XAttachHost> {

        AttachHostItem(
            @Nonnull XAttachPresentationGroup<XAttachHost> group,
            boolean isFirstInGroup,
            @Nonnull XAttachHost host,
            @Nonnull Project project,
            @Nonnull UserDataHolder dataHolder
        ) {
            super(group, isFirstInGroup, group.getGroupName(), host, project, dataHolder);
        }

        @Override
        public boolean hasSubStep() {
            return true;
        }

        @Nonnull
        @Override
        public String getText(@Nonnull Project project) {
            return myGroup.getItemDisplayText(project, myInfo, myDataHolder);
        }

        @Override
        @Nullable
        public String getTooltipText(@Nonnull Project project) {
            return myGroup.getItemDescription(project, myInfo, myDataHolder);
        }

        @Override
        public List<AttachToProcessItem> getSubItems() {
            return collectAttachProcessItems(myProject, myInfo, new EmptyProgressIndicator());
        }
    }

    public static class AttachToProcessItem extends AttachItem<ProcessInfo> {
        @Nonnull
        private final List<XAttachDebugger> myDebuggers;
        private final int mySelectedDebugger;
        @Nonnull
        private final List<AttachToProcessItem> mySubItems;
        @Nonnull
        private final XAttachHost myHost;

        public AttachToProcessItem(
            @Nonnull XAttachPresentationGroup<ProcessInfo> group,
            boolean isFirstInGroup,
            @Nonnull XAttachHost host,
            @Nonnull ProcessInfo info,
            @Nonnull List<XAttachDebugger> debuggers,
            @Nonnull Project project,
            @Nonnull UserDataHolder dataHolder
        ) {
            this(group, isFirstInGroup, group.getGroupName(), host, info, debuggers, 0, project, dataHolder);
        }

        public AttachToProcessItem(
            @Nonnull XAttachPresentationGroup<ProcessInfo> group,
            boolean isFirstInGroup,
            @Nonnull String groupName,
            @Nonnull XAttachHost host,
            @Nonnull ProcessInfo info,
            @Nonnull List<XAttachDebugger> debuggers,
            int selectedDebugger,
            @Nonnull Project project,
            @Nonnull UserDataHolder dataHolder
        ) {
            super(group, isFirstInGroup, groupName, info, project, dataHolder);
            assert !debuggers.isEmpty() : "debugger list should not be empty";
            assert selectedDebugger >= 0 && selectedDebugger < debuggers.size() : "wrong selected debugger index";

            myDebuggers = debuggers;
            mySelectedDebugger = selectedDebugger;
            myHost = host;

            if (debuggers.size() > 1) {
                mySubItems = ContainerUtil.map(
                    debuggers,
                    debugger -> new AttachToProcessItem(
                        myGroup,
                        false,
                        myHost,
                        myInfo,
                        Collections.singletonList(debugger),
                        myProject,
                        dataHolder
                    )
                );
            }
            else {
                mySubItems = Collections.emptyList();
            }
        }

        static AttachToProcessItem createRecentAttachItem(
            AttachToProcessItem item,
            boolean isFirstInGroup,
            List<XAttachDebugger> debuggers,
            int selectedDebugger,
            Project project, UserDataHolder dataHolder
        ) {
            return new AttachToProcessItem(
                item.getGroup(),
                isFirstInGroup,
                XDebuggerLocalize.xdebuggerAttachTolocalPopupRecent().get(),
                item.getHost(),
                item.getProcessInfo(),
                debuggers,
                selectedDebugger,
                project,
                dataHolder
            );
        }

        @Nonnull
        public ProcessInfo getProcessInfo() {
            return myInfo;
        }

        @Nonnull
        public XAttachHost getHost() {
            return myHost;
        }

        @Override
        public boolean hasSubStep() {
            return !mySubItems.isEmpty();
        }

        @Override
        @Nullable
        public String getTooltipText(@Nonnull Project project) {
            return myGroup.getItemDescription(project, myInfo, myDataHolder);
        }

        @Override
        @Nonnull
        public String getText(@Nonnull Project project) {
            String shortenedText = StringUtil.shortenTextWithEllipsis(myGroup.getItemDisplayText(project, myInfo, myDataHolder), 200, 0);
            int pid = myInfo.getPid();
            return (pid == -1 ? "" : pid + " ") + shortenedText;
        }

        @Nonnull
        public List<XAttachDebugger> getDebuggers() {
            return myDebuggers;
        }

        @Override
        @Nonnull
        public List<AttachToProcessItem> getSubItems() {
            return mySubItems;
        }

        @Nonnull
        public XAttachDebugger getSelectedDebugger() {
            return myDebuggers.get(mySelectedDebugger);
        }

        public void startDebugSession(@Nonnull Project project) {
            XAttachDebugger debugger = getSelectedDebugger();

            try {
                debugger.attachDebugSession(project, myHost, myInfo);
            }
            catch (ExecutionException e) {
                ExecutionUtil.handleExecutionError(project, ToolWindowId.DEBUG, "pid " + myInfo.getPid(), e);
            }
        }
    }

    private static class MyBasePopupStep<T extends AttachItem> extends BaseListPopupStep<T> {
        @Nonnull
        final Project myProject;

        MyBasePopupStep(@Nonnull Project project, @Nullable String title, List<T> values) {
            super(title, values);
            myProject = project;
        }

        @Override
        public boolean isSpeedSearchEnabled() {
            return true;
        }

        @Override
        public boolean isAutoSelectionEnabled() {
            return false;
        }

        @Override
        public boolean hasSubstep(AttachItem selectedValue) {
            return !selectedValue.getSubItems().isEmpty();
        }

        @Override
        public PopupStep onChosen(T selectedValue, boolean finalChoice) {
            return null;
        }
    }

    public class AttachListStep extends MyBasePopupStep<AttachItem> implements ListPopupStepEx<AttachItem> {
        public AttachListStep(@Nonnull List<AttachItem> items, @Nullable String title, @Nonnull Project project) {
            super(project, title, items);
        }

        @Nullable
        @Override
        public ListSeparator getSeparatorAbove(AttachItem value) {
            String separatorTitle = value.getSeparatorTitle();
            return separatorTitle == null ? null : new ListSeparator(separatorTitle);
        }

        @Override
        public Image getIconFor(AttachItem value) {
            return value.getIcon(myProject);
        }

        @Nonnull
        @Override
        public String getTextFor(AttachItem value) {
            return value.getText(myProject);
        }

        @Override
        public boolean hasSubstep(AttachItem selectedValue) {
            return selectedValue.hasSubStep();
        }

        @Nullable
        @Override
        public String getTooltipTextFor(AttachItem value) {
            return value.getTooltipText(myProject);
        }

        @Override
        public void setEmptyText(@Nonnull StatusText emptyText) {
            emptyText.setText(XDebuggerLocalize.xdebuggerAttachPopupEmptytext());
        }

        @Override
        public PopupStep onChosen(AttachItem selectedValue, boolean finalChoice) {
            if (selectedValue instanceof AttachToProcessItem attachToProcessItem) {
                if (finalChoice) {
                    addToRecent(myProject, attachToProcessItem);
                    return doFinalStep(() -> attachToProcessItem.startDebugSession(myProject));
                }
                else {
                    return new ActionListStep(attachToProcessItem.getSubItems(), attachToProcessItem.mySelectedDebugger);
                }
            }

            if (selectedValue instanceof AttachHostItem attachHostItem) {
                return new AsyncPopupStep() {
                    @Override
                    public PopupStep call() {
                        List<AttachItem> attachItems = new ArrayList<>(attachHostItem.getSubItems());
                        return new AttachListStep(attachItems, null, myProject);
                    }
                };
            }
            return null;
        }

        @Override
        public PopupStep onChosen(
            AttachItem selectedValue,
            boolean finalChoice,
            @MagicConstant(flagsFromClass = InputEvent.class) int eventModifiers
        ) {
            return onChosen(selectedValue, finalChoice);
        }

        private class ActionListStep extends MyBasePopupStep<AttachToProcessItem> {
            ActionListStep(List<AttachToProcessItem> items, int selectedItem) {
                super(AttachListStep.this.myProject, AttachToProcessActionBase.this.myAttachActionsListTitle, items);
                setDefaultOptionIndex(selectedItem);
            }

            @Nonnull
            @Override
            public String getTextFor(AttachToProcessItem value) {
                return value.getSelectedDebugger().getDebuggerDisplayName();
            }

            @Override
            public PopupStep onChosen(AttachToProcessItem selectedValue, boolean finalChoice) {
                addToRecent(myProject, selectedValue);
                return doFinalStep(() -> selectedValue.startDebugSession(myProject));
            }
        }
    }
}
