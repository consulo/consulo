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
package consulo.web.gwt.shared.transport;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * @author VISTALL
 * @since 18-May-16
 */
public class GwtProjectInfo implements IsSerializable{
  private String myProjectName;
  private GwtVirtualFile myBaseDirectory;
  private List<String> myModuleDirectoryUrls;

  public GwtProjectInfo(String projectName, GwtVirtualFile baseDirectory, List<String> moduleDirectoryUrls) {
    myProjectName = projectName;
    myBaseDirectory = baseDirectory;
    myModuleDirectoryUrls = moduleDirectoryUrls;
  }

  public GwtProjectInfo() {
  }

  public String getProjectName() {
    return myProjectName;
  }

  public GwtVirtualFile getBaseDirectory() {
    return myBaseDirectory;
  }

  public List<String> getModuleDirectoryUrls() {
    return myModuleDirectoryUrls;
  }
}
