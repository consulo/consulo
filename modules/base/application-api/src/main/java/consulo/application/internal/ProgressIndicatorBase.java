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
package consulo.application.internal;

import consulo.application.progress.StandardProgressIndicator;

public class ProgressIndicatorBase extends AbstractProgressIndicatorExBase implements StandardProgressIndicator {
  public ProgressIndicatorBase() {
    this(false);
  }

  public ProgressIndicatorBase(boolean reusable) {
    super(reusable);
  }

  public ProgressIndicatorBase(boolean reusable, boolean allowSystemActivity) {
    super(reusable);
    if (!allowSystemActivity) dontStartActivity();
  }

  @Override
  public final void cancel() {
    super.cancel();
  }

  @Override
  public final boolean isCanceled() {
    return super.isCanceled();
  }
}
