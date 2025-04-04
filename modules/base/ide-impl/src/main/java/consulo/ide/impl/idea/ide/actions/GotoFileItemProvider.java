// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.application.util.matcher.FixingLayoutMatcher;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.ide.impl.idea.ide.actions.searcheverywhere.FoundItemDescriptor;
import consulo.ide.impl.idea.ide.util.gotoByName.*;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.navigation.GotoFileContributor;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.logging.Logger;
import consulo.module.content.ProjectFileIndex;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.collection.FList;
import consulo.util.collection.JBIterable;
import consulo.util.io.FileUtil;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author peter
 */
public class GotoFileItemProvider extends DefaultChooseByNameItemProvider {
    private static final Logger LOG = Logger.getInstance(GotoFileItemProvider.class);

    private static final int EXACT_MATCH_DEGREE = 5000;
    private static final int DIRECTORY_MATCH_DEGREE = 0;

    private final Project myProject;
    private final GotoFileModel myModel;

    public GotoFileItemProvider(@Nonnull Project project, @Nullable PsiElement context, GotoFileModel model) {
        super(context);
        myProject = project;
        myModel = model;
    }

    @Override
    public boolean filterElementsWithWeights(
        @Nonnull ChooseByNameBase base,
        @Nonnull FindSymbolParameters parameters,
        @Nonnull ProgressIndicator indicator,
        @Nonnull Processor<FoundItemDescriptor<?>> consumer
    ) {
        return ProgressManager.getInstance().computePrioritized(() -> doFilterElements(base, parameters, indicator, consumer));
    }

    private boolean doFilterElements(
        @Nonnull ChooseByNameBase base,
        @Nonnull FindSymbolParameters parameters,
        @Nonnull ProgressIndicator indicator,
        @Nonnull Processor<FoundItemDescriptor<?>> consumer
    ) {
        long start = System.currentTimeMillis();
        try {
            String pattern = parameters.getCompletePattern();
            PsiFileSystemItem absolute = getFileByAbsolutePath(pattern);
            if (absolute != null && !consumer.process(new FoundItemDescriptor<>(absolute, EXACT_MATCH_DEGREE))) {
                return true;
            }

            if (pattern.startsWith("./") || pattern.startsWith(".\\")) {
                parameters = parameters.withCompletePattern(pattern.substring(1));
            }

            if (!processItemsForPattern(base, parameters, consumer, indicator)) {
                return false;
            }
            String fixedPattern = FixingLayoutMatcher.fixLayout(pattern);
            return fixedPattern == null || processItemsForPattern(base, parameters.withCompletePattern(fixedPattern), consumer, indicator);
        }
        finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Goto File \"" + parameters.getCompletePattern() + "\" took " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }

    private boolean processItemsForPattern(
        @Nonnull ChooseByNameBase base,
        @Nonnull FindSymbolParameters parameters,
        @Nonnull Processor<FoundItemDescriptor<?>> consumer,
        @Nonnull ProgressIndicator indicator
    ) {
        String sanitized = getSanitizedPattern(parameters.getCompletePattern(), myModel);
        int qualifierEnd = sanitized.lastIndexOf('/') + 1;
        NameGrouper grouper = new NameGrouper(sanitized.substring(qualifierEnd), indicator);
        processNames(FindSymbolParameters.simple(myProject, true), grouper::processName);

        Ref<Boolean> hasSuggestions = Ref.create(false);
        DirectoryPathMatcher dirMatcher = DirectoryPathMatcher.root(myModel, sanitized.substring(0, qualifierEnd));
        while (dirMatcher != null) {
            int index = grouper.index;
            SuffixMatches group = grouper.nextGroup(base);
            if (group == null) {
                break;
            }
            if (!group.processFiles(parameters.withLocalPattern(dirMatcher.dirPattern), consumer, hasSuggestions, dirMatcher)) {
                return false;
            }
            dirMatcher = dirMatcher.appendChar(grouper.namePattern.charAt(index));
            if (!myModel.isSlashlessMatchingEnabled()) {
                return true;
            }
        }
        return true;
    }

