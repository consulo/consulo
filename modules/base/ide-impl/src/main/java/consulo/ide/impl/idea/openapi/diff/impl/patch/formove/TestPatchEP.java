/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.patch.formove;

import consulo.ide.impl.idea.openapi.diff.impl.patch.PatchEP;
import consulo.versionControlSystem.change.CommitContext;
import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 * @since 2011-07-12
 */
public class TestPatchEP implements PatchEP {
  private final static String ourName = "consulo.ide.impl.idea.openapi.diff.impl.patch.formove.TestPatchEP";
  private final static String ourContent = "ourContent\nseveral\nlines\twith\u0142\u0001 different symbols";

  @Nonnull
  @Override
  public String getName() {
    return ourName;
  }

  @Override
  public CharSequence provideContent(@Nonnull String path, CommitContext commitContext) {
    return ourContent + path;
  }

  @Override
  public void consumeContent(@Nonnull String path, @Nonnull CharSequence content, CommitContext commitContext) {
    assert (ourContent + path).equals(content.toString());
  }

  @Override
  public void consumeContentBeforePatchApplied(@Nonnull String path,
                                               @Nonnull CharSequence content,
                                               CommitContext commitContext) {
  }
}
