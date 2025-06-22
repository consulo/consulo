/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInspection.export;

import consulo.application.Application;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 * @since 2002-01-21
 */
public class HTMLExportFrameMaker {
  private final String myRootFolder;
  private final Project myProject;
  private final List<InspectionToolWrapper> myInspectionToolWrappers = new ArrayList<>();

  public HTMLExportFrameMaker(String rootFolder, Project project) {
    myRootFolder = rootFolder;
    myProject = project;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public void start() {
    StringBuffer buf = new StringBuffer();
    buf.append("<HTML><BODY></BODY></HTML>");
    HTMLExportUtil.writeFile(myRootFolder, "empty.html", buf, myProject);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public void done() {
    StringBuffer buf = new StringBuffer();

    for (InspectionToolWrapper toolWrapper : myInspectionToolWrappers) {
      buf.append("<A HREF=\"");
      buf.append(toolWrapper.getFolderName());
      buf.append("-index.html\">");
      buf.append(toolWrapper.getDisplayName());
      buf.append("</A><BR>");
    }

    HTMLExportUtil.writeFile(myRootFolder, "index.html", buf, myProject);
  }

  public void startInspection(@Nonnull InspectionToolWrapper toolWrapper) {
    myInspectionToolWrappers.add(toolWrapper);
    @NonNls StringBuffer buf = new StringBuffer();
    buf.append("<HTML><HEAD><TITLE>");
    buf.append(Application.get().getName());
    buf.append(InspectionLocalize.inspectionExportTitle());
    buf.append("</TITLE></HEAD>");
    buf.append("<FRAMESET cols=\"30%,70%\"><FRAMESET rows=\"30%,70%\">");
    buf.append("<FRAME src=\"");
    buf.append(toolWrapper.getFolderName());
    buf.append("/index.html\" name=\"inspectionFrame\">");
    buf.append("<FRAME src=\"empty.html\" name=\"packageFrame\">");
    buf.append("</FRAMESET>");
    buf.append("<FRAME src=\"empty.html\" name=\"elementFrame\">");
    buf.append("</FRAMESET></BODY></HTML");

    HTMLExportUtil.writeFile(myRootFolder, toolWrapper.getFolderName() + "-index.html", buf, myProject);
  }

  public String getRootFolder() {
    return myRootFolder;
  }
}
