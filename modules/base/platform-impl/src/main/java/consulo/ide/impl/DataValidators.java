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
package consulo.ide.impl;

import com.intellij.ide.impl.DataValidator;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;

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
          LOG.error("Data isn't valid. " + dataId + "=null Provided by: " + dataSource.getClass().getName() + " (" + dataSource.toString() + ")");
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

  private static final DataValidator<VirtualFile> VIRTUAL_FILE_VALIDATOR = new DataValidator<VirtualFile>() {
    @Nonnull
    @Override
    public Key<VirtualFile> getKey() {
      return PlatformDataKeys.VIRTUAL_FILE;
    }

    @Override
    public VirtualFile findInvalid(final Key<VirtualFile> dataId, VirtualFile file, final Object dataSource) {
      return file.isValid() ? null : file;
    }
  };

  private static final DataValidator<Project> PROJECT_VALIDATOR = new DataValidator<Project>() {
    @Nonnull
    @Override
    public Key<Project> getKey() {
      return CommonDataKeys.PROJECT;
    }

    @Override
    public Project findInvalid(final Key<Project> dataId, final Project project, final Object dataSource) {
      return project.isDisposed() ? project : null;
    }
  };

  private static NotNullLazyValue<Map<Key, DataValidator>> ourCache = NotNullLazyValue.createValue(() -> {
    Map<Key, DataValidator> map = new IdentityHashMap<>();
    map.put(PlatformDataKeys.VIRTUAL_FILE, DataValidators.VIRTUAL_FILE_VALIDATOR);
    map.put(PlatformDataKeys.VIRTUAL_FILE_ARRAY, new DataValidators.ArrayValidator<>(DataValidators.VIRTUAL_FILE_VALIDATOR));
    map.put(CommonDataKeys.PROJECT, DataValidators.PROJECT_VALIDATOR);

    for (DataValidator validator : DataValidator.EP_NAME.getExtensionList()) {
      map.put(validator.getKey(), validator);
    }
    return map;
  });

  @Nullable
  @SuppressWarnings("unchecked")
  private static <T> DataValidator<T> getValidator(Key<T> dataId) {
    return ourCache.getValue().get(dataId);
  }

  public static <T> T findInvalidData(Key<T> dataId, T data, final Object dataSource) {
    if (data == null) return null;
    DataValidator<T> validator = getValidator(dataId);
    if (validator != null) return validator.findInvalid(dataId, (T)data, dataSource);
    return null;
  }
}
