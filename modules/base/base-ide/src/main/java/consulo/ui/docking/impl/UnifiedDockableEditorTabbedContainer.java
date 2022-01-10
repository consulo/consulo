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
package consulo.ui.docking.impl;

import com.intellij.openapi.fileEditor.impl.DockableEditorContainerFactory;
import com.intellij.openapi.fileEditor.impl.DockableEditorTabbedContainer;
import com.intellij.openapi.project.Project;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.docking.DockableContent;
import consulo.disposer.Disposable;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.ui.Component;
import consulo.fileEditor.impl.UnifiedEditorsSplitters;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class UnifiedDockableEditorTabbedContainer implements DockableEditorTabbedContainer {
  private final Project myProject;
  private final UnifiedEditorsSplitters mySplitters;

  public UnifiedDockableEditorTabbedContainer(Project project, UnifiedEditorsSplitters splitters, boolean disposeWhenEmpty) {
    myProject = project;

    mySplitters = splitters;
  }

  @Nullable
  @Override
  public EditorsSplitters getSplitters() {
    return mySplitters;
  }

  @Nonnull
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
  public RelativeRectangle getAcceptArea() {
    return null;
  }

  @Override
  public RelativeRectangle getAcceptAreaFallback() {
    return null;
  }

  @Nonnull
  @Override
  public ContentResponse getContentResponse(@Nonnull DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public void add(@Nonnull DockableContent content, RelativePoint dropTarget) {

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

  @Nullable
  @Override
  public Image startDropOver(@Nonnull DockableContent content, RelativePoint point) {
    return null;
  }

  @Nullable
  @Override
  public Image processDropOver(@Nonnull DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public void resetDropOver(@Nonnull DockableContent content) {

  }

  @Override
  public boolean isDisposeWhenEmpty() {
    return false;
  }

  @Override
  public void dispose() {

  }
}
