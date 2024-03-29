// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * For completion FAQ, see {@link CompletionContributor}.
 *
 * @author peter
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class CompletionService {
  /**
   * A "weigher" extension key (see {@link Weigher}) to sort completion items by priority and move the heaviest to the top of the Lookup.
   */
  public static final Key<CompletionWeigher> RELEVANCE_KEY = Key.create("completion");

  public static CompletionService getCompletionService() {
    return Application.get().getInstance(CompletionService.class);
  }

  /**
   * Set lookup advertisement text (at the bottom) at any time. Will do nothing if no completion process is in progress.
   *
   * @param text
   * @deprecated use {@link CompletionResultSet#addLookupAdvertisement(String)}
   */
  @Deprecated
  public abstract void setAdvertisementText(@Nullable String text);

  /**
   * Run all contributors until any of them returns false or the list is exhausted. If from parameter is not null, contributors
   * will be run starting from the next one after that.
   */
  public void getVariantsFromContributors(final CompletionParameters parameters, @Nullable final CompletionContributor from, final Consumer<? super CompletionResult> consumer) {
    getVariantsFromContributors(parameters, from, createMatcher(suggestPrefix(parameters), false), consumer);
  }

  protected void getVariantsFromContributors(CompletionParameters parameters, @Nullable CompletionContributor from, PrefixMatcher matcher, Consumer<? super CompletionResult> consumer) {
    final List<CompletionContributor> contributors = CompletionContributor.forParameters(parameters);

    for (int i = contributors.indexOf(from) + 1; i < contributors.size(); i++) {
      ProgressManager.checkCanceled();
      CompletionContributor contributor = contributors.get(i);

      CompletionResultSet result = createResultSet(parameters, consumer, contributor, matcher);
      contributor.fillCompletionVariants(parameters, result);
      if (result.isStopped()) {
        return;
      }
    }
  }

  protected abstract CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer,
                                                         @Nonnull CompletionContributor contributor, PrefixMatcher matcher);

  protected abstract String suggestPrefix(CompletionParameters parameters);

  @Nonnull
  protected abstract PrefixMatcher createMatcher(String prefix, boolean typoTolerant);


  @Nullable
  public abstract CompletionProcess getCurrentCompletion();

  /**
   * The main method that is invoked to collect all the completion variants
   *
   * @param parameters Parameters specifying current completion environment
   * @param consumer   The consumer of the completion variants. Pass an instance of {@link BatchConsumer} if you need to receive information
   *                   about item batches generated by each completion contributor.
   */
  public void performCompletion(CompletionParameters parameters, Consumer<? super CompletionResult> consumer) {
    final Set<LookupElement> lookupSet = ContainerUtil.newConcurrentSet();

    AtomicBoolean typoTolerant = new AtomicBoolean();

    BatchConsumer<CompletionResult> batchConsumer = new BatchConsumer<CompletionResult>() {
      @Override
      public void startBatch() {
        if (consumer instanceof BatchConsumer) {
          ((BatchConsumer)consumer).startBatch();
        }
      }

      @Override
      public void endBatch() {
        if (consumer instanceof BatchConsumer) {
          ((BatchConsumer)consumer).endBatch();
        }
      }

      @Override
      public void accept(CompletionResult result) {
        if (typoTolerant.get() && result.getLookupElement().getAutoCompletionPolicy() != AutoCompletionPolicy.NEVER_AUTOCOMPLETE) {
          result = result.withLookupElement(AutoCompletionPolicy.NEVER_AUTOCOMPLETE.applyPolicy(result.getLookupElement()));
        }
        if (lookupSet.add(result.getLookupElement())) {
          consumer.accept(result);
        }
      }
    };
    String prefix = suggestPrefix(parameters);
    getVariantsFromContributors(parameters, null, createMatcher(prefix, false), batchConsumer);
    if (lookupSet.isEmpty() && prefix.length() > 2) {
      typoTolerant.set(true);
      getVariantsFromContributors(parameters, null, createMatcher(prefix, true), batchConsumer);
    }
  }

  public abstract CompletionSorter defaultSorter(CompletionParameters parameters, PrefixMatcher matcher);

  public abstract CompletionSorter emptySorter();

  public static boolean isStartMatch(LookupElement element, WeighingContext context) {
    return getItemMatcher(element, context).isStartMatch(element);
  }

  public static PrefixMatcher getItemMatcher(LookupElement element, WeighingContext context) {
    PrefixMatcher itemMatcher = context.itemMatcher(element);
    String pattern = context.itemPattern(element);
    if (!pattern.equals(itemMatcher.getPrefix())) {
      return itemMatcher.cloneWithPrefix(pattern);
    }
    return itemMatcher;
  }
}
