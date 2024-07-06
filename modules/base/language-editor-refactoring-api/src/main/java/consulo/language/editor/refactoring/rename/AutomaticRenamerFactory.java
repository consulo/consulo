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

package consulo.language.editor.refactoring.rename;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageInfo;

import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AutomaticRenamerFactory {
  ExtensionPointName<AutomaticRenamerFactory> EP_NAME = ExtensionPointName.create(AutomaticRenamerFactory.class);

  boolean isApplicable(PsiElement element);

  @Nullable
  LocalizeValue getOptionName();

  boolean isEnabled();

  void setEnabled(boolean enabled);

  AutomaticRenamer createRenamer(final PsiElement element, final String newName, final Collection<UsageInfo> usages);
}
