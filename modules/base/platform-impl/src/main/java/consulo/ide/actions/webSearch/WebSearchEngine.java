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

/**
 * @author VISTALL
 * @since 2019-02-20
 */
public enum WebSearchEngine {
  GOOGLE("Google", "https://www.google.com/search?q={0}"),
  YANDEX("Yandex", "https://yandex.ru/search/?text={0}"),
  BING("Bing", "https://www.bing.com/search?q={0}"),
  DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q={0}");

  private String myPresentableName;
  private String myUrlTemplate;

  WebSearchEngine(String presentableName, String urlTemplate) {
    myPresentableName = presentableName;
    myUrlTemplate = urlTemplate;
  }

  public String getPresentableName() {
    return myPresentableName;
  }

  public String getUrlTemplate() {
    return myUrlTemplate;
  }
}
