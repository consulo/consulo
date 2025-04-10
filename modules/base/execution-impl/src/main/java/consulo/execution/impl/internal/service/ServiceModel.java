// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.impl.internal.service;

import consulo.component.ProcessCanceledException;
import consulo.component.util.PluginExceptionUtil;
import consulo.component.util.WeighedItem;
import consulo.disposer.Disposable;
import consulo.execution.service.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerFactory;
import consulo.ui.ex.util.InvokerSupplier;
import consulo.util.collection.*;
import consulo.util.concurrent.CancellablePromise;
import consulo.util.concurrent.Promises;
import consulo.util.lang.Comparing;
import consulo.util.lang.NotNullizer;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static consulo.execution.service.ServiceViewContributor.CONTRIBUTOR_EP_NAME;

final class ServiceModel implements Disposable, InvokerSupplier {
    private static final Logger LOG = Logger.getInstance(ServiceModel.class);

    static final TreeTraversal NOT_LOADED_LAST_BFS = new TreeTraversal("NOT_LOADED_LAST_BFS") {
        @Nonnull
        @Override
        public <T> It<T> createIterator(
            @Nonnull Iterable<? extends T> roots,
            @Nonnull Function<T, ? extends Iterable<? extends T>> tree
        ) {
            return new NotLoadedLastBfsIt<>(roots, tree);
        }
    };
    static final TreeTraversal ONLY_LOADED_BFS = new TreeTraversal("ONLY_LOADED_BFS") {
        @Nonnull
        @Override
        public <T> It<T> createIterator(
            @Nonnull Iterable<? extends T> roots,
            @Nonnull Function<T, ? extends Iterable<? extends T>> tree
        ) {
            return new OnlyLoadedBfsIt<>(roots, tree);
        }
    };
    private static final NotNullizer ourNotNullizer = new NotNullizer("ServiceViewTreeTraversal.NotNull");

    private final Project myProject;
    private final Invoker myInvoker = InvokerFactory.getInstance().forBackgroundThreadWithoutReadAction(this);
    private final List<ServiceViewItem> myRoots = Lists.newLockFreeCopyOnWriteList();
    private final List<ServiceModelEventListener> myListeners = Lists.newLockFreeCopyOnWriteList();

