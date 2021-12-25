/*
 * Copyright 2013-2021 consulo.io
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
package consulo.project.impl.welcome;

import com.intellij.openapi.components.impl.ProjectPathMacroManager;
import com.intellij.openapi.project.Project;
import consulo.components.impl.stores.ApplicationDefaultStoreCache;
import consulo.components.impl.stores.SingleFileProjectStoreImpl;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25/12/2021
 */
public class WelcomeProjectStoreImpl extends SingleFileProjectStoreImpl {
  public static final String ROOT_TAG_NAME = "welcomeProject";

  @Inject
  public WelcomeProjectStoreImpl(@Nonnull Project project, @Nonnull ProjectPathMacroManager pathMacroManager, @Nonnull Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(project, pathMacroManager, applicationDefaultStoreCache);
  }

  @Nonnull
  @Override
  protected String getRootTagName() {
    return ROOT_TAG_NAME;
  }
}