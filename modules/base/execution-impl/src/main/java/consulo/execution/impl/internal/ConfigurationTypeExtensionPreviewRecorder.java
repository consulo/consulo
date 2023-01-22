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
package consulo.execution.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.component.extension.preview.ExtensionPreview;
import consulo.component.extension.preview.ExtensionPreviewRecorder;
import consulo.execution.configuration.ConfigurationType;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 22/01/2023
 */
@ExtensionImpl
public class ConfigurationTypeExtensionPreviewRecorder implements ExtensionPreviewRecorder<ConfigurationType> {
  private final Application myApplication;

  @Inject
  public ConfigurationTypeExtensionPreviewRecorder(Application application) {
    myApplication = application;
  }

  @Override
  public void analyze(@Nonnull Consumer<ExtensionPreview<ConfigurationType>> recorder) {
    myApplication.getExtensionPoint(ConfigurationType.class).forEachExtensionSafe(it -> {
      ExtensionPreview<ConfigurationType> preview = new ExtensionPreview<>(ConfigurationType.class, it.getId(), it);

      recorder.accept(preview);
    });
  }
}
