/*
 * Copyright 2013-2018 must-be.org
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

package consulo.psi.injection.impl;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.containers.ContainerUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.intellij.plugins.intelliLang.Configuration;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
@Singleton
@State(name = Configuration.COMPONENT_NAME, storages = {@Storage("injecting.xml"), @Storage(value = "IntelliLang.xml", deprecated = true)})
public class ProjectInjectionConfiguration extends Configuration {
  private final Configuration myParentConfiguration;

  @Inject
  public ProjectInjectionConfiguration() {
    myParentConfiguration = getInstance();
  }

  @Override
  public AdvancedConfiguration getAdvancedConfiguration() {
    return myParentConfiguration.getAdvancedConfiguration();
  }

  @Override
  public List<BaseInjection> getDefaultInjections() {
    return myParentConfiguration.getDefaultInjections();
  }

  @Override
  public Collection<BaseInjection> getAllInjections() {
    Collection<BaseInjection> injections = super.getAllInjections();
    injections.addAll(myParentConfiguration.getAllInjections());
    return injections;
  }

  @Nonnull
  @Override
  public List<BaseInjection> getInjections(final String injectorId) {
    return ContainerUtil.concat(myParentConfiguration.getInjections(injectorId), getOwnInjections(injectorId));
  }

  public Configuration getParentConfiguration() {
    return myParentConfiguration;
  }

  public List<BaseInjection> getOwnInjections(final String injectorId) {
    return super.getInjections(injectorId);
  }

  @Override
  public long getModificationCount() {
    return super.getModificationCount() + myParentConfiguration.getModificationCount();
  }

  @Override
  public boolean replaceInjections(List<? extends BaseInjection> newInjections, List<? extends BaseInjection> originalInjections, boolean forceLevel) {
    if (!forceLevel && !originalInjections.isEmpty()) {
      if (myParentConfiguration.replaceInjections(Collections.<BaseInjection>emptyList(), originalInjections, false)) {
        myParentConfiguration.replaceInjections(newInjections, Collections.<BaseInjection>emptyList(), false);
        return true;
      }
    }
    return super.replaceInjections(newInjections, originalInjections, forceLevel);
  }
}
