/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiPredicate;

/**
 * @author peter
 */
public abstract class TreeElementPattern<ParentType, T extends ParentType, Self extends TreeElementPattern<ParentType, T, Self>> extends ObjectPattern<T, Self> {

  protected TreeElementPattern(@Nonnull final InitialPatternCondition<T> condition) {
    super(condition);
  }

  protected TreeElementPattern(final Class<T> aClass) {
    super(aClass);
  }

  @Nullable
  protected abstract ParentType getParent(@Nonnull ParentType parentType);

  protected abstract ParentType[] getChildren(@Nonnull final ParentType parentType);

  public Self withParents(@Nonnull final Class<? extends ParentType>... types) {
    return with(new PatternCondition<T>("withParents") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        ParentType current = getParent(t);
        for (Class<? extends ParentType> type : types) {
          if (current == null || !type.isInstance(current)) {
            return false;
          }
          current = getParent(current);
        }
        return true;
      }
    });
  }

  public Self withParent(@Nonnull final Class<? extends ParentType> type) {
    return withParent(StandardPatterns.instanceOf(type));
  }

  public Self withParent(@Nonnull final ElementPattern<? extends ParentType> pattern) {
    return withSuperParent(1, pattern);
  }

  public Self withChild(@Nonnull final ElementPattern<? extends ParentType> pattern) {
    return withChildren(StandardPatterns.<ParentType>collection().atLeastOne(pattern));
  }

  public Self withFirstChild(@Nonnull final ElementPattern<? extends ParentType> pattern) {
    return withChildren(StandardPatterns.<ParentType>collection().first(pattern));
  }

  public Self withLastChild(@Nonnull final ElementPattern<? extends ParentType> pattern) {
    return withChildren(StandardPatterns.<ParentType>collection().last(pattern));
  }

  public Self withChildren(@Nonnull final ElementPattern<Collection<ParentType>> pattern) {
    return with(new PatternConditionPlus<T, Collection<ParentType>>("withChildren", pattern) {
      @Override
      public boolean processValues(T t, ProcessingContext context, BiPredicate<Collection<ParentType>, ProcessingContext> processor) {
        return processor.test(Arrays.asList(getChildren(t)), context);
      }
    });
  }

  public Self isFirstAcceptedChild(@Nonnull final ElementPattern<? super ParentType> pattern) {
    return with(new PatternCondition<T>("isFirstAcceptedChild") {
      @Override
      public boolean accepts(@Nonnull final T t, final ProcessingContext context) {
        final ParentType parent = getParent(t);
        if (parent != null) {
          final ParentType[] children = getChildren(parent);
          for (ParentType child : children) {
            if (pattern.accepts(child, context)) {
              return child == t;
            }
          }
        }

        return false;
      }
    });
  }

  public Self withSuperParent(final int level, @Nonnull final Class<? extends ParentType> aClass) {
    return withSuperParent(level, StandardPatterns.instanceOf(aClass));
  }

  public Self withSuperParent(final int level, @Nonnull final ElementPattern<? extends ParentType> pattern) {
    return with(new PatternConditionPlus<T, ParentType>(level == 1 ? "withParent" : "withSuperParent", pattern) {

      @Override
      public boolean processValues(T t, ProcessingContext context, BiPredicate<ParentType, ProcessingContext> processor) {
        ParentType parent = t;
        for (int i = 0; i < level; i++) {
          if (parent == null) return true;
          parent = getParent(parent);
        }
        return processor.test(parent, context);
      }
    });
  }

  public Self inside(@Nonnull final Class<? extends ParentType> pattern) {
    return inside(StandardPatterns.instanceOf(pattern));
  }

  public Self inside(@Nonnull final ElementPattern<? extends ParentType> pattern) {
    return inside(false, pattern);
  }

  public Self inside(final boolean strict, @Nonnull final ElementPattern<? extends ParentType> pattern) {
    return with(new PatternConditionPlus<T, ParentType>("inside", pattern) {
      @Override
      public boolean processValues(T t, ProcessingContext context, BiPredicate<ParentType, ProcessingContext> processor) {
        ParentType element = strict ? getParent(t) : t;
        while (element != null) {
          if (!processor.test(element, context)) return false;
          element = getParent(element);
        }
        return true;
      }
    });
  }

  public Self withAncestor(final int levelsUp, @Nonnull final ElementPattern<? extends ParentType> pattern) {
    return with(new PatternCondition<T>("withAncestor") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        ParentType element = t;
        for (int i = 0; i < levelsUp + 1; i++) {
          if (pattern.accepts(element, context)) return true;
          element = getParent(element);
          if (element == null) break;
        }
        return false;
      }
    });
  }

  public Self inside(final boolean strict, @Nonnull final ElementPattern<? extends ParentType> pattern, @Nonnull final ElementPattern<? extends ParentType> stopAt) {
    return with(new PatternCondition<T>("inside") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        ParentType element = strict ? getParent(t) : t;
        while (element != null) {
          if (stopAt.accepts(element, context)) return false;
          if (pattern.accepts(element, context)) return true;
          element = getParent(element);
        }
        return false;
      }
    });
  }

  /**
   * @param strict
   * @return Ensures that first elements in hierarchy accepted by patterns appear in specified order
   */
  public Self insideSequence(final boolean strict, @Nonnull final ElementPattern<? extends ParentType>... patterns) {
    return with(new PatternCondition<T>("insideSequence") {
      @Override
      public boolean accepts(@Nonnull final T t, final ProcessingContext context) {
        int i = 0;
        ParentType element = strict ? getParent(t) : t;
        while (element != null && i < patterns.length) {
          for (int j = i; j < patterns.length; j++) {
            if (patterns[j].accepts(element, context)) {
              if (i != j) return false;
              i++;
              break;
            }
          }
          element = getParent(element);
        }
        return true;
      }
    });
  }

  public Self afterSibling(final ElementPattern<? extends ParentType> pattern) {
    return with(new PatternCondition<T>("afterSibling") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        final ParentType parent = getParent(t);
        if (parent == null) return false;
        final ParentType[] children = getChildren(parent);
        final int i = Arrays.asList(children).indexOf(t);
        if (i <= 0) return false;
        return pattern.accepts(children[i - 1], context);
      }
    });
  }

  public Self afterSiblingSkipping(@Nonnull final ElementPattern skip, final ElementPattern<? extends ParentType> pattern) {
    return with(new PatternCondition<T>("afterSiblingSkipping") {
      @Override
      public boolean accepts(@Nonnull T t, ProcessingContext context) {
        final ParentType parent = getParent(t);
        if (parent == null) return false;
        final ParentType[] children = getChildren(parent);
        int i = Arrays.asList(children).indexOf(t);
        while (--i >= 0) {
          if (!skip.accepts(children[i], context)) {
            return pattern.accepts(children[i], context);
          }
        }
        return false;
      }
    });
  }
}

