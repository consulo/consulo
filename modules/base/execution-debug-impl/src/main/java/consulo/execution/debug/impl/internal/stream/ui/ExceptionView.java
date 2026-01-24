// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.trace.CollectionTreeBuilder;
import consulo.execution.debug.stream.trace.DebuggerCommandLauncher;
import consulo.execution.debug.stream.trace.GenericEvaluationContext;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBLabel;

import javax.swing.*;
import java.util.Collections;

/**
 * @author Vitaliy.Bibaev
 */
public class ExceptionView extends CollectionView {
    public ExceptionView(DebuggerCommandLauncher launcher, GenericEvaluationContext context, TraceElement ex, CollectionTreeBuilder builder) {
        super(new JBLabel(XDebuggerLocalize.exceptionLabel().get()),
            new TerminationTree(ex.getValue(), Collections.singletonList(ex), launcher, context, builder, "ExceptionView"));

        getInstancesTree().setCellRenderer(new TraceTreeCellRenderer() {
            @Override
            public void customizeCellRenderer(JTree tree,
                                              Object value,
                                              boolean selected,
                                              boolean expanded,
                                              boolean leaf,
                                              int row,
                                              boolean hasFocus) {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
                if (row == 0) {
                    // TODO: add this icon to the plugin
                    setIcon(PlatformIconGroup.generalError());
                }
            }
        });
    }
}
