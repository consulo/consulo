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
package consulo.ide.impl.idea.lang;

import consulo.document.util.TextRange;
import consulo.util.lang.Couple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Commenter that knows how to uncomment commented block
 *
 * @author Ilya.Kazakevich
 */
public interface CustomUncommenter {

  /**
   * Finds commented block in provided text.
   *
   * @param text text to search comment for.
   * @return commented block (including comment prefix and suffix!) or null if text does not contain any  commented blocks.
   */
  @Nullable
  TextRange findMaximumCommentedRange(@Nonnull CharSequence text);


  /**
   * Returns couples each pointing to comment prefix and suffiix:
   * [commentPrefix-start,commentPrefix-end] -- [commentSuffix-start,commentSuffix-end].
   * If block has several commented areas you may provide all of them.
   *
   * @param text text with comments
   * @return list of couples: [commentPrefix-start,commentPrefix-end], [commentSuffix-start,commentSuffix-end]
   */
  @Nonnull
  Collection<? extends Couple<TextRange>> getCommentRangesToDelete(@Nonnull CharSequence text);
}
