/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 28-Jun-2006
 * Time: 19:03:32
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.ide.util.BrowseFilesListener;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.InsertPathAction;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.ProductionResourceContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;
import consulo.roots.impl.TestResourceContentFolderTypeProvider;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class CompilerOutputsEditor extends ModuleElementsEditor {
  private JRadioButton myInheritCompilerOutput;
  @SuppressWarnings({"FieldCanBeLocal"})
  private JRadioButton myPerModuleCompilerOutput;

  private CommitableFieldPanel myOutputPathPanel;
  private CommitableFieldPanel myTestsOutputPathPanel;
  private CommitableFieldPanel myResourcesOutputPathPanel;
  private CommitableFieldPanel myTestResourcesOutputPathPanel;
  private JCheckBox myCbExcludeOutput;
  private JLabel myOutputLabel;
  private JLabel myTestOutputLabel;
  private JLabel myResourceOutputLabel;
  private JLabel myTestResourceOutputLabel;

  protected CompilerOutputsEditor(final ModuleConfigurationState state) {
    super(state);
  }

  @Nonnull
  @Override
  public JComponent createComponentImpl() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    myInheritCompilerOutput = new JRadioButton(ProjectBundle.message("project.inherit.compile.output.path"));
    myPerModuleCompilerOutput = new JRadioButton(ProjectBundle.message("project.module.compile.output.path"));
    ButtonGroup group = new ButtonGroup();
    group.add(myInheritCompilerOutput);
    group.add(myPerModuleCompilerOutput);

    final ActionListener listener = e -> enableCompilerSettings(!myInheritCompilerOutput.isSelected());

    myInheritCompilerOutput.addActionListener(listener);
    myPerModuleCompilerOutput.addActionListener(listener);

    myOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.output.title"), url -> {
      if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
        return;
      }
      moduleCompilerPathsManager.setCompilerOutputUrl(ProductionContentFolderTypeProvider.getInstance(), url);
    });
    myTestsOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.test.output.title"), url -> {
      if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
        return;
      }
      moduleCompilerPathsManager.setCompilerOutputUrl(TestContentFolderTypeProvider.getInstance(), url);
    });
    myResourcesOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.resource.output.title"), url -> {
      if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
        return;
      }
      moduleCompilerPathsManager.setCompilerOutputUrl(ProductionResourceContentFolderTypeProvider.getInstance(), url);
    });
    myTestResourcesOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.test.resource.output.title"), url -> {
      if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
        return;
      }
      moduleCompilerPathsManager.setCompilerOutputUrl(TestResourceContentFolderTypeProvider.getInstance(), url);
    });

    myCbExcludeOutput = new JCheckBox(ProjectBundle.message("module.paths.exclude.output.checkbox"), moduleCompilerPathsManager.isExcludeOutput());
    myCbExcludeOutput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        moduleCompilerPathsManager.setExcludeOutput(myCbExcludeOutput.isSelected());
      }
    });

    final JPanel outputPathsPanel = new JPanel(new GridBagLayout());


    outputPathsPanel
            .add(myInheritCompilerOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(6, 0, 0, 4), 0, 0));
    outputPathsPanel
            .add(myPerModuleCompilerOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(6, 0, 0, 4), 0, 0));

    myOutputLabel = new JLabel(ProjectBundle.message("module.paths.output.label"));
    outputPathsPanel.add(myOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(6, 12, 0, 4), 0, 0));
    outputPathsPanel
            .add(myOutputPathPanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(6, 4, 0, 0), 0, 0));

    myTestOutputLabel = new JLabel(ProjectBundle.message("module.paths.test.output.label"));
    outputPathsPanel.add(myTestOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(6, 16, 0, 4), 0, 0));
    outputPathsPanel
            .add(myTestsOutputPathPanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(6, 4, 0, 0), 0, 0));

    myResourceOutputLabel = new JLabel(ProjectBundle.message("module.paths.resource.output.label"));
    outputPathsPanel
            .add(myResourceOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(6, 16, 0, 4), 0, 0));
    outputPathsPanel.add(myResourcesOutputPathPanel,
                         new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(6, 4, 0, 0), 0, 0));

    myTestResourceOutputLabel = new JLabel(ProjectBundle.message("module.paths.test.resource.output.label"));
    outputPathsPanel
            .add(myTestResourceOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, JBUI.insets(6, 16, 0, 4), 0, 0));
    outputPathsPanel.add(myTestResourcesOutputPathPanel,
                         new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(6, 4, 0, 0), 0, 0));

    outputPathsPanel.add(myCbExcludeOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(6, 16, 0, 0), 0, 0));

    // fill with data
    updateOutputPathPresentation();

    //compiler settings
    final boolean outputPathInherited = moduleCompilerPathsManager.isInheritedCompilerOutput();
    myInheritCompilerOutput.setSelected(outputPathInherited);
    myPerModuleCompilerOutput.setSelected(!outputPathInherited);
    enableCompilerSettings(!outputPathInherited);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(JBUI.Borders.empty(UIUtil.PANEL_SMALL_INSETS));
    panel.add(outputPathsPanel, BorderLayout.NORTH);
    return panel;
  }

  private void updateOutputPathPresentation() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
      final String baseUrl = ProjectStructureConfigurable.getInstance(myProject).getProjectConfigurable().getCompilerOutputUrl();
      moduleCompileOutputChanged(baseUrl, getModule().getName());
    }
    else {
      for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
        CommitableFieldPanel commitableFieldPanel = toField(contentFolderTypeProvider);

        final VirtualFile compilerOutputPath = moduleCompilerPathsManager.getCompilerOutput(contentFolderTypeProvider);
        if (compilerOutputPath != null) {
          commitableFieldPanel.setText(FileUtil.toSystemDependentName(compilerOutputPath.getPath()));
        }
        else {
          final String compilerOutputUrl = moduleCompilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider);
          if (compilerOutputUrl != null) {
            commitableFieldPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerOutputUrl)));
          }
        }
      }
    }
  }

  @Nonnull
  public Module getModule() {
    return getModel().getModule();
  }

  private void enableCompilerSettings(final boolean enabled) {
    UIUtil.setEnabled(myOutputPathPanel, enabled, true);
    UIUtil.setEnabled(myOutputLabel, enabled, true);
    UIUtil.setEnabled(myTestsOutputPathPanel, enabled, true);
    UIUtil.setEnabled(myTestOutputLabel, enabled, true);
    UIUtil.setEnabled(myResourcesOutputPathPanel, enabled, true);
    UIUtil.setEnabled(myResourceOutputLabel, enabled, true);
    UIUtil.setEnabled(myTestResourceOutputLabel, enabled, true);
    UIUtil.setEnabled(myTestResourcesOutputPathPanel, enabled, true);
    myCbExcludeOutput.setEnabled(enabled);
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    moduleCompilerPathsManager.setInheritedCompilerOutput(!enabled);
    updateOutputPathPresentation();
  }

  private CommitableFieldPanel createOutputPathPanel(final String title, final CommitPathRunnable commitPathRunnable) {
    final JTextField textField = new JTextField();
    final FileChooserDescriptor outputPathsChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    outputPathsChooserDescriptor.setHideIgnored(false);
    InsertPathAction.addTo(textField, outputPathsChooserDescriptor);
    FileChooserFactory.getInstance().installFileCompletion(textField, outputPathsChooserDescriptor, true, null);

    CommitableFieldPanel commitableFieldPanel = new CommitableFieldPanel(textField, null, null, new BrowseFilesListener(textField, title, "", outputPathsChooserDescriptor), null);
    commitableFieldPanel.myCommitRunnable = new Runnable() {
      @Override
      public void run() {
        if (!getModel().isWritable()) {
          return;
        }
        String url = commitableFieldPanel.getUrl();
        commitPathRunnable.saveUrl(url);
      }
    };
    return commitableFieldPanel;
  }

  @Override
  public boolean isModified() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    if (myInheritCompilerOutput.isSelected() != moduleCompilerPathsManager.isInheritedCompilerOutput()) {
      return true;
    }
    for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.filter(ContentFolderScopes.productionAndTest())) {
      CommitableFieldPanel commitableFieldPanel = toField(contentFolderTypeProvider);
      String compilerOutputUrl = moduleCompilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider);

      String url = commitableFieldPanel.getUrl();
      if (!Comparing.equal(compilerOutputUrl, url)) {
        return true;
      }
    }
    return false;
  }

  private CommitableFieldPanel toField(ContentFolderTypeProvider contentFolderTypeProvider) {
    if (contentFolderTypeProvider == ProductionContentFolderTypeProvider.getInstance()) {
      return myOutputPathPanel;
    }
    else if (contentFolderTypeProvider == ProductionResourceContentFolderTypeProvider.getInstance()) {
      return myResourcesOutputPathPanel;
    }
    else if (contentFolderTypeProvider == TestContentFolderTypeProvider.getInstance()) {
      return myTestsOutputPathPanel;
    }
    else if (contentFolderTypeProvider == TestResourceContentFolderTypeProvider.getInstance()) {
      return myTestResourcesOutputPathPanel;
    }
    throw new IllegalArgumentException(contentFolderTypeProvider.getId());
  }

  @Override
  public void saveData() {
    myOutputPathPanel.commit();
    myTestsOutputPathPanel.commit();
    myResourcesOutputPathPanel.commit();
    myTestResourcesOutputPathPanel.commit();
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("project.roots.path.tab.title");
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "project.structureModulesPage.outputJavadoc";
  }

  @Override
  public void moduleStateChanged() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    //if content enties tree was changed
    myCbExcludeOutput.setSelected(moduleCompilerPathsManager.isExcludeOutput());
  }

  @Override
  public void moduleCompileOutputChanged(final String baseUrl, final String moduleName) {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
      if (baseUrl != null) {
        myOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(moduleCompilerPathsManager.getCompilerOutputUrl(ProductionContentFolderTypeProvider.getInstance()))));
        myTestsOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(moduleCompilerPathsManager.getCompilerOutputUrl(TestContentFolderTypeProvider.getInstance()))));
        myResourcesOutputPathPanel
                .setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(moduleCompilerPathsManager.getCompilerOutputUrl(ProductionResourceContentFolderTypeProvider.getInstance()))));
        myTestResourcesOutputPathPanel
                .setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(moduleCompilerPathsManager.getCompilerOutputUrl(TestResourceContentFolderTypeProvider.getInstance()))));
      }
      else {
        myOutputPathPanel.setText(null);
        myTestsOutputPathPanel.setText(null);
        myResourcesOutputPathPanel.setText(null);
        myTestResourcesOutputPathPanel.setText(null);
      }
    }
  }

  private interface CommitPathRunnable {
    void saveUrl(String url);
  }

  private static class CommitableFieldPanel extends FieldPanel {
    private Runnable myCommitRunnable;

    public CommitableFieldPanel(final JTextField textField, String labelText, final String viewerDialogTitle, ActionListener browseButtonActionListener, final Runnable documentListener) {
      super(textField, labelText, viewerDialogTitle, browseButtonActionListener, documentListener);
    }

    @Nullable
    public String getUrl() {
      final String path = getText().trim();
      if (path.length() == 0) {
        return null;
      }
      else {
        // should set only absolute paths
        String canonicalPath;
        try {
          canonicalPath = FileUtil.resolveShortWindowsName(path);
        }
        catch (IOException e) {
          canonicalPath = path;
        }
        return VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(canonicalPath));
      }
    }

    public void commit() {
      myCommitRunnable.run();
    }
  }
}
