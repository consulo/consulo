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

package com.intellij.openapi.roots.ui.configuration;

import com.google.common.base.Predicate;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.disposer.Disposable;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ProjectLocalize;
import consulo.roots.ContentFolderScopes;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.ui.configuration.ModulesConfigurator;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.image.Image;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/*
 * User: Anna.Kozlova
 * Date: 28-Jun-2006
 * Time: 19:03:32
 */
public class CompilerOutputsEditor extends ModuleElementsEditor {
  private RadioButton myInheritCompilerOutput;
  private RadioButton myPerModuleCompilerOutput;

  private CheckBox myCbExcludeOutput;

  private Map<ContentFolderTypeProvider, CommitableFieldPanel> myOutputFields = new LinkedHashMap<>();

  private Predicate<ContentFolderTypeProvider> myFilter;

  protected CompilerOutputsEditor(final ModuleConfigurationState state) {
    super(state);
    myFilter = ContentFolderScopes.productionAndTest();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component createUIComponentImpl(@Nonnull Disposable parentUIDisposable) {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    myInheritCompilerOutput = RadioButton.create(ProjectLocalize.projectInheritCompileOutputPath());
    myPerModuleCompilerOutput = RadioButton.create(ProjectLocalize.projectModuleCompileOutputPath());

    ValueGroups.boolGroup().add(myInheritCompilerOutput).add(myPerModuleCompilerOutput);

    final ValueComponent.ValueListener<Boolean> listener = e -> enableCompilerSettings(!myInheritCompilerOutput.getValueOrError());

    myInheritCompilerOutput.addValueListener(listener);
    myPerModuleCompilerOutput.addValueListener(listener);

    for (ContentFolderTypeProvider provider : ContentFolderTypeProvider.filter(myFilter)) {
      CommitableFieldPanel panel = createOutputPathPanel("Select " + provider.getName() + " Output", provider, parentUIDisposable, url -> {
        if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
          return;
        }
        moduleCompilerPathsManager.setCompilerOutputUrl(provider, url);
      });

      myOutputFields.put(provider, panel);
    }

    myCbExcludeOutput = CheckBox.create(ProjectBundle.message("module.paths.exclude.output.checkbox"), moduleCompilerPathsManager.isExcludeOutput());
    myCbExcludeOutput.addValueListener(e -> moduleCompilerPathsManager.setExcludeOutput(myCbExcludeOutput.getValueOrError()));

    VerticalLayout panel = VerticalLayout.create();
    panel.add(myInheritCompilerOutput);
    panel.add(myPerModuleCompilerOutput);

    FormBuilder formBuilder = FormBuilder.create();
    for (Map.Entry<ContentFolderTypeProvider, CommitableFieldPanel> entry : myOutputFields.entrySet()) {
      CommitableFieldPanel value = entry.getValue();

      formBuilder.addLabeled(value.getLabelComponent(), value.getComponent());
    }

    formBuilder.addBottom(myCbExcludeOutput);

    Component bottom = formBuilder.build();
    bottom.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, Image.DEFAULT_ICON_SIZE);
    panel.add(bottom);

    //// fill with data
    updateOutputPathPresentation();
    //
    ////compiler settings
    final boolean outputPathInherited = moduleCompilerPathsManager.isInheritedCompilerOutput();
    myInheritCompilerOutput.setValue(outputPathInherited);
    myPerModuleCompilerOutput.setValue(!outputPathInherited);
    enableCompilerSettings(!outputPathInherited);

