/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.FileDocumentManager;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.ExpressionInfo;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.impl.internal.action.handler.XDebuggerEvaluateActionHandler;
import consulo.execution.debug.impl.internal.ui.DebuggerUIUtil;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodePresentationConfigurator;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.language.LanguageConsoleView;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.awt.LinkMouseListenerBase;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * @author nik
 */
public class XValueHint extends AbstractValueHint {
  private static final Logger LOG = Logger.getInstance(XValueHint.class);

  private final XDebuggerEvaluator myEvaluator;
  private final XDebugSession myDebugSession;
  private final String myExpression;
  private final String myValueName;
  private final
  @Nullable
  XSourcePosition myExpressionPosition;
  private final ExpressionInfo myExpressionInfo;
  private Disposable myDisposable;

  private static final Key<XValueHint> HINT_KEY = Key.create("allows only one value hint per editor");

  public XValueHint(
    @Nonnull Project project,
    @Nonnull Editor editor,
    @Nonnull Point point,
    @Nonnull ValueHintType type,
    @Nonnull ExpressionInfo expressionInfo,
    @Nonnull XDebuggerEvaluator evaluator,
    @Nonnull XDebugSession session
  ) {
    super(project, editor, point, type, expressionInfo.getTextRange());

    myEvaluator = evaluator;
    myDebugSession = session;
    myExpression = XDebuggerEvaluateActionHandler.getExpressionText(expressionInfo, editor.getDocument());
    myValueName = XDebuggerEvaluateActionHandler.getDisplayText(expressionInfo, editor.getDocument());
    myExpressionInfo = expressionInfo;

    VirtualFile file;
    ConsoleView consoleView = ConsoleView.CONSOLE_VIEW_IN_EDITOR_VIEW.get(editor);
    if (consoleView instanceof LanguageConsoleView) {
      LanguageConsoleView console = ((LanguageConsoleView)consoleView);
      file = console.getHistoryViewer() == editor ? console.getVirtualFile() : null;
    }
    else {
      file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    }

    myExpressionPosition = file != null
      ? XDebuggerUtil.getInstance().createPositionByOffset(file, expressionInfo.getTextRange().getStartOffset()) : null;
  }

  @Override
  protected boolean canShowHint() {
    return true;
  }

  @Override
  protected boolean showHint(final JComponent component) {
    boolean result = super.showHint(component);
    if (result && getType() == ValueHintType.MOUSE_OVER_HINT) {
      myDisposable = Disposable.newDisposable();
      ShortcutSet shortcut = ActionManager.getInstance().getAction("ShowErrorDescription").getShortcutSet();
      new DumbAwareAction() {
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
          hideHint();
          final Point point = new Point(myPoint.x, myPoint.y + getEditor().getLineHeight());
          new XValueHint(
            getProject(),
            getEditor(),
            point,
            ValueHintType.MOUSE_CLICK_HINT,
            myExpressionInfo,
            myEvaluator,
            myDebugSession
          ).invokeHint();
        }
      }.registerCustomShortcutSet(shortcut, getEditor().getContentComponent(), myDisposable);
    }
    if (result) {
      XValueHint prev = getEditor().getUserData(HINT_KEY);
      if (prev != null) {
        prev.hideHint();
      }
      getEditor().putUserData(HINT_KEY, this);
    }
    return result;
  }

  @Override
  protected void onHintHidden() {
    super.onHintHidden();
    XValueHint prev = getEditor().getUserData(HINT_KEY);
    if (prev == this) {
      getEditor().putUserData(HINT_KEY, null);
    }
    if (myDisposable != null) {
      Disposer.dispose(myDisposable);
      myDisposable = null;
    }
  }

  @Override
  public void hideHint() {
    super.hideHint();
    if (myDisposable != null) {
      Disposer.dispose(myDisposable);
    }
  }

  @Override
  protected void evaluateAndShowHint() {
    myEvaluator.evaluate(myExpression, new XEvaluationCallbackBase() {
      @Override
      public void evaluated(@Nonnull final XValue result) {
        result.computePresentation(new XValueNodePresentationConfigurator.ConfigurableXValueNodeImpl() {
          private XFullValueEvaluator myFullValueEvaluator;
          private boolean myShown = false;

          @Override
          public void applyPresentation(
            @Nullable Image icon,
            @Nonnull XValuePresentation valuePresenter,
            boolean hasChildren
          ) {
            if (isHintHidden()) {
              return;
            }

            SimpleColoredText text = new SimpleColoredText();
            text.append(StringUtil.trimMiddle(myValueName, 200), XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES);
            XValueNodeImpl.buildText(valuePresenter, text);

            if (!hasChildren) {
              SimpleColoredComponent component = HintUtil.createInformationComponent();
              text.appendToComponent(component);
              if (myFullValueEvaluator != null) {
                component.append(
                  myFullValueEvaluator.getLinkText(),
                  XDebuggerTreeNodeHyperlink.TEXT_ATTRIBUTES,
                  (Consumer<MouseEvent>)event ->
                    DebuggerUIUtil.showValuePopup(myFullValueEvaluator, event, getProject(), getEditor())
                );
                LinkMouseListenerBase.installSingleTagOn(component);
              }
              showHint(component);
            }
            else if (getType() == ValueHintType.MOUSE_CLICK_HINT) {
              if (!myShown) {
                showTree(result);
              }
            }
            else {
              if (getType() == ValueHintType.MOUSE_OVER_HINT) {
                text.insert(
                  0,
                  "(" + KeymapUtil.getFirstKeyboardShortcutText("ShowErrorDescription") + ") ",
                  SimpleTextAttributes.GRAYED_ATTRIBUTES
                );
              }

              JComponent component = createExpandableHintComponent(text, () -> showTree(result));
              showHint(component);
            }
            myShown = true;
          }

          @Override
          public void setFullValueEvaluator(@Nonnull XFullValueEvaluator fullValueEvaluator) {
            myFullValueEvaluator = fullValueEvaluator;
          }

          @Override
          public boolean isObsolete() {
            return isHintHidden();
          }
        }, XValuePlace.TOOLTIP);
      }

      @Override
      public void errorOccurred(@Nonnull final String errorMessage) {
        if (getType() == ValueHintType.MOUSE_CLICK_HINT) {
          ApplicationManager.getApplication().invokeLater(() -> showHint(HintUtil.createErrorLabel(errorMessage)));
        }
        LOG.debug("Cannot evaluate '" + myExpression + "':" + errorMessage);
      }
    }, myExpressionPosition);
  }

  private void showTree(@Nonnull XValue value) {
    XValueMarkers<?, ?> valueMarkers = myDebugSession.getValueMarkers();
    XDebuggerTreeCreator creator = new XDebuggerTreeCreator(
      myDebugSession.getProject(),
      myDebugSession.getDebugProcess().getEditorsProvider(),
      myDebugSession.getCurrentPosition(),
      valueMarkers
    );
    showTreePopup(creator, Pair.create(value, myValueName));
  }
}
