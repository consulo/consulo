/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.openapi.vcs.impl.DefaultVcsRootPolicy;
import consulo.ide.impl.idea.util.continuation.ModalityIgnorantBackgroundableTask;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDescriptor;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yole
 */
public class VcsMappingConfigurationDialog extends DialogWrapper {
  private final Project myProject;
  private JComboBox myVCSComboBox;
  private TextFieldWithBrowseButton myDirectoryTextField;
  private JPanel myPanel;
  private JPanel myVcsConfigurablePlaceholder;
  private JRadioButton myProjectRadioButton;
  private JRadioButton myDirectoryRadioButton;
  private JBLabel myProjectButtonComment;
  private UnnamedConfigurable myVcsConfigurable;
  private VcsDirectoryMapping myMappingCopy;
  private JComponent myVcsConfigurableComponent;
  private ProjectLevelVcsManager myVcsManager;
  private final Map<String, VcsDescriptor> myVcses;

  public VcsMappingConfigurationDialog(Project project, String title) {
    super(project, false);
    myProject = project;
    myVcsManager = ProjectLevelVcsManager.getInstance(myProject);
    VcsDescriptor[] vcsDescriptors = myVcsManager.getAllVcss();
    myVcses = new HashMap<>();
    for (VcsDescriptor vcsDescriptor : vcsDescriptors) {
      myVcses.put(vcsDescriptor.getId(), vcsDescriptor);
    }
    myVCSComboBox.setModel(VcsDirectoryConfigurationPanel.buildVcsWrappersModel(project));
    myDirectoryTextField.addActionListener(new MyBrowseFolderListener(
      "Select Directory",
      "Select directory to map to a VCS",
      myDirectoryTextField,
      project,
      FileChooserDescriptorFactory.createSingleFolderDescriptor()
    ));
    myMappingCopy = new VcsDirectoryMapping("", "");
    setTitle(title);
    init();
    myVCSComboBox.addActionListener(e -> updateVcsConfigurable());
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  public void setMapping(@Nonnull VcsDirectoryMapping mapping) {
    myMappingCopy = new VcsDirectoryMapping(mapping.getDirectory(), mapping.getVcs(), mapping.getRootSettings());
    myProjectRadioButton.setSelected(myMappingCopy.isDefaultMapping());
    myDirectoryRadioButton.setSelected(! myProjectRadioButton.isSelected());
    if (myMappingCopy.isDefaultMapping()) {
      myDirectoryTextField.setText("");
    } else {
      myDirectoryTextField.setText(FileUtil.toSystemDependentName(mapping.getDirectory()));
    }

    myVCSComboBox.setSelectedItem(myVcses.get(mapping.getVcs()));
    updateVcsConfigurable();
    myDirectoryTextField.setEnabled(myDirectoryRadioButton.isSelected());

    initProjectMessage();
  }

  @Nonnull
  public VcsDirectoryMapping getMapping() {
    VcsDescriptor wrapper = (VcsDescriptor) myVCSComboBox.getSelectedItem();
    String vcs = wrapper == null || wrapper.isNone() ? "" : wrapper.getId();
    String directory = myProjectRadioButton.isSelected() ? "" : FileUtil.toSystemIndependentName(myDirectoryTextField.getText());
    return new VcsDirectoryMapping(directory, vcs, myMappingCopy.getRootSettings());
  }

  @RequiredUIAccess
  private void updateVcsConfigurable() {
    if (myVcsConfigurable != null) {
      myVcsConfigurablePlaceholder.remove(myVcsConfigurableComponent);
      myVcsConfigurable.disposeUIResources();
      myVcsConfigurable = null;
    }
    VcsDescriptor wrapper = (VcsDescriptor) myVCSComboBox.getSelectedItem();
    if (wrapper != null && (! wrapper.isNone())) {
      AbstractVcs vcs = myVcsManager.findVcsByName(wrapper.getId());
      if (vcs != null) {
        UnnamedConfigurable configurable = vcs.getRootConfigurable(myMappingCopy);
        if (configurable != null) {
          myVcsConfigurable = configurable;
          myVcsConfigurableComponent = myVcsConfigurable.createComponent();
          myVcsConfigurablePlaceholder.add(myVcsConfigurableComponent, BorderLayout.CENTER);
        }
      }
    }
    pack();
  }

  @RequiredUIAccess
  protected void doOKAction() {
    if (myVcsConfigurable != null) {
      try {
        myVcsConfigurable.apply();
      }
      catch (ConfigurationException ex) {
        Messages.showErrorDialog(myPanel, "Invalid VCS options: " + ex.getMessage());
      }
    }
    super.doOKAction();
  }

  private void createUIComponents() {
    ButtonGroup bg = new ButtonGroup();
    myProjectRadioButton = new JRadioButton();
    myDirectoryRadioButton = new JRadioButton();
    bg.add(myProjectRadioButton);
    bg.add(myDirectoryRadioButton);
    ActionListener al = e -> myDirectoryTextField.setEnabled(myDirectoryRadioButton.isSelected());
    myProjectRadioButton.addActionListener(al);
    myDirectoryRadioButton.addActionListener(al);
    myDirectoryRadioButton.setSelected(true);
  }

  public void initProjectMessage() {
    myProjectButtonComment.setText(XmlStringUtil.wrapInHtml(DefaultVcsRootPolicy.getInstance(myProject).getProjectConfigurationMessage(myProject)));
  }

  private class MyBrowseFolderListener extends ComponentWithBrowseButton.BrowseFolderActionListener<JTextField> {

    public MyBrowseFolderListener(String title, String description, TextFieldWithBrowseButton textField, Project project,
                                  FileChooserDescriptor fileChooserDescriptor) {
      super(title, description, textField, project, fileChooserDescriptor, TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    }

    @Override
    protected VirtualFile getInitialFile() {
      // suggest project base dir only if nothing is typed in the component.
      String text = getComponentText();
      if (text.length() == 0) {
        VirtualFile file = myProject.getBaseDir();
        if (file != null) {
          return file;
        }
      }
      return super.getInitialFile();
    }

    @Override
    protected void onFileChosen(@Nonnull final VirtualFile chosenFile) {
      String oldText = myDirectoryTextField.getText();
      super.onFileChosen(chosenFile);
      VcsDescriptor wrapper = (VcsDescriptor) myVCSComboBox.getSelectedItem();
      if (oldText.length() == 0 && (wrapper == null || wrapper.isNone())) {
        ModalityIgnorantBackgroundableTask task =
                new ModalityIgnorantBackgroundableTask(myProject, "Looking for VCS administrative area", false) {
                  VcsDescriptor probableVcs = null;

                  @Override
                  @RequiredUIAccess
                  protected void doInAwtIfFail(Exception e) {
                  }

                  @Override
                  @RequiredUIAccess
                  protected void doInAwtIfCancel() {
                  }

                  @Override
                  @RequiredUIAccess
                  protected void doInAwtIfSuccess() {
                    if (probableVcs != null) {
                      // todo none
                      myVCSComboBox.setSelectedItem(probableVcs);
                    }
                  }

                  @Override
                  protected void runImpl(@Nonnull ProgressIndicator indicator) {
                    for (VcsDescriptor vcs : myVcses.values()) {
                      if (vcs.probablyUnderVcs(chosenFile)) {
                        if (probableVcs != null) {
                          probableVcs = null;
                          break;
                        }
                        probableVcs = vcs;
                      }
                    }
                  }
                };
        ProgressManager.getInstance().run(task);
      }
    }
  }
}
