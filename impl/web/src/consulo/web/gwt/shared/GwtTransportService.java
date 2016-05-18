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
import consulo.web.gwt.client.transport.GwtVirtualFile;
import consulo.web.gwt.client.transport.GwtHighlightInfo;

import java.util.List;

/**
 * @author VISTALL
 * @since 15-May-16
 */
@RemoteServiceRelativePath("transport")
public interface GwtTransportService extends RemoteService {
  GwtVirtualFile getProjectDirectory();

  String getContent(String fileUrl);

  List<GwtHighlightInfo> getLexerHighlight(String fileUrl);

  List<GwtHighlightInfo> runHighlightPasses(String fileUrl);
}