    ServiceModel(@Nonnull Project project) {
        myProject = project;
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    @Override
    public Invoker getInvoker() {
        return myInvoker;
    }

    void addEventListener(@Nonnull ServiceModelEventListener listener) {
        myListeners.add(listener);
    }

    void removeEventListener(@Nonnull ServiceModelEventListener listener) {
        myListeners.remove(listener);
    }

    @Nonnull
    List<? extends ServiceViewItem> getRoots() {
        return myRoots;
    }

    private JBIterable<ServiceViewItem> doFindItems(
        Predicate<? super ServiceViewItem> visitChildrenCondition,
        Predicate<? super ServiceViewItem> condition,
        boolean safe
    ) {
        return JBTreeTraverser.from(
                (Function<ServiceViewItem, List<ServiceViewItem>>)node -> visitChildrenCondition.test(node)
                    ? new ArrayList<>(node.getChildren())
                    : null
            )
            .withRoots(myRoots)
            .traverse(safe ? ONLY_LOADED_BFS : NOT_LOADED_LAST_BFS)
            .filter(condition);
    }

    private JBIterable<ServiceViewItem> findItems(Object service, Class<?> contributorClass, boolean safe) {
        Object value = service instanceof ServiceViewProvidingContributor ?
            ((ServiceViewProvidingContributor<?, ?>)service).asService() : service;
        return doFindItems(
            node -> contributorClass.isInstance(node.getRootContributor()),
            node -> node.getValue().equals(value),
            safe
        );
    }

    @Nullable
    ServiceViewItem findItem(Predicate<? super ServiceViewItem> visitChildrenCondition, Predicate<? super ServiceViewItem> condition) {
        return doFindItems(visitChildrenCondition, condition, false).first();
    }

    @Nullable
    ServiceViewItem findItem(Object service, Class<?> contributorClass) {
        return findItems(service, contributorClass, false).first();
    }

    @Nullable
    ServiceViewItem findItemSafe(Object service, Class<?> contributorClass) {
        return findItems(service, contributorClass, true).first();
    }

    @Nullable
    ServiceViewItem findItemById(List<String> ids, ServiceViewContributor<?> contributor) {
        if (ids.isEmpty()) {
            return null;
        }

        List<? extends ServiceViewItem> roots = ContainerUtil.filter(getRoots(), item -> contributor.equals(item.getContributor()));
        if (roots.isEmpty()) {
            return null;
        }

        return findItemById(new LinkedList<>(ids), roots);
    }

    private static ServiceViewItem findItemById(Deque<String> path, List<? extends ServiceViewItem> roots) {
        String id = path.removeFirst();
        for (ServiceViewItem root : roots) {
            if (id.equals(root.getViewDescriptor().getId())) {
                return path.isEmpty() ? root : findItemById(path, root.getChildren());
            }
        }
        return null;
    }

    @Nonnull
    CancellablePromise<?> handle(@Nonnull ServiceEventListener.ServiceEvent e) {
        Runnable handler = () -> {
            LOG.debug("Handle event: " + e);
            switch (e.type) {
                case SERVICE_ADDED -> addService(e);
                case SERVICE_REMOVED -> removeService(e);
                case SERVICE_CHANGED -> serviceChanged(e);
                case SERVICE_CHILDREN_CHANGED -> serviceChildrenChanged(e);
                case SERVICE_STRUCTURE_CHANGED -> serviceStructureChanged(e);
                case SERVICE_GROUP_CHANGED -> serviceGroupChanged(e);
                case GROUP_CHANGED -> groupChanged(e);
                default -> reset(e.contributorClass);
            }
            notifyListeners(e);
            LOG.debug("Event handled: " + e);
        };
        if (e.type != ServiceEventListener.EventType.UNLOAD_SYNC_RESET) {
            return getInvoker().invokeLater(handler);
        }
        handler.run();
        return Promises.resolvedCancellablePromise(null);
    }

    void notifyListeners(ServiceEventListener.ServiceEvent e) {
        for (ServiceModelEventListener listener : myListeners) {
            listener.eventProcessed(e);
        }
    }

    private void reset(Class<?> contributorClass) {
        int index = -1;

        if (myRoots.isEmpty()) {
            index = 0;
        }
        else {
            ServiceViewItem contributorNode = null;
            for (int i = 0; i < myRoots.size(); i++) {
                ServiceViewItem child = myRoots.get(i);
                if (contributorClass.isInstance(child.getContributor())) {
                    contributorNode = child;
                    index = i;
                    break;
                }
            }
            if (contributorNode != null) {
                myRoots.remove(contributorNode);
            }
            else {
                index = getContributorNodeIndex(contributorClass);
            }
        }

        ContributorNode newRoot = null;
        for (ServiceViewContributor<?> contributor : CONTRIBUTOR_EP_NAME.getExtensionList()) {
            if (contributorClass.isInstance(contributor)) {
                newRoot = new ContributorNode(myProject, contributor);
                newRoot.loadChildren();
                if (newRoot.getChildren().isEmpty()) {
                    newRoot = null;
                }
                break;
            }
        }
        if (newRoot != null) {
            myRoots.add(index, newRoot);
        }
    }

    private int getContributorNodeIndex(Class<?> contributorClass) {
        int index = -1;
        List<ServiceViewContributor> contributors = CONTRIBUTOR_EP_NAME.getExtensionList();
        List<ServiceViewContributor<?>> existingContributors = ContainerUtil.map(myRoots, ServiceViewItem::getContributor);
        for (int i = contributors.size() - 1; i >= 0; i--) {
            ServiceViewContributor<?> contributor = contributors.get(i);
            if (!contributorClass.isInstance(contributor)) {
                index = existingContributors.indexOf(contributor);
                if (index == 0) {
                    break;
                }
            }
            else {
                break;
            }
        }
        if (index < 0) {
            index = myRoots.size();
        }
        return index;
    }

    private void addService(ServiceEventListener.ServiceEvent e) {
        if (e.parent != null) {
            ServiceViewItem parent = findItemSafe(e.parent, e.contributorClass);
            ServiceViewContributor<?> parentContributor =
                parent instanceof ServiceNode serviceNode ? serviceNode.getProvidingContributor() : null;
            if (parentContributor == null) {
                LOG.debug("Parent not found; event: " + e);
                return;
            }

            if (!hasChild(parent, e.target)) {
                addService(e.target, parent.getChildren(), myProject, parent, parentContributor);
            }
            return;
        }

        ServiceViewItem contributorNode = null;
        for (ServiceViewItem child : myRoots) {
            if (e.contributorClass.isInstance(child.getContributor())) {
                contributorNode = child;
                break;
            }
        }
        if (contributorNode == null) {
            int index = getContributorNodeIndex(e.contributorClass);
            for (ServiceViewContributor<?> contributor : CONTRIBUTOR_EP_NAME.getExtensionList()) {
                if (e.contributorClass.isInstance(contributor)) {
                    contributorNode = new ContributorNode(myProject, contributor);
                    myRoots.add(index, contributorNode);
                    break;
                }
            }
            if (contributorNode == null) {
                return;
            }
        }

        if (!hasChild(contributorNode, e.target)) {
            addService(e.target, contributorNode.getChildren(), myProject, contributorNode, contributorNode.getContributor());
        }
    }

    private void removeService(ServiceEventListener.ServiceEvent e) {
        ServiceViewItem item = findItemSafe(e.target, e.contributorClass);
        if (item == null) {
            return;
        }

        ServiceViewItem parent = item.getParent();
        while (parent instanceof ServiceGroupNode) {
            item.markRemoved();
            parent.getChildren().remove(item);
            if (!parent.getChildren().isEmpty()) {
                return;
            }

            item = parent;
            parent = parent.getParent();
        }
        if (parent instanceof ContributorNode) {
            item.markRemoved();
            parent.getChildren().remove(item);
            if (!parent.getChildren().isEmpty()) {
                return;
            }

            item = parent;
            parent = parent.getParent();
        }
        item.markRemoved();
        if (parent == null) {
            myRoots.remove(item);
        }
        else {
            parent.getChildren().remove(item);
        }
    }

    private void serviceChanged(ServiceEventListener.ServiceEvent e) {
        ServiceViewItem item = findItemSafe(e.target, e.contributorClass);
        if (item instanceof ServiceNode serviceNode) {
            updateServiceViewDescriptor(serviceNode, e.target);
        }
    }

    private static void updateServiceViewDescriptor(ServiceNode node, Object target) {
        ServiceViewContributor<?> providingContributor = node.getProvidingContributor();
        if (providingContributor != null && !providingContributor.equals(target)) {
            node.setViewDescriptor(providingContributor.getViewDescriptor(node.myProject));
            return;
        }

        //noinspection unchecked
        ServiceViewDescriptor viewDescriptor =
            ((ServiceViewContributor<Object>)node.getContributor()).getServiceDescriptor(node.myProject, target);
        node.setViewDescriptor(viewDescriptor);
    }

    private void serviceChildrenChanged(ServiceEventListener.ServiceEvent e) {
        ServiceViewItem item = findItemSafe(e.target, e.contributorClass);
        if (item instanceof ServiceNode node) {
            node.reloadChildren();
        }
    }

    private void serviceStructureChanged(ServiceEventListener.ServiceEvent e) {
        ServiceViewItem item = findItemSafe(e.target, e.contributorClass);
        if (item instanceof ServiceNode node) {
            updateServiceViewDescriptor(node, e.target);
            node.reloadChildren();
        }
    }

    private void serviceGroupChanged(ServiceEventListener.ServiceEvent e) {
        ServiceViewItem item = findItemSafe(e.target, e.contributorClass);
        if (!(item instanceof ServiceNode)) {
            return;
        }

        ServiceViewItem parent = item.getParent();
        if (parent == null) {
            return;
        }

        ServiceGroupNode group = null;
        if (parent instanceof ServiceGroupNode groupNode) {
            group = groupNode;
            parent = group.getParent();
            while (parent instanceof ServiceGroupNode) {
                parent = parent.getParent();
            }
            if (parent == null) {
                return;
            }
        }

        if (group != null) {
            group.getChildren().remove(item);
        }
        else {
            parent.getChildren().remove(item);
        }

        Object value = e.target;
        ServiceViewContributor<?> providingContributor = ((ServiceNode)item).getProvidingContributor();
        if (providingContributor != null && !providingContributor.equals(e.target)) {
            value = providingContributor;
        }

        ServiceNode serviceNode = addService(value, parent.getChildren(), myProject, parent, item.getContributor());
        serviceNode.moveChildren((ServiceNode)item);
        while (group != null && group.getChildren().isEmpty()) {
            ServiceViewItem groupParent = group.getParent();
            if (groupParent == null) {
                return;
            }

            groupParent.getChildren().remove(group);
            group = groupParent instanceof ServiceGroupNode groupNode ? groupNode : null;
        }
    }

    private void groupChanged(ServiceEventListener.ServiceEvent e) {
        JBIterable<ServiceGroupNode> groups = findItems(e.target, e.contributorClass, true).filter(ServiceGroupNode.class);
        ServiceGroupNode first = groups.first();
        if (first == null) {
            return;
        }

        //noinspection unchecked
        ServiceViewDescriptor viewDescriptor = ((ServiceViewGroupingContributor)first.getContributor()).getGroupDescriptor(e.target);
        for (ServiceViewItem group : groups) {
            group.setViewDescriptor(viewDescriptor);
            ServiceViewItem parent = group.getParent();
            if (parent != null) {
                List<ServiceViewItem> children = parent.getChildren();
                children.remove(group);
                addGroupOrdered(children, (ServiceGroupNode)group);
            }
        }
    }

    private static boolean hasChild(ServiceViewItem item, Object service) {
        for (ServiceViewItem child : item.getChildren()) {
            if (child.getValue().equals(service)) {
                return true;
            }
        }
        return false;
    }

    private static <T> List<ServiceViewItem> getContributorChildren(
        Project project,
        ServiceViewItem parent,
        ServiceViewContributor<T> contributor
    ) {
        List<ServiceViewItem> children = new ArrayList<>();
        try {
            for (T service : contributor.getServices(project)) {
                addService(service, children, project, parent, contributor);
            }
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Exception e) {
            PluginExceptionUtil.logPluginError(
                LOG,
                "Failed to retrieve service view contributor children " + contributor.getClass(),
                e,
                contributor.getClass()
            );
        }
        return children;
    }

    private static <T> ServiceNode addService(
        Object service,
        List<ServiceViewItem> children,
        Project project,
        ServiceViewItem parent,
        ServiceViewContributor<T> contributor
    ) {
        //noinspection unchecked
        T typedService = (T)service;
        Object value =
            service instanceof ServiceViewProvidingContributor ? ((ServiceViewProvidingContributor<?, ?>)service).asService() : service;
        if (contributor instanceof ServiceViewGroupingContributor) {
            ServiceNode serviceNode =
                addGroupNode((ServiceViewGroupingContributor<T, ?>)contributor, typedService, value, parent, project, children);
            if (serviceNode != null) {
                return serviceNode;
            }
        }

        ServiceNode serviceNode = new ServiceNode(
            value,
            parent,
            contributor,
            contributor.getServiceDescriptor(project, typedService),
            project,
            service instanceof ServiceViewContributor ? (ServiceViewContributor<?>)service : null
        );
        addServiceOrdered(children, serviceNode, contributor);
        return serviceNode;
    }

    private static <T, G> ServiceNode addGroupNode(
        ServiceViewGroupingContributor<T, G> groupingContributor,
        T service,
        Object value,
        ServiceViewItem parent,
        Project project,
        List<ServiceViewItem> children
    ) {
        List<G> groups = groupingContributor.getGroups(service);
        if (groups.isEmpty()) {
            return null;
        }

        List<ServiceViewItem> currentChildren = children;
        ServiceViewItem groupParent = parent;
        for (G group : groups) {
            boolean found = false;
            for (ServiceViewItem child : currentChildren) {
                if (child.getValue().equals(group)) {
                    groupParent = child;
                    currentChildren = groupParent.getChildren();
                    found = true;
                    break;
                }
            }
            if (!found) {
                ServiceGroupNode groupNode =
                    new ServiceGroupNode(group, groupParent, groupingContributor, groupingContributor.getGroupDescriptor(group));
                addGroupOrdered(currentChildren, groupNode);
                groupParent = groupNode;
                currentChildren = groupParent.getChildren();
            }
        }
        ServiceNode serviceNode = new ServiceNode(
            value,
            groupParent,
            groupingContributor,
            groupingContributor.getServiceDescriptor(project, service),
            project,
            service instanceof ServiceViewContributor ? (ServiceViewContributor<?>)service : null
        );
        addServiceOrdered(currentChildren, serviceNode, groupingContributor);
        return serviceNode;
    }

    private static void addServiceOrdered(List<ServiceViewItem> children, ServiceNode child, ServiceViewContributor<?> contributor) {
        if (!children.isEmpty() && contributor instanceof Comparator) {
            @SuppressWarnings("unchecked")
            Comparator<Object> comparator = (Comparator<Object>)contributor;
            for (int i = 0; i < children.size(); i++) {
                ServiceViewItem anchor = children.get(i);
                if (anchor instanceof ServiceNode serviceNode
                    && comparator.compare(child.getService(), serviceNode.getService()) < 0) {
                    children.add(i, child);
                    return;
                }
            }
        }
        children.add(child);
    }

    private static void addGroupOrdered(List<ServiceViewItem> children, ServiceGroupNode child) {
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                ServiceViewItem anchor = children.get(i);
                if (anchor instanceof ServiceNode
                    || anchor instanceof ServiceGroupNode groupNode && compareGroups(child, groupNode) < 0) {
                    children.add(i, child);
                    return;
                }
            }
        }
        children.add(child);
    }

    private static int compareGroups(ServiceGroupNode group1, ServiceGroupNode group2) {
        ServiceViewDescriptor groupDescriptor1 = group1.getViewDescriptor();
        WeighedItem weighedItem1 = ObjectUtil.tryCast(groupDescriptor1, WeighedItem.class);
        ServiceViewDescriptor groupDescriptor2 = group2.getViewDescriptor();
        WeighedItem weighedItem2 = ObjectUtil.tryCast(groupDescriptor2, WeighedItem.class);
        if (weighedItem1 != null) {
            if (weighedItem2 == null) {
                return -1;
            }

            int diff = weighedItem1.getWeight() - weighedItem2.getWeight();
            if (diff != 0) {
                return diff;
            }
        }
        else if (weighedItem2 != null) {
            return 1;
        }
        String name1 = ServiceViewDragHelper.getDisplayName(groupDescriptor1.getPresentation());
        String name2 = ServiceViewDragHelper.getDisplayName(groupDescriptor2.getPresentation());
        return StringUtil.naturalCompare(name1, name2);
    }

    static final class ContributorNode extends ServiceViewItem {
        private final Project myProject;

        ContributorNode(@Nonnull Project project, @Nonnull ServiceViewContributor<?> contributor) {
            super(contributor, null, contributor, contributor.getViewDescriptor(project));
            myProject = project;
        }

        private void loadChildren() {
            List<ServiceViewItem> children = getChildren();
            if (!children.isEmpty()) {
                children.clear();
            }
            children.addAll(getContributorChildren(myProject, this, getContributor()));
        }
    }

    static final class ServiceNode extends ServiceViewItem {
        private final Project myProject;
        private final ServiceViewContributor<?> myProvidingContributor;
        private volatile boolean myChildrenInitialized;
        private volatile boolean myLoaded;

        ServiceNode(
            @Nonnull Object service,
            @Nullable ServiceViewItem parent,
            @Nonnull ServiceViewContributor<?> contributor,
            @Nonnull ServiceViewDescriptor viewDescriptor,
            @Nonnull Project project,
            @Nullable ServiceViewContributor<?> providingContributor
        ) {
            super(service, parent, contributor, viewDescriptor);
            myProject = project;
            myProvidingContributor = providingContributor;
            myChildrenInitialized = providingContributor == null;
            myLoaded = !(providingContributor instanceof ServiceViewLazyContributor);
        }

        @Nonnull
        @Override
        List<ServiceViewItem> getChildren() {
            List<ServiceViewItem> children = super.getChildren();
            if (!myChildrenInitialized) {
                if (myProvidingContributor != null) {
                    children.addAll(getContributorChildren(myProject, this, myProvidingContributor));
                }
                myChildrenInitialized = true;
                myLoaded = true;
            }
            return children;
        }

        boolean isChildrenInitialized() {
            return myChildrenInitialized;
        }

        boolean isLoaded() {
            return myLoaded;
        }

        private void reloadChildren() {
            super.getChildren().clear();
            if (myProvidingContributor != null) {
                myChildrenInitialized = false;
            }
        }

        private void moveChildren(ServiceNode node) {
            List<ServiceViewItem> children = super.getChildren();
            children.clear();
            List<ServiceViewItem> nodeChildren = node.myChildren;
            children.addAll(nodeChildren);
            nodeChildren.clear();
            for (ServiceViewItem child : children) {
                child.setParent(this);
            }
            myChildrenInitialized = node.myChildrenInitialized;
            myLoaded = node.myLoaded;
        }

        @Nullable
        ServiceViewContributor<?> getProvidingContributor() {
            return myProvidingContributor;
        }

        @Nonnull
        private Object getService() {
            return myProvidingContributor != null ? myProvidingContributor : getValue();
        }
    }

    static final class ServiceGroupNode extends ServiceViewItem {
        ServiceGroupNode(
            @Nonnull Object group,
            @Nullable ServiceViewItem parent,
            @Nonnull ServiceViewContributor<?> contributor,
            @Nonnull ServiceViewDescriptor viewDescriptor
        ) {
            super(group, parent, contributor, viewDescriptor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ServiceGroupNode node = (ServiceGroupNode)o;
            return getValue().equals(node.getValue()) && Comparing.equal(getParent(), node.getParent());
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            ServiceViewItem parent = getParent();
            result = 31 * result + (parent != null ? parent.hashCode() : 0);
            return result;
        }
    }

    interface ServiceModelEventListener {
        void eventProcessed(ServiceEventListener.ServiceEvent e);
    }

    private static final class NotLoadedLastBfsIt<T> extends TreeTraversal.It<T> {
        Deque<T> myQueue = new ArrayDeque<>();
        Deque<T> myNotLoadedQueue = new ArrayDeque<>();
        T myTop;

        NotLoadedLastBfsIt(@Nonnull Iterable<? extends T> roots, Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            JBIterable.from(roots).map(ourNotNullizer::notNullize).addAllTo(myQueue);
        }

        @Override
        public T nextImpl() {
            if (myTop != null) {
                if (myTop instanceof ServiceNode serviceNode
                    && !serviceNode.isChildrenInitialized() && !serviceNode.isLoaded()) {
                    myNotLoadedQueue.add(myTop);
                }
                else {
                    Iterable<? extends T> iterable = tree.apply(myTop);
                    if (iterable != null) {
                        JBIterable.from(iterable).map(ourNotNullizer::notNullize).addAllTo(myQueue);
                    }
                }
                myTop = null;
            }
            while (!myNotLoadedQueue.isEmpty() && myQueue.isEmpty()) {
                T notLoaded = myNotLoadedQueue.remove();
                Iterable<? extends T> iterable = tree.apply(notLoaded);
                if (iterable != null) {
                    JBIterable.from(iterable).map(ourNotNullizer::notNullize).addAllTo(myQueue);
                }
            }
            if (myQueue.isEmpty()) {
                return stop();
            }
            myTop = ourNotNullizer.nullize(myQueue.remove());
            return myTop;
        }
    }

    private static final class OnlyLoadedBfsIt<T> extends TreeTraversal.It<T> {
        Deque<T> myQueue = new ArrayDeque<>();
        T myTop;

        OnlyLoadedBfsIt(@Nonnull Iterable<? extends T> roots, Function<? super T, ? extends Iterable<? extends T>> tree) {
            super(tree);
            JBIterable.from(roots).map(ourNotNullizer::notNullize).addAllTo(myQueue);
        }

        @Override
        public T nextImpl() {
            if (myTop != null) {
                if (!(myTop instanceof ServiceNode serviceNode && !serviceNode.isChildrenInitialized() && !serviceNode.isLoaded())) {
                    Iterable<? extends T> iterable = tree.apply(myTop);
                    if (iterable != null) {
                        JBIterable.from(iterable).map(ourNotNullizer::notNullize).addAllTo(myQueue);
                    }
                }
                myTop = null;
            }
            if (myQueue.isEmpty()) {
                return stop();
            }
            myTop = ourNotNullizer.nullize(myQueue.remove());
            return myTop;
        }
    }
}
