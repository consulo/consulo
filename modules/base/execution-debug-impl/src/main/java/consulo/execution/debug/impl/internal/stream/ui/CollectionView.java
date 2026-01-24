// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.impl.internal.stream.ui;

import consulo.disposer.Disposer;
import consulo.execution.debug.stream.trace.TraceElement;
import consulo.execution.debug.stream.ui.TraceContainer;
import consulo.execution.debug.stream.ui.ValuesSelectionListener;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class CollectionView extends JPanel implements TraceContainer {
    private final CollectionTree myInstancesTree;

    CollectionView(@Nonnull JLabel header,
                   @Nonnull CollectionTree collectionTree) {
        super(new BorderLayout());
        add(header, BorderLayout.NORTH);

        myInstancesTree = collectionTree;

        JBScrollPane scroll = new JBScrollPane(myInstancesTree);

        add(scroll, BorderLayout.CENTER);
        Disposer.register(this, myInstancesTree);
    }

    CollectionView(@Nonnull CollectionTree tree) {
        this(createDefaultLabel(tree), tree);
    }

    private static JLabel createDefaultLabel(@Nonnull CollectionTree tree) {
        JLabel label = new JBLabel(String.valueOf(tree.getItemsCount()), SwingConstants.CENTER);
        label.setForeground(JBColor.GRAY);
        Font oldFont = label.getFont();
        label.setFont(oldFont.deriveFont(oldFont.getSize() - JBUIScale.scale(1.f)));
        label.setBorder(JBUI.Borders.empty(3, 0));
        return label;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void highlight(@Nonnull List<TraceElement> elements) {
        myInstancesTree.highlight(elements);
    }

    @Override
    public void select(@Nonnull List<TraceElement> elements) {
        myInstancesTree.select(elements);
    }

    @Override
    public void addSelectionListener(@Nonnull ValuesSelectionListener listener) {
        myInstancesTree.addSelectionListener(listener);
    }

    @Override
    public boolean highlightedExists() {
        return myInstancesTree.highlightedExists();
    }

    protected @Nonnull CollectionTree getInstancesTree() {
        return myInstancesTree;
    }
}
