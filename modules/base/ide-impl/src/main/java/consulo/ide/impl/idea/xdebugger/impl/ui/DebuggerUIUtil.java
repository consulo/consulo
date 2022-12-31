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
package consulo.ide.impl.idea.xdebugger.impl.ui;

import consulo.application.ApplicationManager;
import consulo.application.ui.DimensionService;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.colorScheme.EditorColorsScheme;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.*;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.event.XBreakpointAdapter;
import consulo.execution.debug.event.XBreakpointListener;
import consulo.execution.debug.frame.XFullValueEvaluator;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueModifier;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.debug.ui.XValueTextProvider;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ide.impl.idea.ui.AppUIUtil;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointBase;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.BreakpointsDialogFactory;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.XLightBreakpointPropertiesPanel;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTreeState;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import consulo.language.editor.hint.HintColorUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.Size;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.EditorColorsUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ref.Ref;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DebuggerUIUtil {
  @NonNls public static final String FULL_VALUE_POPUP_DIMENSION_KEY = "XDebugger.FullValuePopup";

  private DebuggerUIUtil() {
  }

  public static void enableEditorOnCheck(final JCheckBox checkbox, final JComponent textfield) {
    checkbox.addActionListener(e -> {
      boolean selected = checkbox.isSelected();
      textfield.setEnabled(selected);
    });
    textfield.setEnabled(checkbox.isSelected());
  }

  public static void focusEditorOnCheck(final JCheckBox checkbox, final JComponent component) {
    final Runnable runnable = () -> IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(component);
    checkbox.addActionListener(e -> {
      if (checkbox.isSelected()) {
        SwingUtilities.invokeLater(runnable);
      }
    });
  }

  public static void invokeLater(Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable);
  }

  @Deprecated
  public static RelativePoint calcPopupLocation(@Nonnull Editor editor, final int line) {
    Point p = editor.logicalPositionToXY(new LogicalPosition(line + 1, 0));

    final Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
    if (!visibleArea.contains(p)) {
      p = new Point((visibleArea.x + visibleArea.width) / 2, (visibleArea.y + visibleArea.height) / 2);
    }
    return new RelativePoint(editor.getContentComponent(), p);
  }

  @Nullable
  public static RelativePoint getPositionForPopup(@Nonnull Editor editor, int line) {
    Point p = editor.logicalPositionToXY(new LogicalPosition(line + 1, 0));
    return editor.getScrollingModel().getVisibleArea().contains(p) ? new RelativePoint(editor.getContentComponent(), p) : null;
  }

  public static void showPopupForEditorLine(@Nonnull JBPopup popup, @Nonnull Editor editor, int line) {
    RelativePoint point = getPositionForPopup(editor, line);
    if (point != null) {
      popup.show(point);
    }
    else {
      Project project = editor.getProject();
      if (project != null) {
        popup.showCenteredInCurrentWindow(project);
      }
      else {
        popup.showInFocusCenter();
      }
    }
  }

  public static void showValuePopup(@Nonnull XFullValueEvaluator evaluator, @Nonnull MouseEvent event, @Nonnull Project project, @Nullable Editor editor) {
    EditorTextField textArea = new TextViewer("Evaluating...", project);
    textArea.setBackground(TargetAWT.to(HintColorUtil.getInformationColor()));

    final FullValueEvaluationCallbackImpl callback = new FullValueEvaluationCallbackImpl(textArea);
    evaluator.startEvaluation(callback);

    Size size = DimensionService.getInstance().getSize(FULL_VALUE_POPUP_DIMENSION_KEY, project);
    if (size == null) {
      Dimension frameSize = TargetAWT.to(WindowManager.getInstance().getWindow(project)).getSize();
      size = new Size(frameSize.width / 2, frameSize.height / 2);
    }

    textArea.setPreferredSize(new Dimension(size.getWidth(), size.getHeight()));

    JBPopup popup = createValuePopup(project, textArea, callback);
    if (editor == null) {
      Rectangle bounds = new Rectangle(event.getLocationOnScreen(), new Dimension(size.getWidth(), size.getHeight()));
      ScreenUtil.fitToScreenVertical(bounds, 5, 5, true);
      if (size.getWidth() != bounds.width || size.getHeight() != bounds.height) {
        size = new Size(bounds.getSize().width, bounds.getSize().height);
        textArea.setPreferredSize(new Dimension(size.getWidth(), size.getHeight()));
      }
      popup.showInScreenCoordinates(event.getComponent(), bounds.getLocation());
    }
    else {
      editor.showPopupInBestPositionFor(popup);
    }
  }

  public static JBPopup createValuePopup(Project project, JComponent component, @Nullable final FullValueEvaluationCallbackImpl callback) {
    ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(component, null);
    builder.setResizable(true).setMovable(true).setDimensionServiceKey(project, FULL_VALUE_POPUP_DIMENSION_KEY, false).setRequestFocus(false);
    if (callback != null) {
      builder.setCancelCallback(() -> {
        callback.setObsolete();
        return true;
      });
    }
    return builder.createPopup();
  }

  public static void showXBreakpointEditorBalloon(final Project project,
                                                  @Nullable final Point point,
                                                  final JComponent component,
                                                  final boolean showAllOptions,
                                                  final XBreakpoint breakpoint) {
    final XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
    final XLightBreakpointPropertiesPanel propertiesPanel =
            new XLightBreakpointPropertiesPanel(project, breakpointManager, (XBreakpointBase)breakpoint, showAllOptions);

    final Ref<Balloon> balloonRef = Ref.create(null);
    final Ref<Boolean> isLoading = Ref.create(Boolean.FALSE);
    final Ref<Boolean> moreOptionsRequested = Ref.create(Boolean.FALSE);

    propertiesPanel.setDelegate(() -> {
      if (!isLoading.get()) {
        propertiesPanel.saveProperties();
      }
      if (!balloonRef.isNull()) {
        balloonRef.get().hide();
      }
      showXBreakpointEditorBalloon(project, point, component, true, breakpoint);
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

    final JComponent mainPanel = propertiesPanel.getMainPanel();
    final Balloon balloon = showBreakpointEditor(project, mainPanel, point, component, showMoreOptions, breakpoint);
    balloonRef.set(balloon);

    final XBreakpointListener<XBreakpoint<?>> breakpointListener = new XBreakpointAdapter<XBreakpoint<?>>() {
      @Override
      public void breakpointRemoved(@Nonnull XBreakpoint<?> removedBreakpoint) {
        if (removedBreakpoint.equals(breakpoint)) {
          balloon.hide();
        }
      }
    };

    balloon.addListener(new JBPopupListener.Adapter() {
      @Override
      public void onClosed(LightweightWindowEvent event) {
        propertiesPanel.saveProperties();
        propertiesPanel.dispose();
        breakpointManager.removeBreakpointListener(breakpointListener);
      }
    });

    breakpointManager.addBreakpointListener(breakpointListener);
    ApplicationManager.getApplication().invokeLater(() -> IdeFocusManager.findInstance().requestFocus(mainPanel, true));
  }

  public static Balloon showBreakpointEditor(Project project,
                                             final JComponent mainPanel,
                                             final Point whereToShow,
                                             final JComponent component,
                                             @Nullable final Runnable showMoreOptions,
                                             Object breakpoint) {
    final BreakpointEditor editor = new BreakpointEditor();
    editor.setPropertiesPanel(mainPanel);
    editor.setShowMoreOptionsLink(true);

    final JPanel panel = editor.getMainPanel();
    final Balloon balloon =
            JBPopupFactory.getInstance().createDialogBalloonBuilder(panel, null).setHideOnClickOutside(true).setCloseButtonEnabled(false).setAnimationCycle(0)
                    .setBlockClicksThroughBalloon(true).createBalloon();


    editor.setDelegate(new BreakpointEditor.Delegate() {
      @Override
      public void done() {
        balloon.hide();
      }

      @Override
      public void more() {
        assert showMoreOptions != null;
        balloon.hide();
        showMoreOptions.run();
      }
    });

    final ComponentAdapter moveListener = new ComponentAdapter() {
      @Override
      public void componentMoved(ComponentEvent e) {
        balloon.hide();
      }
    };
    component.addComponentListener(moveListener);
    Disposer.register(balloon, () -> component.removeComponentListener(moveListener));

    if (whereToShow == null) {
      balloon.showInCenterOf(component);
    }
    else {
      //todo[kb] modify and move to BalloonImpl?
      final Window window = SwingUtilities.windowForComponent(component);
      final RelativePoint p = new RelativePoint(component, whereToShow);
      if (window != null) {
        final RelativePoint point = new RelativePoint(window, new Point(0, 0));
        if (p.getScreenPoint().getX() - point.getScreenPoint().getX() < 40) { // triangle + offsets is ~40px
          p.getPoint().x += 40;
        }
      }
      balloon.show(p, Balloon.Position.below);
    }

    BreakpointsDialogFactory.getInstance(project).setBalloonToHide(balloon, breakpoint);

    return balloon;
  }

  @Nonnull
  public static EditorColorsScheme getColorScheme() {
    return EditorColorsUtil.getGlobalOrDefaultColorScheme();
  }

  @Nonnull
  public static EditorColorsScheme getColorScheme(@Nullable JComponent component) {
    return EditorColorsUtil.getColorSchemeForComponent(component);
  }

  private static class FullValueEvaluationCallbackImpl implements XFullValueEvaluator.XFullValueEvaluationCallback {
    private final AtomicBoolean myObsolete = new AtomicBoolean(false);
    private final EditorTextField myTextArea;

    public FullValueEvaluationCallbackImpl(final EditorTextField textArea) {
      myTextArea = textArea;
    }

    @Override
    public void evaluated(@Nonnull final String fullValue) {
      evaluated(fullValue, null);
    }

    @Override
    public void evaluated(@Nonnull final String fullValue, @Nullable final Font font) {
      AppUIUtil.invokeOnEdt(() -> {
        myTextArea.setText(fullValue);
        if (font != null) {
          myTextArea.setFont(font);
        }
      });
    }

    @Override
    public void errorOccurred(@Nonnull final String errorMessage) {
      AppUIUtil.invokeOnEdt(() -> {
        myTextArea.setForeground(XDebuggerUIConstants.ERROR_MESSAGE_ATTRIBUTES.getFgColor());
        myTextArea.setText(errorMessage);
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

  @Nullable
  public static String getNodeRawValue(@Nonnull XValueNodeImpl valueNode) {
    if (valueNode.getValueContainer() instanceof XValueTextProvider) {
      return ((XValueTextProvider)valueNode.getValueContainer()).getValueText();
    }
    else {
      return valueNode.getRawValue();
    }
  }

  /**
   * Checks if value has evaluation expression ready, or calculation is pending
   */
  public static boolean hasEvaluationExpression(@Nonnull XValue value) {
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

  public static void registerExtraHandleShortcuts(final ListPopupImpl popup, String... actionNames) {
    for (String name : actionNames) {
      KeyStroke stroke = KeymapUtil.getKeyStroke(ActionManager.getInstance().getAction(name).getShortcutSet());
      if (stroke != null) {
        popup.registerAction("handleSelection " + stroke, stroke, new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            popup.handleSelect(true);
          }
        });
      }
    }
  }

  public static String getSelectionShortcutsAdText(String... actionNames) {
    StringBuilder res = new StringBuilder();
    for (String name : actionNames) {
      KeyStroke stroke = KeymapUtil.getKeyStroke(ActionManager.getInstance().getAction(name).getShortcutSet());
      if (stroke != null) {
        if (res.length() > 0) res.append(", ");
        res.append(KeymapUtil.getKeystrokeText(stroke));
      }
    }
    return XDebuggerBundle.message("ad.extra.selection.shortcut", res.toString());
  }

  public static boolean isObsolete(Object object) {
    return object instanceof Obsolescent && ((Obsolescent)object).isObsolete();
  }

  public static void setTreeNodeValue(XValueNodeImpl valueNode, String text, final Consumer<String> errorConsumer) {
    final XDebuggerTree tree = valueNode.getTree();
    final Project project = tree.getProject();
    XValueModifier modifier = valueNode.getValueContainer().getModifier();
    if (modifier == null) return;
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
      public void errorOccurred(@Nonnull final String errorMessage) {
        AppUIUtil.invokeOnEdt(() -> {
          tree.rebuildAndRestore(treeState);
          errorConsumer.accept(errorMessage);
        });
        XDebuggerUtil.getInstance().rebuildAllSessionsViews(project);
      }

      boolean isDetachedTree(XDebuggerTree tree) {
        return DataManager.getInstance().getDataContext(tree).getData(XDebugSessionTab.TAB_KEY) == null;
      }
    });
  }
}
