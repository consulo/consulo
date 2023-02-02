/*
 * Copyright 2013-2023 consulo.io
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
package consulo.task.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.task.TaskRepositoryType;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 02/02/2023
 */
@ExtensionImpl
public class TaskRepositoryTypeExtensionPreviewRecorder implements ExtensionPreviewRecorder<TaskRepositoryType> {
  private final Application myApplication;

  @Inject
  public TaskRepositoryTypeExtensionPreviewRecorder(Application application) {
    myApplication = application;
  }

  @Override
  public void analyze(@Nonnull Consumer<ExtensionPreview<TaskRepositoryType>> recorder) {
    myApplication.getExtensionPoint(TaskRepositoryType.class).forEachExtensionSafe(it -> {
      ExtensionPreview<TaskRepositoryType> preview = new ExtensionPreview<>(TaskRepositoryType.class, it.getId(), it);
      recorder.accept(preview);
    });
  }
}
