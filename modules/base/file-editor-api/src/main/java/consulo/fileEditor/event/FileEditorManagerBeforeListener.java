/*
 * Copyright 2013-2022 consulo.io
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
package consulo.fileEditor.event;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.fileEditor.FileEditorManager;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.EventListener;

@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface FileEditorManagerBeforeListener extends EventListener {
  void beforeFileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file);

  void beforeFileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file);

  class Adapter implements FileEditorManagerBeforeListener {
    @Override
    public void beforeFileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    }

    @Override
    public void beforeFileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
    }
  }
}
