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
package consulo.web.gwt.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import consulo.web.gwt.shared.transport.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 15-May-16
 */
@RemoteServiceRelativePath("transport")
public interface GwtTransportService extends RemoteService {
  GwtProjectInfo getProjectInfo(String path);

  GwtVirtualFile findFileByUrl(String fileUrl);

  @Nullable
  String getContent(String fileUrl);

  @NotNull
  List<GwtHighlightInfo> getLexerHighlight(String fileUrl);

  @NotNull
  List<GwtHighlightInfo> runHighlightPasses(String fileUrl, int offset);

  @Nullable
  GwtNavigateInfo getNavigationInfo(String fileUrl, int offset);

  @NotNull
  GwtEditorColorScheme serviceEditorColorScheme(String[] colorKeys, String[] attributes);

  //String getQuickDocInfo(String fileUrl, int offset);
}
