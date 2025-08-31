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

package consulo.execution.impl.internal.ui.layout;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Singleton
@State(name = "RunnerLayoutSettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/runner.layout.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class RunnerLayoutSettings implements PersistentStateComponent<Element> {
  @Deprecated
  public static RunnerLayoutSettings getInstance() {
    return Application.get().getInstance(RunnerLayoutSettings.class);
  }

  private final Map<String, RunnerLayoutImpl> myRunnerId2Settings = new LinkedHashMap<String, RunnerLayoutImpl>();

  public RunnerLayoutImpl getLayout(@Nonnull String id) {
    RunnerLayoutImpl layout = myRunnerId2Settings.get(id);
    if (layout == null) {
      layout = new RunnerLayoutImpl(id);
      myRunnerId2Settings.put(id, layout);
    }

    return layout;
  }

  @Override
  public Element getState() {
    Element runners = new Element("runners");
    for (String eachID : myRunnerId2Settings.keySet()) {
      RunnerLayoutImpl layout = myRunnerId2Settings.get(eachID);
      Element runnerElement = new Element("runner");
      runnerElement.setAttribute("id", eachID);
      layout.write(runnerElement);
      runners.addContent(runnerElement);
    }
    return runners;
  }

  @Override
  public void loadState(Element state) {
    List runners = state.getChildren("runner");
    for (Object each : runners) {
      Element eachRunnerElement = (Element)each;
      String eachID = eachRunnerElement.getAttributeValue("id");
      RunnerLayoutImpl eachLayout = new RunnerLayoutImpl(eachID);
      eachLayout.read(eachRunnerElement);
      myRunnerId2Settings.put(eachID, eachLayout);
    }
  }
}
