/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.usage;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.find.PsiElement2UsageTargetAdapter;
import consulo.language.psi.PsiElement;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.PsiElementUsageTargetFactory;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 18/04/2023
 */
@ServiceImpl
public class PsiElementUsageTargetFactoryImpl implements PsiElementUsageTargetFactory {
  @Nonnull
  @Override
  public PsiElementUsageTarget create(PsiElement element) {
    return new PsiElement2UsageTargetAdapter(element);
  }
}
