package consulo.endpoint.impl.internal.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataSink;
import consulo.endpoint.EndpointElementItem;
import consulo.endpoint.EndpointModuleEntity;
import consulo.endpoint.EndpointProvider;
import consulo.endpoint.EndpointUrlTargetProvider;
import consulo.endpoint.url.UrlTargetInfo;
import consulo.language.psi.PsiElement;
import org.jspecify.annotations.Nullable;

public final class EndpointRow<G, E> implements EndpointViewRow, EndpointElementItem<G, E> {
    private final EndpointRowData<G, E> myData;
    private final @Nullable EndpointModuleEntity myModule;
    private final EndpointRowKey myKey;
    private final boolean myCompact;

    private EndpointRow(EndpointRowData<G, E> data, @Nullable EndpointModuleEntity module, EndpointRowKey key, boolean compact) {
        myData = data;
        myModule = module;
        myKey = key;
        myCompact = compact;
    }

    public static <G, E> EndpointRow<G, E> create(
        EndpointRowData<G, E> data,
        @Nullable EndpointModuleEntity module,
        @Nullable String moduleKey,
        boolean compact
    ) {
        return new EndpointRow<>(data, module, data.getKey().withModule(moduleKey), compact);
    }

    public EndpointRowData<G, E> getData() {
        return myData;
    }

    public EndpointRowKey getKey() {
        return myKey;
    }

    public boolean isCompact() {
        return myCompact;
    }

    @Override
    public @Nullable EndpointModuleEntity getModule() {
        return myModule;
    }

    @Override
    public EndpointProvider<G, E> getProvider() {
        return myData.getProvider();
    }

    @Override
    public G getGroup() {
        return myData.getGroup();
    }

    @Override
    public E getEndpoint() {
        return myData.getEndpoint();
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return myData.getProvider().isValidEndpoint(myData.getGroup(), myData.getEndpoint());
    }

    @Override
    @RequiredReadAction
    public @Nullable Iterable<UrlTargetInfo> getUrlTargetInfos() {
        if (myData.getProvider() instanceof EndpointUrlTargetProvider<G, E> provider) {
            return provider.getUrlTargetInfo(myData.getGroup(), myData.getEndpoint());
        }
        return null;
    }

    @RequiredReadAction
    public @Nullable PsiElement getNavigationElement() {
        if (!isValid()) {
            return null;
        }
        return myData.getProvider().getNavigationElement(myData.getGroup(), myData.getEndpoint());
    }

    public void uiDataSnapshot(DataSink sink) {
        myData.getProvider().uiDataSnapshot(sink, myData.getGroup(), myData.getEndpoint());
    }

    @Override
    public String getSpeedSearchText() {
        return myData.getUrl();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o || o instanceof EndpointRow<?, ?> that && myKey.equals(that.myKey);
    }

    @Override
    public int hashCode() {
        return myKey.hashCode();
    }

    @Override
    public String toString() {
        return "EndpointRow(" + myKey + ")";
    }
}
