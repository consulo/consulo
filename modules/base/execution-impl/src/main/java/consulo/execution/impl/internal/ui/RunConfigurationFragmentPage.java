package consulo.execution.impl.internal.ui;

import consulo.configurable.ConfigurationException;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.RunConfigurationFragmentType;
import consulo.execution.configuration.fragment.RunnerAndConfigurationSettingsFragment;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.execution.localize.ExecutionLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.HyperlinkStyle;
import consulo.ui.RelativePoint2D;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The run configuration page built from fragments: shared fragments with a negative order, the configuration type's own
 * fragments, then the other shared and contributed ones by {@link RunConfigurationFragment#getOrder()}; one table for every
 * labelled row, the tags in one row, the full width components last.
 */
final class RunConfigurationFragmentPage implements Disposable {
    private static final Logger LOG = Logger.getInstance(RunConfigurationFragmentPage.class);

    private final List<Entry> myEntries = new ArrayList<>();
    private final List<Entry> myAllEntries = new ArrayList<>();
    private final Supplier<RunnerAndConfigurationSettings> mySnapshot;
    private final Runnable myChangeListener;
    private final VerticalLayout myRoot = VerticalLayout.create(Space.MEDIUM);
    private Supplier<List<AnAction>> myBeforeRunActions = List::of;

    @RequiredUIAccess
    RunConfigurationFragmentPage(
        RunnerAndConfigurationSettings settings,
        Supplier<RunnerAndConfigurationSettings> snapshot,
        Runnable changeListener
    ) {
        mySnapshot = snapshot;
        myChangeListener = changeListener;

        RunConfiguration configuration = settings.getConfiguration();

        List<RunConfigurationFragment<?>> shared = new ArrayList<>();
        configuration.getProject().getApplication().getExtensionPoint(RunConfigurationFragment.class).forEach(fragment -> {
            if (fragment.isApplicable(configuration)) {
                shared.add(fragment);
            }
        });
        shared.sort(Comparator.<RunConfigurationFragment<?>>comparingInt(RunConfigurationFragment::getOrder)
            .thenComparing(RunConfigurationFragment::getId));

        List<RunConfigurationFragment<?>> ordered = new ArrayList<>();
        for (RunConfigurationFragment<?> fragment : shared) {
            if (fragment.getOrder() < StandardRunConfigurationFragmentOrder.FIRST) {
                ordered.add(fragment);
            }
        }
        ordered.addAll(configuration.createFragments());
        for (RunConfigurationFragment<?> fragment : shared) {
            if (fragment.getOrder() >= StandardRunConfigurationFragmentOrder.FIRST) {
                ordered.add(fragment);
            }
        }

        Map<String, Entry> groups = new LinkedHashMap<>();
        for (RunConfigurationFragment<?> fragment : ordered) {
            if (fragment.getParentId() == null) {
                Entry entry = new Entry(fragment);
                myEntries.add(entry);
                if (fragment.getType() == RunConfigurationFragmentType.GROUP) {
                    groups.put(fragment.getId(), entry);
                }
            }
        }
        for (RunConfigurationFragment<?> fragment : ordered) {
            String parentId = fragment.getParentId();
            Entry group = parentId == null ? null : groups.get(parentId);
            if (group != null && fragment.getType() != RunConfigurationFragmentType.GROUP) {
                Entry child = new Entry(fragment);
                group.myChildren.add(child);
                myAllEntries.add(child);
            }
        }
        myEntries.removeIf(entry -> entry.isGroup() && entry.myChildren.isEmpty());
        myAllEntries.addAll(0, myEntries);
    }

    /**
     * The "Add ..." actions of before launch, listed in "Modify options" under the before launch group.
     */
    void setBeforeRunActions(Supplier<List<AnAction>> beforeRunActions) {
        myBeforeRunActions = beforeRunActions;
    }

    Component getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    void reset(RunnerAndConfigurationSettings settings) {
        for (Entry entry : myAllEntries) {
            if (entry.isGroup()) {
                continue;
            }

            Object target = entry.target(settings);
            entry.myVisible = entry.isVisible(settings, target);
            if (entry.myVisible) {
                RunConfigurationFragmentEditor<Object> editor = entry.editor(settings);
                if (editor != null) {
                    editor.reset(target);
                }
            }
        }
        rebuild();
    }

    @RequiredUIAccess
    void apply(RunnerAndConfigurationSettings settings) throws ConfigurationException {
        for (Entry entry : myAllEntries) {
            if (entry.isGroup()) {
                continue;
            }

            Object target = entry.target(settings);
            if (entry.myFragment instanceof TagRunConfigurationFragment<?> tag) {
                @SuppressWarnings("unchecked")
                TagRunConfigurationFragment<Object> objectTag = (TagRunConfigurationFragment<Object>) tag;
                objectTag.set(target, entry.myVisible);
            }
            else if (!entry.myVisible) {
                entry.fragment().applyHidden(target);
            }
            else {
                RunConfigurationFragmentEditor<Object> editor = entry.myEditor;
                if (editor != null) {
                    editor.apply(target);
                }
            }
        }

        if (settings.getConfiguration() instanceof RunConfigurationBase configuration) {
            for (Entry entry : myAllEntries) {
                if (!entry.myFragment.isRemovable() || entry.myFragment.getType() != RunConfigurationFragmentType.EDITOR) {
                    continue;
                }

                boolean byDefault = entry.fragment().isVisibleByDefault(entry.target(settings));
                configuration.setFragmentVisibility(entry.myFragment.getId(), entry.myVisible == byDefault ? null : entry.myVisible);
            }
        }
    }

    @RequiredUIAccess
    Hyperlink createModifyOptionsLink() {
        Hyperlink link = Hyperlink.create(ExecutionLocalize.runConfigurationModifyOptions(), event -> {
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                (String) null,
                createModifyOptionsGroup(),
                DataManager.getInstance().getDataContext(event.getComponent()),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.show(RelativePoint2D.of(event));
        });
        link.addStyle(HyperlinkStyle.DROP_DOWN);
        return link;
    }

    private ActionGroup createModifyOptionsGroup() {
        List<Entry> ungrouped = new ArrayList<>();
        Map<String, List<Entry>> groups = new LinkedHashMap<>();
        Map<String, LocalizeValue> groupNames = new LinkedHashMap<>();
        for (Entry entry : myEntries) {
            if (!entry.myFragment.isRemovable()) {
                continue;
            }

            LocalizeValue group = entry.myFragment.getGroup();
            String groupText = group.get();
            if (groupText.isEmpty()) {
                ungrouped.add(entry);
            }
            else {
                groups.computeIfAbsent(groupText, it -> new ArrayList<>()).add(entry);
                groupNames.putIfAbsent(groupText, group);
            }
        }

        LocalizeValue beforeLaunch = ExecutionLocalize.runConfigurationFragmentGroupBeforeLaunch();
        List<AnAction> beforeRunActions = myBeforeRunActions.get();
        if (!beforeRunActions.isEmpty()) {
            groups.putIfAbsent(beforeLaunch.get(), new ArrayList<>());
            groupNames.putIfAbsent(beforeLaunch.get(), beforeLaunch);
        }

        ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
        List<Entry> beforeLaunchEntries = groups.remove(beforeLaunch.get());
        if (beforeLaunchEntries != null) {
            builder.addSeparator(beforeLaunch);
            beforeRunActions.forEach(builder::add);
            beforeLaunchEntries.forEach(entry -> builder.add(new FragmentToggleAction(entry)));
        }
        if (beforeLaunchEntries != null && !ungrouped.isEmpty()) {
            builder.addSeparator();
        }
        ungrouped.forEach(entry -> builder.add(createOptionAction(entry)));
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            builder.addSeparator(groupNames.get(group.getKey()));
            group.getValue().forEach(entry -> builder.add(createOptionAction(entry)));
        }
        return builder.build();
    }

    private AnAction createOptionAction(Entry entry) {
        if (!entry.isGroup()) {
            return new FragmentToggleAction(entry);
        }

        ActionGroup.Builder children = ActionGroup.newImmutableBuilder();
        children.setPopup();
        children.text(entry.myFragment.getName());
        addChildActions(children, entry);
        return children.build();
    }

    private void addChildActions(ActionGroup.Builder builder, Entry group) {
        String lastGroup = "";
        for (Entry child : group.myChildren) {
            if (!child.myFragment.isRemovable()) {
                continue;
            }

            LocalizeValue childGroup = child.myFragment.getGroup();
            if (!childGroup.get().equals(lastGroup)) {
                if (!childGroup.get().isEmpty()) {
                    builder.addSeparator(childGroup);
                }
                else {
                    builder.addSeparator();
                }
            }
            lastGroup = childGroup.get();
            builder.add(new FragmentToggleAction(child));
        }
    }

    @RequiredUIAccess
    private Hyperlink createGroupModifyLink(Entry group) {
        Hyperlink link = Hyperlink.create(ExecutionLocalize.runConfigurationModify(), event -> {
            ActionGroup.Builder builder = ActionGroup.newImmutableBuilder();
            addChildActions(builder, group);
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                ExecutionLocalize.runConfigurationAddGroupOptions(getGroupTitle(group)).get(),
                builder.build(),
                DataManager.getInstance().getDataContext(event.getComponent()),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.show(RelativePoint2D.of(event));
        });
        link.addStyle(HyperlinkStyle.DROP_DOWN);
        return link;
    }

    @RequiredUIAccess
    private void setVisible(Entry entry, boolean visible) {
        if (entry.myVisible == visible) {
            return;
        }

        entry.myVisible = visible;
        if (visible && entry.myEditor == null) {
            RunnerAndConfigurationSettings snapshot = mySnapshot.get();
            RunConfigurationFragmentEditor<Object> editor = entry.editor(snapshot);
            if (editor != null) {
                editor.reset(entry.target(snapshot));
            }
        }
        rebuild();
        myChangeListener.run();
    }

    @RequiredUIAccess
    private void rebuild() {
        myRoot.removeAll();
        addSection(myRoot, myEntries);
        for (Entry entry : myEntries) {
            if (entry.isGroup() && entry.isShown()) {
                myRoot.add(createGroupSection(entry));
            }
        }
    }

    @RequiredUIAccess
    private Component createGroupSection(Entry group) {
        VerticalLayout content = VerticalLayout.create(Space.MEDIUM);
        DockLayout header = DockLayout.create();
        header.right(createGroupModifyLink(group));
        content.add(header);
        addSection(content, group.myChildren);
        return LabeledLayout.create(getGroupTitle(group), content);
    }

    private static LocalizeValue getGroupTitle(Entry group) {
        LocalizeValue title = group.myFragment.getGroup();
        return title.get().isEmpty() ? group.myFragment.getName() : title;
    }

    @RequiredUIAccess
    private void addSection(VerticalLayout target, List<Entry> entries) {
        RunConfigurationFragmentTable table = new RunConfigurationFragmentTable();
        HorizontalLayout tags = HorizontalLayout.create(Space.MEDIUM);
        List<Component> afterTable = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.isGroup() || !entry.myVisible) {
                continue;
            }

            if (entry.myFragment.getType() == RunConfigurationFragmentType.TAG) {
                if (afterTable.isEmpty()) {
                    afterTable.add(tags);
                }
                tags.add(createTagChip(entry));
            }
            else {
                RunConfigurationFragmentEditor<Object> editor = entry.myEditor;
                if (editor != null) {
                    editor.addRows(table);
                }
            }
        }
        table.addTo(target, afterTable);
    }

    @RequiredUIAccess
    private Component createTagChip(Entry entry) {
        Badge badge = Badge.create(entry.myFragment.getName());
        badge.addStyle(BadgeStyle.NEUTRAL);

        Hyperlink remove = Hyperlink.create(LocalizeValue.empty(), event -> setVisible(entry, false));
        remove.setIcon(PlatformIconGroup.actionsClose());
        remove.setToolTipText(ExecutionLocalize.runConfigurationFragmentRemove());

        badge.setSuffixComponent(remove);
        return badge;
    }

    @Override
    public void dispose() {
    }

    private class FragmentToggleAction extends ToggleAction {
        private final Entry myEntry;

        private FragmentToggleAction(Entry entry) {
            super(entry.myFragment.getName(), LocalizeValue.empty(), null);
            myEntry = entry;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myEntry.myVisible;
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            setVisible(myEntry, state);
        }
    }

    private final class Entry {
        private final RunConfigurationFragment<?> myFragment;
        private final List<Entry> myChildren = new ArrayList<>();
        private boolean myVisible;
        private @Nullable RunConfigurationFragmentEditor<Object> myEditor;
        private boolean myEditorCreated;

        private Entry(RunConfigurationFragment<?> fragment) {
            myFragment = fragment;
        }

        private boolean isGroup() {
            return myFragment.getType() == RunConfigurationFragmentType.GROUP;
        }

        private boolean isShown() {
            if (isGroup()) {
                for (Entry child : myChildren) {
                    if (child.myVisible) {
                        return true;
                    }
                }
                return false;
            }
            return myVisible;
        }

        @SuppressWarnings("unchecked")
        private RunConfigurationFragment<Object> fragment() {
            return (RunConfigurationFragment<Object>) myFragment;
        }

        private Object target(RunnerAndConfigurationSettings settings) {
            return myFragment instanceof RunnerAndConfigurationSettingsFragment ? settings : settings.getConfiguration();
        }

        private boolean isVisible(RunnerAndConfigurationSettings settings, Object target) {
            if (!myFragment.isRemovable()) {
                return true;
            }

            if (myFragment.getType() != RunConfigurationFragmentType.TAG
                && settings.getConfiguration() instanceof RunConfigurationBase configuration) {
                Boolean chosen = configuration.getFragmentVisibility(myFragment.getId());
                if (chosen != null) {
                    return chosen;
                }
            }
            return fragment().isVisibleByDefault(target);
        }

        @RequiredUIAccess
        private @Nullable RunConfigurationFragmentEditor<Object> editor(RunnerAndConfigurationSettings settings) {
            if (!myEditorCreated) {
                myEditorCreated = true;
                try {
                    myEditor = fragment().createEditor(settings.getConfiguration().getProject(), RunConfigurationFragmentPage.this);
                }
                catch (Throwable e) {
                    LOG.error("Run configuration fragment editor can not be created: " + myFragment.getId(), e);
                }
            }
            return myEditor;
        }
    }
}
