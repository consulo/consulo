/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.language.codeStyle.arrangement.match.ArrangementMatchRule;

import jakarta.annotation.Nonnull;
import java.util.List;

/**
 * Stands for the {@link ArrangementSettings} which also provide arrangement rules sorted in order of entries matching.
 * <p/>
 * Example: 'public static' rule would have higher priority then 'public'
 *
 * @author Svetlana.Zemlyanskaya
 */
public interface RulePriorityAwareSettings extends ArrangementSettings {

  /**
   * <b>Note:</b> It's expected that rules sort is stable
   * @return list of rules sorted in order of matching
   */
  @Nonnull
  List<? extends ArrangementMatchRule> getRulesSortedByPriority();
}
