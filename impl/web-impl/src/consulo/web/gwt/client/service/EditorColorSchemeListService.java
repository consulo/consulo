/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.client.service;

import consulo.web.gwt.client.util.GwtUtil;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class EditorColorSchemeListService implements FetchService {
  public static final String KEY = "EditorColorSchemeListService";

  private List<String> mySchemes = Arrays.asList("Default");

  @Override
  public void fetch(Runnable onError, Runnable onOk) {
    GwtUtil.rpc().serviceEditorColorSchemeList(new ServiceCallback<List<String>>(onError, onOk) {
      @Override
      public void handle(List<String> result) {
        mySchemes = result;
      }
    });
  }

  public List<String> getSchemes() {
    return mySchemes;
  }

  @Override
  public String getKey() {
    return KEY;
  }
}
