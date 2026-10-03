package consulo.endpoint.impl.internal.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.component.ProcessCanceledException;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointModuleEntity;
import consulo.endpoint.EndpointProjectModel;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointType;
import consulo.endpoint.ExternalEndpointFilter;
import consulo.endpoint.FrameworkPresentation;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.scope.NonProjectFilesScope;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class EndpointLoader {
    private static final Logger LOG = Logger.getInstance(EndpointLoader.class);

    private final Project myProject;
    private final EndpointViewManager myManager;
    private final ConcurrentMap<EndpointCacheKey, EndpointCacheEntry> myCache = new ConcurrentHashMap<>();
    private final AtomicLong myGeneration = new AtomicLong();

    public EndpointLoader(Project project, EndpointViewManager manager) {
        myProject = project;
        myManager = manager;
    }

    public void flush() {
        myGeneration.incrementAndGet();
        myCache.clear();
    }

    @RequiredReadAction
    public EndpointSnapshot load(EndpointQuery query) {
        long generation = myGeneration.get();
        List<EndpointProvider<?, ?>> providers = EndpointProvider.getAvailableProviders(myProject);

        if (providers.isEmpty()) {
            myCache.clear();
            return EndpointSnapshot.EMPTY;
        }

        Map<String, EndpointFilterChoice> types = new LinkedHashMap<>();
        Map<String, EndpointFilterChoice> frameworks = new LinkedHashMap<>();
        List<EndpointProviderInfo> infos = new ArrayList<>(providers.size());
        for (EndpointProvider<?, ?> provider : providers) {
            EndpointType type;
            FrameworkPresentation framework;
            try {
                type = provider.getEndpointType();
                framework = provider.getPresentation();
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
                continue;
            }

            infos.add(new EndpointProviderInfo(provider, type, framework));
            types.putIfAbsent(type.getQueryTag(), new EndpointFilterChoice(type.getQueryTag(), type.getLocalizedMessage(), type.getIcon()));
            frameworks.putIfAbsent(
                framework.getQueryTag(),
                new EndpointFilterChoice(framework.getQueryTag(), LocalizeValue.of(framework.getTitle()), framework.getIcon())
            );
        }

        EndpointProjectModel model = myManager.getProjectModel();
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();

        List<EndpointModuleEntity> entities = new ArrayList<>();
        for (EndpointModuleEntity entity : model.getModuleEntities()) {
            if (query.isFromTests() || !model.isTestModule(entity)) {
                entities.add(entity);
            }
        }
        entities.sort(Comparator.comparing(EndpointModuleEntity::getName, String.CASE_INSENSITIVE_ORDER));

        List<EndpointFilterChoice> moduleChoices = new ArrayList<>(entities.size());
        Set<EndpointCacheKey> usedKeys = new HashSet<>();
        List<EndpointModuleSnapshot> sections = new ArrayList<>();
        for (EndpointModuleEntity entity : entities) {
            String name = entity.getName();
            moduleChoices.add(new EndpointFilterChoice(name, LocalizeValue.of(name), entity.getIcon()));
            if (query.getHiddenModules().contains(name)) {
                continue;
            }

            EndpointFilter filter;
            try {
                filter = model.createFilter(entity, query.isFromLibraries(), query.isFromTests());
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
                continue;
            }

            List<EndpointRowData<?, ?>> rows = new ArrayList<>();
            for (EndpointProviderInfo info : infos) {
                rows.addAll(getRows(info, filter, scheme, null, usedKeys, generation));
            }
            sections.add(new EndpointModuleSnapshot(entity, name, LocalizeValue.of(name), entity.getIcon(), List.copyOf(rows)));
        }

        if (query.isShowExternal()) {
            ColorValue background = FileColorManager.getInstance(myProject).getScopeColorValue(NonProjectFilesScope.NAME);
            List<EndpointRowData<?, ?>> rows = new ArrayList<>();
            for (EndpointProviderInfo info : infos) {
                rows.addAll(getRows(info, ExternalEndpointFilter.INSTANCE, scheme, background, usedKeys, generation));
            }
            if (!rows.isEmpty()) {
                sections.add(new EndpointModuleSnapshot(
                    null,
                    EndpointModuleSnapshot.EXTERNAL_KEY,
                    EndpointLocalize.frameworksFiltersModuleExternal(),
                    PlatformIconGroup.generalWeb(),
                    List.copyOf(rows)
                ));
            }
        }

        myCache.keySet().retainAll(usedKeys);

        return new EndpointSnapshot(
            true,
            List.copyOf(sections),
            List.copyOf(moduleChoices),
            List.copyOf(types.values()),
            List.copyOf(frameworks.values())
        );
    }

    @RequiredReadAction
    private List<EndpointRowData<?, ?>> getRows(
        EndpointProviderInfo info,
        EndpointFilter filter,
        EditorColorsScheme scheme,
        @Nullable ColorValue background,
        Set<EndpointCacheKey> usedKeys,
        long generation
    ) {
        EndpointProvider<?, ?> provider = info.provider();
        long stamp;
        try {
            stamp = provider.getModificationTracker().getModificationCount();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
            return List.of();
        }

        EndpointCacheKey key = new EndpointCacheKey(provider, filter);
        usedKeys.add(key);

        EndpointCacheEntry entry = myCache.get(key);
        if (entry != null && entry.stamp() == stamp && entry.generation() == generation) {
            return entry.rows();
        }

        List<EndpointRowData<?, ?>> rows = computeRows(provider, filter, info.type(), info.framework(), scheme, background);
        myCache.compute(
            key,
            (cacheKey, previous) -> myGeneration.get() == generation ? new EndpointCacheEntry(stamp, generation, rows) : previous
        );
        return rows;
    }

    @RequiredReadAction
    private static <G, E> List<EndpointRowData<?, ?>> computeRows(
        EndpointProvider<G, E> provider,
        EndpointFilter filter,
        EndpointType type,
        FrameworkPresentation framework,
        EditorColorsScheme scheme,
        @Nullable ColorValue background
    ) {
        List<G> groups = new ArrayList<>();
        try {
            for (G group : provider.getEndpointGroups(filter)) {
                ProgressManager.checkCanceled();
                groups.add(group);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            LOG.error(e);
            return List.of();
        }

        List<EndpointRowData<?, ?>> rows = new ArrayList<>();
        for (G group : groups) {
            ProgressManager.checkCanceled();
            try {
                for (E endpoint : provider.getEndpoints(group)) {
                    ProgressManager.checkCanceled();
                    if (!provider.isValidEndpoint(group, endpoint)) {
                        continue;
                    }

                    ItemPresentation presentation = provider.getEndpointPresentation(group, endpoint);
                    rows.add(EndpointRowData.create(provider, group, endpoint, presentation, type, framework, scheme, background));
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return List.copyOf(rows);
    }
}
