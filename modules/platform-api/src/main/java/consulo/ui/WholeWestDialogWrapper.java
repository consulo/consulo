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
package consulo.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.DimensionService;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;
import java.awt.*;
import java.awt.Component;

/**
 * @author VISTALL
 * @since 28.07.2015
 */
public abstract class WholeWestDialogWrapper extends DialogWrapper {
  public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent) {
    super(project, canBeParent);
  }

  public WholeWestDialogWrapper(@Nullable Project project, boolean canBeParent, IdeModalityType ideModalityType) {
    super(project, canBeParent, ideModalityType);
  }

  public WholeWestDialogWrapper(@Nullable Project project) {
    super(project);
  }

  public WholeWestDialogWrapper(boolean canBeParent) {
    super(canBeParent);
  }

  public WholeWestDialogWrapper(boolean canBeParent, boolean applicationModalIfPossible) {
    super(canBeParent, applicationModalIfPossible);
  }

  public WholeWestDialogWrapper(Project project, boolean canBeParent, boolean applicationModalIfPossible) {
    super(project, canBeParent, applicationModalIfPossible);
  }

  public WholeWestDialogWrapper(@NotNull Component parent, boolean canBeParent) {
    super(parent, canBeParent);
  }

  @Override
  protected LayoutManager createRootLayout() {
    return new BorderLayout();
  }

  @NotNull
  public String getSplitterKey() {
    return getClass().getName();
  }

  public float getSplitterDefaultValue() {
    return 0.3f;
  }

  public Dimension getDefaultSize() {
    return new Dimension(500, 500);
  }

  @NotNull
  @RequiredDispatchThread
  public abstract Couple<JComponent> createSplitterComponents(JPanel rootPanel);

  @Override
  protected final JComponent createCenterPanel() {
    throw new UnsupportedOperationException();
  }

  @Override
  @RequiredDispatchThread
  protected void initRootPanel(@NotNull JPanel rootPanel) {
    JBSplitter splitter = new OnePixelSplitter();
    splitter.setProportion(getSplitterDefaultValue());
    splitter.setSplitterProportionKey(getSplitterKey());

    rootPanel.add(splitter, BorderLayout.CENTER);

    Couple<JComponent> splitterComponents = createSplitterComponents(rootPanel);

    JComponent first = splitterComponents.getFirst();
    assert first != null;
    splitter.setFirstComponent(first);

    JPanel rightComponent = new JPanel(new BorderLayout());
    rightComponent.setBorder(createContentPaneBorder());
    splitter.setSecondComponent(rightComponent);

    final JComponent second = splitterComponents.getSecond();
    assert second != null;
    rightComponent.add(second, BorderLayout.CENTER);
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
      final Project projectGuess = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(rightComponent));
      Dimension size = DimensionService.getInstance().getSize(dimensionKey, projectGuess);
      if (size == null) {
        DimensionService.getInstance().setSize(dimensionKey, JBUI.size(getDefaultSize()));
      }
    }
  }
}
