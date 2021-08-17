/*
 * Copyright 2013-2021 consulo.io
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
package consulo.web.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.impl.IntentionsUI;
import com.intellij.codeInsight.intention.impl.CachedIntentions;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
public class WebIntentionsUIImpl extends IntentionsUI {
  public WebIntentionsUIImpl(Project project) {
    super(project);
  }

  @Override
  public void update(@Nonnull CachedIntentions cachedIntentions, boolean actionsChanged) {

  }

  @Override
  public void hide() {

  }
}
