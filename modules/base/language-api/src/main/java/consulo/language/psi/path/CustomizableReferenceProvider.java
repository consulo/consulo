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

package consulo.language.psi.path;

import consulo.language.util.ProcessingContext;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;


/**
 * @author Maxim.Mossienko
 */
public interface CustomizableReferenceProvider {
  public final class CustomizationKey<T> {
    private final String myOptionDescription;

    public CustomizationKey(@NonNls String optionDescription) {
      myOptionDescription = optionDescription;
    }

    @Override
    public String toString() { return myOptionDescription; }

    public T getValue(@Nullable Map<CustomizationKey,Object> options) {
      return options == null ? null : (T)options.get(this);
    }

    public boolean getBooleanValue(@Nullable Map<CustomizationKey,Object> options) {
      if (options == null) {
        return false;
      }
      Boolean o = (Boolean)options.get(this);
      return o != null && o.booleanValue();
    }

    public void putValue(Map<CustomizationKey,Object> options, T value) {
      options.put(this, value);
    }
  }

  void setOptions(@Nullable Map<CustomizationKey,Object> options);
  @Nullable Map<CustomizationKey,Object> getOptions();

  @Nonnull
  public abstract PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext matchingContext);
}
