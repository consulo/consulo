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
package consulo.language.editor.inspection.scheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.component.persist.StoragePathMacros;
import consulo.content.scope.NamedScope;
import consulo.disposer.Disposable;
import consulo.disposer.util.DisposerUtil;
import consulo.language.editor.inspection.scheme.event.ProfileChangeAdapter;
import consulo.language.editor.rawHighlight.SeverityProvider;
import consulo.logging.Logger;
import consulo.util.collection.Lists;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * User: anna
 * Date: 29-Nov-2005
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class InspectionProfileManager extends ApplicationProfileManager implements SeverityProvider {
  public static final String INSPECTION_DIR = "inspection";
  public static final String FILE_SPEC = StoragePathMacros.ROOT_CONFIG + '/' + INSPECTION_DIR;

  private final List<ProfileChangeAdapter> myProfileChangeAdapters = Lists.newLockFreeCopyOnWriteList();

  protected static final Logger LOG = Logger.getInstance(InspectionProfileManager.class);

  public static InspectionProfileManager getInstance() {
    return Application.get().getInstance(InspectionProfileManager.class);
  }

  public InspectionProfileManager() {
  }

  protected abstract void initProfiles();

  @Nonnull
  public abstract Collection<InspectionProfile> getProfiles();

  public abstract Profile loadProfile(@Nonnull String path) throws IOException, JDOMException;

  @Override
  public void addProfileChangeListener(@Nonnull final ProfileChangeAdapter listener) {
    myProfileChangeAdapters.add(listener);
  }

  @Override
  public void addProfileChangeListener(@Nonnull ProfileChangeAdapter listener, @Nonnull Disposable parentDisposable) {
    DisposerUtil.add(listener, myProfileChangeAdapters, parentDisposable);
  }

  @Override
  public void removeProfileChangeListener(@Nonnull final ProfileChangeAdapter listener) {
    myProfileChangeAdapters.remove(listener);
  }

  @Override
  public void fireProfileChanged(final Profile profile) {
    if (profile instanceof ProfileEx) {
      ((ProfileEx)profile).profileChanged();
    }
    for (ProfileChangeAdapter adapter : myProfileChangeAdapters) {
      adapter.profileChanged(profile);
    }
  }

  @Override
  public void fireProfileChanged(final Profile oldProfile, final Profile profile, final NamedScope scope) {
    for (ProfileChangeAdapter adapter : myProfileChangeAdapters) {
      adapter.profileActivated(oldProfile, profile);
    }
  }

  @Override
  public Profile getProfile(@Nonnull final String name) {
    return getProfile(name, true);
  }
}
