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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.frame.XFullValueEvaluator;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.debug.frame.presentation.XValueNodePresentationConfigurator;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.impl.internal.frame.UnifiedColoredTextContainer;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.execution.debug.ui.XValuePresentationUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A value - the counterpart of the swing {@link XValueNodeImpl}. Its presentation is asked for when the node is made,
 * and the level it is on waits for it a while before it is shown.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXValueNodeImpl extends UnifiedXValueContainerNode<XValue>
    implements XValueNode, XValueNodePresentationConfigurator.ConfigurableXValueNode {

    private static final int MAX_NAME_LENGTH = 100;

    private final LocalizeValue myName;
    private volatile @Nullable XValuePresentation myValuePresentation;
    private volatile @Nullable XFullValueEvaluator myFullValueEvaluator;

    private final CountDownLatch myPresented = new CountDownLatch(1);

    public UnifiedXValueNodeImpl(UnifiedXDebuggerTree tree, @Nullable UnifiedXDebuggerTreeNode parent, LocalizeValue name, XValue value) {
        super(tree, parent, value);
        myName = name;

        value.computePresentation(this, XValuePlace.TREE);

        // add "Collecting" message only if computation is not yet done
        if (!isComputed()) {
            UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
            if (myName.isNotEmpty()) {
                text.append(myName.get(), XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES);
                text.append(XDebuggerUIConstants.EQ_TEXT, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            text.append(XDebuggerUIConstants.COLLECTING_DATA_MESSAGE, XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES);
            setText(text);
        }
    }

    @Override
    public void setPresentation(@Nullable Image icon, @Nullable String type, String value, boolean hasChildren) {
        XValueNodePresentationConfigurator.setPresentation(icon, type, value, hasChildren, this);
    }

    @Override
    public void setPresentation(@Nullable Image icon, @Nullable String type, String separator, @Nullable String value, boolean hasChildren) {
        XValueNodePresentationConfigurator.setPresentation(icon, type, separator, value, hasChildren, this);
    }

    @Override
    public void setPresentation(@Nullable Image icon, XValuePresentation presentation, boolean hasChildren) {
        XValueNodePresentationConfigurator.setPresentation(icon, presentation, hasChildren, this);
    }

    @Override
    public void applyPresentation(@Nullable Image icon, XValuePresentation valuePresentation, boolean hasChildren) {
        // extra check for obsolete nodes - tree root was changed
        if (isObsolete()) {
            return;
        }

        setIcon(icon);
        myValuePresentation = valuePresentation;
        updateText();
        setLeaf(!hasChildren);

        if (myPresented.getCount() > 0) {
            myPresented.countDown();
        }
        else {
            // came after the level was shown - the value is still being computed, or it was recomputed
            fireNodeChanged();
        }
    }

    private void updateText() {
        UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
        if (myName.isNotEmpty()) {
            XValuePresentationUtil.renderValue(myName.get(), text, XDebuggerUIConstants.VALUE_NAME_ATTRIBUTES, MAX_NAME_LENGTH, null);
        }

        XValuePresentation valuePresentation = myValuePresentation;
        if (valuePresentation != null) {
            XValueNodeImpl.buildText(valuePresentation, text);
        }
        setText(text);
    }

    void awaitPresentation(long deadline) {
        try {
            myPresented.await(Math.max(0, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // a level shown without the presentation keeps showing the loading text until it comes
        myPresented.countDown();
    }

    @Override
    public void setFullValueEvaluator(XFullValueEvaluator fullValueEvaluator) {
        myFullValueEvaluator = fullValueEvaluator;
    }

    @Override
    public void clearFullValueEvaluator() {
        myFullValueEvaluator = null;
    }

    public @Nullable XFullValueEvaluator getFullValueEvaluator() {
        return myFullValueEvaluator;
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @Override
    public @Nullable String getRestorableName() {
        return myName.get();
    }

    public @Nullable XValuePresentation getValuePresentation() {
        return myValuePresentation;
    }

    public boolean isComputed() {
        return myValuePresentation != null;
    }

    @Override
    public XValue getValueContainer() {
        return super.getValueContainer();
    }

    @Override
    public String toString() {
        return myName.get();
    }
}
