/*
 * Copyright 2013-2019 consulo.io
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
package consulo.disposer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Traces creation and disposal by storing corresponding stacktraces.
 * In constructor it saves creation stacktrace
 * In kill() it saves disposal stacktrace
 */
public interface TraceableDisposable {
  static TraceableDisposable newTraceDisposable(boolean debug) {
    return Disposer.newTraceDisposable(debug);
  }

  void kill(@Nullable String msg);

  void killExceptionally(@Nonnull Throwable throwable);

  void throwObjectNotDisposedError(@Nonnull final String msg);

  void throwDisposalError(String msg) throws RuntimeException;

  String getStackTrace();
}
