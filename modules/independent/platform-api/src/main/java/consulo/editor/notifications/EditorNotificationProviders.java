/*
 * Copyright 2013-2016 consulo.io
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
package consulo.editor.notifications;

import com.intellij.openapi.project.Project;
import com.intellij.util.NotNullFunction;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import consulo.annotations.Exported;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 12.11.2015
 */
public class EditorNotificationProviders {
  private static final List<NotNullFunction<Project, EditorNotificationProvider<?>>> ourAdditionalEditorProviderFactories =
          new ArrayList<NotNullFunction<Project, EditorNotificationProvider<?>>>();

  @Nonnull
  public static List<EditorNotificationProvider<?>> createProviders(@Nonnull Project project) {
    EditorNotificationProvider<?>[] extensions = EditorNotificationProvider.EP_NAME.getExtensions(project);
    List<EditorNotificationProvider<?>> providers =
            new ArrayList<EditorNotificationProvider<?>>(extensions.length + ourAdditionalEditorProviderFactories.size());
    ContainerUtil.addAll(providers, extensions);
    for (NotNullFunction<Project, EditorNotificationProvider<?>> function : ourAdditionalEditorProviderFactories) {
      providers.add(function.fun(project));
    }
    return providers;
  }

  @Exported
  public static void registerProvider(@Nonnull NotNullFunction<Project, EditorNotificationProvider<?>> function) {
    ourAdditionalEditorProviderFactories.add(function);
  }
}
