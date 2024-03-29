/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.push;

import jakarta.annotation.Nonnull;

/**
 * For a single repository, specifies what is pushed and where.
 */
public class PushSpec<S extends PushSource, T extends PushTarget> {

  @Nonnull
  private S mySource;
  @Nonnull
  private T myTarget;

  public PushSpec(@Nonnull S source, @Nonnull T target) {
    mySource = source;
    myTarget = target;
  }

  @Nonnull
  public S getSource() {
    return mySource;
  }

  @Nonnull
  public T getTarget() {
    return myTarget;
  }

  @Override
  public String toString() {
    return mySource + "->" + myTarget;
  }
}
