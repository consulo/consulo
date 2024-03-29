/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.change;

import consulo.versionControlSystem.AbstractVcsHelper;

import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * CommitResultHandler may be passed to {@link AbstractVcsHelper#commitChanges(Collection, LocalChangeList, String, CommitResultHandler)}.
 * It is called after commit is performed: successful or failed.
 *
 * @author Kirill Likhodedov
 */
public interface CommitResultHandler {
  void onSuccess(@Nonnull String commitMessage);

  void onFailure();
}
