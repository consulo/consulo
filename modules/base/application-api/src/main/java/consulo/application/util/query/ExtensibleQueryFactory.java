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

package consulo.application.util.query;

import consulo.application.Application;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author yole
 */
public class ExtensibleQueryFactory<Result, Parameters> extends QueryFactory<Result, Parameters> {
  private final Class<? extends QueryExecutor<Result, Parameters>> myExtensionClass;

  protected ExtensibleQueryFactory(Class<? extends QueryExecutor<Result, Parameters>> extensionClass) {
    myExtensionClass = extensionClass;
  }

  @Override
  public void registerExecutor(@Nonnull final QueryExecutor<Result, Parameters> queryExecutor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unregisterExecutor(@Nonnull final QueryExecutor<Result, Parameters> queryExecutor) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  protected List<? extends QueryExecutor<Result, Parameters>> getExecutors() {
    return Application.get().getExtensionList(myExtensionClass);
  }

  @Override
  public boolean hasAnyExecutors() {
    return Application.get().getExtensionPoint(myExtensionClass).hasAnyExtensions();
  }
}