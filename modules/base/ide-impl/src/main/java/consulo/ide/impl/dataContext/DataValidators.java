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
package consulo.ide.impl.dataContext;

import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataValidator;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-Oct-17
 */
public class DataValidators {
  public static class ArrayValidator<T> implements DataValidator<T[]> {
    private static final Logger LOG = Logger.getInstance(ArrayValidator.class);

    private final DataValidator<T> myElementValidator;

    public ArrayValidator(DataValidator<T> elementValidator) {
      myElementValidator = elementValidator;
    }

    @Nonnull
    @Override
    public Key<T[]> getKey() {
      throw new UnsupportedOperationException("should never called");
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] findInvalid(final Key<T[]> dataId, T[] array, final Object dataSource) {
      for (T element : array) {
        if (element == null) {
          LOG.error("Data isn't valid. " + dataId + "=null Provided by: " + dataSource.getClass()
                                                                                      .getName() + " (" + dataSource.toString() + ")");
        }
        T invalid = myElementValidator.findInvalid(myElementValidator.getKey(), element, dataSource);
        if (invalid != null) {
          T[] result = (T[])Array.newInstance(array[0].getClass(), 1);
          result[0] = invalid;
          return result;
        }
      }
      return null;
    }
  }

  private static final DataValidator<VirtualFile> VIRTUAL_FILE_VALIDATOR = new DataValidator<>() {
    @Nonnull
    @Override
    public Key<VirtualFile> getKey() {
      return VirtualFile.KEY;
    }

    @Override
    public VirtualFile findInvalid(final Key<VirtualFile> dataId, VirtualFile file, final Object dataSource) {
      return file.isValid() ? null : file;
    }
  };

  private static final DataValidator<Project> PROJECT_VALIDATOR = new DataValidator<>() {
    @Nonnull
    @Override
    public Key<Project> getKey() {
      return Project.KEY;
    }

    @Override
    public Project findInvalid(final Key<Project> dataId, final Project project, final Object dataSource) {
      return project.isDisposed() ? project : null;
    }
  };

  private static final ExtensionPointCacheKey<DataValidator, Map<Key, DataValidator>> CACHE_KEY =
    ExtensionPointCacheKey.create("DataValidator", dataValidators -> {
      Map<Key, DataValidator> map = new IdentityHashMap<>();
      map.put(VirtualFile.KEY, DataValidators.VIRTUAL_FILE_VALIDATOR);
      map.put(VirtualFile.KEY_OF_ARRAY, new DataValidators.ArrayValidator<>(DataValidators.VIRTUAL_FILE_VALIDATOR));
      map.put(Project.KEY, DataValidators.PROJECT_VALIDATOR);

      for (DataValidator validator : dataValidators) {
        map.put(validator.getKey(), validator);
      }
      return map;
    });

  @Nullable
  @SuppressWarnings("unchecked")
  private static <T> DataValidator<T> getValidator(Key<T> dataId) {
    Map<Key, DataValidator> map = Application.get().getExtensionPoint(DataValidator.class).getOrBuildCache(CACHE_KEY);
    return map.get(dataId);
  }

  public static <T> T findInvalidData(Key<T> dataId, T data, final Object dataSource) {
    if (data == null) return null;
    DataValidator<T> validator = getValidator(dataId);
    if (validator != null) return validator.findInvalid(dataId, (T)data, dataSource);
    return null;
  }
}
