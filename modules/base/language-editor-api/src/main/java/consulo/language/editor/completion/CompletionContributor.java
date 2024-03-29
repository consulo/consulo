/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.ProcessingContext;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.util.collection.MultiMap;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Completion FAQ:<p>
 * <p>
 * Q: How do I implement code completion?<br>
 * A: Define a completion.contributor extension of type {@link CompletionContributor}.
 * Or, if the place you want to complete in contains a {@link PsiReference}, just return the variants
 * you want to suggest from its {@link PsiReference#getVariants()} method as {@link String}s,
 * {@link PsiElement}s, or better {@link LookupElement}s.<p>
 * <p>
 * Q: OK, but what to do with CompletionContributor?<br>
 * A: There are two ways. The easier and preferred one is to provide constructor in your contributor and register completion providers there:
 * {@link #extend(CompletionType, ElementPattern, CompletionProvider)}.<br>
 * A more generic way is to override default {@link #fillCompletionVariants(CompletionParameters, CompletionResultSet)} implementation
 * and provide your own. It's easier to debug, but harder to write. Remember, that completion variant collection is done in a dedicated thread
 * WITHOUT read action, so you'll have to manually invoke {@link Application#runReadAction(Runnable)} each time
 * you access PSI. Don't spend long time inside read action, since this will prevent user from selecting lookup element or cancelling completion.<p>
 * <p>
 * Q: What does the {@link CompletionParameters#getPosition()} return?<br>
 * A: When completion is invoked, the file being edited is first copied (the original file can be accessed from {@link PsiFile#getOriginalFile()}
 * and {@link CompletionParameters#getOriginalFile()}. Then a special 'dummy identifier' string is inserted to the copied file at caret offset (removing the selection).
 * Most often this string is an identifier (see {@link consulo.ide.impl.idea.codeInsight.completion.CompletionInitializationContext#DUMMY_IDENTIFIER}).
 * This is usually done to guarantee that there'll always be some non-empty element there, which will be easy to describe via {@link ElementPattern}s.
 * Also a reference can suddenly appear in that position, which will certainly help invoking its {@link PsiReference#getVariants()}.
 * Dummy identifier string can be easily changed in {@link #beforeCompletion(CompletionInitializationContext)} method.<p>
 * <p>
 * Q: How do I get automatic lookup element filtering by prefix?<br>
 * A: When you return variants from reference ({@link PsiReference#getVariants()}), the filtering will be done
 * automatically, with prefix taken as the reference text from its start ({@link PsiReference#getRangeInElement()}) to
 * the caret position.
 * In {@link CompletionContributor} you will be given a {@link consulo.ide.impl.idea.codeInsight.completion.CompletionResultSet}
 * which will match {@link LookupElement}s against its prefix matcher {@link CompletionResultSet#getPrefixMatcher()}.
 * If the default prefix calculated by IntelliJ IDEA doesn't satisfy you, you can obtain another result set via
 * {@link consulo.ide.impl.idea.codeInsight.completion.CompletionResultSet#withPrefixMatcher(PrefixMatcher)} and feed your lookup elements to the latter.
 * It's one of the item's lookup strings ({@link LookupElement#getAllLookupStrings()} that is matched against prefix matcher.<p>
 * <p>
 * Q: How do I plug into those funny texts below the items in shown lookup?<br>
 * A: Use {@link CompletionResultSet#addLookupAdvertisement(String)} <p>
 * <p>
 * Q: How do I change the text that gets shown when there are no suitable variants at all? <br>
 * A: Use {@link CompletionContributor#handleEmptyLookup(CompletionParameters, Editor)}.
 * Don't forget to check whether you are in correct place (see {@link CompletionParameters}).<p>
 * <p>
 * Q: How do I affect lookup element's appearance (icon, text attributes, etc.)?<br>
 * A: See {@link LookupElement#renderElement(LookupElementPresentation)}.<p>
 * <p>
 * Q: I'm not satisfied that completion just inserts the item's lookup string on item selection. How make IDEA write something else?<br>
 * A: See {@link LookupElement#handleInsert(InsertionContext)}.<p>
 * <p>
 * Q: What if I select item with a Tab key?<br>
 * A: Semantics is, that the identifier that you're standing inside gets removed completely, and then the lookup string is inserted. You can change
 * the deleting range end offset, do it in {@link CompletionContributor#beforeCompletion(CompletionInitializationContext)}
 * by putting new offset to {@link CompletionInitializationContext#getOffsetMap()} as {@link consulo.ide.impl.idea.codeInsight.completion.CompletionInitializationContext#IDENTIFIER_END_OFFSET}.<p>
 * <p>
 * Q: I know about my environment more than IDEA does, and I can swear that those 239 variants that IDEA suggest me in some place aren't all that relevant,
 * so I'd be happy to filter out 42 of them. How do I do this?<br>
 * A: This is a bit harder than just adding variants. First, you should invoke
 * {@link consulo.ide.impl.idea.codeInsight.completion.CompletionResultSet#runRemainingContributors(CompletionParameters, consulo.ide.impl.idea.util.Consumer)}.
 * The consumer you provide should pass all the lookup elements to the {@link consulo.ide.impl.idea.codeInsight.completion.CompletionResultSet}
 * given to you, except for the ones you wish to filter out. Be careful: it's too easy to break completion this way. Since you've
 * ordered to invoke remaining contributors yourself, they won't be invoked automatically after yours finishes (see
 * {@link CompletionResultSet#stopHere()} and {@link CompletionResultSet#isStopped()}).
 * Calling {@link CompletionResultSet#stopHere()} explicitly will stop other contributors, that happened to be loaded after yours,
 * from execution, and the user will never see their so useful and precious completion variants, so please be careful with this method.<p>
 * <p>
 * Q: How are the lookup elements sorted?<br>
 * A: Basically in lexicographic order, ascending, by lookup string ({@link LookupElement#getLookupString()}. But some of elements
 * may be considered more relevant, i.e. having a bigger probability of being chosen by user. Such elements (no more than 5) may be moved to
 * the top of lookup and highlighted with green background. This is done by hooking into lookup elements comparator via creating your own
 * {@link CompletionWeigher} and registering it as a "weigher" extension under "completion" key.<p>
 * <p>
 * Q: My completion is not working! How do I debug it?<br>
 * A: One source of common errors is that the pattern you gave to {@link #extend(CompletionType, ElementPattern, CompletionProvider)} method
 * may be incorrect. To debug this problem you can still override {@link #fillCompletionVariants(CompletionParameters, CompletionResultSet)} in
 * your contributor, make it only call its super and put a breakpoint there.<br>
 * If you want to know which contributor added a particular lookup element, the best place for a breakpoint will be
 * {@link CompletionService#performCompletion(CompletionParameters, Consumer)}. The consumer passed there
 * is the 'final' consumer, it will pass your lookup elements directly to the lookup.<br>
 * If your contributor isn't even invoked, probably there was another contributor that said 'stop' to the system, and yours happened to be ordered after
 * that contributor. To test this hypothesis, put a breakpoint to
 * {@link CompletionService#getVariantsFromContributors(CompletionParameters, CompletionContributor, java.util.function.Consumer)},
 * to the 'return false' line.<p>
 *
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CompletionContributor implements LanguageExtension {
  private static final ExtensionPointCacheKey<CompletionContributor, ByLanguageValue<List<CompletionContributor>>> KEY =
          ExtensionPointCacheKey.create("CompletionContributor", LanguageOneToMany.build(true));

  @Nonnull
  public static List<CompletionContributor> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(CompletionContributor.class).getOrBuildCache(KEY).requiredGet(language);
  }

  private final MultiMap<CompletionType, Pair<ElementPattern<? extends PsiElement>, CompletionProvider>> myMap = new MultiMap<>();

  public final void extend(@Nullable CompletionType type, final ElementPattern<? extends PsiElement> place, CompletionProvider provider) {
    myMap.putValue(type, Pair.create(place, provider));
  }

  /**
   * The main contributor method that is supposed to provide completion variants to result, basing on completion parameters.
   * The default implementation looks for {@link CompletionProvider}s you could register by
   * invoking {@link #extend(CompletionType, ElementPattern, CompletionProvider)} from you contributor constructor,
   * matches the desired completion type and {@link ElementPattern} with actual ones, and, depending on it, invokes those
   * completion providers.<p>
   * <p>
   * If you want to implement this functionality directly by overriding this method, the following is for you.
   * Always check that parameters match your situation, and that completion type ({@link CompletionParameters#getCompletionType()}
   * is of your favourite kind. This method is run outside of read action, so you have to manage this manually
   * ({@link Application#runReadAction(Runnable)}). Don't take read actions for too long.<p>
   *
   * @param parameters
   * @param result
   */
  @RequiredReadAction
  public void fillCompletionVariants(final CompletionParameters parameters, CompletionResultSet result) {
    for (final Pair<ElementPattern<? extends PsiElement>, CompletionProvider> pair : myMap.get(parameters.getCompletionType())) {
      final ProcessingContext context = new ProcessingContext();
      if (pair.first.accepts(parameters.getPosition(), context)) {
        pair.second.addCompletions(parameters, context, result);
        if (result.isStopped()) {
          return;
        }
      }
    }
    for (final Pair<ElementPattern<? extends PsiElement>, CompletionProvider> pair : myMap.get(null)) {
      final ProcessingContext context = new ProcessingContext();
      if (pair.first.accepts(parameters.getPosition(), context)) {
        pair.second.addCompletions(parameters, context, result);
        if (result.isStopped()) {
          return;
        }
      }
    }
  }

  /**
   * Invoked before completion is started. Is used mainly for determining custom offsets in editor, and to change default dummy identifier.
   *
   * @param context
   */
  public void beforeCompletion(@Nonnull CompletionInitializationContext context) {
  }

  /**
   * @param parameters
   * @return text to be shown at the bottom of lookup list
   * @deprecated use {@link consulo.ide.impl.idea.codeInsight.completion.CompletionResultSet#addLookupAdvertisement(String)}
   */
  @Nullable
  public String advertise(@Nonnull CompletionParameters parameters) {
    return null;
  }

  /**
   * @param parameters
   * @param editor
   * @return hint text to be shown if no variants are found, typically "No suggestions"
   */
  @Nullable
  public String handleEmptyLookup(@Nonnull CompletionParameters parameters, final Editor editor) {
    return null;
  }

  /**
   * Called when the completion is finished quickly, lookup hasn't been shown and gives possibility to autoinsert some item (typically - the only one).
   */
  @Nullable
  public AutoCompletionDecision handleAutoCompletionPossibility(AutoCompletionContext context) {
    return null;
  }

  /**
   * Allow autoPopup to appear after custom symbol
   */
  public boolean invokeAutoPopup(@Nonnull PsiElement position, char typeChar) {
    return false;
  }

  /**
   * Invoked in a read action in parallel to the completion process. Used to calculate the replacement offset
   * (see {@link consulo.ide.impl.idea.codeInsight.completion.CompletionInitializationContext#setReplacementOffset(int)})
   * if it takes too much time to spend it in {@link #beforeCompletion(CompletionInitializationContext)},
   * e.g. doing {@link PsiFile#findReferenceAt(int)}
   * <p>
   * Guaranteed to be invoked before any lookup element is selected
   *
   * @param context context
   */
  public void duringCompletion(@Nonnull CompletionInitializationContext context) {
  }

  /**
   * @param actionId
   * @return String representation of action shortcut. Useful while advertising something
   * @see #advertise(CompletionParameters)
   */
  public static String getActionShortcut(@NonNls final String actionId) {
    return KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(actionId));
  }

  public static List<CompletionContributor> forParameters(final CompletionParameters parameters) {
    return ApplicationManager.getApplication()
            .runReadAction((Supplier<List<CompletionContributor>>)() -> forLanguage(PsiUtilCore.getLanguageAtOffset(parameters.getPosition().getContainingFile(), parameters.getOffset())));
  }

  @Nonnull
  public static List<CompletionContributor> forLanguageHonorDumbness(@Nonnull Language language, @Nonnull Project project) {
    return DumbService.getInstance(project).filterByDumbAwareness(forLanguage(language));
  }
}
