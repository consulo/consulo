/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.pattern;

import consulo.language.util.ProcessingContext;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import java.util.function.BiPredicate;

/**
 * @author Gregory.Shrago
 */
public abstract class PatternConditionPlus<Target, Value> extends PatternCondition<Target> implements BiPredicate<Value, ProcessingContext> {
  private final ElementPattern myValuePattern;

  public PatternConditionPlus(@NonNls String methodName, ElementPattern valuePattern) {
    super(methodName);
    myValuePattern = valuePattern;
  }

  public ElementPattern getValuePattern() {
    return myValuePattern;
  }

  public abstract boolean processValues(Target t, ProcessingContext context, BiPredicate<Value, ProcessingContext> processor);

  @Override
  public boolean accepts(@Nonnull Target t, ProcessingContext context) {
    return !processValues(t, context, this);
  }

  @Override
  public final boolean test(Value p, ProcessingContext context) {
    return !myValuePattern.accepts(p, context);
  }
}
