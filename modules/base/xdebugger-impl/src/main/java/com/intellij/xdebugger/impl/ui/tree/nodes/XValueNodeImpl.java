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
package com.intellij.xdebugger.impl.ui.tree.nodes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ThreeState;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.*;
import com.intellij.xdebugger.frame.presentation.XValuePresentation;
import com.intellij.xdebugger.impl.XDebuggerInlayUtil;
import com.intellij.xdebugger.impl.frame.XDebugView;
import com.intellij.xdebugger.impl.frame.XValueMarkers;
import com.intellij.xdebugger.impl.frame.XValueWithInlinePresentation;
import com.intellij.xdebugger.impl.frame.XVariablesView;
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil;
import com.intellij.xdebugger.impl.ui.XDebuggerUIConstants;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree;
import com.intellij.xdebugger.settings.XDebuggerSettingsManager;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Comparator;

/**
 * @author nik
 */
public class XValueNodeImpl extends XValueContainerNode<XValue>
        implements XValueNode, XCompositeNode, XValueNodePresentationConfigurator.ConfigurableXValueNode, RestorableStateNode {
  public static final Comparator<XValueNodeImpl> COMPARATOR = (o1, o2) -> StringUtil.naturalCompare(o1.getName(), o2.getName());

  private static final int MAX_NAME_LENGTH = 100;

  private final String myName;
  @Nullable
  private String myRawValue;
  private XFullValueEvaluator myFullValueEvaluator;
  private boolean myChanged;
  private XValuePresentation myValuePresentation;

  //todo[nik] annotate 'name' with @NotNull
  public XValueNodeImpl(XDebuggerTree tree, @Nullable XDebuggerTreeNode parent, String name, @Nonnull XValue value) {
    super(tree, parent, value);
    myName = name;

    value.computePresentation(this, XValuePlace.TREE);

    // add "Collecting" message only if computation is not yet done
    if (!isComputed()) {
      if (myName != null) {
        myText.append(myName, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES);
        myText.append(XDebuggerUIConstants.EQ_TEXT, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      myText.append(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE, XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES);
    }
  }

  @Override
  public void setPresentation(@Nullable Image icon, @NonNls @Nullable String type, @NonNls @Nonnull String value, boolean hasChildren) {
    XValueNodePresentationConfigurator.setPresentation(icon, type, value, hasChildren, this);
  }

  @Override
  public void setPresentation(@Nullable Image icon,
                              @NonNls @Nullable String type,
                              @NonNls @Nonnull String separator,
                              @NonNls @Nullable String value,
                              boolean hasChildren) {
    XValueNodePresentationConfigurator.setPresentation(icon, type, separator, value, hasChildren, this);
  }

  @Override
  public void setPresentation(@Nullable Image icon, @Nonnull XValuePresentation presentation, boolean hasChildren) {
    XValueNodePresentationConfigurator.setPresentation(icon, presentation, hasChildren, this);
  }

  @Override
  public void applyPresentation(@Nullable Image icon, @Nonnull XValuePresentation valuePresentation, boolean hasChildren) {
    // extra check for obsolete nodes - tree root was changed
    // too dangerous to put this into isObsolete - it is called from anywhere, not only EDT
    if (isObsolete()) return;

    setIcon(icon);
    myValuePresentation = valuePresentation;
    myRawValue = XValuePresentationUtil.computeValueText(valuePresentation);
    if (XDebuggerSettingsManager.getInstance().getDataViewSettings().isShowValuesInline()) {
      updateInlineDebuggerData();
    }
    updateText();
    setLeaf(!hasChildren);
    fireNodeChanged();
    myTree.nodeLoaded(this, myName);
  }

  public void updateInlineDebuggerData() {
    try {
      XDebugSession session = XDebugView.getSession(getTree());
      final XSourcePosition debuggerPosition = session == null ? null : session.getCurrentPosition();
      if (debuggerPosition == null) {
        return;
      }

      final XInlineDebuggerDataCallback callback = new XInlineDebuggerDataCallback() {
        @Override
        public void computed(XSourcePosition position) {
          if (isObsolete() || position == null) return;
          VirtualFile file = position.getFile();
          // filter out values from other files
          if (!Comparing.equal(debuggerPosition.getFile(), file)) {
            return;
          }
          final Document document = ReadAction.compute(() -> FileDocumentManager.getInstance().getDocument(file));
          if (document == null) return;

          XVariablesView.InlineVariablesInfo data = myTree.getProject().getUserData(XVariablesView.DEBUG_VARIABLES);
          if (data == null) {
            return;
          }

          if (!showAsInlay(file, position, debuggerPosition)) {
            data.put(file, position, XValueNodeImpl.this, document.getModificationStamp());

            myTree.updateEditor();
          }
        }
      };

      if (getValueContainer().computeInlineDebuggerData(callback) == ThreeState.UNSURE) {
        getValueContainer().computeSourcePosition(callback::computed);
      }
    }
    catch (Exception ignore) {
    }
  }

  private boolean showAsInlay(VirtualFile file, XSourcePosition position, XSourcePosition debuggerPosition) {
    if (!Registry.is("debugger.show.values.inplace")) return false;
    if (!debuggerPosition.getFile().equals(position.getFile()) || debuggerPosition.getLine() != position.getLine()) return false;
    XValue container = getValueContainer();
    if (!(container instanceof XValueWithInlinePresentation)) return false;
    String presentation = ((XValueWithInlinePresentation)container).computeInlinePresentation();
    if (presentation == null) return false;
    XDebuggerInlayUtil.createInlay(myTree.getProject(), file, position.getOffset(), presentation);
    return true;
  }

  @Override
  public void setFullValueEvaluator(@Nonnull final XFullValueEvaluator fullValueEvaluator) {
    invokeNodeUpdate(() -> {
      myFullValueEvaluator = fullValueEvaluator;
      XValueNodeImpl.this.fireNodeChanged();
    });
  }

  public void clearFullValueEvaluator() {
    myFullValueEvaluator = null;
  }

  private void updateText() {
    myText.clear();
    XValueMarkers<?, ?> markers = myTree.getValueMarkers();
    if (markers != null) {
      ValueMarkup markup = markers.getMarkup(getValueContainer());
      if (markup != null) {
        myText.append("[" + markup.getText() + "] ", new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, markup.getColor()));
      }
    }
    appendName();
    buildText(myValuePresentation, myText);
  }

  private void appendName() {
    if (!StringUtil.isEmpty(myName)) {
      SimpleTextAttributes attributes = myChanged ? XDebuggerUIConstants.CHANGED_VALUE_ATTRIBUTES : XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES;
      XValuePresentationUtil.renderValue(myName, myText, attributes, MAX_NAME_LENGTH, null);
    }
  }

  public static void buildText(@Nonnull XValuePresentation valuePresenter, @Nonnull ColoredTextContainer text) {
    buildText(valuePresenter, text, true);
  }

  public static void buildText(@Nonnull XValuePresentation valuePresenter, @Nonnull ColoredTextContainer text, boolean appendSeparator) {
    if (appendSeparator) {
      XValuePresentationUtil.appendSeparator(text, valuePresenter.getSeparator());
    }
    String type = valuePresenter.getType();
    if (type != null) {
      text.append("{" + type + "} ", XDebuggerUIConstants.TYPE_ATTRIBUTES);
    }
    valuePresenter.renderValue(new XValueTextRendererImpl(text));
  }

  @Override
  public void markChanged() {
    if (myChanged) return;

    ApplicationManager.getApplication().assertIsDispatchThread();
    myChanged = true;
    if (myName != null && myValuePresentation != null) {
      updateText();
      fireNodeChanged();
    }
  }

  @Nullable
  public XFullValueEvaluator getFullValueEvaluator() {
    return myFullValueEvaluator;
  }

  @Nullable
  @Override
  protected XDebuggerTreeNodeHyperlink getLink() {
    if (myFullValueEvaluator != null) {
      return new XDebuggerTreeNodeHyperlink(myFullValueEvaluator.getLinkText()) {
        @Override
        public boolean alwaysOnScreen() {
          return true;
        }

        @Override
        public void onClick(MouseEvent event) {
          if (myFullValueEvaluator.isShowValuePopup()) {
            DebuggerUIUtil.showValuePopup(myFullValueEvaluator, event, myTree.getProject(), null);
          }
          else {
            new HeadlessValueEvaluationCallback(XValueNodeImpl.this).startFetchingValue(myFullValueEvaluator);
          }
          event.consume();
        }
      };
    }
    return null;
  }

  @Override
  @Nullable
  public String getName() {
    return myName;
  }

  @Nullable
  public XValuePresentation getValuePresentation() {
    return myValuePresentation;
  }

  @Override
  @Nullable
  public String getRawValue() {
    return myRawValue;
  }

  @Override
  public boolean isComputed() {
    return myValuePresentation != null;
  }

  public void setValueModificationStarted() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myRawValue = null;
    myText.clear();
    appendName();
    XValuePresentationUtil.appendSeparator(myText, myValuePresentation.getSeparator());
    myText.append(XDebuggerUIConstants.MODIFYING_VALUE_MESSAGE, XDebuggerUIConstants.MODIFYING_VALUE_HIGHLIGHT_ATTRIBUTES);
    setLeaf(true);
    fireNodeStructureChanged();
  }

  @Override
  public String toString() {
    return getName();
  }
}