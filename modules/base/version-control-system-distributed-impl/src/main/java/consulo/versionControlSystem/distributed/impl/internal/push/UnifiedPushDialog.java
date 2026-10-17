/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataContext;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.OptionButtonItem;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.OptionButtonAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.push.PrePushHandler;
import consulo.versionControlSystem.distributed.push.PushActionBase;
import consulo.versionControlSystem.distributed.push.PushDialogActionsProvider;
import consulo.versionControlSystem.distributed.push.PushDialogCustomizer;
import consulo.versionControlSystem.distributed.push.PushInfo;
import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushSupport;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.push.VcsPushOptionValue;
import consulo.versionControlSystem.distributed.push.VcsPushOptionsPanel;
import consulo.versionControlSystem.distributed.push.VcsPushUi;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedPushDialog extends DialogDescriptor implements VcsPushUi {
    private static final Logger LOG = Logger.getInstance(UnifiedPushDialog.class);

    static final String PUSH_ACTIONS_GROUP = "Vcs.Push.Actions";
    private static final String DIMENSION_KEY = "Vcs.Push.Dialog.v2";
    private static final String PLACE = "VcsPushDialog";

    private final Project myProject;
    private final Collection<? extends Repository> myAllRepos;
    private final List<? extends Repository> mySelectedRepositories;
    private final @Nullable Repository myCurrentRepo;
    private final @Nullable PushSource myPushSource;
    private final @Nullable PushTarget myPushTarget;
    private final List<PushActionBase> myPushActions;

    private @Nullable PushController myController;
    private Map<PushSupport<?, ?, ?>, VcsPushOptionsPanel> myAdditionalPanels = Map.of();
    private Map<String, VcsPushOptionsPanel> myCustomPanels = Map.of();
    private @Nullable Dialog myDialog;
    private boolean myDisposed;
    private boolean myClosed;

    private UnifiedPushDialog(Project project,
                              LocalizeValue title,
                              Collection<? extends Repository> allRepos,
                              List<? extends Repository> selectedRepositories,
                              @Nullable Repository currentRepo,
                              @Nullable PushSource pushSource,
                              @Nullable PushTarget pushTarget) {
        super(title);
        myProject = project;
        myAllRepos = allRepos;
        mySelectedRepositories = selectedRepositories;
        myCurrentRepo = currentRepo;
        myPushSource = pushSource;
        myPushTarget = pushTarget;
        myPushActions = collectPushActions();
    }

    @RequiredUIAccess
    public static CompletableFuture<?> show(Project project, List<? extends Repository> selectedRepositories, @Nullable Repository currentRepo) {
        Collection<Repository> allRepos = project.getInstance(VcsRepositoryManager.class).getRepositories();
        return show(project, allRepos, selectedRepositories, currentRepo, null, null);
    }

    @RequiredUIAccess
    public static CompletableFuture<?> show(Project project,
                                            Collection<? extends Repository> allRepos,
                                            List<? extends Repository> selectedRepositories,
                                            @Nullable Repository currentRepo,
                                            @Nullable PushSource pushSource,
                                            @Nullable PushTarget pushTarget) {
        LocalizeValue title = allRepos.size() == 1
            ? DistributedVcsLocalize.pushDialogPushCommitsToTitle(DvcsUtil.getShortRepositoryName(allRepos.iterator().next()))
            : DistributedVcsLocalize.pushDialogPushCommitsTitle();

        UnifiedPushDialog descriptor =
            new UnifiedPushDialog(project, title, allRepos, selectedRepositories, currentRepo, pushSource, pushTarget);
        Dialog dialog = project.getApplication().getInstance(DialogService.class).build(descriptor);
        descriptor.myDialog = dialog;
        return dialog.showAsync();
    }

    private List<PushActionBase> collectPushActions() {
        ActionManager actionManager = ActionManager.getInstance();
        List<PushActionBase> pushActions = new ArrayList<>();
        if (actionManager.getAction(PUSH_ACTIONS_GROUP) instanceof DefaultActionGroup group) {
            for (AnAction action : group.getChildren(null, actionManager)) {
                if (action instanceof PushActionBase pushAction) {
                    pushActions.add(pushAction);
                }
            }
        }

        for (PushActionBase action : pushActions) {
            if (action instanceof SimplePushAction simplePushAction) {
                customizeDialog(simplePushAction);
            }
        }

        List<PushDialogActionsProvider> providers = myProject.getApplication().getExtensionList(PushDialogActionsProvider.class);
        for (int i = providers.size() - 1; i >= 0; i--) {
            try {
                pushActions.addAll(0, providers.get(i).getCustomActionsAboveDefault(myProject));
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
        return pushActions;
    }

    private void customizeDialog(SimplePushAction simplePushAction) {
        List<PushDialogCustomizer> customizers = myProject.getApplication().getExtensionList(PushDialogCustomizer.class);
        if (customizers.isEmpty()) {
            return;
        }
        if (customizers.size() > 1) {
            LOG.warn("There can be only one push actions customizer, found: " + customizers);
            return;
        }
        PushDialogCustomizer customizer = customizers.get(0);
        simplePushAction.getTemplatePresentation().setText(customizer.getNameForSimplePushAction(this));
        simplePushAction.setCondition(customizer.getCondition());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        Disposer.register(uiDisposable, () -> myDisposed = true);

        PushController controller =
            new PushController(myProject, this, myAllRepos, mySelectedRepositories, myCurrentRepo, myPushSource, myPushTarget, uiDisposable);
        myController = controller;
        myAdditionalPanels = controller.createAdditionalPanels();
        myCustomPanels = controller.createCustomPanels(myAllRepos);

        DockLayout root = DockLayout.create();
        root.putUserData(UiDataProvider.KEY, sink -> sink.set(VCS_PUSH_DIALOG, this));
        root.center(controller.getPushPanelLog().getComponent());

        Component options = createOptionsPanel();
        if (options != null) {
            root.bottom(options);
        }

        controller.startLoadingCommits();
        return root;
    }

    @RequiredUIAccess
    private @Nullable Component createOptionsPanel() {
        List<VcsPushOptionsPanel> panels = new ArrayList<>(myAdditionalPanels.values());
        panels.addAll(myCustomPanels.values());
        if (panels.isEmpty()) {
            return null;
        }

        VerticalLayout options = VerticalLayout.create(Space.SMALL);
        HorizontalLayout south = HorizontalLayout.create(Space.LARGE);
        boolean hasSouth = false;
        for (VcsPushOptionsPanel panel : panels) {
            Component component = panel.getComponent();
            if (panel.getPosition() == VcsPushOptionsPanel.OptionsPanelPosition.SOUTH) {
                south.add(component);
                hasSouth = true;
            }
            else {
                options.add(component);
            }
        }
        if (hasSouth) {
            options.add(south);
        }
        options.paddingBuilder().topSet(Space.MEDIUM).leftSet(Space.LARGE).apply();
        return options;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(75, 34);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return DIMENSION_KEY;
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component getPreferredFocusedComponent() {
        PushController controller = myController;
        return controller == null ? null : controller.getPushPanelLog().getPreferredFocusedComponent();
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        ComplexPushAction push = new ComplexPushAction();
        DialogCancelAction cancel = new DialogCancelAction();
        return inverseOrder ? new AnAction[]{cancel, push} : new AnAction[]{push, cancel};
    }

    @Override
    public boolean isDefaultAction(AnAction action) {
        return action instanceof ComplexPushAction;
    }

    @RequiredUIAccess
    @Override
    public boolean canPush() {
        PushController controller = myController;
        return controller != null && controller.isPushAllowed();
    }

    @RequiredUIAccess
    @Override
    public boolean hasWarnings() {
        PushController controller = myController;
        return controller != null && controller.hasCommitWarnings();
    }

    @Override
    public Map<PushSupport<Repository, PushSource, PushTarget>, Collection<PushInfo>> getSelectedPushSpecs() {
        PushController controller = myController;
        return controller == null ? Map.of() : controller.getSelectedPushSpecs();
    }

    @Override
    public @Nullable VcsPushOptionValue getAdditionalOptionValue(PushSupport support) {
        VcsPushOptionsPanel panel = myAdditionalPanels.get(support);
        return panel == null ? null : panel.getValue();
    }

    public @Nullable VcsPushOptionsPanel getCustomPanel(String id) {
        return myCustomPanels.get(id);
    }

    public Map<String, VcsPushOptionValue> getCustomParams() {
        Map<String, VcsPushOptionValue> result = new HashMap<>();
        myCustomPanels.forEach((id, panel) -> {
            VcsPushOptionValue value = panel.getValue();
            if (value != null) {
                result.put(id, value);
            }
        });
        return result;
    }

    boolean isDisposed() {
        return myDisposed;
    }

    @RequiredUIAccess
    public void updateOkActions() {
        if (!myDisposed) {
            updateOkButtonState();
        }
    }

    @RequiredUIAccess
    @Override
    public void push(boolean forcePush) {
        PushController controller = myController;
        if (controller == null) {
            return;
        }

        Map<PushSupport<?, ?, ?>, @Nullable VcsPushOptionValue> options = new HashMap<>();
        for (PushSupport<?, ?, ?> support : myAdditionalPanels.keySet()) {
            options.put(support, getAdditionalOptionValue(support));
        }
        Map<String, VcsPushOptionValue> customParams = getCustomParams();

        executeAfterRunningPrePushHandlers(() -> myProject.getApplication().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(myProject, DistributedVcsLocalize.pushProcessPushing())
            .cancelable()
            .execute(UIAccess.current(), () -> Coroutine.first(CodeExecution.<Object, Object>apply((input, continuation) -> {
                controller.push(forcePush, options, customParams);
                return null;
            }))));
    }

    @RequiredUIAccess
    @Override
    public void executeAfterRunningPrePushHandlers(Supplier<CompletableFuture<?>> activity) {
        FileDocumentManager.getInstance().saveAllDocuments();

        UIAccess uiAccess = UIAccess.current();
        runPrePushHandlersInModalTask().whenCompleteAsync((result, error) -> {
            if (error != null) {
                LOG.error(error);
                return;
            }

            if (result == PrePushHandler.Result.OK) {
                activity.get().whenComplete((value, pushError) -> {
                    if (pushError != null && !isCancellation(pushError)) {
                        LOG.error(pushError);
                    }
                });
                close(true);
            }
            else if (result == PrePushHandler.Result.ABORT_AND_CLOSE) {
                close(false);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<PrePushHandler.Result> runPrePushHandlersInModalTask() {
        PushController controller = myController;
        if (controller == null || myProject.getApplication().getExtensionList(PrePushHandler.class).isEmpty()) {
            return CompletableFuture.completedFuture(PrePushHandler.Result.OK);
        }

        UIAccess uiAccess = UIAccess.current();
        CompletableFuture<PrePushHandler.Result> result = new CompletableFuture<>();
        myProject.getApplication().getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(myProject, DistributedVcsLocalize.pushProcessCheckingCommits())
            .modal()
            .cancelable()
            .execute(uiAccess, () -> Coroutine.first(CodeExecution.<Object, PrePushHandler.Result>apply((input, continuation) -> {
                ProgressIndicator indicator = ProgressIndicator.from(continuation);
                return controller.executeHandlers(indicator);
            })))
            .whenCompleteAsync((value, error) -> {
                if (error == null) {
                    result.complete(value);
                    return;
                }

                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                LocalizeValue message;
                if (cause instanceof PrePushHandlerException handlerException) {
                    LocalizeValue problem;
                    if (handlerException.getCause() instanceof ProcessCanceledException) {
                        problem = DistributedVcsLocalize.pushDialogPushCancelledMessage(handlerException.getFailedHandlerName());
                    }
                    else {
                        LOG.error(handlerException.getCause());
                        problem = DistributedVcsLocalize.pushDialogPushFailedMessage(handlerException.getFailedHandlerName());
                    }
                    LocalizeValue question = handlerException.getSkippedHandlers().isEmpty()
                        ? DistributedVcsLocalize.pushDialogPushAnywayConfirmation()
                        : DistributedVcsLocalize.pushDialogSkipAllRemainingStepsConfirmation();
                    message = LocalizeValue.join(problem, LocalizeValue.of("\n"), question);
                }
                else if (isCancellation(cause)) {
                    message = DistributedVcsLocalize.pushDialogSkipAllStepsConfirmation();
                }
                else {
                    LOG.error(cause);
                    result.complete(PrePushHandler.Result.ABORT);
                    return;
                }

                suggestToSkipOrPush(message).whenComplete((answer, answerError) -> result.complete(answer != null ? answer : PrePushHandler.Result.ABORT));
            }, uiAccess);
        return result;
    }

    @RequiredUIAccess
    private static CompletableFuture<PrePushHandler.Result> suggestToSkipOrPush(LocalizeValue message) {
        return MessageBoxBuilder.<PrePushHandler.Result>create()
            .asWarning()
            .title(DistributedVcsLocalize.actionPush())
            .text(message)
            .button(MessageButtonRole.YES, DistributedVcsLocalize.actionPushAnyway(), PrePushHandler.Result.OK)
            .asDefaultButton()
            .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), PrePushHandler.Result.ABORT)
            .asExitButton()
            .showAsync();
    }

    private static boolean isCancellation(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause instanceof CancellationException || cause instanceof ProcessCanceledException;
    }

    @RequiredUIAccess
    private void close(boolean ok) {
        Dialog dialog = myDialog;
        if (dialog == null || myClosed) {
            return;
        }
        myClosed = true;
        if (ok) {
            dialog.doOkAction(DialogValue.OK_VALUE);
        }
        else {
            dialog.doCancelAction();
        }
    }

    private DataContext createDataContext() {
        return new DataContext() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> @Nullable T getData(Key<T> key) {
                if (VCS_PUSH_DIALOG == key) {
                    return (T) UnifiedPushDialog.this;
                }
                if (Project.KEY == key) {
                    return (T) myProject;
                }
                return null;
            }
        };
    }

    @RequiredUIAccess
    private Presentation buildActionPresentation(PushActionBase action) {
        Presentation presentation = action.getTemplatePresentation().clone();
        if (action instanceof SimplePushAction simplePushAction) {
            customizeDialog(simplePushAction);
        }
        action.update(AnActionEvent.createFromDataContext(PLACE, presentation, createDataContext()));
        return presentation;
    }

    @RequiredUIAccess
    private void perform(PushActionBase action) {
        if (!canPush()) {
            return;
        }
        action.actionPerformed(AnActionEvent.createFromDataContext(PLACE, buildActionPresentation(action), createDataContext()));
    }

    private class ComplexPushAction extends OptionButtonAction implements AnActionWithAsyncUpdate {
        private @Nullable PushActionBase myDefaultAction;
        private List<OptionButtonItem> myOptions = List.of();
        private List<Object> myOptionsSignature = List.of();

        private ComplexPushAction() {
            super(DistributedVcsLocalize.actionPush(), true);
            setEnabledInModalContext(true);
        }

        @Override
        protected List<OptionButtonItem> getOptions() {
            return myOptions;
        }

        @RequiredUIAccess
        @Override
        protected void performMain() {
            PushActionBase action = myDefaultAction;
            if (action != null) {
                perform(action);
            }
        }

        @Override
        public Coroutine<?, ?> updateAsync(AnActionEvent e) {
            return Coroutine.first(UIAction.apply(input -> {
                updateOnUI(e);
                return null;
            }));
        }

        @RequiredUIAccess
        private void updateOnUI(AnActionEvent e) {
            if (myPushActions.isEmpty()) {
                e.getPresentation().setEnabled(false);
                return;
            }

            List<Presentation> presentations = new ArrayList<>(myPushActions.size());
            int firstEnabled = -1;
            for (int i = 0; i < myPushActions.size(); i++) {
                Presentation presentation = buildActionPresentation(myPushActions.get(i));
                presentations.add(presentation);
                if (firstEnabled < 0 && presentation.isEnabled()) {
                    firstEnabled = i;
                }
            }

            int defaultIndex = Math.max(firstEnabled, 0);
            myDefaultAction = myPushActions.get(defaultIndex);

            List<Object> signature = new ArrayList<>();
            List<OptionButtonItem> options = new ArrayList<>();
            for (int i = 0; i < myPushActions.size(); i++) {
                if (i == defaultIndex) {
                    continue;
                }
                PushActionBase action = myPushActions.get(i);
                Presentation presentation = presentations.get(i);
                signature.add(presentation.getTextValue().get());
                signature.add(presentation.isEnabled());
                options.add(new OptionButtonItem(presentation.getTextValue(), null, presentation.isEnabled(), event -> perform(action)));
            }
            if (!signature.equals(myOptionsSignature)) {
                myOptionsSignature = signature;
                myOptions = options;
            }

            Presentation defaultPresentation = presentations.get(defaultIndex);
            e.getPresentation().setEnabled(canPush());
            e.getPresentation().setText(defaultPresentation.getTextValue());
            e.getPresentation().setDescription(defaultPresentation.getDescription());
            e.getPresentation().putClientProperty(OPTIONS, myOptions);
        }
    }
}
