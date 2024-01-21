/*
 * Copyright 2013-2023 consulo.io
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
package consulo.dataContext;

import consulo.application.Application;
import consulo.util.concurrent.ThreadIssueException;
import consulo.util.dataholder.Key;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2023-12-09
 */
public class DataProtector {
  public static <T> T dataUnderRead(Application application, Key<T> key, Supplier<T> dataGetter) {
    if (application.isReadAccessAllowed()) {
      return dataGetter.get();
    }

    throw new ThreadIssueException("Data " + key + " require read acces. Use async version DataProvider#getDataAsync");
  }
}
