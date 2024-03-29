/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.richcopy.settings;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.StoragePathMacros;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.ide.ServiceManager;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Denis Zhdanov
 * @since 3/26/13 6:40 PM
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "EditorRichCopySettings", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/editor.rich.copy.xml")})
public class RichCopySettings implements PersistentStateComponent<RichCopySettings> {

  @Nonnull
  public static final String ACTIVE_GLOBAL_SCHEME_MARKER = "__ACTIVE_GLOBAL_SCHEME__";

  private boolean myEnabled = true;
  private String  mySchemeName = ACTIVE_GLOBAL_SCHEME_MARKER;

  @Nonnull
  public static RichCopySettings getInstance() {
    return ServiceManager.getService(RichCopySettings.class);
  }

  @Nonnull
  public EditorColorsScheme getColorsScheme(@Nonnull EditorColorsScheme editorColorsScheme) {
    EditorColorsScheme result = null;
    if (mySchemeName != null && !ACTIVE_GLOBAL_SCHEME_MARKER.equals(mySchemeName)) {
      result = EditorColorsManager.getInstance().getScheme(mySchemeName);
    }
    return result == null ? editorColorsScheme : result;
  }

  @Nullable
  @Override
  public RichCopySettings getState() {
    return this;
  }

  @Override
  public void loadState(RichCopySettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @Nonnull
  public String getSchemeName() {
    return mySchemeName == null ? ACTIVE_GLOBAL_SCHEME_MARKER : mySchemeName;
  }

  public void setSchemeName(@Nullable String schemeName) {
    mySchemeName = schemeName;
  }

  public boolean isEnabled() {
    return myEnabled;
  }

  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
  }
}
