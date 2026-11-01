/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.spellcheker.text;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.component.util.ModificationTracker;
import consulo.component.util.PluginExceptionUtil;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;

import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.file.FileViewProvider;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.SyntaxTraverser;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static consulo.util.collection.ContainerUtil.createConcurrentWeakKeyWeakValueMap;

/**
 * An extension specifying how to extract natural language text from PSI for specific programming languages.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class TextExtractor implements LanguageExtension {
    private static final Logger LOG = Logger.getInstance(TextExtractor.class);

    /**
     * The PSI element for which a {@link TextContent} was requested via {@link #findTextsAt}.
     */
    public static final Key<PsiElement> EXTRACTOR_SOURCE = Key.create("TextContent extractor source element");
    private static final ExtensionPointCacheKey<TextExtractor, ByLanguageValue<TextExtractor>> KEY =
        ExtensionPointCacheKey.create("TextExtractor", LanguageOneToOne.build());
    private static final Key<CachedValue<Cache>> COMMON_PARENT_CACHE = Key.create("TextExtractor common parent cache");
    private static final Key<CachedValue<AtomicReference<List<TextContent>>>> QUERY_CACHE = Key.create("TextExtractor query cache");
    private static final Key<Boolean> IGNORED = Key.create("TextExtractor ignored");
    private static final Key<CachedValue<Map<TextContent, TextContent>>> CONTENT_INTERNER = Key.create("TextExtractor interner");

    /**
     * @return the text extractor registered for the given language (or one of its base languages), if any
     */
    public static @Nullable TextExtractor forLanguage(Language language) {
        return Application.get().getExtensionPoint(TextExtractor.class).getOrBuildCache(KEY).get(language);
    }

    /**
     * <h2>Text extracting</h2>
     * Extract text from the given PSI element, if possible.
     * The returned text is most often fully embedded in {@code element},
     * but it may also include other PSI elements (e.g., adjacent comments).
     * In the latter case, this extension should return an equal {@link TextContent} for every one of those adjacent elements.
     *
     * <h2>Typical usage</h2>
     * <pre><code class="java">TextContentBuilder.FromPsi.build(element, textDomain)</code></pre>
     *
     * <h2>Common Pitfalls to Avoid</h2>
     * <ul>
     * <li><b>Adjacent elements text extraction:</b> Extracting text from adjacent elements must return identical results to avoid cache corruption.</li>
     * <li><b>IDE:</b> Ignore IDE non-textual fragments like references or JSON Schemas. For example, {@code JavaTextExtractor#shouldBeIgnored}</li>
     * <li><b>Forgotten exclusions:</b> There are three different types of exclusions: {@code excluding}, {@code withUnknown}, {@code withMarkup}.
     * Lack of exclusions can cause unintended code or markup fragments being analyzed as part of text, leading to false positives.
     * Read more at {@link TextContent.ExclusionKind} and {@link TextContent#excludeRanges(List)}</li>
     * <li><b>Escapes:</b> {@code \n}, {@code \t}, escaped quotes etc. should be normalized to match language behavior.
     * {@code \n}, {@code \t} should be replaced with whitespaces and the rest marked as unknown fragments.</li>
     * </ul>
     * <p>
     *
     * @param allowedDomains the set of the text domains that are expected by the caller.
     * @see TextContentBuilder
     * @see #buildTextContents
     */
    protected @Nullable TextContent buildTextContent(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        throw new UnsupportedOperationException("Please implement either buildTextContent or buildTextContents");
    }

    /**
     * Same as {@link #buildTextContent}, but may return several texts intersecting (most often embedded into) the given PSI element.
     * If any of those contents include other PSI elements (e.g. adjacent comments),
     * this extension should return equal {@code List<TextContent>} for all such "other" PSI elements.
     */
    protected List<TextContent> buildTextContents(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        return ContainerUtil.createMaybeSingletonList(buildTextContent(element, allowedDomains));
    }

    /**
     * Find a text content compatible with the given domain set intersecting with the given offset in a file.
     */
    public static @Nullable TextContent findTextAt(PsiFile file, int offset, Set<TextContent.TextDomain> allowedDomains) {
        PsiElement leaf = file.findElementAt(offset);
        TextContent result = leaf == null ? null :
            ContainerUtil.find(findTextsAt(leaf, allowedDomains), c -> c.fileOffsetToText(offset) != null);
        if (result != null) {
            return result;
        }
        if (leaf == null || offset == leaf.getTextRange().getStartOffset()) {
            leaf = file.findElementAt(offset - 1);
            if (leaf != null) {
                return ContainerUtil.find(findTextsAt(leaf, allowedDomains), c -> c.fileOffsetToText(offset) != null);
            }
        }
        return null;
    }

    /**
     * @return text contents intersecting the given PSI element with the domains from the allowed set.
     * <p>
     * Same as {@link #findTextsAt}, but the extensions are queried only for the given {@code psi}. The results are cached and reused.
     * @deprecated {@link #findTextsAt} or {@link #findUniqueTextsAt} should be used instead.
     */
    @SuppressWarnings("unused")
    @Deprecated(forRemoval = true)
    public static List<TextContent> findTextsExactlyAt(PsiElement psi, Set<TextContent.TextDomain> allowedDomains) {
        PsiFile file = psi.getContainingFile();
        return ContainerUtil.filter(
            obtainContents(file, psi),
            c -> Boolean.FALSE.equals(c.getUserData(IGNORED)) && allowedDomains.contains(c.getDomain())
        );
    }

    /**
     * @return text contents intersecting the given PSI element with the domains from the allowed set.
     * The extensions are queried for the given {@code psi} and its parents, the results are cached and reused.
     */
    public static List<TextContent> findTextsAt(PsiElement psi, Set<TextContent.TextDomain> allowedDomains) {
        TextRange psiRange = psi.getTextRange();
        PsiFile file = null;
        for (PsiElement each = psi; each != null; each = each.getParent()) {
            CachedValue<Cache> cache = each.getUserData(COMMON_PARENT_CACHE);
            if (cache != null) {
                List<TextContent> textsInRange = ContainerUtil.filter(
                    cache.getValue().getCached(),
                    c -> c.intersectsRange(psiRange)
                );
                if (!textsInRange.isEmpty()) {
                    return ContainerUtil.filter(
                        textsInRange,
                        c -> allowedDomains.contains(c.getDomain()) && Boolean.FALSE.equals(c.getUserData(IGNORED))
                    );
                }
            }
            if (each instanceof PsiFile) {
                file = (PsiFile) each;
                break;
            }
        }

        for (PsiElement each = psi; each != null && each != file; each = each.getParent()) {
            RecursionGuard.StackStamp stamp = RecursionManager.markStack();

            List<TextContent> contents = obtainContents(file != null ? file : psi.getContainingFile(), each);
            contents.forEach(content -> content.putUserData(EXTRACTOR_SOURCE, psi));
            if (stamp.mayCacheNow() && !contents.isEmpty()) {
                Map<PsiElement, List<TextContent>> groups = new LinkedHashMap<>();
                for (TextContent content : contents) {
                    groups.computeIfAbsent(content.getCommonParent(), _ -> new ArrayList<>()).add(content);
                }
                groups.forEach((commonParent, group) -> obtainCommonParentCache(commonParent).register(group));
            }

            if (!contents.isEmpty()) {
                return ContainerUtil.filter(
                    contents,
                    c -> Boolean.FALSE.equals(c.getUserData(IGNORED)) && allowedDomains.contains(c.getDomain()) && c.intersectsRange(psiRange)
                );
            }
        }

        return Collections.emptyList();
    }

    private static AtomicReference<List<TextContent>> obtainQueryCache(PsiElement psi) {
        return obtainCache(psi, QUERY_CACHE, () -> new AtomicReference<>(null));
    }

    private static Cache obtainCommonParentCache(PsiElement psi) {
        return obtainCache(psi, COMMON_PARENT_CACHE, () -> new Cache());
    }

    private static <T> T obtainCache(PsiElement psi, Key<CachedValue<T>> key, Supplier<T> supplier) {
        TextContentModificationTrackerProvider provider = TextContentModificationTrackerProvider.forLanguage(psi.getLanguage());
        ModificationTracker providedTracker = provider != null ? provider.getModificationTracker(psi) : null;
        Object tracker = providedTracker != null ? providedTracker : PsiModificationTracker.MODIFICATION_COUNT;

        CachedValue<T> cache = CachedValuesManager.getManager(psi.getProject())
            .createCachedValue(() -> CachedValueProvider.Result.create(supplier.get(), tracker));
        cache = ((UserDataHolderEx) psi).putUserDataIfAbsent(key, cache);
        return cache.getValue();
    }

    private static Map<TextContent, TextContent> obtainInterner(PsiFile file) {
        CachedValue<Map<TextContent, TextContent>> cache = CachedValuesManager.getManager(file.getProject())
            .createCachedValue(() -> CachedValueProvider.Result.create(createConcurrentWeakKeyWeakValueMap(), file));
        cache = ((UserDataHolderEx) file).putUserDataIfAbsent(CONTENT_INTERNER, cache);
        return cache.getValue();
    }

    private static List<TextContent> obtainContents(PsiFile file, PsiElement psi) {
        CachedValue<AtomicReference<List<TextContent>>> cv = psi.getUserData(QUERY_CACHE);
        List<TextContent> cachedContent = cv != null ? cv.getValue().get() : null;
        if (cachedContent != null) {
            return cachedContent;
        }

        RecursionGuard.StackStamp stamp = RecursionManager.markStack();

        Language psiLanguage = psi.getLanguage();
        List<TextContent> contents = doExtract(psi, psiLanguage);
        Language fileLanguage = file.getLanguage();
        if (contents.isEmpty() && fileLanguage != psiLanguage) {
            contents = doExtract(psi, fileLanguage);
        }
        if (contents.isEmpty()) {
            return List.of();
        }

        // deduplicate equal contents created by different threads to avoid O(token_count) 'equals' checks later on
        Map<TextContent, TextContent> interner = obtainInterner(file);
        contents = ContainerUtil.map(contents, content -> interner.computeIfAbsent(content, _ -> content));

        for (TextContent content : contents) {
            if (content.getUserData(IGNORED) != null) {
                continue;
            }
            content.putUserData(IGNORED, shouldIgnore(content));
        }

        contents = ContainerUtil.map(contents, content -> excludeNonEditableFragments(content));

        if (stamp.mayCacheNow()) {
            obtainQueryCache(psi).compareAndSet(null, contents);
            cacheOnSiblings(psi, contents);
        }
        return contents;
    }

    private static boolean hasIntersectingInjection(TextContent content) {
        PsiFile file = content.getContainingFile();
        return InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, content.textOffsetToFile(0)) != null;
    }

    private static TextContent excludeNonEditableFragments(TextContent content) {
        if (Boolean.TRUE.equals(content.getUserData(IGNORED))) {
            return content;
        }
        PsiFile file = content.getContainingFile();
        Document document = file.getViewProvider().getDocument();
        if (!(document instanceof DocumentWindow documentWindow)) {
            return content;
        }
        List<TextRange> ranges = ContainerUtil.mapNotNull(
            InjectedLanguageManager.getInstance(file.getProject()).getNonEditableFragments(documentWindow),
            range -> content.fileRangeToText(range)
        );
        if (ranges.isEmpty()) {
            return content;
        }
        return content.excludeRanges(ContainerUtil.map(ranges, range -> TextContent.Exclusion.exclude(range)));
    }

    private static void cacheOnSiblings(PsiElement psi, List<TextContent> contents) {
        contents.forEach(content -> {
            int startOffset = content.textOffsetToFile(0);
            int endOffset = content.textOffsetToFile(content.length());
            TextRange psiRangeInFile = psi.getTextRange();
            if (psiRangeInFile.getStartOffset() > startOffset || psiRangeInFile.getEndOffset() < endOffset) {
                for (ASTNode child : psi.getParent().getNode().getChildren(null)) {
                    PsiElement sibling = child.getPsi();
                    List<TextContent> intersectingContents = contents.stream()
                        .filter(it -> it.intersectsRange(sibling.getTextRange()))
                        .toList();
                    if (!intersectingContents.isEmpty()) {
                        obtainQueryCache(sibling).compareAndSet(null, intersectingContents);
                    }
                }
            }
        });
    }

    private static class Cache {
        private final Set<TextContent> myFoundContents = new LinkedHashSet<>();

        synchronized void register(List<TextContent> contents) {
            myFoundContents.addAll(contents);
        }

        synchronized List<TextContent> getCached() {
            return new ArrayList<>(myFoundContents);
        }
    }

    private static boolean shouldIgnore(TextContent content) {
        return hasIntersectingInjection(content)
            || content.getContainingFile().getApplication().getInstance(TextExtractionFilter.class).shouldIgnore(content);
    }

    /**
     * The same as {@link #findTextsAt}, but each text content is returned only once even if it covers several PSI elements
     * (one of those elements is chosen as an anchor).
     * That's useful if you iterate over PSI elements and want to process each of their contents just once
     * (e.g. during highlighting).
     */
    public static List<TextContent> findUniqueTextsAt(PsiElement psi, Set<TextContent.TextDomain> allowedDomains) {
        if (psi.getFirstChild() != null) {
            return Collections.emptyList();
        }

        TextRange psiRange = psi.getTextRange();
        return ContainerUtil.filter(findTextsAt(psi, allowedDomains), c -> psiRange.contains(c.textOffsetToFile(0)));
    }


    /**
     * Extract all text contents from a file view provider that match the specified domains.
     * Traverses through all PSI elements in all root files of the view provider and collects matching text contents.
     */
    public static Set<TextContent> findAllTextContents(FileViewProvider vp, Set<TextContent.TextDomain> domains) {
        Set<TextContent> allContents = new HashSet<>();
        for (PsiFile root : vp.getAllFiles()) {
            for (PsiElement element : SyntaxTraverser.psiTraverser(root)) {
                if (element instanceof PsiWhiteSpace) {
                    continue;
                }
                allContents.addAll(findTextsAt(element, domains));
            }
        }
        return allContents;
    }

    private static List<TextContent> doExtract(PsiElement anyRoot, Language language) {
        TextExtractor extractor = forLanguage(language);
        if (extractor != null) {
            List<TextContent> contents = extractor.buildTextContents(anyRoot, TextContent.TextDomain.ALL);
            for (TextContent content : contents) {
                if (!content.getCommonParent().getTextRange().contains(content.textRangeToFile(TextRange.from(0, content.length())))) {
                    PluginExceptionUtil.logPluginError(LOG, "Inconsistent text content", null, extractor.getClass());
                    return List.of();
                }
            }
            return contents;
        }


        return Collections.emptyList();
    }

    /**
     * @return all languages for which a text extractor is explicitly registered
     */
    public static Set<Language> getSupportedLanguages() {
        Set<Language> result = new HashSet<>();
        Application.get().getExtensionPoint(TextExtractor.class).forEachExtensionSafe(extractor -> result.add(extractor.getLanguage()));
        return result;
    }

    @TestOnly
    public List<TextContent> buildTextContentsTestAccessor(PsiElement element, Set<TextContent.TextDomain> allowedDomains) {
        return buildTextContents(element, allowedDomains);
    }
}
