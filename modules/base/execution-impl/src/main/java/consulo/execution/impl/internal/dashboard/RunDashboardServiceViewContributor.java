// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.dashboard;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.component.util.WeighedItem;
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.dashboard.*;
import consulo.execution.impl.internal.action.FakeRerunAction;
import consulo.execution.impl.internal.action.StopAction;
import consulo.execution.impl.internal.configuration.RunManagerImpl;
import consulo.execution.impl.internal.dashboard.tree.*;
import consulo.execution.internal.layout.RunnerLayoutUiImpl;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.execution.service.*;
import consulo.execution.ui.RunContentDescriptor;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.util.PsiNavigateUtil;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigateOptions;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.lazy.LazyValue;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;

@ExtensionImpl
public final class RunDashboardServiceViewContributor
    implements ServiceViewGroupingContributor<RunDashboardServiceViewContributor.RunConfigurationContributor, GroupingNode> {

    public static final String RUN_DASHBOARD_CONTENT_TOOLBAR = "RunDashboardContentToolbar";

    private static final Key<DefaultActionGroup> MORE_ACTION_GROUP_KEY = Key.create("ServicesMoreActionGroup");

    private final Application myApplication;

    @Inject
    public RunDashboardServiceViewContributor(Application application) {
        myApplication = application;
    }

    
    @Override
    public ServiceViewDescriptor getViewDescriptor(Project project) {
        return new RunDashboardContributorViewDescriptor(project);
    }

    
    @Override
    public List<RunConfigurationContributor> getServices(Project project) {
        RunDashboardManagerImpl runDashboardManager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
        return ContainerUtil.map(
            runDashboardManager.getRunConfigurations(),
            value -> new RunConfigurationContributor(new RunConfigurationNode(
                project,
                value,
                RunDashboardManagerImpl.getCustomizers(
                    value.getSettings(),
                    value.getDescriptor()
                )
            ))
        );
    }

    
    @Override
    public ServiceViewDescriptor getServiceDescriptor(Project project, RunConfigurationContributor contributor) {
        return contributor.getViewDescriptor(project);
    }

    
    @Override
    public List<GroupingNode> getGroups(RunConfigurationContributor contributor) {
        List<GroupingNode> result = new ArrayList<>();
        SimpleReference<GroupingNode> parentGroupNode = SimpleReference.create();

        myApplication.getExtensionPoint(RunDashboardGroupingRule.class).forEach(groupingRule -> {
            RunDashboardGroup group = groupingRule.getGroup(contributor.asService());
            if (group == null) {
                return;
            }
            GroupingNode node = new GroupingNode(
                contributor.asService().getProject(),
                parentGroupNode.isNull() ? null : parentGroupNode.get().getGroup(),
                group
            );
            node.setParent(parentGroupNode.get());
            result.add(node);
            parentGroupNode.set(node);
        });
        return result;
    }

    
    @Override
    public ServiceViewDescriptor getGroupDescriptor(GroupingNode node) {
        RunDashboardGroup group = node.getGroup();
        return group instanceof FolderDashboardGroupingRule.FolderDashboardGroup
            ? new RunDashboardFolderGroupViewDescriptor(node)
            : new RunDashboardGroupViewDescriptor(node);
    }

    private static ActionGroup getToolbarActions(@Nullable RunContentDescriptor descriptor) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(ActionManager.getInstance().getAction(RUN_DASHBOARD_CONTENT_TOOLBAR));

        List<AnAction> leftToolbarActions = null;
        RunnerLayoutUiImpl ui = RunDashboardManagerImpl.getRunnerLayoutUi(descriptor);
        if (ui != null) {
            leftToolbarActions = ui.getActions();
        }
        else {
            ActionToolbar toolbar = RunDashboardManagerImpl.findActionToolbar(descriptor);
            if (toolbar != null) {
                leftToolbarActions = toolbar.getActions();
            }
        }

        if (leftToolbarActions != null) {
            if (leftToolbarActions.size() == 1 && leftToolbarActions.get(0) instanceof ActionGroup group) {
                leftToolbarActions = Arrays.asList(group.getChildren(null));
            }
            for (AnAction action : leftToolbarActions) {
                if (action instanceof MoreActionGroup group) {
                    actionGroup.add(getServicesMoreActionGroup(group, descriptor));
                }
                else if (!(action instanceof StopAction) && !(action instanceof FakeRerunAction) && !(action instanceof ExecutorAction)) {
                    actionGroup.add(action);
                }
            }
        }
        return actionGroup;
    }

    private static DefaultActionGroup getServicesMoreActionGroup(MoreActionGroup contentGroup, RunContentDescriptor descriptor) {
        if (descriptor == null) {
            return contentGroup;
        }

        Content content = descriptor.getAttachedContent();
        if (content == null) {
            return contentGroup;
        }

        DefaultActionGroup moreGroup = content.getUserData(MORE_ACTION_GROUP_KEY);
        if (moreGroup == null) {
            moreGroup = new MoreActionGroup(false);
            content.putUserData(MORE_ACTION_GROUP_KEY, moreGroup);
        }
        moreGroup.removeAll();
        moreGroup.addAll(contentGroup.getChildren(null));
        return moreGroup;
    }

    private static ActionGroup getPopupActions() {
        DefaultActionGroup actions = new DefaultActionGroup();
        ActionManager actionManager = ActionManager.getInstance();
        actions.add(actionManager.getAction(RUN_DASHBOARD_CONTENT_TOOLBAR));
        actions.addSeparator();
        actions.add(actionManager.getAction(ActionPlaces.RUN_DASHBOARD_POPUP));
        return actions;
    }

    private static @Nullable RunDashboardRunConfigurationNode getRunConfigurationNode(DnDEvent event, Project project) {
        Object object = event.getAttachedObject();
        if (!(object instanceof DataProvider dataProvider)) {
            return null;
        }

        Object data = dataProvider.getData(PlatformDataKeys.SELECTED_ITEMS);
        if (!(data instanceof Object[] items)) {
            return null;
        }

        if (items.length != 1) {
            return null;
        }

        RunDashboardRunConfigurationNode node = ObjectUtil.tryCast(items[0], RunDashboardRunConfigurationNode.class);
        if (node != null && !node.getConfigurationSettings().getConfiguration().getProject().equals(project)) {
            return null;
        }

        return node;
    }

    private static final class RunConfigurationServiceViewDescriptor implements ServiceViewDescriptor,
        ServiceViewLocatableDescriptor,
        ServiceViewDnDDescriptor {
        private final RunConfigurationNode myNode;

        RunConfigurationServiceViewDescriptor(RunConfigurationNode node) {
            myNode = node;
        }

        @Override
        public @Nullable String getId() {
            RunConfiguration configuration = myNode.getConfigurationSettings().getConfiguration();
            return configuration.getType().getId() + "/" + configuration.getName();
        }

        @Override
        public JComponent getContentComponent() {
            RunDashboardManagerImpl manager = ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject()));
            RunDashboardComponentWrapper wrapper = manager.getContentWrapper();
            Content content = myNode.getContent();
            if (content == null) {
                wrapper.setContent(manager.getEmptyContent());
                wrapper.setContentId(null);
            }
            else {
                ContentManager contentManager = content.getManager();
                if (contentManager == null) {
                    return null;
                }

                wrapper.setContent(contentManager.getComponent());
                wrapper.setContentId(getContentId());
            }
            return wrapper;
        }

        private Integer getContentId() {
            RunContentDescriptor descriptor = myNode.getDescriptor();
            ProcessHandler handler = descriptor == null ? null : descriptor.getProcessHandler();
            return handler == null ? null : handler.hashCode();
        }

        
        @Override
        public ItemPresentation getContentPresentation() {
            Content content = myNode.getContent();
            if (content != null) {
                return new PresentationData(content.getDisplayName(), null, content.getIcon(), null);
            }
            else {
                RunConfiguration configuration = myNode.getConfigurationSettings().getConfiguration();
                return new PresentationData(configuration.getName(), null, configuration.getIcon(), null);
            }
        }

        @Override
        public ActionGroup getToolbarActions() {
            return RunDashboardServiceViewContributor.getToolbarActions(myNode.getDescriptor());
        }

        @Override
        public ActionGroup getPopupActions() {
            return RunDashboardServiceViewContributor.getPopupActions();
        }

        
        @Override
        public ItemPresentation getPresentation() {
            return myNode.getPresentation();
        }

        @Override
        public void onNodeSelected(List<Object> selectedServices) {
            Content content = myNode.getContent();
            if (content == null) {
                return;
            }

            ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject())).setSelectedContent(content);
        }

        @Override
        public void onNodeUnselected() {
            Content content = myNode.getContent();
            if (content == null) {
                return;
            }

            ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject())).removeFromSelection(content);
        }

        @Override
        public @Nullable Navigatable getNavigatable() {
            Supplier<PsiElement> value = LazyValue.nullable(() -> {
                for (RunDashboardCustomizer customizer : myNode.getCustomizers()) {
                    PsiElement psiElement = customizer.getPsiElement(myNode);
                    if (psiElement != null) {
                        return psiElement;
                    }
                }
                return null;
            });
            return new Navigatable() {
                @Override
                public void navigate(boolean requestFocus) {
                    PsiNavigateUtil.navigate(value.get(), requestFocus);
                }

                @Override
                public NavigateOptions getNavigateOptions() {
                    return value.get() != null ? NavigateOptions.CAN_NAVIGATE_FULL : NavigateOptions.CANT_NAVIGATE;
                }

            };

        }

        @Override
        public @Nullable VirtualFile getVirtualFile() {
            return ReadAction.compute(() -> {
                for (RunDashboardCustomizer customizer : myNode.getCustomizers()) {
                    PsiElement psiElement = customizer.getPsiElement(myNode);
                    if (psiElement != null) {
                        return PsiUtilCore.getVirtualFile(psiElement);
                    }
                }
                return null;
            });
        }

        @Override
        public @Nullable Object getPresentationTag(Object fragment) {
            Map<Object, Object> links = myNode.getUserData(RunDashboardCustomizer.NODE_LINKS);
            return links == null ? null : links.get(fragment);
        }

        @Override
        public @Nullable Runnable getRemover() {
            RunnerAndConfigurationSettings settings = myNode.getConfigurationSettings();
            RunManager runManager = RunManager.getInstance(settings.getConfiguration().getProject());
            return runManager.hasSettings(settings) ? () -> runManager.removeConfiguration(settings) : null;
        }

        @Override
        public boolean canDrop(DnDEvent event, Position position) {
            if (position != Position.INTO) {
                return getRunConfigurationNode(event, myNode.getConfigurationSettings().getConfiguration().getProject()) != null;
            }
            for (RunDashboardCustomizer customizer : myNode.getCustomizers()) {
                if (customizer.canDrop(myNode, event)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void drop(DnDEvent event, Position position) {
            if (position != Position.INTO) {
                Project project = myNode.getConfigurationSettings().getConfiguration().getProject();
                RunDashboardRunConfigurationNode node = getRunConfigurationNode(event, project);
                if (node != null) {
                    reorderConfigurations(project, node, position);
                }
                return;
            }
            for (RunDashboardCustomizer customizer : myNode.getCustomizers()) {
                if (customizer.canDrop(myNode, event)) {
                    customizer.drop(myNode, event);
                    return;
                }
            }
        }

        private void reorderConfigurations(Project project, RunDashboardRunConfigurationNode node, Position position) {
            RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
            runManager.fireBeginUpdate();
            try {
                node.getConfigurationSettings().setFolderName(myNode.getConfigurationSettings().getFolderName());

                ObjectIntMap<RunnerAndConfigurationSettings> indices = ObjectMaps.newObjectIntHashMap();
                int i = 0;
                for (RunnerAndConfigurationSettings each : runManager.getAllSettings()) {
                    if (each.equals(node.getConfigurationSettings())) {
                        continue;
                    }

                    if (each.equals(myNode.getConfigurationSettings())) {
                        if (position == Position.ABOVE) {
                            indices.putInt(node.getConfigurationSettings(), i++);
                            indices.putInt(myNode.getConfigurationSettings(), i++);
                        }
                        else if (position == Position.BELOW) {
                            indices.putInt(myNode.getConfigurationSettings(), i++);
                            indices.putInt(node.getConfigurationSettings(), i++);
                        }
                    }
                    else {
                        indices.putInt(each, i++);
                    }
                }
                runManager.setOrder(Comparator.comparingInt(indices::getInt));
            }
            finally {
                runManager.fireEndUpdate();
            }
        }

        @Override
        public boolean isVisible() {
            RunDashboardStatusFilter statusFilter =
                ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject())).getStatusFilter();
            return statusFilter.isVisible(myNode);
        }
    }

    private static class RunDashboardGroupViewDescriptor implements ServiceViewDescriptor, WeighedItem {
        protected final RunDashboardGroup myGroup;
        private final GroupingNode myNode;
        private final PresentationData myPresentationData;

        protected RunDashboardGroupViewDescriptor(GroupingNode node) {
            myNode = node;
            myGroup = node.getGroup();
            myPresentationData = new PresentationData();
            myPresentationData.setPresentableText(myGroup.getName());
            myPresentationData.setIcon(myGroup.getIcon());
        }

        @Override
        public @Nullable String getId() {
            return getId(myNode);
        }

        @Override
        public ActionGroup getToolbarActions() {
            return RunDashboardServiceViewContributor.getToolbarActions(null);
        }

        @Override
        public ActionGroup getPopupActions() {
            return RunDashboardServiceViewContributor.getPopupActions();
        }

        
        @Override
        public ItemPresentation getPresentation() {
            return myPresentationData;
        }

        @Override
        public int getWeight() {
            Object value = ((RunDashboardGroupImpl<?>)myGroup).getValue();
            return value instanceof WeighedItem weighedItem ? weighedItem.getWeight() : 0;
        }

        @Override
        public @Nullable Runnable getRemover() {
            ConfigurationType type = ObjectUtil.tryCast(((RunDashboardGroupImpl<?>)myGroup).getValue(), ConfigurationType.class);
            if (type != null) {
                return () -> {
                    RunDashboardManager runDashboardManager = RunDashboardManager.getInstance(myNode.getProject());
                    Set<String> types = new HashSet<>(runDashboardManager.getTypes());
                    types.remove(type.getId());
                    runDashboardManager.setTypes(types);
                };
            }
            return null;
        }

        @Override
        public @Nullable JComponent getContentComponent() {
            return ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject())).getEmptyContent();
        }

        private static String getId(GroupingNode node) {
            AbstractTreeNode<?> parent = (AbstractTreeNode<?>)node.getParent();
            if (parent instanceof GroupingNode groupingNode) {
                return getId(groupingNode) + "/" + getId(node.getGroup());
            }
            return getId(node.getGroup());
        }

        private static String getId(RunDashboardGroup group) {
            if (group instanceof RunDashboardGroupImpl) {
                Object value = ((RunDashboardGroupImpl<?>)group).getValue();
                if (value instanceof ConfigurationType configType) {
                    return configType.getId();
                }
            }
            return group.getName();
        }
    }

    private static final class RunDashboardFolderGroupViewDescriptor extends RunDashboardGroupViewDescriptor implements ServiceViewDnDDescriptor {
        RunDashboardFolderGroupViewDescriptor(GroupingNode node) {
            super(node);
        }

        @Override
        public @Nullable Runnable getRemover() {
            return () -> {
                String groupName = myGroup.getName();
                Project project = ((FolderDashboardGroupingRule.FolderDashboardGroup)myGroup).getProject();
                List<RunDashboardManager.RunDashboardService> services = RunDashboardManager.getInstance(project).getRunConfigurations();

                RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
                runManager.fireBeginUpdate();
                try {
                    for (RunDashboardManager.RunDashboardService service : services) {
                        RunnerAndConfigurationSettings settings = service.getSettings();
                        if (groupName.equals(settings.getFolderName())) {
                            settings.setFolderName(null);
                        }
                    }
                }
                finally {
                    runManager.fireEndUpdate();
                }
            };
        }

        @Override
        public boolean canDrop(DnDEvent event, ServiceViewDnDDescriptor.Position position) {
            return position == Position.INTO
                && getRunConfigurationNode(event, ((FolderDashboardGroupingRule.FolderDashboardGroup)myGroup).getProject()) != null;
        }

        @Override
        public void drop(DnDEvent event, ServiceViewDnDDescriptor.Position position) {
            Project project = ((FolderDashboardGroupingRule.FolderDashboardGroup)myGroup).getProject();
            RunDashboardRunConfigurationNode node = getRunConfigurationNode(event, project);
            if (node == null) {
                return;
            }

            RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
            runManager.fireBeginUpdate();
            try {
                node.getConfigurationSettings().setFolderName(myGroup.getName());
            }
            finally {
                runManager.fireEndUpdate();
            }
        }
    }

    static final class RunConfigurationContributor implements ServiceViewProvidingContributor<AbstractTreeNode<?>, RunConfigurationNode> {
        private final RunConfigurationNode myNode;

        RunConfigurationContributor(RunConfigurationNode node) {
            myNode = node;
        }

        
        @Override
        public RunConfigurationNode asService() {
            return myNode;
        }

        
        @Override
        public ServiceViewDescriptor getViewDescriptor(Project project) {
            return new RunConfigurationServiceViewDescriptor(myNode);
        }

        
        @Override
        public List<AbstractTreeNode<?>> getServices(Project project) {
            return new ArrayList<>(myNode.getChildren());
        }

        
        @Override
        public ServiceViewDescriptor getServiceDescriptor(Project project, AbstractTreeNode service) {
            return new ServiceViewDescriptor() {
                @Override
                public ActionGroup getToolbarActions() {
                    return RunDashboardServiceViewContributor.getToolbarActions(null);
                }

                @Override
                public ActionGroup getPopupActions() {
                    return RunDashboardServiceViewContributor.getPopupActions();
                }

                
                @Override
                public ItemPresentation getPresentation() {
                    return service.getPresentation();
                }

                @Override
                public @Nullable String getId() {
                    ItemPresentation presentation = getPresentation();
                    String text = presentation.getPresentableText();
                    if (!StringUtil.isEmpty(text)) {
                        return text;
                    }
                    if (presentation instanceof PresentationData presentationData) {
                        List<PresentableNodeDescriptor.ColoredFragment> fragments = presentationData.getColoredText();
                        if (!fragments.isEmpty()) {
                            StringBuilder result = new StringBuilder();
                            for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
                                result.append(fragment.getText());
                            }
                            return result.toString();
                        }
                    }
                    return null;
                }

                @Override
                public @Nullable Runnable getRemover() {
                    return service instanceof RunDashboardNode runDashboardNode ? runDashboardNode.getRemover() : null;
                }

                @Override
                public @Nullable JComponent getContentComponent() {
                    return ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myNode.getProject())).getEmptyContent();
                }
            };
        }
    }

    private static final class RunDashboardContributorViewDescriptor extends SimpleServiceViewDescriptor
        implements ServiceViewToolWindowDescriptor, UiDataProvider {
        private final Project myProject;

        RunDashboardContributorViewDescriptor(Project project) {
            super("Run Dashboard", PlatformIconGroup.actionsExecute());
            myProject = project;
        }

        @Override
        public ActionGroup getToolbarActions() {
            return RunDashboardServiceViewContributor.getToolbarActions(null);
        }

        @Override
        public ActionGroup getPopupActions() {
            return RunDashboardServiceViewContributor.getPopupActions();
        }

        @Override
        public @Nullable JComponent getContentComponent() {
            return ((RunDashboardManagerImpl)RunDashboardManager.getInstance(myProject)).getEmptyContent();
        }

        @Override
        public String getToolWindowId() {
            return getId();
        }

        @Override
        public Image getToolWindowIcon() {
            return PlatformIconGroup.toolwindowsToolwindowrun();
        }

        @Override
        public String getStripeTitle() {
            String title = getToolWindowId();
            return title;
        }

        @Override
        public boolean isExclusionAllowed() {
            return false;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.set(DeleteProvider.KEY, new RunDashboardServiceViewDeleteProvider());
        }
    }
}
