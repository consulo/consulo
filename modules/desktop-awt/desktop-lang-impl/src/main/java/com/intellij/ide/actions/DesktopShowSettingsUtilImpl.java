/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.options.newEditor.DesktopSettingsDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DefaultProjectFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkTable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.DefaultSdksModel;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.Function;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.disposer.Disposer;
import consulo.ide.base.BaseShowSettingsUtil;
import consulo.ide.settings.impl.SettingsSdksModel;
import consulo.logging.Logger;
import consulo.options.BaseProjectStructureShowSettingsUtil;
import consulo.options.ProjectStructureSelector;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.impl.ModalityPerProjectEAPDescriptor;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author max
 */
@Singleton
public class DesktopShowSettingsUtilImpl extends BaseProjectStructureShowSettingsUtil  {
  private static final Logger LOG = Logger.getInstance(DesktopShowSettingsUtilImpl.class);

  private final AtomicBoolean myShown = new AtomicBoolean(false);

  private final DefaultProjectFactory myDefaultProjectFactory;

  private final DefaultSdksModel mySdksModel;

  @Inject
  DesktopShowSettingsUtilImpl(DefaultProjectFactory defaultProjectFactory, Provider<SdkTable> sdkTableProvider) {
    myDefaultProjectFactory = defaultProjectFactory;
    mySdksModel = new DefaultSdksModel(sdkTableProvider);
  }

  @Nonnull
  @Override
  public SettingsSdksModel getSdksModel() {
    mySdksModel.initializeIfNeed();
    return mySdksModel;
  }

  @SuppressWarnings("deprecation")
  private void showSettingsImpl(@Nullable Project tempProject,
                                @Nonnull Function<Project, Configurable[]> buildConfigurables,
                                @Nullable Configurable toSelect,
                                @Nonnull Consumer<DesktopSettingsDialog> onShow) {
    Project actualProject = tempProject != null ? tempProject : myDefaultProjectFactory.getDefaultProject();

    new Task.Backgroundable(actualProject, "Opening " + CommonBundle.settingsTitle() + "...") {
      private Configurable[] myConfigurables;
      private long myStartTime;

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        myStartTime = System.currentTimeMillis();
        myConfigurables = buildConfigurables.apply(actualProject);
      }

      @RequiredUIAccess
      @Override
      public void onFinished() {
        UIAccess uiAccess = UIAccess.current();
        uiAccess.give(() -> {
          myShown.set(true);

          DesktopSettingsDialog dialog;
          if (ModalityPerProjectEAPDescriptor.is()) {
            dialog = new DesktopSettingsDialog(actualProject, myConfigurables, toSelect, true);
          }
          else {
            dialog = new DesktopSettingsDialog(actualProject, myConfigurables, toSelect);
          }

          Disposer.register(dialog.getDisposable(), () -> clearCaches());

          new UiNotifyConnector.Once(dialog.getContentPane(), new Activatable() {
            @Override
            public void showNotify() {
              onShow.accept(dialog);
            }
          });

          long time = System.currentTimeMillis() - myStartTime;
          LOG.info("Settings dialog initialization took " + time + " ms.");
          dialog.showAsync().doWhenProcessed(() -> myShown.set(false));
        });
      }
    }.queue();
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  @Override
  public <T extends UnnamedConfigurable> void showAndSelect(@Nullable Project project, @Nonnull Class<T> configurableClass, @Nonnull Consumer<T> afterSelect) {
    assert Configurable.class.isAssignableFrom(configurableClass) : "Not a configurable: " + configurableClass.getName();

    Configurable[] configurables = buildConfigurables(project);

    showSettingsImpl(project, project1 -> configurables, null, dialog -> {
      final Settings editor = dialog.getDataUnchecked(Settings.KEY);
      assert editor != null;
      editor.select(configurableClass).doWhenDone(afterSelect);
    });
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable final Project project, @Nonnull final String nameToSelect) {
    Configurable[] configurables = buildConfigurables(project);

    Configurable toSelect = DesktopSettingsDialog.getPreselectedByDisplayName(configurables, nameToSelect, project);

    showSettingsImpl(project, it -> configurables, toSelect, dialog -> {
    });
  }

