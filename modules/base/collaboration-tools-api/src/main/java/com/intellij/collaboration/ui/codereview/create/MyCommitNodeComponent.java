// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import com.intellij.collaboration.ui.codereview.commits.CommitNodeComponent;
import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;

import java.awt.*;

final class MyCommitNodeComponent extends CommitNodeComponent {
    private static final int NODE_SIZE = 10;

    @Override
    public Dimension getPreferredSize() {
        return new JBDimension(NODE_SIZE, NODE_SIZE);
    }

    @Override
    protected int calcRadius(Rectangle rect) {
        return JBUI.scale(NODE_SIZE / 2) - 1;
    }

    @Override
    protected float calcLineThickness(Rectangle rect) {
        return JBUIScale.scale(1.5f);
    }
}
