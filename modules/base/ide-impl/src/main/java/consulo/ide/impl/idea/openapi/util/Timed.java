/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.util;

public class Timed<T> {
  private final T myT;
  private final long myTime;

  public Timed(final T t, final long time) {
    myT = t;
    myTime = time;
  }

  public Timed(final T t) {
    myT = t;
    myTime = System.currentTimeMillis();
  }

  public T getT() {
    return myT;
  }

  public long getTime() {
    return myTime;
  }
}
