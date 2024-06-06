/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.data;

import consulo.util.lang.function.Condition;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.log.CommitId;
import jakarta.annotation.Nonnull;

public class CommitIdByStringCondition implements Condition<CommitId> {
  @Nonnull
  private final String myHashString;

  public CommitIdByStringCondition(@Nonnull String hashString) {
    myHashString = hashString;
  }

  @Override
  public boolean value(CommitId commitId) {
    return StringUtil.startsWithIgnoreCase(commitId.getHash().asString(), myHashString);
  }
}