    /**
     * Invoke contributors directly, as multi-threading isn't of much value in Goto File,
     * and filling {@link ContributorsBasedGotoByModel#myContributorToItsSymbolsMap} is expensive for the default contributor.
     */
    private void processNames(@Nonnull FindSymbolParameters parameters, @Nonnull Processor<? super String> nameProcessor) {
        List<GotoFileContributor> contributors =
            DumbService.getDumbAwareExtensions(myProject, myProject.getApplication().getExtensionPoint(GotoFileContributor.class));
        for (GotoFileContributor contributor : contributors) {
            if (contributor instanceof DefaultFileNavigationContributor) {
                FilenameIndex.processAllFileNames(
                    nameProcessor,
                    parameters.getSearchScope(), // todo why it was true?
                    parameters.getIdFilter()
                );
            }
            else {
                myModel.processContributorNames(contributor, parameters, nameProcessor);
            }
        }
    }

    @Nonnull
    public static String getSanitizedPattern(@Nonnull String pattern, GotoFileModel model) {
        return removeSlashes(StringUtil.replace(ChooseByNamePopup.getTransformedPattern(pattern, model), "\\", "/"));
    }

    @Nonnull
    public static MinusculeMatcher getQualifiedNameMatcher(@Nonnull String pattern) {
        pattern = "*" + StringUtil.replace(StringUtil.replace(pattern, "\\", "*\\*"), "/", "*/*");
        return NameUtil.buildMatcher(pattern).withCaseSensitivity(NameUtil.MatchingCaseSensitivity.NONE).preferringStartMatches().build();
    }

    @Nonnull
    private static String removeSlashes(String s) {
        return StringUtil.trimLeading(StringUtil.trimTrailing(s, '/'), '/');
    }

    @Nullable
    private PsiFileSystemItem getFileByAbsolutePath(@Nonnull String pattern) {
        if (pattern.contains("/") || pattern.contains("\\")) {
            String path = FileUtil.toSystemIndependentName(ChooseByNamePopup.getTransformedPattern(pattern, myModel));
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByPathIfCached(path);
            if (vFile != null) {
                ProjectFileIndex index = ProjectFileIndex.getInstance(myProject);
                if (index.isInContent(vFile) || index.isInLibrary(vFile)) {
                    return PsiUtilCore.findFileSystemItem(myProject, vFile);
                }
            }
        }
        return null;
    }

    private Iterable<FoundItemDescriptor<PsiFileSystemItem>> matchQualifiers(
        MinusculeMatcher qualifierMatcher,
        Iterable<? extends PsiFileSystemItem> iterable
    ) {
        List<FoundItemDescriptor<PsiFileSystemItem>> matching = new ArrayList<>();
        for (PsiFileSystemItem item : iterable) {
            ProgressManager.checkCanceled();
            String qualifier = Objects.requireNonNull(getParentPath(item));
            FList<MatcherTextRange> fragments = qualifierMatcher.matchingFragments(qualifier);
            if (fragments != null) {
                int gapPenalty = fragments.isEmpty() ? 0 : qualifier.length() - fragments.get(fragments.size() - 1).getEndOffset();
                int degree = qualifierMatcher.matchingDegree(qualifier, false, fragments) - gapPenalty;
                matching.add(new FoundItemDescriptor<>(item, degree));
            }
        }
        if (matching.size() > 1) {
            Comparator<FoundItemDescriptor<PsiFileSystemItem>> comparator =
                Comparator.comparing((FoundItemDescriptor<PsiFileSystemItem> res) -> res.getWeight()).reversed();
            Collections.sort(matching, comparator);
        }
        return matching;
    }

    @Nullable
    private String getParentPath(@Nonnull PsiFileSystemItem item) {
        String fullName = myModel.getFullName(item);
        return fullName == null ? null : StringUtil.getPackageName(FileUtil.toSystemIndependentName(fullName), '/') + '/';
    }

    private static JBIterable<FoundItemDescriptor<PsiFileSystemItem>> moveDirectoriesToEnd(
        Iterable<FoundItemDescriptor<PsiFileSystemItem>> iterable
    ) {
        List<FoundItemDescriptor<PsiFileSystemItem>> dirs = new ArrayList<>();
        return JBIterable.from(iterable).filter(res -> {
            if (res.getItem() instanceof PsiDirectory directory) {
                dirs.add(new FoundItemDescriptor<>(directory, DIRECTORY_MATCH_DEGREE));
                return false;
            }
            return true;
        }).append(dirs);
    }

