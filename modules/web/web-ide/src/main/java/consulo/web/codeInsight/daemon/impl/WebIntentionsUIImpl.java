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

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.IntentionsUI;
import consulo.ide.impl.idea.codeInsight.intention.impl.CachedIntentions;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17/08/2021
 */
@ServiceImpl
@Singleton
public class WebIntentionsUIImpl extends IntentionsUI {
  @Inject
  public WebIntentionsUIImpl(Project project) {
    super(project);
  }

  @Override
  public Object getLastIntentionHint() {
    return null;
  }

  @Override
  public void update(@Nonnull CachedIntentions cachedIntentions, boolean actionsChanged) {

  }

  @Override
  public void hide() {

  }
}
