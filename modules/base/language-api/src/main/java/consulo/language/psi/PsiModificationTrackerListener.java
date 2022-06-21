/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Topic;
import consulo.annotation.component.TopicBroadcastDirection;

/**
 * A listener to be notified on any PSI modification count change (which happens on any physical PSI change).
 */
@Topic(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface PsiModificationTrackerListener {

  /**
   * A method invoked on Swing EventDispatchThread each time any physical PSI change is detected
   */
  void modificationCountChanged();
}
