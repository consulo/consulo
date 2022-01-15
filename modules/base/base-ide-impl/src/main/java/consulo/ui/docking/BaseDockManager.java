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
package consulo.ui.docking;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.MutualMap;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockContainerFactory;
import com.intellij.ui.docking.DockManager;
import com.intellij.ui.docking.DockableContent;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.util.TraverseUtil;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 2020-11-21
 * <p>
 * Internal - indendent impl
 */
public abstract class BaseDockManager extends DockManager implements PersistentStateComponent<Element> {
  public interface DockWindow {
    void show();

    String getId();
  }

  protected final Map<String, DockContainerFactory> myFactories = new HashMap<>();
  protected final MutualMap<DockContainer, DockWindow> myWindows = new MutualMap<>();
  protected final Set<DockContainer> myContainers = new HashSet<>();
  protected final Project myProject;
  private Element myLoadedState;
  protected int myWindowIdCounter = 1;

  protected BaseDockManager(Project project) {
    myProject = project;
  }

  @Override
  public void register(final DockContainer container) {
    myContainers.add(container);
    Disposer.register(container, () -> myContainers.remove(container));
  }

  @Override
  public void register(final String id, DockContainerFactory factory) {
    myFactories.put(id, factory);
    Disposer.register(factory, () -> myFactories.remove(id));

    readStateFor(id);
  }

  @Nullable
  @Override
  public DockContainer getContainerFor(Component c) {
    if (c == null) return null;

    for (DockContainer eachContainer : myContainers) {
      if (TraverseUtil.isDescendingFrom(c, eachContainer.getUIContainerComponent())) {
        return eachContainer;
      }
    }

    Component parent = TraverseUtil.findUltimateParent(c);
    if (parent == null) return null;

    for (DockContainer eachContainer : myContainers) {
      if (parent == TraverseUtil.findUltimateParent(eachContainer.getUIContainerComponent())) {
        return eachContainer;
      }
    }

    return null;
  }

  public void readState() {
    for (String id : myFactories.keySet()) {
      readStateFor(id);
    }
  }

  private void readStateFor(String type) {
    if (myLoadedState == null) return;

    List<Element> windows = myLoadedState.getChildren("window");
    for (Element eachWindow : windows) {
      String eachId = eachWindow.getAttributeValue("id");

      Element eachContent = eachWindow.getChild("content");
      if (eachContent == null) continue;

      String eachType = eachContent.getAttributeValue("type");
      if (eachType == null || !type.equals(eachType) || !myFactories.containsKey(eachType)) continue;

      DockContainerFactory factory = myFactories.get(eachType);
      if (!(factory instanceof DockContainerFactory.Persistent)) continue;

      DockContainerFactory.Persistent persistentFactory = (DockContainerFactory.Persistent)factory;
      DockContainer container = persistentFactory.loadContainerFrom(eachContent);
      register(container);

      final DockWindow window = createWindowFor(eachId, container);
      myProject.getApplication().getLastUIAccess().giveIfNeed(window::show);
    }
  }

  protected abstract DockWindow createWindowFor(@Nullable String id, DockContainer container);

  @Override
  public void loadState(Element state) {
    myLoadedState = state;
  }

  @Override
  public Element getState() {
    Element root = new Element("DockManager");
    for (DockContainer each : myContainers) {
      DockWindow eachWindow = myWindows.getValue(each);
      if (eachWindow != null) {
        if (each instanceof DockContainer.Persistent) {
          DockContainer.Persistent eachContainer = (DockContainer.Persistent)each;
          Element eachWindowElement = new Element("window");
          eachWindowElement.setAttribute("id", eachWindow.getId());
          Element content = new Element("content");
          content.setAttribute("type", eachContainer.getDockContainerType());
          content.addContent(eachContainer.getState());
          eachWindowElement.addContent(content);

          root.addContent(eachWindowElement);
        }
      }
    }
    return root;
  }

  @Override
  public Set<DockContainer> getContainers() {
    return Collections.unmodifiableSet(ContainerUtil.newHashSet(myContainers));
  }

  public void createNewDockContainerFor(DockableContent content, RelativePoint point) {
    throw new UnsupportedOperationException("desktop impl only");
  }

  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> createNewDockContainerFor(@Nonnull VirtualFile file, @Nonnull FileEditorManagerImpl fileEditorManager) {
    throw new UnsupportedOperationException("desktop impl only");
  }
}
