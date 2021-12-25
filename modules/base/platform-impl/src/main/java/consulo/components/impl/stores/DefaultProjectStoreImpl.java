/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.components.impl.stores;

import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
public class DefaultProjectStoreImpl extends SingleFileProjectStoreImpl {
  public static final String ROOT_TAG_NAME = "defaultProject";

  @Inject
  public DefaultProjectStoreImpl(@Nonnull Project project, @Nonnull ProjectPathMacroManager pathMacroManager, @Nonnull Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(project, pathMacroManager, applicationDefaultStoreCache);
  }

  @Nonnull
  @Override
  protected String getRootTagName() {
    return ROOT_TAG_NAME;
  }
}