// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.util.gotoByName;

import consulo.application.AccessRule;
import consulo.application.progress.ProgressIndicator;
import consulo.application.util.NotNullLazyValue;
import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.NameUtil;
import consulo.application.util.matcher.WordPrefixMatcher;
import consulo.application.util.registry.Registry;
import consulo.configurable.SearchableOptionsRegistrar;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.SearchTopHitProvider;
import consulo.ide.impl.idea.ide.actions.ApplyIntentionAction;
import consulo.ide.impl.idea.ide.ui.OptionsTopHitProvider;
import consulo.ide.impl.idea.ide.ui.search.ActionFromOptionDescriptorProvider;
import consulo.ide.impl.idea.ide.ui.search.OptionDescription;
import consulo.ide.impl.idea.ide.ui.search.SearchableOptionsRegistrarImpl;
import consulo.ide.impl.idea.openapi.actionSystem.AbbreviationManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.util.CollectConsumer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.QuickActionProvider;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Predicate;

import static consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel.*;

/**
 * @author peter
 */
public class GotoActionItemProvider implements ChooseByNameItemProvider {
    private final ActionManager myActionManager = ActionManager.getInstance();
    private final GotoActionModel myModel;
    private final NotNullLazyValue<Map<String, ApplyIntentionAction>> myIntentions;

    public GotoActionItemProvider(GotoActionModel model) {
        myModel = model;
        myIntentions = NotNullLazyValue.createValue(() -> AccessRule.read(myModel::getAvailableIntentions));
    }

    @Nonnull
    @Override
    public List<String> filterNames(@Nonnull ChooseByNameBase base, @Nonnull String[] names, @Nonnull String pattern) {
        return Collections.emptyList(); // no common prefix insertion in goto action
    }

    @Override
    public boolean filterElements(
        @Nonnull ChooseByNameBase base,
        @Nonnull String pattern,
        boolean everywhere,
        @Nonnull ProgressIndicator cancelled,
        @Nonnull Predicate<Object> consumer
    ) {
        return filterElements(
            pattern,
            value -> !everywhere && value.value instanceof ActionWrapper actionWrapper && !actionWrapper.isAvailable()
                || consumer.test(value)
        );
    }

    public boolean filterElements(@Nonnull String pattern, @Nonnull Predicate<? super MatchedValue> consumer) {
        DataContext dataContext = DataManager.getInstance().getDataContext(myModel.getContextComponent());

        return processAbbreviations(pattern, consumer, dataContext)
            && processIntentions(pattern, consumer, dataContext)
            && processActions(pattern, consumer, dataContext)
            && (Registry.is("goto.action.skip.tophits.and.options")
            || processTopHits(pattern, consumer, dataContext)
            && processOptions(pattern, consumer, dataContext));
        }

    private boolean processAbbreviations(@Nonnull String pattern, Predicate<? super MatchedValue> consumer, DataContext context) {
        List<String> actionIds = AbbreviationManager.getInstance().findActions(pattern);
        JBIterable<MatchedValue> wrappers = JBIterable.from(actionIds).filterMap(myActionManager::getAction).transform(action -> {
            ActionWrapper wrapper = wrapAnAction(action, context);
            return new MatchedValue(wrapper, pattern) {
                @Nonnull
                @Override
                public String getValueText() {
                    return pattern;
                }
            };
        });
        return processItems(pattern, wrappers, consumer);
    }

    private boolean processTopHits(String pattern, Predicate<? super MatchedValue> consumer, DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        CollectConsumer<Object> collector = new CollectConsumer<>();
        for (SearchTopHitProvider provider : SearchTopHitProvider.EP_NAME.getExtensionList()) {
            if (provider instanceof OptionsTopHitProvider.CoveredByToggleActions) {
                continue;
            }
            if (provider instanceof OptionsTopHitProvider topHitProvider && !topHitProvider.isEnabled(project)) {
                continue;
            }
            if (provider instanceof OptionsTopHitProvider topHitProvider && !StringUtil.startsWith(pattern, "#")) {
                String prefix = "#" + topHitProvider.getId() + " ";
                provider.consumeTopHits(prefix + pattern, collector, project);
            }
            provider.consumeTopHits(pattern, collector, project);
        }
        Collection<Object> result = collector.getResult();
        List<Comparable> c = new ArrayList<>();
        for (Object o : result) {
            if (o instanceof Comparable comparable) {
                c.add(comparable);
            }
        }
        return processItems(pattern, JBIterable.from(c), consumer);
    }