    // returns a lazy iterable, where the next element is calculated only when it's needed
    @Nonnull
    private JBIterable<FoundItemDescriptor<PsiFileSystemItem>> getFilesMatchingPath(
        @Nonnull FindSymbolParameters parameters,
        @Nonnull List<MatchResult> fileNames,
        @Nonnull DirectoryPathMatcher dirMatcher,
        @Nonnull ProgressIndicator indicator
    ) {
        GlobalSearchScope scope = dirMatcher.narrowDown((GlobalSearchScope)parameters.getSearchScope());
        FindSymbolParameters adjusted = parameters.withScope(scope);

        List<List<MatchResult>> sortedNames =
            sortAndGroup(fileNames, Comparator.comparing(mr -> StringUtil.toLowerCase(FileUtil.getNameWithoutExtension(mr.elementName))));
        return JBIterable.from(sortedNames).flatMap(nameGroup -> getItemsForNames(indicator, adjusted, nameGroup));
    }

    private Iterable<FoundItemDescriptor<PsiFileSystemItem>> getItemsForNames(
        @Nonnull ProgressIndicator indicator,
        FindSymbolParameters parameters,
        List<MatchResult> matchResults
    ) {
        List<PsiFileSystemItem> group = new ArrayList<>();
        Map<PsiFileSystemItem, Integer> nesting = new HashMap<>();
        Map<PsiFileSystemItem, Integer> matchDegrees = new HashMap<>();
        for (MatchResult matchResult : matchResults) {
            ProgressManager.checkCanceled();
            for (Object o : myModel.getElementsByName(matchResult.elementName, parameters, indicator)) {
                ProgressManager.checkCanceled();
                if (o instanceof PsiFileSystemItem psiItem) {
                    String qualifier = getParentPath(psiItem);
                    if (qualifier != null) {
                        group.add(psiItem);
                        nesting.put(psiItem, StringUtil.countChars(qualifier, '/'));
                        matchDegrees.put(psiItem, matchResult.matchingDegree);
                    }
                }
            }
        }

        if (group.size() > 1) {
            Collections.sort(group, Comparator.<PsiFileSystemItem, Integer>comparing(nesting::get).
                thenComparing(getPathProximityComparator()).
                thenComparing(myModel::getFullName));
        }
        return ContainerUtil.map(group, item -> new FoundItemDescriptor<>(item, matchDegrees.get(item)));
    }

    /**
     * @return Minimal {@code pos} such that {@code candidateName} can potentially match {@code namePattern.substring(pos)}
     * (i.e. contains the same letters as a sub-sequence).
     * Matching attempts with longer pattern substrings certainly will fail.
     */
    private static int findMatchStartingPosition(String candidateName, String namePattern) {
        int namePos = candidateName.length();
        for (int i = namePattern.length(); i > 0; i--) {
            char c = namePattern.charAt(i - 1);
            if (Character.isLetterOrDigit(c)) {
                namePos = StringUtil.lastIndexOfIgnoreCase(candidateName, c, namePos - 1);
                if (namePos < 0) {
                    return i;
                }
            }
        }
        return 0;
    }

    private class NameGrouper {
        private final String namePattern;
        @Nonnull
        private final ProgressIndicator indicator;

        /**
         * Names placed into buckets where the index of bucket == {@link #findMatchStartingPosition}
         */
        private final List<List<String>> candidateNames;

        private int index;

        NameGrouper(@Nonnull String namePattern, @Nonnull ProgressIndicator indicator) {
            this.namePattern = namePattern;

            candidateNames = IntStream.range(0, namePattern.length())
                .mapToObj(__ -> new ArrayList<String>())
                .collect(Collectors.toList());
            this.indicator = indicator;
        }

        boolean processName(String name) {
            indicator.checkCanceled();
            int position = findMatchStartingPosition(name, namePattern);
            if (position < namePattern.length()) {
                candidateNames.get(position).add(name);
            }
            return true;
        }

        @Nullable
        SuffixMatches nextGroup(ChooseByNameBase base) {
            if (index >= namePattern.length()) {
                return null;
            }

            SuffixMatches matches = new SuffixMatches(namePattern, index, indicator);
            for (String name : candidateNames.get(index)) {
                if (!matches.matchName(base, name) && index + 1 < namePattern.length()) {
                    candidateNames.get(index + 1).add(name); // try later with a shorter matcher
                }
            }
            candidateNames.set(index, null);
            index++;
            return matches;
        }
    }

    private class SuffixMatches {
        final String patternSuffix;
        final MinusculeMatcher matcher;
        final List<MatchResult> matchingNames = new ArrayList<>();
        final ProgressIndicator indicator;

        SuffixMatches(String pattern, int from, @Nonnull ProgressIndicator indicator) {
            patternSuffix = pattern.substring(from);
            matcher = NameUtil.buildMatcher((from > 0 ? " " : "*") + patternSuffix, NameUtil.MatchingCaseSensitivity.NONE);
            this.indicator = indicator;
        }

