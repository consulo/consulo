/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.util;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.base.FilePathImpl;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ObjectsConvertor {
  private final static DownCast DOWN_CAST = new DownCast();

  public static class DownCast<Sup, Sub extends Sup> implements Function<Sub, Sup> {
    @Override
    public Sup apply(Sub o) {
      return o;
    }
  }

  public static <Sup, Sub extends Sup> List<Sup> downcast(List<Sub> list) {
    return convert(list, (Function<Sub, Sup>)DOWN_CAST);
  }

  public static final Function<FilePath, VirtualFile> FILEPATH_TO_VIRTUAL = FilePath::getVirtualFile;

  public static final Function<VirtualFile, FilePath> VIRTUAL_FILEPATH = FilePathImpl::new;

  public static final Function<FilePath, File> FILEPATH_FILE = FilePath::getIOFile;

  public static final Function<File, FilePath> FILE_FILEPATH = FilePathImpl::create;

  public static final Function<Object, Boolean> NOT_NULL = Objects::nonNull;

  private ObjectsConvertor() {
  }

  public static List<VirtualFile> fp2vf(@Nonnull final Collection<FilePath> in) {
    return convert(in, FILEPATH_TO_VIRTUAL);
  }

  public static List<FilePath> vf2fp(@Nonnull final List<VirtualFile> in) {
    return convert(in, VIRTUAL_FILEPATH);
  }

  public static List<File> fp2jiof(@Nonnull final Collection<FilePath> in) {
    return convert(in, FILEPATH_FILE);
  }

  public static <T, S> List<S> convert(@Nonnull final Collection<T> in, final Function<T, S> convertor) {
    return convert(in, convertor, null);
  }

  public static <T, U, S extends U> List<S> convert(@Nonnull final Collection<T> in, final Function<T, S> convertor, @Nullable final Function<U, Boolean> outFilter) {
    final List<S> out = new ArrayList<S>();
    for (T t : in) {
      final S converted = convertor.apply(t);
      if ((outFilter != null) && (!Boolean.TRUE.equals(outFilter.apply(converted)))) continue;
      out.add(converted);
    }
    return out;
  }
}
