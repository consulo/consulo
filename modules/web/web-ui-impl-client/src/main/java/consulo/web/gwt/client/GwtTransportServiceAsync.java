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
package consulo.web.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import consulo.annotations.DeprecationInfo;
import consulo.web.gwt.shared.transport.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public interface GwtTransportServiceAsync {

  void getApplicationStatus(AsyncCallback<Boolean> async);

  void getProjectInfo(String path, AsyncCallback<GwtProjectInfo> async);

  @Nonnull
  void listChildren(String fileUrl, AsyncCallback<List<GwtVirtualFile>> async);

  void findFileByUrl(String fileUrl, AsyncCallback<GwtVirtualFile> async);

  @javax.annotation.Nullable
  void getContent(String fileUrl, AsyncCallback<String> async);

  @Nonnull
  void getLexerHighlight(String fileUrl, AsyncCallback<List<GwtHighlightInfo>> async);

  @Nonnull
  void runHighlightPasses(String fileUrl, int offset, AsyncCallback<List<GwtHighlightInfo>> async);

  @Nullable
  void getNavigationInfo(String fileUrl, int offset, AsyncCallback<GwtNavigateInfo> async);

  @Nonnull
  void serviceEditorColorScheme(String name, String[] colorKeys, String[] attributes, AsyncCallback<GwtEditorColorScheme> async);

  @Nonnull
  void serviceEditorColorSchemeList(AsyncCallback<List<String>> async);
}
