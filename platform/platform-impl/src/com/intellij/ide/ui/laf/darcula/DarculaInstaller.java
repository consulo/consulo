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
package com.intellij.ide.ui.laf.darcula;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaInstaller {

  public static void update(boolean old) {
    boolean underDarkBuildInLaf = UIUtil.isUnderDarkBuildInLaf();
    JBColor.setDark(underDarkBuildInLaf);
    IconLoader.setUseDarkIcons(underDarkBuildInLaf);
    if(old != underDarkBuildInLaf) {
      String name = underDarkBuildInLaf ? "Darcula" : EditorColorsScheme.DEFAULT_SCHEME_NAME;
      if (name.equals(EditorColorsManager.getInstance().getGlobalScheme().getName())) {
        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getScheme(EditorColorsScheme.DEFAULT_SCHEME_NAME);
        if (scheme != null) {
          EditorColorsManager.getInstance().setGlobalScheme(scheme);
        }
      }
    }

    UISettings.getInstance().fireUISettingsChanged();
    EditorFactory.getInstance().refreshAllEditors();

    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    for (Project openProject : openProjects) {
      FileStatusManager.getInstance(openProject).fileStatusesChanged();
      DaemonCodeAnalyzer.getInstance(openProject).restart();
    }
    for (IdeFrame frame : WindowManagerEx.getInstanceEx().getAllProjectFrames()) {
      if (frame instanceof IdeFrameImpl) {
        ((IdeFrameImpl)frame).updateView();
      }
    }
    //Editor[] editors = EditorFactory.getInstance().getAllEditors();
    //for (Editor editor : editors) {
    //  ((EditorEx)editor).reinitSettings();
    //}
    ActionToolbarImpl.updateAllToolbarsImmediately();
  }
}
