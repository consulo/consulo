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
package consulo.language.codeStyle;

import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.util.ModificationTracker;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.language.codeStyle.event.CodeStyleSettingsChangeEvent;
import consulo.language.codeStyle.event.CodeStyleSettingsListener;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.Lists;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.DifferenceFilter;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CodeStyleSettingsManager implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(CodeStyleSettingsManager.class);

  public volatile CodeStyleSettings PER_PROJECT_SETTINGS = null;
  public volatile boolean USE_PER_PROJECT_SETTINGS = false;
  public volatile String PREFERRED_PROJECT_CODE_STYLE = null;
  private volatile CodeStyleSettings myTemporarySettings;
  private volatile boolean myIsLoaded = false;

  private final SimpleModificationTracker myTracker = new SimpleModificationTracker();
  private final EventDispatcher<CodeStyleSettingsListener> myEventDispatcher = EventDispatcher.create(CodeStyleSettingsListener.class);

  private final List<CodeStyleSettingsListener> myListeners = Lists.newLockFreeCopyOnWriteList();

  public static CodeStyleSettingsManager getInstance(@Nullable Project project) {
    if (project == null || project.isDefault()) {
      return getInstance();
    }
    return ProjectCodeStyleSettingsManager.getInstance(project);
  }

  public static CodeStyleSettingsManager getInstance() {
    return Application.get().getInstance(AppCodeStyleSettingsManager.class);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public CodeStyleSettingsManager(Project project) {
  }

  public CodeStyleSettingsManager() {
  }

  @Nonnull
  public static CodeStyleSettings getSettings(@Nullable final Project project) {
    return getInstance(project).getCurrentSettings();
  }

  @Nonnull
  public CodeStyleSettings getCurrentSettings() {
    CodeStyleSettings temporarySettings = myTemporarySettings;
    if (temporarySettings != null) return temporarySettings;
    CodeStyleSettings projectSettings = PER_PROJECT_SETTINGS;
    if (USE_PER_PROJECT_SETTINGS && projectSettings != null) return projectSettings;
    return CodeStyleSchemes.getInstance().findPreferredScheme(PREFERRED_PROJECT_CODE_STYLE).getCodeStyleSettings();
  }

  private void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  private void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element, new DifferenceFilter<>(this, new CodeStyleSettingsManager()));
  }

  @Override
  public Element getState() {
    Element result = new Element("state");
    try {
      writeExternal(result);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return result;
  }

  @Override
  public void loadState(Element state) {
    try {
      readExternal(state);
      myIsLoaded = true;
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  public CodeStyleSettings getTemporarySettings() {
    return myTemporarySettings;
  }

  public void setTemporarySettings(@Nonnull CodeStyleSettings settings) {
    myTemporarySettings = settings;
  }

  public void dropTemporarySettings() {
    myTemporarySettings = null;
  }

  public boolean isLoaded() {
    return myIsLoaded;
  }

  @Deprecated
  public void addListener(@Nonnull CodeStyleSettingsListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void addListener(@Nonnull CodeStyleSettingsListener listener, @Nonnull Disposable disposable) {
    myEventDispatcher.addListener(listener, disposable);
  }

  /**
   * Increase current project's code style modification tracker and notify all the listeners on changed code style. The
   * method must be called if project code style is changed programmatically so that editors and etc. are aware of
   * code style update and refresh their settings accordingly.
   *
   * @see CodeStyleSettingsListener
   * @see #addListener(CodeStyleSettingsListener, Disposable)
   */
  public final void notifyCodeStyleSettingsChanged() {
    updateSettingsTracker();
    fireCodeStyleSettingsChanged(null);
  }

  public void updateSettingsTracker() {
    CodeStyleSettings settings = getCurrentSettings();
    settings.getModificationTracker().incModificationCount();

    myTracker.incModificationCount();
  }

  public void fireCodeStyleSettingsChanged(@Nullable PsiFile file) {
    for (CodeStyleSettingsListener listener : myListeners) {
      listener.codeStyleSettingsChanged(new CodeStyleSettingsChangeEvent(file));
    }
  }

  @Nonnull
  public final ModificationTracker getModificationTracker() {
    return myTracker;
  }
}
