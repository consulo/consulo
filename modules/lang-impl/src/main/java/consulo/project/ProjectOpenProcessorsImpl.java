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
package consulo.project;

import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.projectImport.ProjectOpenProcessor;
import consulo.moduleImport.ModuleImportBasedProjectOpenProcessor;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 31-Jan-17
 */
public class ProjectOpenProcessorsImpl implements ProjectOpenProcessors {

  private NotNullLazyValue<ProjectOpenProcessor[]> myValue = new NotNullLazyValue<ProjectOpenProcessor[]>() {
    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    protected ProjectOpenProcessor[] compute() {
      List<ProjectOpenProcessor> processors = new ArrayList<>();
      processors.add(PlatformProjectOpenProcessor.getInstance());

      for (ModuleImportProvider<?> provider : ModuleImportProviders.getExtensions()) {
        processors.add(new ModuleImportBasedProjectOpenProcessor(provider));
      }
      return processors.toArray(new ProjectOpenProcessor[processors.size()]);
    }
  };

  @NotNull
  @Override
  public ProjectOpenProcessor[] getProcessors() {
    return myValue.getValue();
  }
}