    private boolean processOptions(String pattern, Predicate<? super MatchedValue> consumer, DataContext dataContext) {
        Map<String, String> map = myModel.getConfigurablesNames();
        SearchableOptionsRegistrarImpl registrar = (SearchableOptionsRegistrarImpl)SearchableOptionsRegistrar.getInstance();

        List<Object> options = new ArrayList<>();
        Set<String> words = registrar.getProcessedWords(pattern);
        Set<OptionDescription> optionDescriptions = null;
        String actionManagerName = "ActionManager";
        for (String word : words) {
            Set<OptionDescription> descriptions = registrar.getAcceptableDescriptions(word);
            if (descriptions != null) {
                descriptions.removeIf(description -> actionManagerName.equals(description.getPath()));
                if (!descriptions.isEmpty()) {
                    if (optionDescriptions == null) {
                        optionDescriptions = descriptions;
                    }
                    else {
                        optionDescriptions.retainAll(descriptions);
                    }
                }
            }
            else {
                optionDescriptions = null;
                break;
            }
        }
        if (!StringUtil.isEmptyOrSpaces(pattern)) {
            Matcher matcher = buildMatcher(pattern);
            if (optionDescriptions == null) {
                optionDescriptions = new HashSet<>();
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (matcher.matches(entry.getValue())) {
                    optionDescriptions.add(new OptionDescription(null, entry.getKey(), entry.getValue(), null, entry.getValue()));
                }
            }
        }
        if (optionDescriptions != null && !optionDescriptions.isEmpty()) {
            Set<String> currentHits = new HashSet<>();
            for (Iterator<OptionDescription> iterator = optionDescriptions.iterator(); iterator.hasNext(); ) {
                OptionDescription description = iterator.next();
                String hit = description.getHit();
                if (hit == null || !currentHits.add(hit.trim())) {
                    iterator.remove();
                }
            }
            for (OptionDescription description : optionDescriptions) {
                for (ActionFromOptionDescriptorProvider converter : ActionFromOptionDescriptorProvider.EP.getExtensionList()) {
                    AnAction action = converter.provide(description);
                    if (action != null) {
                        options.add(new ActionWrapper(action, null, MatchMode.NAME, dataContext, myModel));
                    }
                    options.add(description);
                }
            }
        }
        return processItems(pattern, JBIterable.from(options), consumer);
    }

    private boolean processActions(String pattern, Predicate<? super MatchedValue> consumer, DataContext dataContext) {
        Set<String> ids = ((ActionManagerImpl)myActionManager).getActionIds();
        JBIterable<AnAction> actions = JBIterable.from(ids).filterMap(myActionManager::getAction);
        Matcher matcher = buildMatcher(pattern);

        QuickActionProvider provider = dataContext.getData(QuickActionProvider.KEY);
        if (provider != null) {
            actions = actions.append(provider.getActions(true));
        }

        JBIterable<ActionWrapper> actionWrappers = actions.unique().filterMap(action -> {
            MatchMode mode = myModel.actionMatches(pattern, matcher, action);
            if (mode == MatchMode.NONE) {
                return null;
            }
            return new ActionWrapper(action, myModel.getGroupMapping(action), mode, dataContext, myModel);
        });
        return processItems(pattern, actionWrappers, consumer);
    }

    @Nonnull
    static Matcher buildMatcher(String pattern) {
        return pattern.contains(" ")
            ? new WordPrefixMatcher(pattern)
            : NameUtil.buildMatcher("*" + pattern, NameUtil.MatchingCaseSensitivity.NONE);
    }

    private boolean processIntentions(String pattern, Predicate<? super MatchedValue> consumer, DataContext dataContext) {
        Matcher matcher = buildMatcher(pattern);
        Map<String, ApplyIntentionAction> intentionMap = myIntentions.getValue();
        JBIterable<ActionWrapper> intentions = JBIterable.from(intentionMap.keySet()).filterMap(intentionText -> {
            ApplyIntentionAction intentionAction = intentionMap.get(intentionText);
            if (myModel.actionMatches(pattern, matcher, intentionAction) == MatchMode.NONE) {
                return null;
            }
            GroupMapping groupMapping = GroupMapping.createFromText(intentionText);
            return new ActionWrapper(intentionAction, groupMapping, MatchMode.INTENTION, dataContext, myModel);
        });
        return processItems(pattern, intentions, consumer);
    }

    @Nonnull
    private ActionWrapper wrapAnAction(@Nonnull AnAction action, DataContext dataContext) {
        return new ActionWrapper(action, myModel.getGroupMapping(action), MatchMode.NAME, dataContext, myModel);
    }

    private static final Logger LOG = Logger.getInstance(GotoActionItemProvider.class);

    private static boolean processItems(String pattern, JBIterable<?> items, Predicate<? super MatchedValue> consumer) {
        List<MatchedValue> matched = ContainerUtil.newArrayList(
            items.map(o -> o instanceof MatchedValue matchedValue ? matchedValue : new MatchedValue(o, pattern))
        );
        try {
            Collections.sort(matched, MatchedValue::compareWeights);
        }
        catch (IllegalArgumentException e) {
            LOG.error("Comparison method violates its general contract with pattern '" + pattern + "'", e);
        }
        return ContainerUtil.process(matched, consumer);
    }
}
