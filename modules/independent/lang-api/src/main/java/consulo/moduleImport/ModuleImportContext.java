/*
 * Copyright 2013-2017 consulo.io
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
package consulo.moduleImport;

import com.intellij.openapi.Disposable;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public class ModuleImportContext implements Disposable {
  private String myFileToImport;
  private boolean myUpdate;

  public ModuleImportContext setFileToImport(String fileToImport) {
    myFileToImport = fileToImport;
    return this;
  }

  public String getFileToImport() {
    return myFileToImport;
  }

  public boolean isUpdate() {
    return myUpdate;
  }

  public void setUpdate(final boolean update) {
    myUpdate = update;
  }

  public boolean isOpenProjectSettingsAfter() {
    return false;
  }

  @Override
  public void dispose() {
  }
}
