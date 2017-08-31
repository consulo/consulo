/*
 * Copyright 2013-2016 consulo.io
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

import consulo.web.gwt.client.util.AsyncCallbackAdapter;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.shared.transport.GwtEditorColorScheme;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class EditorColorSchemeService implements FetchService {
  public interface Listener {
    void schemeChanged(GwtEditorColorScheme scheme);
  }

  public static final String KEY = "EditorColorSchemeService";

  private List<Listener> myListeners = new ArrayList<Listener>();
  private GwtEditorColorScheme myScheme;

  public GwtEditorColorScheme getScheme() {
    return myScheme;
  }

  public void setScheme(String scheme) {
    AsyncCallbackAdapter<GwtEditorColorScheme> callback = new AsyncCallbackAdapter<GwtEditorColorScheme>() {
      @Override
      public void onSuccess(GwtEditorColorScheme result) {
        myScheme = result;
        for (Listener listener : myListeners) {
          listener.schemeChanged(myScheme);
        }
      }
    };

    GwtUtil.rpc().serviceEditorColorScheme(scheme, GwtEditorColorScheme.fetchColors, GwtEditorColorScheme.fetchAttributes, callback);
  }

  public void addListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeListener(Listener listener) {
    myListeners.add(listener);
  }

  @Override
  public void fetch(Runnable onError, Runnable onOk) {
    GwtUtil.rpc().serviceEditorColorScheme("Default", GwtEditorColorScheme.fetchColors, GwtEditorColorScheme.fetchAttributes,
                                           new ServiceCallback<GwtEditorColorScheme>(onError, onOk) {
                                             @Override
                                             public void handle(GwtEditorColorScheme result) {
                                               myScheme = result;
                                             }
                                           });
  }

  @Override
  public String getKey() {
    return KEY;
  }
}
