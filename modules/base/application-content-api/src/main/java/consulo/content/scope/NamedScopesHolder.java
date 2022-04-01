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
package consulo.content.scope;

import consulo.component.persist.PersistentStateComponent;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class NamedScopesHolder implements PersistentStateComponent<Element> {
  private List<NamedScope> myScopes = new ArrayList<>();
  private static final String SCOPE_TAG = "scope";
  private static final String NAME_ATT = "name";
  private static final String PATTERN_ATT = "pattern";

  protected final Project myProject;
  private VirtualFile myProjectBaseDir;

  public NamedScopesHolder(@Nonnull Project project) {
    myProject = project;
  }

  public abstract String getDisplayName();

  public abstract Image getIcon();

  public interface ScopeListener {

    void scopesChanged();
  }

  public VirtualFile getProjectBaseDir() {
    if (myProjectBaseDir == null) {
      myProjectBaseDir = myProject.getBaseDir();
    }
    return myProjectBaseDir;
  }

  private final List<ScopeListener> myScopeListeners = Lists.newLockFreeCopyOnWriteList();


  /**
   * @deprecated use {@link #addScopeListener(ScopeListener, Disposable)} instead
   */
  @Deprecated
  public void addScopeListener(@Nonnull ScopeListener scopeListener) {
    myScopeListeners.add(scopeListener);
  }

  public void addScopeListener(@Nonnull ScopeListener scopeListener, @Nonnull Disposable parentDisposable) {
    myScopeListeners.add(scopeListener);
    Disposer.register(parentDisposable, () -> myScopeListeners.remove(scopeListener));
  }

  @Deprecated
  public void removeScopeListener(@Nonnull ScopeListener scopeListener) {
    myScopeListeners.remove(scopeListener);
  }

  public void fireScopeListeners() {
    for (ScopeListener listener : myScopeListeners) {
      listener.scopesChanged();
    }
  }

  @Nonnull
  public NamedScope[] getScopes() {
    final List<NamedScope> scopes = new ArrayList<>();
    List<NamedScope> list = getPredefinedScopes();
    scopes.addAll(list);
    scopes.addAll(myScopes);
    return scopes.toArray(new NamedScope[scopes.size()]);
  }

  public NamedScope[] getEditableScopes() {
    return myScopes.toArray(new NamedScope[myScopes.size()]);
  }

  public void removeAllSets() {
    myScopes.clear();
    fireScopeListeners();
  }

  public void setScopes(NamedScope[] scopes) {
    myScopes = new ArrayList<>(Arrays.asList(scopes));
    fireScopeListeners();
  }

  public void addScope(NamedScope scope) {
    myScopes.add(scope);
    fireScopeListeners();
  }

  @Nullable
  public static NamedScope getScope(@Nonnull Project project, final String scopeId) {
    final NamedScopesHolder[] holders = getAllNamedScopeHolders(project);
    for (NamedScopesHolder holder : holders) {
      final NamedScope scope = holder.getScope(scopeId);
      if (scope != null) {
        return scope;
      }
    }
    return null;
  }

  @Nullable
  public static Pair<NamedScopesHolder, NamedScope> getScopeWithHolder(@Nonnull Project project, final String scopeId) {
    final NamedScopesHolder[] holders = getAllNamedScopeHolders(project);
    for (NamedScopesHolder holder : holders) {
      final NamedScope scope = holder.getScope(scopeId);
      if (scope != null) {
        return Pair.create(holder, scope);
      }
    }
    return null;
  }

  @Nonnull
  public static NamedScopesHolder[] getAllNamedScopeHolders(@Nonnull Project project) {
    List<NamedScopesHolder> holders = new ArrayList<>();
    NamedScopesHolderProvider.EP.forEachExtensionSafe(project, namedScopesHolderProvider -> {
      NamedScopesHolder scopesHolder = namedScopesHolderProvider.getScopesHolder();
      if (scopesHolder != null) {
        holders.add(scopesHolder);
      }
    });
    return holders.toArray(new NamedScopesHolder[holders.size()]);
  }

  @Nullable
  public static NamedScopesHolder getHolder(final Project project, final String scopeName, final NamedScopesHolder defaultHolder) {
    final NamedScopesHolder[] holders = getAllNamedScopeHolders(project);
    for (NamedScopesHolder holder : holders) {
      final NamedScope scope = holder.getScope(scopeName);
      if (scope != null) {
        return holder;
      }
    }
    return defaultHolder;
  }

  private static Element writeScope(NamedScope scope) {
    Element setElement = new Element(SCOPE_TAG);
    setElement.setAttribute(NAME_ATT, scope.getScopeId());
    final PackageSet packageSet = scope.getValue();
    setElement.setAttribute(PATTERN_ATT, packageSet != null ? packageSet.getText() : "");
    return setElement;
  }

  private static NamedScope readScope(Element setElement) {
    String name = setElement.getAttributeValue(NAME_ATT);
    PackageSet set;
    final String attributeValue = setElement.getAttributeValue(PATTERN_ATT);
    try {
      set = PackageSetFactory.getInstance().compile(attributeValue);
    }
    catch (ParsingException e) {
      set = new InvalidPackageSet(attributeValue);
    }
    return new NamedScope(name, set);
  }

  @Override
  public void loadState(final Element state) {
    myScopes.clear();
    List<Element> sets = state.getChildren(SCOPE_TAG);
    for (Element set : sets) {
      myScopes.add(readScope(set));
    }
    fireScopeListeners();
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    for (NamedScope myScope : myScopes) {
      element.addContent(writeScope(myScope));
    }
    return element;
  }

  @Nullable
  public NamedScope getScope(@Nullable String name) {
    if (name == null) return null;
    for (NamedScope scope : myScopes) {
      if (name.equals(scope.getName())) return scope;
    }
    return getPredefinedScope(name);
  }

  @Nonnull
  public List<NamedScope> getPredefinedScopes() {
    return Collections.emptyList();
  }

  @Nullable
  public NamedScope getPredefinedScope(String name) {
    return null;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  public final NamedScope createScope(@Nonnull String name, @Nullable PackageSet value) {
    return new NamedScope(name, value);
  }
}
