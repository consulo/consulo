package consulo.endpoint.impl.internal.diagram;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.component.ProcessCanceledException;
import consulo.endpoint.EndpointFilter;
import consulo.endpoint.EndpointModuleEntity;
import consulo.endpoint.EndpointProjectModel;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointType;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.FrameworkPresentation;
import consulo.endpoint.url.Authority;
import consulo.endpoint.url.UrlResolveRequest;
import consulo.endpoint.url.UrlResolverManager;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiModificationTracker;
import consulo.logging.Logger;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.ItemPresentation;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ServicesDiagramGraphBuilder {
    private static final Logger LOG = Logger.getInstance(ServicesDiagramGraphBuilder.class);

    private static final Key<CachedValue<ServicesDiagramGraph>> GRAPH_KEY = Key.create("ServicesDiagramGraph");

    private final Project myProject;
    private final EndpointProjectModel myProjectModel;
    private final UrlResolverManager myResolverManager;
    private final Map<String, List<ServicesDiagramMember>> myServiceMembers = new LinkedHashMap<>();
    private final Map<String, Image> myIcons = new LinkedHashMap<>();
    private final Map<String, List<ServicesDiagramMember>> myExternalMembers = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> myEdges = new LinkedHashMap<>();

    private ServicesDiagramGraphBuilder(Project project, EndpointProjectModel projectModel) {
        myProject = project;
        myProjectModel = projectModel;
        myResolverManager = UrlResolverManager.getInstance(project);
    }

    @RequiredReadAction
    static ServicesDiagramGraph getGraph(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            GRAPH_KEY,
            () -> CachedValueProvider.Result.create(
                build(project),
                PsiModificationTracker.MODIFICATION_COUNT,
                DumbService.getInstance(project).getModificationTracker(),
                ProjectRootManager.getInstance(project)
            ),
            false
        );
    }

    @RequiredReadAction
    private static ServicesDiagramGraph build(Project project) {
        EndpointProjectModel projectModel = project.getExtensionPoint(EndpointProjectModel.class).findFirstSafe(model -> true);
        if (projectModel == null) {
            return ServicesDiagramGraph.EMPTY;
        }
        return new ServicesDiagramGraphBuilder(project, projectModel).build();
    }

    @RequiredReadAction
    private ServicesDiagramGraph build() {
        List<EndpointProvider<?, ?>> providers = EndpointProvider.getAvailableProviders(myProject);

        for (EndpointModuleEntity module : myProjectModel.getModuleEntities()) {
            if (myProjectModel.isTestModule(module)) {
                continue;
            }

            String serviceName = ServicesDiagramService.qualifiedName(module.getName());
            EndpointFilter filter = myProjectModel.createFilter(module, false, false);
            for (EndpointProvider<?, ?> provider : providers) {
                try {
                    collect(provider, filter, module, serviceName);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (RuntimeException e) {
                    LOG.error(e);
                }
            }
        }

        Set<String> linked = new LinkedHashSet<>();
        List<ServicesDiagramEdgeData> edges = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> sourceEntry : myEdges.entrySet()) {
            for (Map.Entry<String, Integer> targetEntry : sourceEntry.getValue().entrySet()) {
                edges.add(new ServicesDiagramEdgeData(sourceEntry.getKey(), targetEntry.getKey(), targetEntry.getValue()));
                linked.add(sourceEntry.getKey());
                linked.add(targetEntry.getKey());
            }
        }

        List<ServicesDiagramElement> elements = new ArrayList<>();
        for (Map.Entry<String, List<ServicesDiagramMember>> entry : myServiceMembers.entrySet()) {
            ServicesDiagramService service = new ServicesDiagramService(entry.getKey(), List.copyOf(entry.getValue()));
            if (!entry.getValue().isEmpty() || linked.contains(service.getQualifiedName())) {
                elements.add(service);
            }
        }
        for (Map.Entry<String, List<ServicesDiagramMember>> entry : myExternalMembers.entrySet()) {
            elements.add(new ServicesDiagramExternal(entry.getKey(), List.copyOf(entry.getValue())));
        }

        return new ServicesDiagramGraph(List.copyOf(elements), Map.copyOf(myIcons), List.copyOf(edges));
    }

    @RequiredReadAction
    private <G, E> void collect(EndpointProvider<G, E> provider, EndpointFilter filter, EndpointModuleEntity module, String serviceName) {
        List<ServicesDiagramMember> members = myServiceMembers.computeIfAbsent(module.getName(), name -> new ArrayList<>());
        myIcons.putIfAbsent(serviceName, module.getIcon());

        EndpointType endpointType = provider.getEndpointType();
        FrameworkPresentation framework = provider.getPresentation();
        boolean client = isClient(endpointType);

        for (G group : provider.getEndpointGroups(filter)) {
            for (E endpoint : provider.getEndpoints(group)) {
                if (!provider.isValidEndpoint(group, endpoint)) {
                    continue;
                }

                ItemPresentation presentation = provider.getEndpointPresentation(group, endpoint);
                String text = presentation.getPresentableText();
                if (text != null) {
                    @Nullable Image icon = endpointType.getIcon();
                    members.add(new ServicesDiagramMember(text, framework.getTitle(), client, icon != null ? icon : framework.getIcon()));
                }

                if (client && provider instanceof EndpointUrlTargetProvider<G, E> urlTargetProvider) {
                    for (UrlTargetInfo urlTargetInfo : urlTargetProvider.getUrlTargetInfo(group, endpoint)) {
                        link(serviceName, urlTargetInfo);
                    }
                }
            }
        }
    }

    @RequiredReadAction
    private void link(String serviceName, UrlTargetInfo urlTargetInfo) {
        Set<String> targets = new LinkedHashSet<>();
        for (UrlResolveRequest request : createRequests(urlTargetInfo)) {
            for (UrlTargetInfo target : myResolverManager.resolve(request)) {
                PsiElement element = target.resolveToPsiElement();
                PsiFile file = element == null ? null : element.getContainingFile();
                EndpointModuleEntity targetModule = file == null ? null : myProjectModel.getModuleEntityForFile(file);
                if (targetModule != null && !myProjectModel.isTestModule(targetModule)) {
                    String targetName = ServicesDiagramService.qualifiedName(targetModule.getName());
                    targets.add(targetName);
                    myServiceMembers.computeIfAbsent(targetModule.getName(), name -> new ArrayList<>());
                    myIcons.putIfAbsent(targetName, targetModule.getIcon());
                }
            }
        }

        if (targets.isEmpty()) {
            String authority = getAuthority(urlTargetInfo);
            List<ServicesDiagramMember> members = myExternalMembers.computeIfAbsent(authority, name -> new ArrayList<>());
            String path = urlTargetInfo.getPath().getPresentation();
            if (members.stream().noneMatch(member -> member.text().equals(path))) {
                members.add(new ServicesDiagramMember(path, "", false, urlTargetInfo.getIcon()));
            }
            targets.add(ServicesDiagramExternal.qualifiedName(authority));
        }

        for (String target : targets) {
            if (!target.equals(serviceName)) {
                myEdges.computeIfAbsent(serviceName, name -> new LinkedHashMap<>()).merge(target, 1, Integer::sum);
            }
        }
    }

    private static List<UrlResolveRequest> createRequests(UrlTargetInfo urlTargetInfo) {
        List<@Nullable String> schemes = orNull(urlTargetInfo.getSchemes());
        List<@Nullable String> authorities = new ArrayList<>();
        for (Authority authority : urlTargetInfo.getAuthorities()) {
            if (authority instanceof Authority.Exact exact) {
                authorities.add(exact.getText());
            }
        }
        List<@Nullable String> methods = orNull(urlTargetInfo.getMethods());

        List<UrlResolveRequest> requests = new ArrayList<>();
        for (@Nullable String scheme : schemes) {
            for (@Nullable String authority : orNull(authorities)) {
                for (@Nullable String method : methods) {
                    requests.add(new UrlResolveRequest(scheme, authority, urlTargetInfo.getPath(), method));
                }
            }
        }
        return requests;
    }

    private static List<@Nullable String> orNull(Iterable<? extends @Nullable String> values) {
        List<@Nullable String> result = new ArrayList<>();
        for (@Nullable String value : values) {
            result.add(value);
        }
        return result.isEmpty() ? Collections.singletonList(null) : result;
    }

    private static String getAuthority(UrlTargetInfo urlTargetInfo) {
        for (Authority authority : urlTargetInfo.getAuthorities()) {
            if (authority instanceof Authority.Exact exact) {
                return exact.getText();
            }
        }
        return "";
    }

    private static boolean isClient(EndpointType endpointType) {
        return endpointType == EndpointType.HTTP_CLIENT_TYPE || endpointType == EndpointType.WEBSOCKET_CLIENT_TYPE;
    }
}
