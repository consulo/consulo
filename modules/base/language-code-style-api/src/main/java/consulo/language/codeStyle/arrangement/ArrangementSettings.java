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

import consulo.language.codeStyle.arrangement.group.ArrangementGroupingRule;
import consulo.language.codeStyle.arrangement.match.ArrangementMatchRule;
import consulo.language.codeStyle.arrangement.match.ArrangementSectionRule;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Holds arrangement rules.
 * <p/>
 * Implementations of this interface are expected to provide correct {@link #equals(Object)} & {@link #hashCode()} implementations.
 *
 * @author Denis Zhdanov
 * @since 9/17/12 11:51 AM
 */
public interface ArrangementSettings extends Cloneable {

  @Nonnull
  List<ArrangementGroupingRule> getGroupings();

  @Nonnull
  List<ArrangementSectionRule> getSections();

  /**
   * @deprecated collect match rules from {@link #getSections()}
   * @return
   */
  @Nonnull
  List<? extends ArrangementMatchRule> getRules();

  /**
   * <b>Note:</b> It's expected that rules sort is stable
   * <p/>
   * Example: 'public static' rule would have higher priority then 'public'
   * @return list of rules sorted in order of matching
   */
  @Nonnull
  List<? extends ArrangementMatchRule> getRulesSortedByPriority();

  @Nonnull
  ArrangementSettings clone();
}
