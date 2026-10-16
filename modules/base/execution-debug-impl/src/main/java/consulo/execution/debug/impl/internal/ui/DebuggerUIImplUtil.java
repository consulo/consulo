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
package consulo.execution.debug.impl.internal.ui;

import consulo.application.ApplicationManager;
import consulo.application.ui.DimensionService;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.execution.debug.XBreakpointManager;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.event.XTopicBreakpointListener;
import consulo.execution.debug.frame.XFullValueEvaluator;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueModifier;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointsDialogFactory;
import consulo.execution.debug.impl.internal.breakpoint.ui.XLightBreakpointPropertiesPanel;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreeState;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.DebuggerUIUtil;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.debug.ui.XValueTextProvider;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.util.AppUIUtil;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.RelativePoint2D;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.util.ShortcutUtil;
import consulo.ui.ex.awt.EditorColorsUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Obsolescent;
import consulo.util.lang.ref.Ref;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DebuggerUIImplUtil {
    public static final String FULL_VALUE_POPUP_DIMENSION_KEY = DebuggerUIUtil.FULL_VALUE_POPUP_DIMENSION_KEY;

    private DebuggerUIImplUtil() {
    }

    @Deprecated
    public static void invokeLater(Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable);
    }

    public static @Nullable RelativePoint2D getPositionForPopup(Editor editor, int line) {
        return DebuggerUIUtil.getPositionForPopup(editor, line);
    }

    public static void showPopupForEditorLine(JBPopup popup, Editor editor, int line) {
        DebuggerUIUtil.showPopupForEditorLine(popup, editor, line);
    }

    public static void showValuePopup(XFullValueEvaluator evaluator, MouseEvent event, Project project, @Nullable Editor editor) {
        EditorTextField textArea = new TextViewer("Evaluating...", project);
        textArea.setBackground(TargetAWT.to(HintColorUtil.getInformationColor()));

        FullValueEvaluationCallbackImpl callback = new FullValueEvaluationCallbackImpl(textArea);
        evaluator.startEvaluation(callback);

        Size2D size = DimensionService.getInstance().getSize(FULL_VALUE_POPUP_DIMENSION_KEY, project);
        if (size == null) {
            Dimension frameSize = TargetAWT.to(WindowManager.getInstance().getWindow(project)).getSize();
            size = new Size2D(frameSize.width / 2, frameSize.height / 2);
        }

        textArea.setPreferredSize(new Dimension(size.width(), size.height()));

        JBPopup popup = createValuePopup(project, textArea, callback);
        if (editor == null) {
            Rectangle bounds = new Rectangle(event.getLocationOnScreen(), new Dimension(size.width(), size.height()));
            ScreenUtil.fitToScreenVertical(bounds, 5, 5, true);
            if (size.width() != bounds.width || size.height() != bounds.height) {
                size = new Size2D(bounds.getSize().width, bounds.getSize().height);
                textArea.setPreferredSize(new Dimension(size.width(), size.height()));
            }
            popup.showInScreenCoordinates(event.getComponent(), bounds.getLocation());
        }
        else {
            editor.showPopupInBestPositionFor(popup);
        }
    }

    public static JBPopup createValuePopup(Project project, JComponent component, @Nullable FullValueEvaluationCallbackImpl callback) {
        return DebuggerUIUtil.createValuePopup(project, component, callback == null ? null : callback::setObsolete);
    }

    @RequiredUIAccess
    public static void showXBreakpointEditorBalloon(
        Project project,
        Component anchor,
        @Nullable Point2D point,
        boolean showAllOptions,
        XBreakpoint breakpoint
    ) {
        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        XLightBreakpointPropertiesPanel propertiesPanel =
            new XLightBreakpointPropertiesPanel(project, breakpointManager, (XBreakpointBase) breakpoint, showAllOptions);

        Ref<LightPopup> popupRef = Ref.create(null);
        Ref<Boolean> isLoading = Ref.create(Boolean.FALSE);
        Ref<Boolean> moreOptionsRequested = Ref.create(Boolean.FALSE);

        propertiesPanel.setDelegate(() -> {
            if (!isLoading.get()) {
                propertiesPanel.saveProperties();
            }
            if (!popupRef.isNull()) {
                popupRef.get().close();
            }
            showXBreakpointEditorBalloon(project, anchor, point, true, breakpoint);
            moreOptionsRequested.set(true);
        });

        isLoading.set(Boolean.TRUE);
        propertiesPanel.loadProperties();
        isLoading.set(Boolean.FALSE);

        if (moreOptionsRequested.get()) {
            return;
        }

        Runnable showMoreOptions = () -> {
            propertiesPanel.saveProperties();
            propertiesPanel.dispose();
            BreakpointsDialogFactory.getInstance(project).showDialog(breakpoint);
        };

        LightPopup popup = showBreakpointEditor(project, propertiesPanel.getMainPanel(), point, anchor, showMoreOptions, breakpoint);
        popupRef.set(popup);

        XTopicBreakpointListener breakpointListener = new XTopicBreakpointListener() {
            @Override
            public void breakpointRemoved(XBreakpoint<?> removedBreakpoint) {
                if (removedBreakpoint.equals(breakpoint)) {
                    popup.close();
                }
            }
        };

        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(XTopicBreakpointListener.class, breakpointListener);

        popup.addCloseListener(event -> {
            propertiesPanel.saveProperties();
            propertiesPanel.dispose();
            connection.disconnect();
        });

        UIAccess.current().give(propertiesPanel::requestDefaultFocus);
    }

    @RequiredUIAccess
    public static LightPopup showBreakpointEditor(
        Project project,
        Component mainPanel,
        @Nullable Point2D whereToShow,
        Component anchor,
        @Nullable Runnable showMoreOptions,
        Object breakpoint
    ) {
        BreakpointEditor editor = new BreakpointEditor();
        editor.setPropertiesPanel(mainPanel);
        editor.setShowMoreOptionsLink(showMoreOptions != null);

        LightPopup popup = LightPopup.create(PopupOptions.builder().position(PopupPosition.END).arrow().build());
        popup.setContent(editor.getMainPanel());

        editor.setDelegate(new BreakpointEditor.Delegate() {
            @Override
            public void done() {
                popup.close();
            }

            @Override
            public void more() {
                popup.close();
                if (showMoreOptions != null) {
                    showMoreOptions.run();
                }
            }
        });

        if (whereToShow == null) {
            popup.showBy(anchor);
        }
        else {
            popup.showAt(anchor, whereToShow.x(), whereToShow.y(), 0);
        }

        BreakpointsDialogFactory.getInstance(project).setPopupToHide(popup, breakpoint);

        return popup;
    }


    
    public static EditorColorsScheme getColorScheme() {
        return EditorColorsUtil.getGlobalOrDefaultColorScheme();
    }

    
    public static EditorColorsScheme getColorScheme(@Nullable JComponent component) {
        return EditorColorsUtil.getColorSchemeForComponent(component);
    }

    private static class FullValueEvaluationCallbackImpl implements XFullValueEvaluator.XFullValueEvaluationCallback {
        private final AtomicBoolean myObsolete = new AtomicBoolean(false);
        private final EditorTextField myTextArea;

        public FullValueEvaluationCallbackImpl(EditorTextField textArea) {
            myTextArea = textArea;
        }

        @Override
        public void evaluated(String fullValue) {
            evaluated(fullValue, null);
        }

        @Override
        public void evaluated(String fullValue, @Nullable Font font) {
            AppUIUtil.invokeOnEdt(() -> {
                myTextArea.setText(fullValue);
                if (font != null) {
                    myTextArea.setFont(font);
                }
            });
        }

        @Override
        public void errorOccurred(LocalizeValue errorMessage) {
            AppUIUtil.invokeOnEdt(() -> {
                myTextArea.setForeground(XDebuggerUIConstants.ERROR_MESSAGE_ATTRIBUTES.getFgColor());
                myTextArea.setText(errorMessage.get());
            });
        }

        private void setObsolete() {
            myObsolete.set(true);
        }

        @Override
        public boolean isObsolete() {
            return myObsolete.get();
        }
    }

    public static @Nullable String getNodeRawValue(XValueNodeImpl valueNode) {
        if (valueNode.getValueContainer() instanceof XValueTextProvider) {
            return ((XValueTextProvider) valueNode.getValueContainer()).getValueText();
        }
        else {
            return valueNode.getRawValue();
        }
    }

    /**
     * Checks if value has evaluation expression ready, or calculation is pending
     */
    public static boolean hasEvaluationExpression(XValue value) {
        AsyncResult<XExpression> promise = value.calculateEvaluationExpression();
        if (promise.isDone()) {
            return promise.getResult() != null;
        }
        return true;
    }

    public static void registerActionOnComponent(String name, JComponent component, Disposable parentDisposable) {
        AnAction action = ActionManager.getInstance().getAction(name);
        action.registerCustomShortcutSet(action.getShortcutSet(), component, parentDisposable);
    }

    public static void registerExtraHandleShortcuts(ListPopup popup, String... actionNames) {
        DebuggerUIUtil.registerExtraHandleShortcuts(popup, actionNames);
    }

    public static String getSelectionShortcutsAdText(String... actionNames) {
        StringBuilder res = new StringBuilder();
        for (String name : actionNames) {
            KeyStroke stroke = ShortcutUtil.getKeyStroke(ActionManager.getInstance().getAction(name).getShortcutSet());
            if (stroke != null) {
                if (res.length() > 0) {
                    res.append(", ");
                }
                res.append(ShortcutUtil.getKeystrokeText(stroke));
            }
        }
        return XDebuggerLocalize.adExtraSelectionShortcut(res.toString()).get();
    }

    public static boolean isObsolete(Object object) {
        return Obsolescent.isObsolete(object);
    }

    public static void setTreeNodeValue(XValueNodeImpl valueNode, String text, @RequiredUIAccess Consumer<LocalizeValue> errorConsumer) {
        final XDebuggerTree tree = valueNode.getTree();
        final Project project = tree.getProject();
        XValueModifier modifier = valueNode.getValueContainer().getModifier();
        if (modifier == null) {
            return;
        }
        final XDebuggerTreeState treeState = XDebuggerTreeState.saveState(tree);
        valueNode.setValueModificationStarted();
        modifier.setValue(text, new XValueModifier.XModificationCallback() {
            @Override
            public void valueModified() {
                if (isDetachedTree(tree)) {
                    AppUIUtil.invokeOnEdt(() -> tree.rebuildAndRestore(treeState));
                }
                XDebuggerUtil.getInstance().rebuildAllSessionsViews(project);
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                AppUIUtil.invokeOnEdt(() -> {
                    tree.rebuildAndRestore(treeState);
                    errorConsumer.accept(errorMessage);
                });
                XDebuggerUtil.getInstance().rebuildAllSessionsViews(project);
            }

            boolean isDetachedTree(XDebuggerTree tree) {
                return !DataManager.getInstance().getDataContext(tree).hasData(XDebugSessionTab.TAB_KEY);
            }
        });
    }
}
