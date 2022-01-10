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
package com.intellij.execution.testframework.autotest;

import com.intellij.execution.*;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.openapi.components.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@State(
        name = "AutoTestManager",
        storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)}
)
public class AutoTestManager extends AbstractAutoTestManager {

  @Nonnull
  public static AutoTestManager getInstance(Project project) {
    return ServiceManager.getService(project, AutoTestManager.class);
  }

  @Inject
  public AutoTestManager(@Nonnull Project project) {
    super(project);
  }

  @Override
  @Nonnull
  protected AutoTestWatcher createWatcher(Project project) {
    return new DelayedDocumentWatcher(project, myDelayMillis, this::restartAllAutoTests, file -> {
      if (ScratchFileService.getInstance().getRootType(file) != null) {
        return false;
      }
      // Vladimir.Krivosheev - I don't know, why AutoTestManager checks it, but old behavior is preserved
      return FileEditorManager.getInstance(project).isFileOpen(file);
    });
  }
}
