/*
 * Copyright 2013-2021 consulo.io
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
package consulo.module.creation.ui;

import consulo.application.Application;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetManager;
import consulo.application.ui.PlatformTargetComboBox;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.module.creation.NewModuleWizardContext;
import consulo.module.creation.localize.ModuleCreationLocalize;
import consulo.module.ui.BundleBox;
import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ui.ex.TextComponentAccessor;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 08/01/2021
 */
public class UnifiedProjectOrModuleNameStep<C extends NewModuleWizardContext> implements WizardStep<C> {
  private final C myContext;

  private Component myRootPanel;
  private TextBox myNameTextBox;
  private FileChooserTextBoxBuilder.Controller myFileChooserController;
  private boolean myUserPathEntered;
  private boolean myUserNameEntered;
  private boolean myPathChosen;
  // the machine of the project - chosen for a new project
  private Platform myPlatform;
  // the target of the platform, null for the local machine - known without a connection
  private @Nullable String myPlatformTargetId;
  private final List<Consumer<Platform>> myPlatformListeners = new ArrayList<>();

  public UnifiedProjectOrModuleNameStep(C context) {
    myContext = context;
    myPlatform = context.getPlatform();
    myPlatformTargetId = context.getPlatformTargetId();
  }

  @RequiredUIAccess
  @Override
  public Component getComponent(C context, Disposable uiDisposable) {
    if (myRootPanel == null) {
      myRootPanel = buildComponent(uiDisposable);
    }
    return myRootPanel;
  }

  @RequiredUIAccess
  private Component buildComponent(Disposable uiDisposable) {
    myUserNameEntered = false;
    myUserPathEntered = false;

    FormBuilder formBuilder = FormBuilder.create();

    // the machine first - the name and the location are of it; only a new project chooses it, and only when there is another
    PlatformTargetComboBox targetComboBox = null;
    PlatformTargetManager targetManager = Application.get().getInstance(PlatformTargetManager.class);
    boolean chooseTarget = myContext.isPlatformChoosable() && targetManager.getTargets().size() > 1;
    if (chooseTarget) {
      targetComboBox = new PlatformTargetComboBox(targetManager);
      formBuilder.addLabeled(ModuleCreationLocalize.labelProjectTarget(), targetComboBox.getComponent());
    }

    myNameTextBox = TextBox.create();

    formBuilder.addLabeled(myContext.isNewProject() ? ModuleCreationLocalize.labelProjectName() : ModuleCreationLocalize.labelModuleName(), myNameTextBox);

    FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myContext.getProject());
    builder.uiDisposable(uiDisposable);
    builder.textBoxAccessor(new TextComponentAccessor<>() {
      @RequiredUIAccess
      @Override
      public String getValue(TextBox component) {
        return component.getValueOrError();
      }

      @RequiredUIAccess
      @Override
      public void setValue(TextBox component, String path, boolean fireListeners) {
        component.setValue(path, fireListeners);

        if (fireListeners) {
          myUserPathEntered = true;

          if (!myUserNameEntered) {
            int lastSeparatorIndex = lastSeparatorIndex(path);
            if (lastSeparatorIndex >= 0 && (lastSeparatorIndex + 1) < path.length()) {
              myNameTextBox.setValue(path.substring(lastSeparatorIndex + 1), false);
            }
          }
        }
      }
    });

    // a new project may be created on any machine - a module is of its project, an import of its files
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    builder.fileChooserDescriptor(chooseTarget ? descriptor.withAnyPlatform() : descriptor.withPlatform(myPlatform));

    myFileChooserController = builder.build();

    formBuilder.addLabeled(myContext.isNewProject() ? ModuleCreationLocalize.labelProjectFilesLocation() : ModuleCreationLocalize.labelModuleContentRoot(), myFileChooserController.getComponent());

    String projectOrModuleName = myContext.getName();
    String projectOrModulePath = myContext.getPath();

    myNameTextBox.setValue(projectOrModuleName);
    myFileChooserController.setValue(projectOrModulePath, false);

