// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.resolve.ResolvedStreamCall;
import consulo.execution.debug.stream.resolve.ResolvedStreamChain;
import consulo.execution.debug.stream.trace.*;
import consulo.execution.debug.stream.ui.TraceController;
import consulo.execution.debug.stream.wrapper.QualifierExpression;
import consulo.execution.debug.stream.wrapper.StreamCall;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.execution.debug.stream.wrapper.TraceUtil;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class EvaluationAwareTraceWindow extends DialogWrapper {
    private static final String IS_FLAT_MODE_PROPERTY = "org.jetbrains.debugger.streams:isTraceWindowInFlatMode";
    private static final boolean IS_DEFAULT_MODE_FLAT = false;

    private static final int DEFAULT_WIDTH = 870;
    private static final int DEFAULT_HEIGHT = 400;
    private final MyCenterPane myCenterPane;
    private final List<MyPlaceholder> myTabContents;
    private final MyPlaceholder myFlatContent;
    private final TabbedPaneWrapper myTabsPane;

    private MyMode myMode;

    public EvaluationAwareTraceWindow(@Nonnull XDebugSession session, @Nonnull StreamChain chain) {
        super(session.getProject(), true);
        myTabsPane = new TabbedPaneWrapper(getDisposable());
        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                ApplicationManager.getApplication().invokeLater(() -> close(CLOSE_EXIT_CODE));
            }
        }, myDisposable);
        setModal(false);
        setTitle(XDebuggerLocalize.streamDebuggerDialogTitle());
        myCenterPane = new MyCenterPane();
        myCenterPane.add(MyMode.SPLIT.name(), myTabsPane.getComponent());

        myTabContents = new ArrayList<>();
        QualifierExpression qualifierExpression = chain.getQualifierExpression();
        MyPlaceholder firstTab = new MyPlaceholder();
        myTabsPane.insertTab(TraceUtil.formatQualifierExpression(qualifierExpression.getText(), 30),
            ExecutionIconGroup.console(), firstTab, qualifierExpression.getText(), 0);
        myTabContents.add(firstTab);

        for (int i = 0, chainLength = chain.length(); i < chainLength; i++) {
            StreamCall call = chain.getCall(i);
            MyPlaceholder tab = new MyPlaceholder();
            String tabTitle = call.getTabTitle();
            String tabTooltip = call.getTabTooltip();
            myTabsPane.insertTab(tabTitle, ExecutionIconGroup.console(), tab, tabTooltip, i + 1);
            myTabContents.add(tab);
        }

        myFlatContent = new MyPlaceholder();
        myCenterPane.add(MyMode.FLAT.name(), myFlatContent);
        myCenterPane.setPreferredSize(new JBDimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        if (!ApplicationPropertiesComponent.getInstance().isValueSet(IS_FLAT_MODE_PROPERTY)) {
            ApplicationPropertiesComponent.getInstance().setValue(IS_FLAT_MODE_PROPERTY, IS_DEFAULT_MODE_FLAT);
        }

        myMode = ApplicationPropertiesComponent.getInstance().getBoolean(IS_FLAT_MODE_PROPERTY) ? MyMode.FLAT : MyMode.SPLIT;
        updateWindowMode(myCenterPane, myMode);

        init();
    }

    @Override
    protected @Nullable String getDimensionServiceKey() {
        return "#com.intellij.debugger.streams.ui.EvaluationAwareTraceWindow";
    }

    public void setTrace(@Nonnull ResolvedTracingResult resolvedTrace, @Nonnull DebuggerCommandLauncher launcher, @Nonnull GenericEvaluationContext context,
                         @Nonnull CollectionTreeBuilder builder) {
        if (Disposer.isDisposed(myDisposable)) {
            return;
        }

        ResolvedStreamChain chain = resolvedTrace.getResolvedChain();

        assert chain.length() == myTabContents.size();
        List<TraceControllerImpl> controllers = createControllers(resolvedTrace);

        if (controllers.isEmpty()) {
            return;
        }
        List<TraceElement> trace = controllers.get(0).getTrace();
        CollectionTree tree = CollectionTree.create(controllers.get(0).getStreamResult(), trace, launcher, context, builder, "setTrace#Tree#0#");
        CollectionView sourceView = new CollectionView(tree);
        controllers.get(0).register(sourceView);
        myTabContents.get(0).setContent(sourceView, BorderLayout.CENTER);

        for (int i = 1; i < myTabContents.size(); i++) {
            if (i == myTabContents.size() - 1 &&
                (resolvedTrace.exceptionThrown() ||
                    resolvedTrace.getSourceChain().getTerminationCall().returnsVoid())) {
                break;
            }

            MyPlaceholder tab = myTabContents.get(i);
            TraceController previous = controllers.get(i - 1);
            TraceController current = controllers.get(i);

            StreamTracesMappingView
                view = new StreamTracesMappingView(launcher, context, previous, current, builder, "setTrace#MappingView#" + i + "#");
            tab.setContent(view, BorderLayout.CENTER);
        }

        TraceElement result = resolvedTrace.getResult();
        MyPlaceholder resultTab = myTabContents.get(myTabContents.size() - 1);

        if (resolvedTrace.exceptionThrown()) {
            JBLabel label = new JBLabel(XDebuggerLocalize.tabContentExceptionThrown().get(), SwingConstants.CENTER);
            resultTab.setContent(label, BorderLayout.CENTER);
            setTitle(XDebuggerLocalize.streamDebuggerDialogWithExceptionTitle());
            ExceptionView exceptionView = new ExceptionView(launcher, context, result, builder);
            Disposer.register(myDisposable, exceptionView);
            myTabsPane.insertTab(XDebuggerLocalize.exceptionTabName().get(), PlatformIconGroup.generalError(), exceptionView, "", 0);
            myTabsPane.setSelectedIndex(0);
        }
        else if (resolvedTrace.getSourceChain().getTerminationCall().returnsVoid()) {
            JBLabel label = new JBLabel(XDebuggerLocalize.tabContentNoResult().get(), SwingConstants.CENTER);
            resultTab.setContent(label, BorderLayout.CENTER);
        }

        FlatView flatView = new FlatView(controllers, launcher, context, builder, "setTrace#FlatView#");
        myFlatContent.setContent(flatView, BorderLayout.CENTER);
        myCenterPane.revalidate();
        myCenterPane.repaint();
    }

    public void setFailMessage(@Nonnull String reason) {
        List<MyPlaceholder> placeholders = new ArrayList<>(myTabContents);
        placeholders.addFirst(myFlatContent);

        for (MyPlaceholder placeholder : placeholders) {
            placeholder.setContent(new JBLabel(reason, SwingConstants.CENTER), BorderLayout.CENTER);
        }
    }

    @Override
    @Nonnull
    protected Action[] createActions() {
        return new Action[]{new DialogWrapperExitAction(CommonLocalize.buttonClose().map(Presentation.NO_MNEMONIC).get(), CLOSE_EXIT_CODE)};
    }

    @Override
    @Nonnull
    protected Action[] createLeftSideActions() {
        return new Action[]{new MyToggleViewAction()};
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return myCenterPane;
    }

    private @Nonnull List<TraceControllerImpl> createControllers(@Nonnull ResolvedTracingResult resolvedResult) {
        List<TraceControllerImpl> controllers = new ArrayList<>();
        ResolvedStreamChain chain = resolvedResult.getResolvedChain();

        List<ResolvedStreamCall.Intermediate> intermediateCalls = chain.getIntermediateCalls();
        NextAwareState firstState = intermediateCalls.isEmpty()
            ? chain.getTerminator().getStateBefore()
            : intermediateCalls.get(0).getStateBefore();
        TraceControllerImpl firstController = new TraceControllerImpl(firstState);

        controllers.add(firstController);
        TraceControllerImpl prevController = firstController;
        for (ResolvedStreamCall.Intermediate intermediate : intermediateCalls) {
            PrevAwareState after = intermediate.getStateAfter();
            TraceControllerImpl controller = new TraceControllerImpl(after);

            prevController.setNextController(controller);
            controller.setPreviousController(prevController);
            prevController = controller;

            controllers.add(controller);
        }

        ResolvedStreamCall.Terminator terminator = chain.getTerminator();
        IntermediateState afterTerminationState = terminator.getStateAfter();
        if (afterTerminationState != null && !terminator.getCall().returnsVoid()) {

            TraceControllerImpl terminationController = new TraceControllerImpl(afterTerminationState);

            terminationController.setPreviousController(prevController);
            prevController.setNextController(terminationController);
            controllers.add(terminationController);
        }

        controllers.forEach(x -> Disposer.register(myDisposable, x));
        return controllers;
    }

    private static void updateWindowMode(@Nonnull MyCenterPane pane, @Nonnull MyMode mode) {
        pane.getLayout().show(pane, mode.name());
        ApplicationPropertiesComponent.getInstance().setValue(IS_FLAT_MODE_PROPERTY, MyMode.FLAT.equals(mode));
    }

    private static @Nonnull LocalizeValue getButtonText(@Nonnull MyMode currentState) {
        return MyMode.SPLIT.equals(currentState)
            ? XDebuggerLocalize.streamDebuggerDialogFlatModeButton()
            : XDebuggerLocalize.streamDebuggerDialogSplitModeButton();
    }

    private class MyToggleViewAction extends DialogWrapperAction {
        MyToggleViewAction() {
            super(getButtonText(myMode));
        }

        @Override
        protected void doAction(ActionEvent e) {
            JButton button = getButton(this);
            if (button != null) {
                myMode = toggleMode(myMode);
                button.setText(getButtonText(myMode).get());
            }

            updateWindowMode(myCenterPane, myMode);
        }

        private static @Nonnull MyMode toggleMode(@Nonnull MyMode mode) {
            return MyMode.FLAT.equals(mode) ? MyMode.SPLIT : MyMode.FLAT;
        }
    }

    private static class MyPlaceholder extends JPanel {
        MyPlaceholder() {
            super(new BorderLayout());
            add(new JBLabel(XDebuggerLocalize.evaluationInProgress().get(), SwingConstants.CENTER), BorderLayout.CENTER);
        }

        void setContent(@Nonnull JComponent view, String placement) {
            Arrays.stream(getComponents()).forEach(this::remove);
            add(view, placement);
            revalidate();
            repaint();
        }
    }

    private enum MyMode {
        FLAT,
        SPLIT
    }

    private static class MyCenterPane extends JPanel {
        MyCenterPane() {
            super(new JBCardLayout());
        }

        @Override
        public JBCardLayout getLayout() {
            return (JBCardLayout) super.getLayout();
        }
    }
}
