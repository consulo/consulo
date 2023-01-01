/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.execution;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.dataContext.DataContext;
import consulo.execution.internal.ExecutionDataContextCacher;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 09-Feb-22
 */
@Singleton
@ServiceImpl
public class ExecutionDataContextCacherImpl implements ExecutionDataContextCacher {
  private static class CachingDataContext implements DataContext {
    private static final Key[] keys = {Project.KEY, PlatformDataKeys.PROJECT_FILE_DIRECTORY, CommonDataKeys.EDITOR, CommonDataKeys.VIRTUAL_FILE, Module.KEY, CommonDataKeys.PSI_FILE};
    private final Map<Key, Object> values = new HashMap<>();

    @Nonnull
    static CachingDataContext cacheIfNeed(@Nonnull DataContext context) {
      if (context instanceof CachingDataContext) return (CachingDataContext)context;
      return new CachingDataContext(context);
    }

    @SuppressWarnings("unchecked")
    private CachingDataContext(DataContext context) {
      for (Key key : keys) {
        values.put(key, context.getData(key));
      }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@Nonnull Key<T> dataId) {
      return (T)values.get(dataId);
    }
  }

  @Nonnull
  @Override
  public DataContext getCachedContext(@Nonnull DataContext dataContext) {
    return CachingDataContext.cacheIfNeed(dataContext);
  }
}
