/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.actions.webSearch;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.ObjectUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2019-02-20
 */
@Singleton
@State(name = "WebSearchOptions", storages = @Storage("web.search.xml"))
public class WebSearchOptions implements PersistentStateComponent<WebSearchOptions> {
  private WebSearchEngine myEngine = WebSearchEngine.GOOGLE;

  @Nonnull
  public WebSearchEngine getEngine() {
    return myEngine;
  }

  public void setEngine(@Nonnull WebSearchEngine engine) {
    myEngine = engine;
  }

  @Nullable
  @Override
  public WebSearchOptions getState() {
    return this;
  }

  @Override
  public void loadState(WebSearchOptions state) {
    myEngine = ObjectUtil.notNull(state.myEngine, WebSearchEngine.GOOGLE);
  }
}
