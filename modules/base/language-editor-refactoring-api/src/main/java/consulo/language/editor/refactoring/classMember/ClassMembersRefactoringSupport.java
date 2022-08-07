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

package consulo.language.editor.refactoring.classMember;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dennis.Ushakov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ClassMembersRefactoringSupport extends LanguageExtension {
  ExtensionPointCacheKey<ClassMembersRefactoringSupport, ByLanguageValue<ClassMembersRefactoringSupport>> KEY =
          ExtensionPointCacheKey.create("ClassMembersRefactoringSupport", LanguageOneToOne.build());

  @Nullable
  static ClassMembersRefactoringSupport forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(ClassMembersRefactoringSupport.class).getOrBuildCache(KEY).get(language);
  }

  DependentMembersCollectorBase createDependentMembersCollector(Object clazz, Object superClass);

  boolean isProperMember(MemberInfoBase member);
}
