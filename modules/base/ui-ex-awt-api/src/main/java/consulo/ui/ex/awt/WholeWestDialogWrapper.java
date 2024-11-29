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

import consulo.application.ui.ApplicationWindowStateService;
import consulo.application.ui.WindowStateService;
import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.project.ui.ProjectWindowStateService;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.Couple;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 28.07.2015
 */
public abstract class WholeWestDialogWrapper extends DialogWrapper {
  protected final TitlelessDecorator myTitlelessDecorator;

  public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent) {
    super(project, canBeParent);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent, IdeModalityType ideModalityType) {
    super(project, canBeParent, ideModalityType);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(@Nullable Project project) {
    super(project);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(boolean canBeParent) {
    super(canBeParent);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(boolean canBeParent, boolean applicationModalIfPossible) {
    super(canBeParent, applicationModalIfPossible);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(Project project, boolean canBeParent, boolean applicationModalIfPossible) {
    super(project, canBeParent, applicationModalIfPossible);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  public WholeWestDialogWrapper(@Nonnull Component parent, boolean canBeParent) {
    super(parent, canBeParent);
    myTitlelessDecorator = TitlelessDecorator.of(getRootPane());
  }

  @Override
  protected void init() {
    super.init();
    myTitlelessDecorator.install(getWindow());
  }

  @Override
  protected LayoutManager createRootLayout() {
    return new BorderLayout();
  }

  @Nonnull
  public String getSplitterKey() {
    return getClass().getName();
  }

  public float getSplitterDefaultValue() {
    return 0.3f;
  }

  public Size getDefaultSize() {
    return new Size(500, 500);
  }

  @Nonnull
  @RequiredUIAccess
  public abstract Couple<JComponent> createSplitterComponents(JPanel rootPanel);

  @Override
  protected final JComponent createCenterPanel() {
    throw new UnsupportedOperationException();
  }

  @Override
  @RequiredUIAccess
  protected void initRootPanel(@Nonnull JPanel rootPanel) {
    JBSplitter splitter = new OnePixelSplitter();
    splitter.setProportion(getSplitterDefaultValue());
    splitter.setSplitterProportionKey(getSplitterKey());

    rootPanel.add(splitter, BorderLayout.CENTER);

    Couple<JComponent> splitterComponents = createSplitterComponents(rootPanel);

    JComponent first = splitterComponents.getFirst();
    assert first != null;
    splitter.setFirstComponent(first);
    myTitlelessDecorator.makeLeftComponentLower(first);

    JPanel rightComponent = new JPanel(new BorderLayout());
    rightComponent.setBorder(createContentPaneBorder());
    splitter.setSecondComponent(rightComponent);

    final JComponent second = splitterComponents.getSecond();
    assert second != null;
    rightComponent.add(myTitlelessDecorator.modifyRightComponent(rootPanel, second), BorderLayout.CENTER);
    myErrorPane = second;

    final JPanel southSection = new JPanel(new BorderLayout());
    rightComponent.add(southSection, BorderLayout.SOUTH);

    southSection.add(myErrorText, BorderLayout.CENTER);
    final JComponent south = createSouthPanel();
    if (south != null) {
      southSection.add(south, BorderLayout.SOUTH);
    }

    String dimensionKey = getDimensionKey();
    if (dimensionKey != null) {
      final Project projectGuess = DataManager.getInstance().getDataContext(rightComponent).getData(Project.KEY);
      WindowStateService stateService = projectGuess == null ? ApplicationWindowStateService.getInstance() : ProjectWindowStateService.getInstance(projectGuess);
      Size size = stateService.getSize(dimensionKey);
      if (size == null) {
        stateService.putSize(dimensionKey, getDefaultSize());
      }
    }
  }
}
