/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.ui.docking.impl;

import consulo.fileEditor.impl.internal.DockableEditorContainerFactory;
import consulo.fileEditor.DockableEditorTabbedContainer;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditorsSplitters;
import consulo.ide.impl.fileEditor.UnifiedFileEditorsSplitters;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.ui.Component;
import consulo.ui.ex.RelativePoint;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class UnifiedDockableEditorTabbedContainer implements DockableEditorTabbedContainer {
  private final Project myProject;
  private final UnifiedFileEditorsSplitters mySplitters;

  public UnifiedDockableEditorTabbedContainer(Project project, UnifiedFileEditorsSplitters splitters, boolean disposeWhenEmpty) {
    myProject = project;

    mySplitters = splitters;
  }

  @Override
  public @Nullable FileEditorsSplitters getSplitters() {
    return mySplitters;
  }

  
  @Override
  public Component getUIContainerComponent() {
    return mySplitters.getUIComponent();
  }

  @Override
  public String getDockContainerType() {
    return DockableEditorContainerFactory.TYPE;
  }

  @Override
  public Element getState() {
    return null;
  }

  
  @Override
  public ContentResponse getContentResponse(DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public void add(DockableContent content, RelativePoint dropTarget) {

  }

  @Override
  public void closeAll() {

  }

  @Override
  public void addListener(Listener listener, Disposable parent) {

  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public @Nullable Image startDropOver(DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public @Nullable Image processDropOver(DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public void resetDropOver(DockableContent content) {

  }

  @Override
  public boolean isDisposeWhenEmpty() {
    return false;
  }

  @Override
  public void dispose() {

  }
}
