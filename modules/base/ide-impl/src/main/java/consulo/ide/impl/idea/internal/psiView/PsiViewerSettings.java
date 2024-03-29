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
package consulo.ide.impl.idea.internal.psiView;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

/**
 * @author Konstantin Bulenkov
 */
@Singleton
@State(name = "PsiViewerSettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class PsiViewerSettings implements PersistentStateComponent<PsiViewerSettings> {
  public boolean showWhiteSpaces = true;
  public boolean showTreeNodes = true;
  public String type = "JAVA file";
  public String text = "";
  public String dialect = "";
  public int textDividerLocation = 250;
  public int treeDividerLocation = 400;
  public int blockRefDividerLocation = 600;
  public boolean showBlocks = true;

  public static PsiViewerSettings getSettings() {
    return ServiceManager.getService(PsiViewerSettings.class);
  }

  @Override
  public PsiViewerSettings getState() {
    return this;
  }

  @Override
  public void loadState(PsiViewerSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
