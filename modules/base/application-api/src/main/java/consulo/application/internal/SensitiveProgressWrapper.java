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

import consulo.application.progress.ProgressIndicator;
import jakarta.annotation.Nonnull;

/**
 * Progress indicator wrapper which reacts to its own cancellation in addition to the cancellation of its wrappee.
 */
public class SensitiveProgressWrapper extends ProgressWrapper {
  public SensitiveProgressWrapper(@Nonnull ProgressIndicator indicator) {
    super(indicator, true);
  }
}