  @Override
  @RequiredUIAccess
  public void showSettingsDialog(@Nullable Project project, final String id2Select, final String filter) {
    Configurable[] configurables = buildConfigurables(project);

    final Configurable configurable2Select = findConfigurable2Select(id2Select, configurables);

    showSettingsImpl(project, it -> configurables, configurable2Select, dialog -> {
      final Settings editor = dialog.getDataUnchecked(Settings.KEY);
      assert editor != null;
      editor.select(configurable2Select, filter);
    });
  }

  @Nullable
  private static Configurable findConfigurable2Select(String id2Select, Configurable[] configurables) {
    for (Configurable configurable : configurables) {
      final Configurable conf = containsId(id2Select, configurable);
      if (conf != null) return conf;
    }
    return null;
  }

  @Nullable
  private static Configurable containsId(String id2Select, Configurable configurable) {
    if (configurable instanceof SearchableConfigurable && id2Select.equals(((SearchableConfigurable)configurable).getId())) {
      return configurable;
    }
    if (configurable instanceof SearchableConfigurable.Parent) {
      for (Configurable subConfigurable : ((SearchableConfigurable.Parent)configurable).getConfigurables()) {
        final Configurable config = containsId(id2Select, subConfigurable);
        if (config != null) return config;
      }
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  public void showSettingsDialog(@Nullable Project project, @Nullable Configurable toSelect) {
    showSettingsImpl(project, BaseShowSettingsUtil::buildConfigurables, toSelect, dialog -> {
    });
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> showProjectStructureDialog(@Nonnull Project project, @Nonnull Consumer<ProjectStructureSelector> consumer) {
    Configurable[] configurables = buildConfigurables(project);

    AsyncResult<Void> result = AsyncResult.undefined();
    showSettingsImpl(project, it -> configurables, SKIP_SELECTION_CONFIGURATION, dialog -> {
      final ProjectStructureSelector editor = dialog.getDataUnchecked(ProjectStructureSelector.KEY);
      assert editor != null;
      consumer.accept(editor);
      result.setDone();
    });
    return result;
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable) {
    return editConfigurable(title, project, createDimensionKey(configurable), configurable);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(@Nullable String title, Project project, String dimensionServiceKey, @Nonnull Configurable configurable) {
    return editConfigurable(null, project, configurable, title, dimensionServiceKey, null);
  }

  @Override
  @RequiredUIAccess
  public AsyncResult<Void> editConfigurable(String title, Project project, Configurable configurable, Runnable advancedInitialization) {
    return editConfigurable(null, project, configurable, title, createDimensionKey(configurable), advancedInitialization);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, Configurable configurable) {
    return editConfigurable(parent, configurable, null);
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(final Component parent, final Configurable configurable, @Nullable final Runnable advancedInitialization) {
    return editConfigurable(parent, null, configurable, null, createDimensionKey(configurable), advancedInitialization);
  }

  @RequiredUIAccess
  private static AsyncResult<Void> editConfigurable(@Nullable Component parent,
                                                    @Nullable Project project,
                                                    Configurable configurable,
                                                    String title,
                                                    String dimensionKey,
                                                    @Nullable final Runnable advancedInitialization) {
    SingleConfigurableEditor editor;
    if (parent != null) {
      editor = new SingleConfigurableEditor(parent, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
    }
    else {
      editor = new SingleConfigurableEditor(project, configurable, title, dimensionKey, true, DialogWrapper.IdeModalityType.IDE);
    }
    if (advancedInitialization != null) {
      new UiNotifyConnector.Once(editor.getContentPane(), new Activatable() {
        @Override
        public void showNotify() {
          advancedInitialization.run();
        }
      });
    }
    return editor.showAsync();
  }

  @RequiredUIAccess
  @Override
  public AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable) {
    return editConfigurable(parent, null, configurable, null, dimensionServiceKey, null);
  }

  @Override
  public boolean isAlreadyShown() {
    return myShown.get();
  }
}