    myNameTextBox.addValueListener(event -> {
      if (myUserPathEntered) {
        return;
      }

      String name = myNameTextBox.getValue();
      String path = myFileChooserController.getValue().trim();
      int lastSeparatorIndex = lastSeparatorIndex(path);

      if (lastSeparatorIndex >= 0) {
        String newPath = path.substring(0, lastSeparatorIndex + 1) + name;

        // do not fire events
        myFileChooserController.setValue(newPath, false);

        myUserNameEntered = true;
      }
    });

    if (targetComboBox != null) {
      PlatformTargetComboBox comboBox = targetComboBox;
      UIAccess uiAccess = UIAccess.current();
      Path localBaseDir = Path.of(projectOrModulePath).getParent();
      comboBox.addListener(targetId -> {
        PlatformTarget target = targetManager.findTarget(targetId);
        if (target == null) {
          return;
        }
        // a target of a chosen path keeps that path
        boolean reinitialize = !myPathChosen;
        target.connect().whenComplete((platform, error) -> {
          if (platform == null) {
            return;
          }
          // the new project of that machine - looked up there, not in the UI thread
          Path location = null;
          if (reinitialize) {
            Path baseDir = platform == Platform.current() && localBaseDir != null ? localBaseDir : platform.user().homePath();
            location = findNonexistent(baseDir, "untitled");
          }
          Path newLocation = location;
          uiAccess.give(() -> setPlatform(platform, targetId, newLocation));
        });
      });
      // a path chosen on another machine - that machine is the target
      myFileChooserController.addPathChosenListener(path -> {
        PlatformTarget target = path.getFileSystem() == FileSystems.getDefault() ? null : targetManager.findTargetOf(path);
        myPathChosen = true;
        try {
          comboBox.setTargetId(target == null ? null : target.getId());
        }
        finally {
          myPathChosen = false;
        }
      });
    }

    extend(formBuilder, uiDisposable);

    return formBuilder.build();
  }

  private static int lastSeparatorIndex(String path) {
    // a path of any machine
    return Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
  }

  @RequiredUIAccess
  protected void extend(FormBuilder builder, Disposable uiDisposable) {
    // for plugins
  }

  @Override
  public void onStepLeave(C c) {
    c.setName(getName());
    c.setPath(getPath());
    c.setPlatform(getPlatform());
  }

  @RequiredUIAccess
  public String getPath() {
    return myFileChooserController.getValue();
  }

  /**
   * The machine of the project or module.
   */
  public Platform getPlatform() {
    return myPlatform;
  }

  /**
   * Called when another machine is chosen for a new project.
   */
  public void addPlatformListener(Consumer<Platform> listener) {
    myPlatformListeners.add(listener);
  }

  /**
   * Offers the bundles of the machine of the project - the local ones by default, the ones of another machine when it is chosen.
   */
  @RequiredUIAccess
  public void bindBundleBox(BundleBox bundleBox) {
    // by the target - the platform of a bundle is not known while its machine is not connected
    Consumer<Platform> filter = platform -> {
      String targetId = myPlatformTargetId;
      bundleBox.setBundleFilter(sdk -> Objects.equals(sdk.getPlatformTargetId(), targetId));
    };
    filter.accept(myPlatform);
    addPlatformListener(filter);
  }

  private static Path findNonexistent(Path baseDir, String prefix) {
    Path candidate = baseDir.resolve(prefix);
    for (int postfix = 1; Files.exists(candidate); postfix++) {
      candidate = baseDir.resolve(prefix + postfix);
    }
    return candidate;
  }

  /**
   * @param location the new project of that machine, null to keep the entered one
   */
  @RequiredUIAccess
  private void setPlatform(Platform platform, @Nullable String targetId, @Nullable Path location) {
    if (location != null) {
      myNameTextBox.setValue(location.getFileName().toString(), false);
      myFileChooserController.setValue(location.toString(), false);
      myUserNameEntered = false;
      myUserPathEntered = false;
    }

    if (platform == myPlatform) {
      return;
    }
    myPlatform = platform;
    myPlatformTargetId = targetId;
    myFileChooserController.setFileSystem(platform == Platform.current() ? null : platform.fs().getPath("/").getFileSystem());

    for (Consumer<Platform> listener : myPlatformListeners) {
      listener.accept(platform);
    }
  }

  public String getName() {
    return myNameTextBox.getValueOrError();
  }

  @Override
  public @Nullable Component getPreferredFocusedComponent() {
    return myNameTextBox;
  }
}
