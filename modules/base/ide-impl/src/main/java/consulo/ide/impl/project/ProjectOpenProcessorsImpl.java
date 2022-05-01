/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.impl.project;

import consulo.ide.impl.idea.openapi.util.NotNullLazyValue;
import consulo.ide.impl.idea.platform.DefaultProjectOpenProcessor;
import consulo.ide.impl.idea.projectImport.ProjectOpenProcessor;
import consulo.ide.impl.moduleImport.ImportProjectOpenProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
@Singleton
public class ProjectOpenProcessorsImpl implements ProjectOpenProcessors {
  private final NotNullLazyValue<ProjectOpenProcessor[]> myCacheValue;

  @Inject
  @SuppressWarnings("unchecked")
  public ProjectOpenProcessorsImpl() {
    myCacheValue = NotNullLazyValue.createValue(() -> {
      List<ProjectOpenProcessor> processors = new ArrayList<>();
      processors.add(DefaultProjectOpenProcessor.getInstance());
      processors.add(new ImportProjectOpenProcessor());
      return processors.toArray(new ProjectOpenProcessor[processors.size()]);
    });
  }

  @Nonnull
  @Override
  public ProjectOpenProcessor[] getProcessors() {
    return myCacheValue.getValue();
  }
}
