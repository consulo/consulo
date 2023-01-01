/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.test.autotest;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.fileEditor.FileEditorManager;
import consulo.language.editor.scratch.ScratchUtil;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@State(name = "AutoTestManager", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class AutoTestManager extends AbstractAutoTestManager {

  @Nonnull
  public static AutoTestManager getInstance(Project project) {
    return project.getInstance(AutoTestManager.class);
  }

  @Inject
  public AutoTestManager(@Nonnull Project project) {
    super(project);
  }

  @Override
  @Nonnull
  protected AutoTestWatcher createWatcher(Project project) {
    return new DelayedDocumentWatcher(project, myDelayMillis, this::restartAllAutoTests, file -> {
      if (ScratchUtil.isScratch(file)) {
        return false;
      }
      // Vladimir.Krivosheev - I don't know, why AutoTestManager checks it, but old behavior is preserved
      return FileEditorManager.getInstance(project).isFileOpen(file);
    });
  }
}