        @Override
        public String toString() {
            return "SuffixMatches{" + "patternSuffix='" + patternSuffix + '\'' + ", matchingNames=" + matchingNames + '}';
        }

        boolean matchName(@Nonnull ChooseByNameBase base, String name) {
            MatchResult result = matches(base, patternSuffix, matcher, name);
            if (result != null) {
                matchingNames.add(result);
                return true;
            }
            return false;
        }

        boolean processFiles(
            @Nonnull FindSymbolParameters parameters,
            @Nonnull Processor<FoundItemDescriptor<?>> processor,
            @Nonnull Ref<Boolean> hasSuggestions,
            @Nonnull DirectoryPathMatcher dirMatcher
        ) {
            MinusculeMatcher qualifierMatcher = getQualifiedNameMatcher(parameters.getLocalPatternName());

            List<MatchResult> matchingNames = this.matchingNames;
            if (patternSuffix.length() <= 3 && !dirMatcher.dirPattern.isEmpty()) {
                // just enumerate over files
                // otherwise there are too many names matching the remaining few letters,
                // and querying index for all of them with a very constrained scope is expensive
                Set<String> existingNames = dirMatcher.findFileNamesMatchingIfCheap(patternSuffix.charAt(0), matcher);
                if (existingNames != null) {
                    matchingNames = ContainerUtil.filter(matchingNames, mr -> existingNames.contains(mr.elementName));
                }
            }

            List<List<MatchResult>> groups = groupByMatchingDegree(!parameters.getCompletePattern().startsWith("*"), matchingNames);
            for (List<MatchResult> group : groups) {
                JBIterable<FoundItemDescriptor<PsiFileSystemItem>> filesMatchingPath =
                    getFilesMatchingPath(parameters, group, dirMatcher, indicator);
                Iterable<FoundItemDescriptor<PsiFileSystemItem>> matchedFiles =
                    parameters.getLocalPatternName().isEmpty() ? filesMatchingPath : matchQualifiers(
                        qualifierMatcher,
                        filesMatchingPath.map(FoundItemDescriptor::getItem)
                    );

                matchedFiles = moveDirectoriesToEnd(matchedFiles);
                Processor<FoundItemDescriptor<PsiFileSystemItem>> trackingProcessor = res -> {
                    hasSuggestions.set(true);
                    return processor.process(res);
                };
                if (!ContainerUtil.process(matchedFiles, trackingProcessor)) {
                    return false;
                }
            }

            // let the framework switch to searching outside project to display these well-matching suggestions
            // instead of worse-matching ones in project (that are very expensive to calculate)
            return hasSuggestions.get() || parameters.isSearchInLibraries()
                || !hasSuggestionsOutsideProject(parameters.getCompletePattern(), groups, dirMatcher);
        }

        private boolean hasSuggestionsOutsideProject(
            @Nonnull String pattern,
            @Nonnull List<List<MatchResult>> groups,
            @Nonnull DirectoryPathMatcher dirMatcher
        ) {
            return ContainerUtil.exists(
                groups,
                group -> !getFilesMatchingPath(FindSymbolParameters.wrap(pattern, myProject, true), group, dirMatcher, indicator)
                    .isEmpty()
            );
        }

        private List<List<MatchResult>> groupByMatchingDegree(boolean preferStartMatches, List<MatchResult> matchingNames) {
            Comparator<MatchResult> comparator = (mr1, mr2) -> {
                boolean exactPrefix1 = StringUtil.startsWith(mr1.elementName, patternSuffix);
                boolean exactPrefix2 = StringUtil.startsWith(mr2.elementName, patternSuffix);
                if (exactPrefix1 && exactPrefix2) {
                    return 0;
                }
                if (exactPrefix1 != exactPrefix2) {
                    return exactPrefix1 ? -1 : 1;
                }
                return mr1.compareDegrees(mr2, preferStartMatches);
            };

            return sortAndGroup(matchingNames, comparator);
        }
    }

    private static <T> List<List<T>> sortAndGroup(@Nonnull List<T> items, @Nonnull Comparator<? super T> comparator) {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort(comparator);

        List result = new ArrayList<>();
        ContainerUtil.groupAndRuns(sorted, (n1, n2) -> comparator.compare(n1, n2) == 0, ts -> result.add(ts));
        return result;
    }
}
