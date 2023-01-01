/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.execution.test.export;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.util.xml.serializer.annotation.Attribute;
import jakarta.inject.Singleton;

@Singleton
@State(name = "ExportTestResults", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ExportTestResultsConfiguration implements PersistentStateComponent<ExportTestResultsConfiguration.State> {

  public enum ExportFormat {
    Xml("xml"),
    BundledTemplate("html"),
    UserTemplate("html");

    private final String myExtension;

    ExportFormat(String extension) {
      myExtension = extension;
    }

    public String getDefaultExtension() {
      return myExtension;
    }
  }

  public static class State {

    @Attribute("outputFolder")
    public String outputFolder;

    @Attribute("openResultsInEditor")
    public boolean openResultsInEditor;

    @Attribute("userTempatePath")
    public String userTemplatePath;

    private ExportFormat myExportFormat = ExportFormat.BundledTemplate;

    @Attribute("exportFormat")
    public String getExportFormat() {
      return myExportFormat.name();
    }

    public void setExportFormat(String exportFormat) {
      try {
        myExportFormat = ExportFormat.valueOf(exportFormat);
      }
      catch (IllegalArgumentException e) {
        myExportFormat = ExportFormat.BundledTemplate;
      }
    }
  }

  private State myState = new State();

  public static ExportTestResultsConfiguration getInstance(Project project) {
    return project.getInstance(ExportTestResultsConfiguration.class);
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  public String getOutputFolder() {
    return myState.outputFolder;
  }

  public void setOutputFolder(String outputFolder) {
    myState.outputFolder = outputFolder;
  }

  public boolean isOpenResults() {
    return myState.openResultsInEditor;
  }

  public void setOpenResults(boolean openResultsInEditor) {
    myState.openResultsInEditor = openResultsInEditor;
  }

  public ExportFormat getExportFormat() {
    return myState.myExportFormat;
  }

  public void setExportFormat(ExportFormat exportFormat) {
    myState.myExportFormat = exportFormat;
  }

  public String getUserTemplatePath() {
    return myState.userTemplatePath;
  }

  public void setUserTemplatePath(String userTemplatePath) {
    myState.userTemplatePath = userTemplatePath;
  }

}
