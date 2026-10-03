// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.util.ModificationTracker;
import consulo.dataContext.DataSink;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.ItemPresentation;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides information about client/server endpoints declared in the project using a framework-specific API.
 * Good examples of such endpoints are: Spring MVC Controllers and Retrofit Client interfaces.
 *
 * @param <G> type of endpoint groups
 * @param <E> type of endpoints
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface EndpointProvider<G, E> {
    Key<Iterable<UrlTargetInfo>> URL_TARGET_INFO = Key.create("endpoint.urlTargetInfo");

    /**
     * Endpoint type implemented by framework.
     */
    EndpointType getEndpointType();

    /**
     * UI presentation of the framework in Endpoints View.
     */
    FrameworkPresentation getPresentation();

    /**
     * Fast check if there may be endpoints in the project without searching for them.
     */
    Status getStatus();

    /**
     * Groups correspond to locations where endpoints are or can be declared.
     * Usually they are classes with some annotation (e.g., Spring Controllers) or some special files (e.g., Swagger YAML/JSON).
     * Providers may return all possible locations using some good-enough heuristics to speed-up list loading.
     *
     * @return endpoint groups corresponding to the passed filter,
     */
    Iterable<G> getEndpointGroups(EndpointFilter filter);

    /**
     * @return endpoints of the group to show in the list
     */
    Iterable<E> getEndpoints(G group);

    /**
     * Checks if endpoint instance is valid, i.e., can be used to get its presentation and data.
     */
    boolean isValidEndpoint(G group, E endpoint);

    /**
     * @return presentation of a single endpoint, e.g. URL, HTTP handler, message queue topic
     * @see consulo.endpoint.presentation.EndpointMethodPresentation
     */
    ItemPresentation getEndpointPresentation(G group, E endpoint);

    /**
     * Modification tracker related to the underlying data models.
     * Implementations may use language modification trackers, e.g., YAML/JSON or UAST languages modification tracker.
     *
     * @see PsiModificationTracker#forLanguage
     */
    ModificationTracker getModificationTracker();

    default @Nullable PsiElement getDocumentationElement(G group, E endpoint) {
        return null;
    }

    default @Nullable PsiElement getNavigationElement(G group, E endpoint) {
        return getDocumentationElement(group, endpoint);
    }

    default void uiDataSnapshot(DataSink sink, G group, E endpoint) {
    }

    static boolean hasAnyProviders(Project project) {
        return project.getExtensionPoint(EndpointProvider.class).hasAnyExtensions();
    }

    static List<EndpointProvider<?, ?>> getAllProviders(Project project) {
        return project.getExtensionPoint(EndpointProvider.class).collectMapped(provider -> (EndpointProvider<?, ?>) provider);
    }

    static List<EndpointProvider<?, ?>> getAvailableProviders(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, () -> {
            List<EndpointProvider<?, ?>> available = new ArrayList<>();
            project.getExtensionPoint(EndpointProvider.class).forEachExtensionSafe(provider -> {
                if (provider.getStatus() != Status.UNAVAILABLE) {
                    available.add(provider);
                }
            });
            return CachedValueProvider.Result.create(
                List.copyOf(available),
                PsiModificationTracker.MODIFICATION_COUNT,
                DumbService.getInstance(project).getModificationTracker(),
                ProjectRootManager.getInstance(project)
            );
        });
    }

    enum Status {
        /**
         * Provider is not relevant for the project, should not be shown to user.
         */
        UNAVAILABLE,

        /**
         * There may be endpoints from this provider declared in the project, or they can be added by user.
         */
        AVAILABLE,

        /**
         * There are declared endpoints in the project, or there is a high probability of that.
         */
        HAS_ENDPOINTS
    }
}
