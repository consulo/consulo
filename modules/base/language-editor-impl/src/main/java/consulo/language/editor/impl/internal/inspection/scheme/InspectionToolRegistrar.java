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

package consulo.language.editor.impl.internal.inspection.scheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.language.editor.inspection.GlobalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class InspectionToolRegistrar {
  public static InspectionToolRegistrar getInstance() {
    return Application.get().getInstance(InspectionToolRegistrar.class);
  }

  private final Application myApplication;

  @Inject
  public InspectionToolRegistrar(Application application) {
    myApplication = application;
  }

  @Nonnull
  public List<InspectionToolWrapper> createTools() {
    final List<InspectionToolWrapper> tools = new ArrayList<>();
    walkWrappers(tools::add);
    return tools;
  }

  private void walkWrappers(Consumer<InspectionToolWrapper<?>> consumer) {
    List<LocalInspectionToolWrapper> localInspectionToolWrappers =
      myApplication.getExtensionPoint(LocalInspectionTool.class).getOrBuildCache(InspectionToolWrappers.LOCAL_WRAPPERS);
    for (LocalInspectionToolWrapper wrapper : localInspectionToolWrappers) {
      consumer.accept(wrapper.createCopy());
    }

    List<GlobalInspectionToolWrapper> globalInspectionToolWrappers =
      myApplication.getExtensionPoint(GlobalInspectionTool.class).getOrBuildCache(InspectionToolWrappers.GLOBAL_WRAPPERS);
    for (GlobalInspectionToolWrapper globalInspectionToolWrapper : globalInspectionToolWrappers) {
      consumer.accept(globalInspectionToolWrapper.createCopy());
    }
  }
}
