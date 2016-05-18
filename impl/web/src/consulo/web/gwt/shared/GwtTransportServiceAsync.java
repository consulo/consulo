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

import com.google.gwt.user.client.rpc.AsyncCallback;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtVirtualFile;

import java.util.List;

public interface GwtTransportServiceAsync {

  void getProjectDirectory(AsyncCallback<GwtVirtualFile> async);

  void getContent(String fileUrl, AsyncCallback<String> async);

  void getLexerHighlight(String fileUrl, AsyncCallback<List<GwtHighlightInfo>> async);

  void runHighlightPasses(String fileUrl, int offset, AsyncCallback<List<GwtHighlightInfo>> async);
}
