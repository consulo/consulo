/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.application.presentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.util.matcher.NameUtilCore;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class TypePresentationService {
  @Nonnull
  public static TypePresentationService getInstance() {
    return Application.get().getInstance(TypePresentationService.class);
  }

  @Nullable
  public abstract Image getIcon(Object o);

  @Nullable
  public abstract String getPresentableName(Object o);

  @Nullable
  public abstract Image getTypeIcon(Class type);

  @Nullable
  public abstract String getTypeName(Object o);

  @Nonnull
  public String getTypeNameOrStub(Object o) {
    String typeName = getTypeName(o);
    return typeName != null ? typeName : getDefaultTypeName(o.getClass());
  }

  public static String getDefaultTypeName(Class aClass) {
    String simpleName = aClass.getSimpleName();
    int i = simpleName.indexOf('$');
    if (i >= 0) {
      simpleName = simpleName.substring(i + 1);
    }
    return StringUtil.capitalizeWords(StringUtil.join(NameUtilCore.nameToWords(simpleName), " "), true);
  }
}
