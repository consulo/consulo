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

package consulo.language.inject.advanced.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.List;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
@Singleton
@State(name = Configuration.COMPONENT_NAME, storages = {@Storage("injecting.xml"), @Storage(value = "IntelliLang.xml", deprecated = true)})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ApplicationInjectionConfiguration extends Configuration {
  private final List<BaseInjection> myDefaultInjections;
  private final AdvancedConfiguration myAdvancedConfiguration;

  @Inject
  public ApplicationInjectionConfiguration() {
    myDefaultInjections = loadDefaultInjections();
    myAdvancedConfiguration = new AdvancedConfiguration();
  }

  @Override
  public List<BaseInjection> getDefaultInjections() {
    return myDefaultInjections;
  }

  @Override
  public AdvancedConfiguration getAdvancedConfiguration() {
    return myAdvancedConfiguration;
  }

  @Override
  public void loadState(Element element) {
    myAdvancedConfiguration.loadState(element);
    super.loadState(element);
  }

  @Override
  public Element getState() {
    Element element = new Element(COMPONENT_NAME);
    myAdvancedConfiguration.writeState(element);
    return getState(element);
  }
}
