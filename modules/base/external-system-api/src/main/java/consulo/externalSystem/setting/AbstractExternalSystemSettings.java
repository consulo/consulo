/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.setting;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.externalSystem.ExternalSystemManager;
import consulo.project.Project;
import consulo.util.lang.lazy.LazyValue;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Common base class for external system settings. Defines a minimal api which is necessary for the common external system
 * support codebase.
 * <p/>
 * <b>Note:</b> non-abstract sub-classes of this class are expected to be marked by {@link State} annotation configured as necessary.
 *
 * @author Denis Zhdanov
 * @since 4/3/13 4:04 PM
 */
public abstract class AbstractExternalSystemSettings<SS extends AbstractExternalSystemSettings<SS, PS, L>, PS extends ExternalProjectSettings, L extends ExternalSystemSettingsListener<PS>>
        implements Disposable {

  
  private final Class<L> myChangesTopic;

  private Project myProject;

  private final LazyValue<@Nullable ExternalSystemManager<?, ?, ?, ?, ?>> myManager = LazyValue.nullable(this::deduceManager);

  
  private final Map<String/* project path */, PS> myLinkedProjectsSettings = new HashMap<String, PS>();

  
  private final Map<String/* project path */, PS> myLinkedProjectsSettingsView = Collections.unmodifiableMap(myLinkedProjectsSettings);

  protected AbstractExternalSystemSettings(Class<L> topic, Project project) {
    myChangesTopic = topic;
    myProject = project;
    Disposer.register(project, this);
  }

  @Override
  public void dispose() {
    myProject = null;
  }

  
  public Project getProject() {
    return myProject;
  }

  private @Nullable ExternalSystemManager<?, ?, ?, ?, ?> deduceManager() {
    Project project = myProject;
    if (project == null) {
      return null;
    }
    return Application.get().getExtensionPoint(ExternalSystemManager.class)
      .findFirstSafe(it -> equals(it.getSettingsProvider().apply(project)));
  }

  /**
   * Every time particular external system setting is changed corresponding message is sent via ide
   * <a href="http://confluence.jetbrains.com/display/IDEADEV/IntelliJ+IDEA+Messaging+infrastructure">messaging sub-system</a>.
   * The problem is that every external system implementation defines it's own topic/listener pair. Listener interface is derived
   * from the common {@link ExternalSystemSettingsListener} interface and is specific to external sub-system implementation.
   * However, it's possible that a client wants to perform particular actions based only on {@link ExternalSystemSettingsListener}
   * facilities. There is no way for such external system-agnostic client to create external system-specific listener
   * implementation then.
   * <p/>
   * That's why this method allows to wrap given 'generic listener' into external system-specific one.
   *
   * @param listener target generic listener to wrap to external system-specific implementation
   */
  public abstract void subscribe(ExternalSystemSettingsListener<PS> listener);

  public void copyFrom(SS settings) {
    myLinkedProjectsSettings.clear();
    for (PS projectSettings : settings.getLinkedProjectsSettings()) {
      myLinkedProjectsSettings.put(projectSettings.getExternalProjectPath(), projectSettings);
    }
    copyExtraSettingsFrom(settings);
  }

  protected abstract void copyExtraSettingsFrom(SS settings);

  @SuppressWarnings("unchecked")
  
  public Collection<PS> getLinkedProjectsSettings() {
    return myLinkedProjectsSettingsView.values();
  }

  public @Nullable PS getLinkedProjectSettings(String linkedProjectPath) {
    PS ps = myLinkedProjectsSettings.get(linkedProjectPath);
    if (ps == null) {
      for (PS ps1 : myLinkedProjectsSettings.values()) {
        for (String modulePath : ps1.getModules()) {
          if (linkedProjectPath.equals(modulePath)) return ps1;
        }
      }
    }
    return ps;
  }

  public void linkProject(PS settings) throws IllegalArgumentException {
    PS existing = getLinkedProjectSettings(settings.getExternalProjectPath());
    if (existing != null) {
      throw new IllegalArgumentException(String.format("Can't link external project '%s'. Reason: it's already registered at the current ide project", settings.getExternalProjectPath()));
    }
    myLinkedProjectsSettings.put(settings.getExternalProjectPath(), settings);
    onProjectsLinked(Collections.singleton(settings));
  }

  /**
   * Un-links given external project from the current ide project.
   *
   * @param linkedProjectPath path of external project to be unlinked
   * @return <code>true</code> if there was an external project with the given config path linked to the current
   * ide project;
   * <code>false</code> otherwise
   */
  public boolean unlinkExternalProject(String linkedProjectPath) {
    PS removed = myLinkedProjectsSettings.remove(linkedProjectPath);
    if (removed == null) {
      return false;
    }

    onProjectsUnlinked(Collections.singleton(linkedProjectPath));
    return true;
  }

  public void setLinkedProjectsSettings(Collection<? extends PS> settings) {
    setLinkedProjectsSettings(settings, new ExternalSystemSettingsListener<>() {
      @Override
      public void onProjectsLinked(Collection<PS> settings) {
        AbstractExternalSystemSettings.this.onProjectsLinked(settings);
      }

      @Override
      public void onProjectsUnlinked(Set<String> linkedProjectPaths) {
        AbstractExternalSystemSettings.this.onProjectsUnlinked(linkedProjectPaths);
      }
    });
  }

  private void setLinkedProjectsSettings(Collection<? extends PS> settings, ExternalSystemSettingsListener<PS> listener) {
    List<PS> validSettings = new ArrayList<>();
    for (PS ps : settings) {
      if (ps.getExternalProjectPath() != null) {
        validSettings.add(ps);
      }
    }

    List<PS> added = new ArrayList<>();
    Map<String, PS> removed = new HashMap<>(myLinkedProjectsSettings);
    myLinkedProjectsSettings.clear();
    for (PS current : validSettings) {
      myLinkedProjectsSettings.put(current.getExternalProjectPath(), current);
    }

    for (PS current : validSettings) {
      PS old = removed.remove(current.getExternalProjectPath());
      if (old == null) {
        added.add(current);
      }
      else {
        checkSettings(old, current);
      }
    }
    if (!added.isEmpty()) {
      listener.onProjectsLinked(added);
    }
    if (!removed.isEmpty()) {
      listener.onProjectsUnlinked(removed.keySet());
    }
  }

  /**
   * Is assumed to check if given old settings external system-specific state differs from the given new one
   * and {@link #getPublisher() notify} listeners in case of the positive answer.
   *
   * @param old     old settings state
   * @param current current settings state
   */
  protected abstract void checkSettings(PS old, PS current);

  
  public Class<L> getChangesTopic() {
    return myChangesTopic;
  }

  
  public L getPublisher() {
    return myProject.getMessageBus().syncPublisher(myChangesTopic);
  }

  protected void fillState(State<PS> state) {
    state.setLinkedExternalProjectsSettings(new TreeSet<PS>(myLinkedProjectsSettings.values()));
  }

  protected void loadState(State<PS> state) {
    Set<PS> settings = state.getLinkedExternalProjectsSettings();
    if (settings != null) {
      Project project = myProject;
      setLinkedProjectsSettings(settings, new ExternalSystemSettingsListener<>() {
        @Override
        public void onProjectsLinked(Collection<PS> settings) {
          project.getUIAccess().give(() -> {
            if (project.isDisposed()) {
              return;
            }
            fireProjectsLinkedExtensions(settings);
            AbstractExternalSystemSettings.this.onProjectsLoaded(settings);
          });
        }

        @Override
        public void onProjectsUnlinked(Set<String> linkedProjectPaths) {
          project.getUIAccess().give(() -> {
            if (project.isDisposed()) {
              return;
            }
            AbstractExternalSystemSettings.this.onProjectsUnlinked(linkedProjectPaths);
          });
        }
      });
    }
  }

  private void onProjectsLoaded(Collection<PS> settings) {
    getPublisher().onProjectsLoaded(settings);
    ExternalSystemManager<?, ?, ?, ?, ?> manager = myManager.get();
    if (manager != null) {
      myProject.getExtensionPoint(ExternalSystemSettingsListenerEx.class).forEach(it -> it.onProjectsLoaded(manager, settings));
    }
  }

  private void onProjectsLinked(Collection<PS> settings) {
    getPublisher().onProjectsLinked(settings);
    fireProjectsLinkedExtensions(settings);
  }

  private void fireProjectsLinkedExtensions(Collection<PS> settings) {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = myManager.get();
    if (manager != null) {
      myProject.getExtensionPoint(ExternalSystemSettingsListenerEx.class).forEach(it -> it.onProjectsLinked(manager, settings));
    }
  }

  private void onProjectsUnlinked(Set<String> linkedProjectPaths) {
    getPublisher().onProjectsUnlinked(linkedProjectPaths);
    ExternalSystemManager<?, ?, ?, ?, ?> manager = myManager.get();
    if (manager != null) {
      myProject.getExtensionPoint(ExternalSystemSettingsListenerEx.class).forEach(it -> it.onProjectsUnlinked(manager, linkedProjectPaths));
    }
  }

  public interface State<S> {

    Set<S> getLinkedExternalProjectsSettings();

    void setLinkedExternalProjectsSettings(Set<S> settings);
  }
}
