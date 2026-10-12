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
package consulo.externalService.impl.internal.startup;

import consulo.application.Application;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.component.ProcessCanceledException;
import consulo.container.plugin.PluginDescriptor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalService.internal.startup.StartupCustomizeContext;
import consulo.externalService.internal.startup.StartupCustomizeFinishTask;
import consulo.externalService.internal.startup.StartupCustomizeStep;
import consulo.externalService.internal.startup.StartupCustomizeStepProvider;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.ex.wizard.WizardSession;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.SwipeLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.xml.XmlStringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

/**
 * The window of the startup wizard. The repository is read while the window already shows, the pages are asked for
 * once it was read, and the last page is the one finishing the wizard.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public class StartupCustomizeDialogDescriptor extends DialogDescriptor {
    private static final Logger LOG = Logger.getInstance(StartupCustomizeDialogDescriptor.class);

    private static final String LOADING_PAGE = "loading";

    private final Application myApplication;
    private final StartupCustomizeContextImpl myContext;
    private final Supplier<List<PluginDescriptor>> myRepositoryLoader;

    private final List<StartupCustomizeStep> mySteps = new ArrayList<>();
    private @Nullable WizardSession<StartupCustomizeContext> mySession;
    private int myDepth;
    private boolean myFinished;
    private boolean myRestartRequested;

    private volatile @Nullable List<PluginDescriptor> myRepositoryPlugins;
    private volatile @Nullable Boolean myFinishResult;
    private @Nullable Dialog myDialog;

    private @Nullable Disposable myUiDisposable;
    private @Nullable Component myRoot;
    private @Nullable SwipeLayout mySwipeLayout;
    private @Nullable HtmlLabel myStepChainLabel;
    private @Nullable WizardStep<StartupCustomizeContext> myCurrentStep;

    public StartupCustomizeDialogDescriptor(
        Application application,
        StartupCustomizeContextImpl context,
        Supplier<List<PluginDescriptor>> repositoryLoader
    ) {
        super(ExternalServiceLocalize.startupCustomizeTitle(application.getName()));
        myApplication = application;
        myContext = context;
        myRepositoryLoader = repositoryLoader;
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(50, 34);
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return null;
    }

    @Override
    public boolean isReattachable() {
        return true;
    }

    public boolean isRestartRequested() {
        return myRestartRequested;
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        myUiDisposable = uiDisposable;
        Disposer.register(uiDisposable, this::disposeSession);

        SwipeLayout swipeLayout = SwipeLayout.create();
        mySwipeLayout = swipeLayout;
        swipeLayout.register(LOADING_PAGE, () -> DockLayout.create().center(Label.create(ExternalServiceLocalize.startupCustomizeLoading())));
        swipeLayout.swipeLeftTo(LOADING_PAGE);

        HtmlLabel stepChain = HtmlLabel.create(LocalizeValue.empty());
        myStepChainLabel = stepChain;

        LocalizeManager.get().addListener((oldLocale, newLocale) -> giveOnShownUI(() -> {
            WizardStep<StartupCustomizeContext> current = myCurrentStep;
            if (current != null) {
                updateStepChain(current);
            }
            updateOkButtonState();
        }), uiDisposable);

        DockLayout root = DockLayout.create(Space.LARGE);
        root.top(stepChain);
        root.center(swipeLayout);
        root.addAttachListener(event -> resume());
        myRoot = root;

        CompletableFuture.supplyAsync(myRepositoryLoader, AppExecutorUtil.getAppExecutorService())
            .whenComplete((plugins, error) -> {
                myRepositoryPlugins = plugins == null ? List.of() : plugins;
                giveOnShownUI(this::resume);
            });
        return root;
    }

    private void giveOnShownUI(@RequiredUIAccess Runnable runnable) {
        Component root = myRoot;
        UIAccess uiAccess = root == null ? null : root.getUIAccess();
        if (uiAccess != null) {
            uiAccess.give(runnable);
        }
    }

    @RequiredUIAccess
    private void resume() {
        if (mySession == null && myRepositoryPlugins != null) {
            start(myRepositoryPlugins);
        }

        Boolean finishResult = myFinishResult;
        Dialog dialog = myDialog;
        if (finishResult != null && dialog != null) {
            myFinishResult = null;
            completeFinish(dialog, finishResult);
        }
    }

    @RequiredUIAccess
    private void start(List<PluginDescriptor> repositoryPlugins) {
        Disposable uiDisposable = myUiDisposable;
        if (uiDisposable == null || Disposer.isDisposed(uiDisposable)) {
            return;
        }

        myContext.setRepositoryPlugins(repositoryPlugins);

        myApplication.getExtensionPoint(StartupCustomizeStepProvider.class).forEach(provider -> {
            try {
                provider.collectSteps(myContext, mySteps::add);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error("Failed to collect startup wizard steps of " + provider.getClass().getName(), e);
            }
        });
        mySteps.add(new FinishStartupCustomizeStep(myApplication));

        List<WizardStep<StartupCustomizeContext>> steps = new ArrayList<>(mySteps);
        WizardSession<StartupCustomizeContext> session = new WizardSession<>(myContext, steps);
        mySession = session;

        goTo(session.next(), true);
    }

    private void disposeSession() {
        WizardSession<StartupCustomizeContext> session = mySession;
        if (session == null) {
            return;
        }

        if (!myFinished) {
            myFinished = true;
            session.finish();
        }
        session.dispose();
    }

    @Override
    public AnAction[] createActions(boolean inverseOrder) {
        return new AnAction[]{new SkipAction(), new BackAction(), new ForwardAction()};
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return mySession != null;
    }

    @RequiredUIAccess
    private void goTo(WizardStep<StartupCustomizeContext> step, boolean forward) {
        SwipeLayout swipeLayout = mySwipeLayout;
        WizardSession<StartupCustomizeContext> session = mySession;
        if (swipeLayout == null || session == null) {
            return;
        }

        String id = "step-" + session.getCurrentStepIndex();
        swipeLayout.register(id, () -> stepLayout(step));
        if (forward) {
            swipeLayout.swipeLeftTo(id);
        }
        else {
            swipeLayout.swipeRightTo(id);
        }

        myCurrentStep = step;
        updateStepChain(step);
        updateOkButtonState();
    }

    @RequiredUIAccess
    private Layout stepLayout(WizardStep<StartupCustomizeContext> step) {
        Component component = step.getComponent(myContext, myUiDisposable);
        return component instanceof Layout<?> layout ? layout : DockLayout.create().center(component);
    }

    @RequiredUIAccess
    private void updateStepChain(WizardStep<StartupCustomizeContext> current) {
        HtmlLabel label = myStepChainLabel;
        if (label == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (StartupCustomizeStep step : mySteps) {
            if (!step.isVisible(myContext)) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(" &#8594; ");
            }

            String title = XmlStringUtil.escapeText(step.getTitle().get());
            builder.append(step == current ? "<b>" + title + "</b>" : title);
        }
        label.setText(LocalizeValue.of("<html>" + builder + "</html>"));
    }

    private boolean isLastStep() {
        WizardSession<StartupCustomizeContext> session = mySession;
        return session != null && !session.hasNext();
    }

    @RequiredUIAccess
    private void withValidCurrentStep(@RequiredUIAccess Runnable onValid) {
        WizardSession<StartupCustomizeContext> session = mySession;
        if (session == null) {
            return;
        }

        session.validateCurrentStep().whenComplete((ignored, throwable) -> giveOnShownUI(() -> {
            if (throwable == null) {
                onValid.run();
                return;
            }

            Throwable reason = throwable instanceof CompletionException ? throwable.getCause() : throwable;
            MessageBoxes.okError(LocalizeValue.of(reason)).showAsync();
        }));
    }

    @RequiredUIAccess
    private void finish(Dialog dialog) {
        WizardSession<StartupCustomizeContext> session = mySession;
        if (session == null || myFinished) {
            return;
        }

        myFinished = true;
        myDialog = dialog;
        session.finish();

        myApplication.getInstance(ProgressBuilderFactory.class)
            .newProgressBuilder(null, ExternalServiceLocalize.startupCustomizeInstalling())
            .modal()
            .parent(dialog.getWindow())
            .execute(UIAccess.current(), () -> Coroutine.first(CodeExecution.<Object, Boolean>supply(continuation -> {
                ProgressIndicator indicator = continuation.getConfiguration(ProgressIndicator.KEY);
                return runFinishTasks(indicator == null ? new EmptyProgressIndicator() : indicator);
            })))
            .whenComplete((restart, error) -> {
                if (error != null) {
                    LOG.warn("Startup wizard finish failed", error);
                    myContext.addProblem(LocalizeValue.of(error));
                }

                myFinishResult = Boolean.TRUE.equals(restart);
                giveOnShownUI(this::resume);
            });
    }

    @RequiredUIAccess
    private void completeFinish(Dialog dialog, boolean restart) {
        List<LocalizeValue> problems = myContext.getProblems();
        CompletableFuture<?> shown = problems.isEmpty()
            ? CompletableFuture.completedFuture(null)
            : MessageBoxes.okError(LocalizeValue.join("\n", problems.toArray(LocalizeValue[]::new))).showAsync();

        shown.whenComplete((value, ignored) -> giveOnShownUI(() -> {
            myRestartRequested = restart;
            dialog.doOkAction(getOkValue());
        }));
    }

    private boolean runFinishTasks(ProgressIndicator indicator) {
        boolean restart = false;
        for (StartupCustomizeFinishTask task : myContext.getFinishTasks()) {
            try {
                restart |= task.run(myContext, indicator);
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.warn("Startup wizard finish task failed", e);
                myContext.addProblem(LocalizeValue.of(e));
            }
        }
        return restart;
    }

    private class SkipAction extends DialogCancelAction implements AnActionWithSyncUpdate {
        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText(CommonLocalize.buttonSkip());
            e.getPresentation().setVisible(!isLastStep());
        }
    }

    private class BackAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private BackAction() {
            super(CommonLocalize.buttonBack());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            WizardSession<StartupCustomizeContext> session = mySession;
            if (session == null || myDepth == 0 || myFinished) {
                return;
            }

            myDepth--;
            goTo(session.prev(), false);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(mySession != null && myDepth > 0 && !myFinished);
        }
    }

    private class ForwardAction extends DialogOkAction {
        private ForwardAction() {
            super(CommonLocalize.buttonNext());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            Dialog dialog = e.getRequiredData(Dialog.KEY);

            withValidCurrentStep(() -> {
                WizardSession<StartupCustomizeContext> session = mySession;
                if (session == null || myFinished) {
                    return;
                }

                if (isLastStep()) {
                    finish(dialog);
                    return;
                }

                myDepth++;
                goTo(session.next(), true);
            });
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);

            LocalizeValue text;
            if (!isLastStep()) {
                text = CommonLocalize.buttonNext();
            }
            else if (FinishStartupCustomizeStep.needsRestart(myContext)) {
                text = ExternalServiceLocalize.actionRestartAppText(myApplication.getName());
            }
            else {
                text = ExternalServiceLocalize.startupCustomizeStart(myApplication.getName());
            }
            e.getPresentation().setText(text);
        }
    }
}