    panel.addBorders(BorderStyle.EMPTY, null, 5);
    return panel;
  }

  @RequiredUIAccess
  private void updateOutputPathPresentation() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    if (moduleCompilerPathsManager.isInheritedCompilerOutput()) {
      final String baseUrl = getState().getModulesConfigurator().getCompilerOutputUrl();

      moduleCompileOutputChanged(baseUrl, getModule().getName());
    }
    else {
      for (ContentFolderTypeProvider contentFolderTypeProvider : ContentFolderTypeProvider.filter(myFilter)) {
        CommitableFieldPanel commitableFieldPanel = toField(contentFolderTypeProvider);

        final VirtualFile compilerOutputPath = moduleCompilerPathsManager.getCompilerOutput(contentFolderTypeProvider);
        if (compilerOutputPath != null) {
          commitableFieldPanel.setValue(FileUtil.toSystemDependentName(compilerOutputPath.getPath()));
        }
        else {
          final String compilerOutputUrl = moduleCompilerPathsManager.getCompilerOutputUrl(contentFolderTypeProvider);
          if (compilerOutputUrl != null) {
            commitableFieldPanel.setValue(FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(compilerOutputUrl)));
          }
        }
      }
    }
  }

  @Nonnull
  public Module getModule() {
    return getModel().getModule();
  }

  @RequiredUIAccess
  private void enableCompilerSettings(final boolean enabled) {
    for (CommitableFieldPanel commitableFieldPanel : myOutputFields.values()) {
      commitableFieldPanel.setEnabled(enabled);
    }

    myCbExcludeOutput.setEnabled(enabled);
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    moduleCompilerPathsManager.setInheritedCompilerOutput(!enabled);
    updateOutputPathPresentation();
  }

  @RequiredUIAccess
  private CommitableFieldPanel createOutputPathPanel(String title, ContentFolderTypeProvider provider, Disposable parentUIDisposable, Consumer<String> commitPathRunnable) {
    FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myProject);
    builder.dialogTitle(title);
    builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());
    builder.uiDisposable(parentUIDisposable);

    FileChooserTextBoxBuilder.Controller controller = builder.build();

    Label label = Label.create(LocalizeValue.localizeTODO(provider.getName() + " Output: "));
    CommitableFieldPanel commitableFieldPanel = new CommitableFieldPanel(controller, label);
    commitableFieldPanel.myCommitRunnable = () -> {
      if (!getModel().isWritable()) {
        return;
      }
      String url = commitableFieldPanel.getUrl();
      commitPathRunnable.accept(url);
    };
    return commitableFieldPanel;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    if (myInheritCompilerOutput.getValueOrError() != moduleCompilerPathsManager.isInheritedCompilerOutput()) {
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
    return Objects.requireNonNull(myOutputFields.get(contentFolderTypeProvider));
  }

  @Override
  public void saveData() {
    for (CommitableFieldPanel panel : myOutputFields.values()) {
      panel.commit();
    }
  }

  @Override
  public String getDisplayName() {
    return ProjectBundle.message("project.roots.path.tab.title");
  }

  @Override
  @RequiredUIAccess
  public void moduleStateChanged() {
    ModuleCompilerPathsManager moduleCompilerPathsManager = ModuleCompilerPathsManager.getInstance(getModule());
    //if content enties tree was changed
    myCbExcludeOutput.setValue(moduleCompilerPathsManager.isExcludeOutput());
  }

  @Override
  @RequiredUIAccess
  public void moduleCompileOutputChanged(final String baseUrl, final String moduleName) {
    if (myInheritCompilerOutput.getValueOrError()) {
      if (baseUrl != null) {
        for (Map.Entry<ContentFolderTypeProvider, CommitableFieldPanel> entry : myOutputFields.entrySet()) {
          ContentFolderTypeProvider key = entry.getKey();
          CommitableFieldPanel value = entry.getValue();

          value.setValue(buildOutputUrl(key));
        }
      }
      else {
        for (CommitableFieldPanel panel : myOutputFields.values()) {
          panel.setValue("");
        }
      }
    }
  }

  @Nonnull
  private String buildOutputUrl(@Nonnull ContentFolderTypeProvider provider) {
    ModulesConfigurator modulesConfigurator = getState().getModulesConfigurator();
    String relativePathForProvider = modulesConfigurator.getCompilerOutputUrl() + "/" + ModuleCompilerPathsManager.getRelativePathForProvider(provider, getModule());
    return FileUtil.toSystemDependentName(VfsUtilCore.urlToPath(relativePathForProvider));
  }

  private static class CommitableFieldPanel {
    private final FileChooserTextBoxBuilder.Controller myController;
    private final Label myLabel;

    private Runnable myCommitRunnable;

    public CommitableFieldPanel(FileChooserTextBoxBuilder.Controller controller, Label label) {
      myController = controller;
      myLabel = label;
    }

    @RequiredUIAccess
    public void setEnabled(boolean value) {
      myLabel.setEnabled(value);
      myController.getComponent().setEnabled(value);
    }

    @RequiredUIAccess
    public Component getComponent() {
      return myController.getComponent();
    }

    public Label getLabelComponent() {
      return myLabel;
    }

    @RequiredUIAccess
    public void setValue(String text) {
      myController.setValue(text);
    }

    @Nullable
    @RequiredUIAccess
    public String getUrl() {
      final String path = myController.getValue().trim();
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
