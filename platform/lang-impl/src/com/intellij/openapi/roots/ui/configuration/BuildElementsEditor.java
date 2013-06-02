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
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.InsertPathAction;
import com.intellij.util.ui.UIUtil;
import org.consulo.compiler.CompilerPathsManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class BuildElementsEditor extends ModuleElementsEditor {
  private JRadioButton myInheritCompilerOutput;
  @SuppressWarnings({"FieldCanBeLocal"})
  private JRadioButton myPerModuleCompilerOutput;

  private CommitableFieldPanel myOutputPathPanel;
  private CommitableFieldPanel myTestsOutputPathPanel;
  private CommitableFieldPanel myResourcesOutputPathPanel;
  private JCheckBox myCbExcludeOutput;
  private JLabel myOutputLabel;
  private JLabel myTestOutputLabel;
  private JLabel myResourceOutputLabel;

  protected BuildElementsEditor(final ModuleConfigurationState state) {
    super(state);
  }

  @Override
  public JComponent createComponentImpl() {
    final CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
    myInheritCompilerOutput = new JRadioButton(ProjectBundle.message("project.inherit.compile.output.path"));
    myPerModuleCompilerOutput = new JRadioButton(ProjectBundle.message("project.module.compile.output.path"));
    ButtonGroup group = new ButtonGroup();
    group.add(myInheritCompilerOutput);
    group.add(myPerModuleCompilerOutput);

    final ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        enableCompilerSettings(!myInheritCompilerOutput.isSelected());
      }
    };

    myInheritCompilerOutput.addActionListener(listener);
    myPerModuleCompilerOutput.addActionListener(listener);

    myOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.output.title"), new CommitPathRunnable() {
      @Override
      public void saveUrl(String url) {
        if (compilerPathsManager.isInheritedCompilerOutput(getModule())) {
          return;
        }
        compilerPathsManager.setCompilerOutputUrl(getModule(), ContentFolderType.SOURCE, url);
      }
    });
    myTestsOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.test.output.title"), new CommitPathRunnable() {
      @Override
      public void saveUrl(String url) {
        if (compilerPathsManager.isInheritedCompilerOutput(getModule())) {
          return;
        }
        compilerPathsManager.setCompilerOutputUrl(getModule(), ContentFolderType.TEST, url);
      }
    });
    myResourcesOutputPathPanel = createOutputPathPanel(ProjectBundle.message("module.paths.resource.output.title"), new CommitPathRunnable() {
      @Override
      public void saveUrl(String url) {
        if (compilerPathsManager.isInheritedCompilerOutput(getModule())) {
          return;
        }
        compilerPathsManager.setCompilerOutputUrl(getModule(), ContentFolderType.RESOURCE, url);
      }
    });

    myCbExcludeOutput = new JCheckBox(ProjectBundle.message("module.paths.exclude.output.checkbox"), compilerPathsManager.isExcludeOutput(getModule()));
    myCbExcludeOutput.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        compilerPathsManager.setExcludeOutput(getModule(), myCbExcludeOutput.isSelected());
      }
    });

    final JPanel outputPathsPanel = new JPanel(new GridBagLayout());


    outputPathsPanel.add(myInheritCompilerOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0,
                                                                         GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                         new Insets(6, 0, 0, 4), 0, 0));
    outputPathsPanel.add(myPerModuleCompilerOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0,
                                                                           GridBagConstraints.WEST, GridBagConstraints.NONE,
                                                                           new Insets(6, 0, 0, 4), 0, 0));

    myOutputLabel = new JLabel(ProjectBundle.message("module.paths.output.label"));
    outputPathsPanel.add(myOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                                                               GridBagConstraints.NONE, new Insets(6, 12, 0, 4), 0, 0));
    outputPathsPanel.add(myOutputPathPanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                                                                   GridBagConstraints.HORIZONTAL, new Insets(6, 4, 0, 0), 0, 0));

    myTestOutputLabel = new JLabel(ProjectBundle.message("module.paths.test.output.label"));
    outputPathsPanel.add(myTestOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                                                                   GridBagConstraints.NONE, new Insets(6, 16, 0, 4), 0, 0));
    outputPathsPanel.add(myTestsOutputPathPanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0,
                                                                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                                                        new Insets(6, 4, 0, 0), 0, 0));

    myResourceOutputLabel = new JLabel(ProjectBundle.message("module.paths.resource.output.label"));
    outputPathsPanel.add(myResourceOutputLabel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                                                                   GridBagConstraints.NONE, new Insets(6, 16, 0, 4), 0, 0));
    outputPathsPanel.add(myResourcesOutputPathPanel, new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0,
                                                                        GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                                                                        new Insets(6, 4, 0, 0), 0, 0));

    outputPathsPanel.add(myCbExcludeOutput, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST,
                                                                   GridBagConstraints.NONE, new Insets(6, 16, 0, 0), 0, 0));

    // fill with data
    updateOutputPathPresentation();

    //compiler settings
    final boolean outputPathInherited =compilerPathsManager.isInheritedCompilerOutput(getModule());
    myInheritCompilerOutput.setSelected(outputPathInherited);
    myPerModuleCompilerOutput.setSelected(!outputPathInherited);
    enableCompilerSettings(!outputPathInherited);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(IdeBorderFactory.createTitledBorder(ProjectBundle.message("project.roots.output.compiler.title"),
                                                        true));
    panel.add(outputPathsPanel, BorderLayout.NORTH);
    return panel;
  }

  private void updateOutputPathPresentation() {
    CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
    if (compilerPathsManager.isInheritedCompilerOutput(getModule())) {
      final String baseUrl = ProjectStructureConfigurable.getInstance(myProject).getProjectConfig().getCompilerOutputUrl();
      moduleCompileOutputChanged(baseUrl, getModule().getName());
    } else {
      final VirtualFile compilerOutputPath = compilerPathsManager.getCompilerOutput(getModule(), ContentFolderType.SOURCE);
      if (compilerOutputPath != null) {
        myOutputPathPanel.setText(FileUtil.toSystemDependentName(compilerOutputPath.getPath()));
      }
      else {
        final String compilerOutputUrl = compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.SOURCE);
        if (compilerOutputUrl != null) {
          myOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerOutputUrl)));
        }
      }
      final VirtualFile testsOutputPath = compilerPathsManager.getCompilerOutput(getModule(), ContentFolderType.TEST);
      if (testsOutputPath != null) {
        myTestsOutputPathPanel.setText(FileUtil.toSystemDependentName(testsOutputPath.getPath()));
      }
      else {
        final String testsOutputUrl = compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.TEST);
        if (testsOutputUrl != null) {
          myTestsOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(testsOutputUrl)));
        }
      }
      final VirtualFile resourcesOutputPath = compilerPathsManager.getCompilerOutput(getModule(), ContentFolderType.RESOURCE);
      if (resourcesOutputPath != null) {
        myResourcesOutputPathPanel.setText(FileUtil.toSystemDependentName(resourcesOutputPath.getPath()));
      }
      else {
        final String resourcesOutputUrl = compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.RESOURCE);
        if (resourcesOutputUrl != null) {
          myResourcesOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(resourcesOutputUrl)));
        }
      }
    }
  }

  @NotNull
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
    myCbExcludeOutput.setEnabled(enabled);
    CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
    compilerPathsManager.setInheritedCompilerOutput(getModule(), !enabled);
    updateOutputPathPresentation();
  }

  private CommitableFieldPanel createOutputPathPanel(final String title, final CommitPathRunnable commitPathRunnable) {
    final JTextField textField = new JTextField();
    final FileChooserDescriptor outputPathsChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    outputPathsChooserDescriptor.setHideIgnored(false);
    InsertPathAction.addTo(textField, outputPathsChooserDescriptor);
    FileChooserFactory.getInstance().installFileCompletion(textField, outputPathsChooserDescriptor, true, null);
    final Runnable commitRunnable = new Runnable() {
      @Override
      public void run() {
        if (!getModel().isWritable()) {
          return;
        }
        final String path = textField.getText().trim();
        if (path.length() == 0) {
          commitPathRunnable.saveUrl(null);
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
          commitPathRunnable.saveUrl(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(canonicalPath)));
        }
      }
    };

    return new CommitableFieldPanel(textField, null, null, new BrowseFilesListener(textField, title, "", outputPathsChooserDescriptor), null, commitRunnable);
  }

  @Override
  public void saveData() {
    myOutputPathPanel.commit();
    myTestsOutputPathPanel.commit();
    myResourcesOutputPathPanel.commit();
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("output.tab.title");
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "project.structureModulesPage.outputJavadoc";
  }


  @Override
  public void moduleStateChanged() {
    CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
    //if content enties tree was changed
    myCbExcludeOutput.setSelected(compilerPathsManager.isExcludeOutput(getModule()));
  }

  @Override
  public void moduleCompileOutputChanged(final String baseUrl, final String moduleName) {
    CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
    if (compilerPathsManager.isInheritedCompilerOutput(getModule())) {
      if (baseUrl != null) {
        myOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.SOURCE))));
        myTestsOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.TEST))));
        myResourcesOutputPathPanel.setText(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerPathsManager.getCompilerOutputUrl(getModule(), ContentFolderType.RESOURCE))));
      }
      else {
        myOutputPathPanel.setText(null);
        myTestsOutputPathPanel.setText(null);
        myResourcesOutputPathPanel.setText(null);
      }
    }
  }

  private interface CommitPathRunnable {
    void saveUrl(String url);
  }

  private static class CommitableFieldPanel extends FieldPanel {
    private final Runnable myCommitRunnable;

    public CommitableFieldPanel(final JTextField textField,
                                String labelText,
                                final String viewerDialogTitle,
                                ActionListener browseButtonActionListener,
                                final Runnable documentListener,
                                final Runnable commitPathRunnable) {
      super(textField, labelText, viewerDialogTitle, browseButtonActionListener, documentListener);
      myCommitRunnable = commitPathRunnable;
    }

    public void commit() {
      myCommitRunnable.run();
    }
  }
}
