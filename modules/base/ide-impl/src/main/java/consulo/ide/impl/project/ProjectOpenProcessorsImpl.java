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

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.platform.DefaultProjectOpenProcessor;
import consulo.ide.impl.idea.projectImport.ProjectOpenProcessor;
import consulo.ide.impl.moduleImport.ImportProjectOpenProcessor;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
@Singleton
@ServiceImpl
public class ProjectOpenProcessorsImpl implements ProjectOpenProcessors {
  @Nonnull
  @Override
  public List<ProjectOpenProcessor> getProcessors() {
    List<ProjectOpenProcessor> processors = new ArrayList<>(2);
    processors.add(DefaultProjectOpenProcessor.getInstance());
    processors.add(new ImportProjectOpenProcessor());
    return processors;
  }
}
