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
package consulo.web.gwt.server;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.DeprecationInfo;
import consulo.web.gwt.shared.transport.GwtVirtualFile;

/**
 * @author VISTALL
 * @since 18-May-16
 */
@Deprecated
@DeprecationInfo("This is part of research 'consulo as web app'. Code was written in hacky style. Must be dropped, or replaced by Consulo UI API")
public class GwtVirtualFileUtil {
  public static GwtVirtualFile createVirtualFile(final Project project, final VirtualFile virtualFile) {
    final GwtVirtualFile gwtVirtualFile = new GwtVirtualFile();
    gwtVirtualFile.url = virtualFile.getUrl();
    gwtVirtualFile.name = virtualFile.getName();
    gwtVirtualFile.isDirectory = virtualFile.isDirectory();


    return gwtVirtualFile;
  }
}
