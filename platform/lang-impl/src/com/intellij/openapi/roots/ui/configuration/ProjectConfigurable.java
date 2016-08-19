/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration;

import consulo.compiler.CompilerConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.BrowseFilesListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ex.ProjectEx;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectStructureElementConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureDaemonAnalyzer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.FieldPanel;
import com.intellij.ui.InsertPathAction;
import consulo.roots.ui.configuration.WholeWestConfigurable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.RequiredDispatchThread;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.IOException;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 15, 2003
 */
public class ProjectConfigurable extends ProjectStructureElementConfigurable<Project> implements DetailsComponent.Facade, WholeWestConfigurable {

  private final Project myProject;

  private FieldPanel myProjectCompilerOutput;

  private JTextField myProjectName;

  private JPanel myPanel;

  private final StructureConfigurableContext myContext;
  private final ModulesConfigurator myModulesConfigurator;

  private boolean myFreeze = false;
  private DetailsComponent myDetailsComponent;
  private final GeneralProjectSettingsElement mySettingsElement;

  private final ConfigurationErrorsComponent myErrorsComponent;

  public ProjectConfigurable(Project project, final StructureConfigurableContext context, ModulesConfigurator configurator) {
    myProject = project;
    myContext = context;
    myErrorsComponent = new ConfigurationErrorsComponent(project);
    myModulesConfigurator = configurator;
    mySettingsElement = new GeneralProjectSettingsElement(context);
    final ProjectStructureDaemonAnalyzer daemonAnalyzer = context.getDaemonAnalyzer();
    myModulesConfigurator.addAllModuleChangeListener(new ModuleEditor.ChangeListener() {
      @Override
      public void moduleStateChanged(ModifiableRootModel moduleRootModel) {
        daemonAnalyzer.queueUpdate(mySettingsElement);
      }
    });
    init();
  }

  @Override
  public ProjectStructureElement getProjectStructureElement() {
    return mySettingsElement;
  }

  @Override
  public DetailsComponent getDetailsComponent() {
    return myDetailsComponent;
  }

  @Override
  public JComponent createOptionsPanel() {
    myDetailsComponent = new DetailsComponent(false, false);
    myDetailsComponent.setContent(myPanel);
    myDetailsComponent.setText(getBannerSlogan());
    return myDetailsComponent.getComponent();
  }

  private void init() {
    myPanel = new JPanel(new VerticalFlowLayout(true, false));

    final JPanel namePanel = new JPanel(new BorderLayout());
    final JLabel label = new JLabel("<html><body><b>Project name:</b></body></html>", SwingConstants.LEFT);
    namePanel.add(label, BorderLayout.NORTH);

    myProjectName = new JTextField();
    myProjectName.setColumns(40);

    final JPanel nameFieldPanel = new JPanel();
    nameFieldPanel.setLayout(new BoxLayout(nameFieldPanel, BoxLayout.X_AXIS));
    nameFieldPanel.add(Box.createHorizontalStrut(4));
    nameFieldPanel.add(myProjectName);

    namePanel.add(nameFieldPanel, BorderLayout.CENTER);
    final JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
    wrapper.add(namePanel);
    wrapper.setAlignmentX(0);
    myPanel.add(wrapper);

    myPanel.add(new JLabel(ProjectBundle.message("project.compiler.output")));

    final JTextField textField = new JTextField();
    final FileChooserDescriptor outputPathsChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    InsertPathAction.addTo(textField, outputPathsChooserDescriptor);
    outputPathsChooserDescriptor.setHideIgnored(false);
    BrowseFilesListener listener = new BrowseFilesListener(textField, "", ProjectBundle.message("project.compiler.output"), outputPathsChooserDescriptor);
    myProjectCompilerOutput = new FieldPanel(textField, null, null, listener, EmptyRunnable.getInstance());
    FileChooserFactory.getInstance().installFileCompletion(myProjectCompilerOutput.getTextField(), outputPathsChooserDescriptor, true, null);

    myProjectCompilerOutput.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        if (myFreeze) return;
        myModulesConfigurator.processModuleCompilerOutputChanged(getCompilerOutputUrl());
      }
    });

    myPanel.add(myProjectCompilerOutput);
    myPanel.add(myErrorsComponent);
  }

  @RequiredDispatchThread
  @Override
  public void disposeUIResources() {
  }

  @RequiredDispatchThread
  @Override
  public void reset() {
    myFreeze = true;
    try {

      final String compilerOutput = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
      if (compilerOutput != null) {
        myProjectCompilerOutput.setText(FileUtil.toSystemDependentName(VfsUtil.urlToPath(compilerOutput)));
      }
      if (myProjectName != null) {
        myProjectName.setText(myProject.getName());
      }
    }
    finally {
      myFreeze = false;
    }

    myContext.getDaemonAnalyzer().queueUpdate(mySettingsElement);
  }


  @RequiredDispatchThread
  @Override
  public void apply() throws ConfigurationException {
    final CompilerConfiguration compilerProjectExtension = CompilerConfiguration.getInstance(myProject);

    if (myProjectName != null && StringUtil.isEmptyOrSpaces(myProjectName.getText())) {
      throw new ConfigurationException("Please, specify project name!");
    }

    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        // set the output path first so that handlers of RootsChanged event sent after JDK is set
        // would see the updated path
        String canonicalPath = myProjectCompilerOutput.getText();
        if (canonicalPath != null && canonicalPath.length() > 0) {
          try {
            canonicalPath = FileUtil.resolveShortWindowsName(canonicalPath);
          }
          catch (IOException e) {
            //file doesn't exist yet
          }
          canonicalPath = FileUtil.toSystemIndependentName(canonicalPath);
          compilerProjectExtension.setCompilerOutputUrl(VfsUtil.pathToUrl(canonicalPath));
        }
        else {
          compilerProjectExtension.setCompilerOutputUrl(null);
        }
        if (myProjectName != null) {
          ((ProjectEx)myProject).setProjectName(myProjectName.getText().trim());
          if (myDetailsComponent != null) myDetailsComponent.setText(getBannerSlogan());
        }
      }
    });
  }


  @Override
  public void setDisplayName(final String name) {
    //do nothing
  }

  @Override
  public Project getEditableObject() {
    return myProject;
  }

  @Override
  public String getBannerSlogan() {
    return ProjectBundle.message("project.roots.project.banner.text", myProject.getName());
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("project.roots.project.display.name");
  }

  @Override
  public Icon getIcon(boolean open) {
    return AllIcons.Nodes.ProjectTab;
  }

  @Override
  @Nullable
  @NonNls
  public String getHelpTopic() {
    return "reference.settingsdialog.project.structure.general";
  }


  @RequiredDispatchThread
  @Override
  @SuppressWarnings({"SimplifiableIfStatement"})
  public boolean isModified() {
    final String compilerOutput = CompilerConfiguration.getInstance(myProject).getCompilerOutputUrl();
    if (!Comparing.strEqual(FileUtil.toSystemIndependentName(VfsUtil.urlToPath(compilerOutput)),
                            FileUtil.toSystemIndependentName(myProjectCompilerOutput.getText()))) {
      return true;
    }
    if (myProjectName != null) {
      if (!myProjectName.getText().trim().equals(myProject.getName())) return true;
    }

    return false;
  }

  public String getCompilerOutputUrl() {
    return VfsUtil.pathToUrl(myProjectCompilerOutput.getText().trim());
  }

  @NotNull
  @Override
  public Couple<JComponent> createSplitterComponents() {
    return Couple.of(new JPanel(), createComponent());
  }
}
