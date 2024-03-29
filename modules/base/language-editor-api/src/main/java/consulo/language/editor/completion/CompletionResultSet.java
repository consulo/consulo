// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.application.progress.ProgressManager;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.StandardPatterns;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

/**
 * {@link CompletionResultSet}s feed on {@link LookupElement}s,
 * match them against specified
 * {@link PrefixMatcher} and give them to special {@link Consumer}
 * for further processing, which usually means
 * they will sooner or later appear in completion list. If they don't, there must be some {@link CompletionContributor}
 * up the invocation stack that filters them out.
 * <p>
 * If you want to change the matching prefix, use {@link #withPrefixMatcher(PrefixMatcher)} or {@link #withPrefixMatcher(String)}
 * to obtain another {@link CompletionResultSet} and give your lookup elements to that one.
 *
 * @author peter
 */
public abstract class CompletionResultSet implements Consumer<LookupElement> {
  private final PrefixMatcher myPrefixMatcher;
  private final Consumer<? super CompletionResult> myConsumer;
  protected final CompletionService myCompletionService = CompletionService.getCompletionService();
  protected final CompletionContributor myContributor;
  private boolean myStopped;

  protected CompletionResultSet(final PrefixMatcher prefixMatcher, Consumer<? super CompletionResult> consumer, CompletionContributor contributor) {
    myPrefixMatcher = prefixMatcher;
    myConsumer = consumer;
    myContributor = contributor;
  }

  protected Consumer<? super CompletionResult> getConsumer() {
    return myConsumer;
  }

  @Override
  public void accept(LookupElement element) {
    addElement(element);
  }

  /**
   * If a given element matches the prefix, give it for further processing (which may eventually result in its appearing in the completion list).
   *
   * @see #addAllElements(Iterable)
   */
  public abstract void addElement(@Nonnull final LookupElement element);

  public void passResult(@Nonnull CompletionResult result) {
    myConsumer.accept(result);
  }

  public void startBatch() {
    if (myConsumer instanceof BatchConsumer) {
      ((BatchConsumer)myConsumer).startBatch();
    }
  }

  public void endBatch() {
    if (myConsumer instanceof BatchConsumer) {
      ((BatchConsumer)myConsumer).endBatch();
    }
  }

  /**
   * Adds all elements from the given collection that match the prefix for further processing. The elements are processed in batch,
   * so that they'll appear in lookup all together.<p/>
   * This can be useful to ensure predictable order of top suggested elements.
   * Otherwise, when the lookup is shown, most relevant elements processed to that moment are put to the top
   * and remain there even if more relevant elements appear later.
   * These "first" elements may differ from completion invocation to completion invocation due to performance fluctuations,
   * resulting in varying preselected item in completion and worse user experience. Using {@code addAllElements}
   * instead of {@link #addElement(LookupElement)} helps to avoid that.
   */
  public void addAllElements(@Nonnull final Iterable<? extends LookupElement> elements) {
    startBatch();
    int seldomCounter = 0;
    for (LookupElement element : elements) {
      seldomCounter++;
      addElement(element);
      if (seldomCounter % 1000 == 0) {
        ProgressManager.checkCanceled();
      }
    }
    endBatch();
  }

  @Contract(pure = true)
  @Nonnull
  public abstract CompletionResultSet withPrefixMatcher(@Nonnull PrefixMatcher matcher);

  /**
   * Creates a default camel-hump prefix matcher based on given prefix
   */
  @Contract(pure = true)
  @Nonnull
  public abstract CompletionResultSet withPrefixMatcher(@Nonnull String prefix);

  @Nonnull
  @Contract(pure = true)
  public abstract CompletionResultSet withRelevanceSorter(@Nonnull CompletionSorter sorter);

  public abstract void addLookupAdvertisement(@Nonnull String text);

  /**
   * @return A result set with the same prefix, but the lookup strings will be matched case-insensitively. Their lookup strings will
   * remain as they are though, so upon insertion the prefix case will be changed.
   */
  @Contract(pure = true)
  @Nonnull
  public abstract CompletionResultSet caseInsensitive();

  @Nonnull
  public PrefixMatcher getPrefixMatcher() {
    return myPrefixMatcher;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public void stopHere() {
    myStopped = true;
  }

  public LinkedHashSet<CompletionResult> runRemainingContributors(CompletionParameters parameters, final boolean passResult) {
    final LinkedHashSet<CompletionResult> elements = new LinkedHashSet<>();
    runRemainingContributors(parameters, result -> {
      if (passResult) {
        passResult(result);
      }
      elements.add(result);
    });
    return elements;
  }

  public void runRemainingContributors(CompletionParameters parameters, Consumer<? super CompletionResult> consumer) {
    runRemainingContributors(parameters, (Consumer)consumer, true);
  }

  public void runRemainingContributors(CompletionParameters parameters, Consumer<CompletionResult> consumer, final boolean stop) {
    if (stop) {
      stopHere();
    }
    myCompletionService.getVariantsFromContributors(parameters, myContributor, getPrefixMatcher(), new BatchConsumer<CompletionResult>() {
      @Override
      public void startBatch() {
        CompletionResultSet.this.startBatch();
      }

      @Override
      public void endBatch() {
        CompletionResultSet.this.endBatch();
      }

      @Override
      public void accept(CompletionResult result) {
        consumer.accept(result);
      }
    });
  }

  /**
   * Request that the completion contributors be run again when the user changes the prefix so that it becomes equal to the one given.
   */
  public void restartCompletionOnPrefixChange(String prefix) {
    restartCompletionOnPrefixChange(StandardPatterns.string().equalTo(prefix));
  }

  /**
   * Request that the completion contributors be run again when the user changes the prefix in a way satisfied by the given condition.
   */
  public abstract void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition);

  /**
   * Request that the completion contributors be run again when the user changes the prefix in any way.
   */
  public void restartCompletionOnAnyPrefixChange() {
    restartCompletionOnPrefixChange(StandardPatterns.string());
  }

  /**
   * Request that the completion contributors be run again when the user types something into the editor so that no existing lookup elements match that prefix anymore.
   */
  public abstract void restartCompletionWhenNothingMatches();
}
