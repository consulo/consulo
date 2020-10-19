/*
 * Copyright 2013-2020 consulo.io
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
package consulo.hacking.java.base;

import consulo.logging.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2020-10-18
 */
public final class ThrowableHacking {
  private static final Logger LOG = Logger.getInstance(ThrowableHacking.class);

  private static Field ourBacktraceField;

  static {
    try {
      Field backtrace = Throwable.class.getDeclaredField("backtrace");
      backtrace.setAccessible(true);
      ourBacktraceField = backtrace;
    }
    catch (Throwable e) {
      LOG.warn(e);
    }
  }

  @Nullable
  public static Function<Throwable, Object> getBacktraceAccess() {
    if(ourBacktraceField == null) {
      return null;
    }

    return t -> {
      try {
        return ourBacktraceField.get(t);
      }
      catch (IllegalAccessException e) {
        LOG.warn(e);
        return null;
      }
    };
  }
}
