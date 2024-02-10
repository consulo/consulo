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
package consulo.language.codeStyle.arrangement;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The whole arrangement idea is to allow to change file entries order according to the user-provided rules.
 * <p/>
 * That means that we can re-use the same mechanism during, say, new members generation - arrangement rules can be used to
 * determine position where a new element should be inserted.
 * <p/>
 * This service provides utility methods for that.
 *
 * @author Denis Zhdanov
 * @since 9/4/12 11:12 AM
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface MemberOrderService {

  /**
   * Tries to find an element at the given context which should be the previous sibling for the given 'member'element according to the
   * {@link CommonCodeStyleSettings#getArrangementSettings() user-defined arrangement rules}.
   * <p/>
   * E.g. the IDE might generate given 'member' element and wants to know element after which it should be inserted
   *
   * @param member   target member which anchor should be calculated
   * @param settings code style settings to use
   * @param context  given member's context
   * @return given member's anchor if the one can be computed;
   * given 'context' element if given member should be the first child
   * <code>null</code> otherwise
   */
  @Nullable
  @RequiredReadAction
  PsiElement getAnchor(@Nonnull PsiElement member, @Nonnull CommonCodeStyleSettings settings, @Nonnull PsiElement context);
}
