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
package consulo.externalSystem.impl.internal.service.project.autoimport;

import consulo.externalSystem.ExternalSystemAutoImportAware;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link ExternalSystemAutoImportAware} implementation which caches positive answers, i.e. mappings between file paths and
 * corresponding root external project path.
 * 
 * @author Denis Zhdanov
 * @since 6/8/13 3:43 PM
 */
public class CachingExternalSystemAutoImportAware implements ExternalSystemAutoImportAware {

  @Nonnull
  private final ConcurrentMap<String/* file path */, String/* root external project path */> myCache
    = new ConcurrentHashMap<>();
  
  @Nonnull
  private final ExternalSystemAutoImportAware myDelegate;

  public CachingExternalSystemAutoImportAware(@Nonnull ExternalSystemAutoImportAware delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public String getAffectedExternalProjectPath(@Nonnull String changedFileOrDirPath, @Nonnull Project project) {
    String cached = myCache.get(changedFileOrDirPath);
    if (cached != null) {
      return cached;
    }
    String result = myDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
    if (result != null) {
      myCache.put(changedFileOrDirPath, result);
    }
    return result;
  }
}
