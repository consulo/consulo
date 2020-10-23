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
package consulo.test.impl.ui;

import consulo.ui.UIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2020-08-24
 */
public class TestUIAccess implements UIAccess {
  public static final TestUIAccess INSTANCE = new TestUIAccess();

  private ThreadLocal<Boolean> myInsideUI = ThreadLocal.withInitial(() -> Boolean.TRUE);

  private Executor myExecutor = Executors.newSingleThreadExecutor();

  @Override
  public boolean isValid() {
    return true;
  }

  @Nonnull
  @Override
  public <T> AsyncResult<T> give(@Nonnull Supplier<T> supplier) {
    AsyncResult<T> result = AsyncResult.undefined();

    myExecutor.execute(() -> {
      try {
        myInsideUI.set(true);
        result.setDone(supplier.get());
      }
      finally {
        myInsideUI.set(false);
      }
    });
    return result;
  }

  public boolean insideUI() {
    return myInsideUI.get();
  }
}
