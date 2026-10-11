/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.ex.awt;

import consulo.project.Project;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import consulo.util.lang.Couple;

import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2015-07-28
 */
public abstract class WholeWestDialogWrapper extends DialogWrapper {
    public static final float DEFAULT_SPLITTER_PROPORTION = 0.3f;

    protected final TitlelessDecorator myTitlelessDecorator;

    public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent, IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(@Nullable Project project) {
        super(project);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(boolean canBeParent) {
        super(canBeParent);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(boolean canBeParent, boolean applicationModalIfPossible) {
        super(canBeParent, applicationModalIfPossible);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(Project project, boolean canBeParent, boolean applicationModalIfPossible) {
        super(project, canBeParent, applicationModalIfPossible);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    public WholeWestDialogWrapper(Component parent, boolean canBeParent) {
        super(parent, canBeParent);
        myTitlelessDecorator = TitlelessDecoratorService.getInstance().of(getRootPane());
    }

    @Override
    protected void init() {
        super.init();

        if (myTitlelessDecorator instanceof AWTTitlelessDecorator awtTitlelessDecorator) {
            awtTitlelessDecorator.install(getWindow());
        }
    }

    @Override
    protected LayoutManager createRootLayout() {
        return new BorderLayout();
    }

    
    public String getSplitterKey() {
        return getClass().getName();
    }

    public float getSplitterDefaultValue() {
        return DEFAULT_SPLITTER_PROPORTION;
    }

    public Size2D getDefaultSize() {
        return new Size2D(500, 500);
    }

    
    @RequiredUIAccess
    public abstract Couple<JComponent> createSplitterComponents(JPanel rootPanel);

    @Override
    protected final JComponent createCenterPanel() {
        throw new UnsupportedOperationException();
    }

    @Override
    @RequiredUIAccess
    protected void initRootPanel(JPanel rootPanel) {
        initWholeWestRootPanel(
            rootPanel,
            () -> createSplitterComponents(rootPanel),
            getSplitterKey(),
            getSplitterDefaultValue(),
            getDefaultSize(),
            myTitlelessDecorator
        );
    }
}
