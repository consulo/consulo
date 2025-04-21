package consulo.builtinWebServer.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.builtinWebServer.custom.CustomPortServerManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@State(name = "BuiltInServerOptions", storages = @Storage("other.xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class BuiltInServerOptions implements PersistentStateComponent<BuiltInServerOptionsState> {
    public static final int DEFAULT_PORT = 63342;

    private final Application myApplication;

    private final BuiltInServerOptionsState myState = new BuiltInServerOptionsState();

    @Inject
    public BuiltInServerOptions(Application application) {
        myApplication = application;
    }

    public static BuiltInServerOptions getInstance() {
        return Application.get().getInstance(BuiltInServerOptions.class);
    }

    @Nullable
    @Override
    public BuiltInServerOptionsState getState() {
        return myState;
    }

    @Override
    public void loadState(BuiltInServerOptionsState state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    public int getEffectiveBuiltInServerPort() {
        DefaultCustomPortServerManager portServerManager = myApplication.getExtensionPoint(CustomPortServerManager.class)
            .findExtensionOrFail(DefaultCustomPortServerManager.class);
        if (!portServerManager.isBound()) {
            return BuiltInServerManager.getInstance().getPort();
        }
        return myState.builtInServerPort;
    }

    public static void onBuiltInServerPortChanged() {
        Application.get().getExtensionPoint(CustomPortServerManager.class)
            .findExtensionOrFail(DefaultCustomPortServerManager.class)
            .portChanged();
    }

    public int getBuiltInServerPort() {
        return myState.builtInServerPort;
    }

    public void setBuiltInServerPort(int builtInServerPort) {
        myState.builtInServerPort = builtInServerPort;
    }

    public boolean isBuiltInServerAvailableExternally() {
        return myState.builtInServerAvailableExternally;
    }

    public void setBuiltInServerAvailableExternally(boolean builtInServerAvailableExternally) {
        myState.builtInServerAvailableExternally = builtInServerAvailableExternally;
    }

    public boolean isAllowUnsignedRequests() {
        return myState.allowUnsignedRequests;
    }

    public void setAllowUnsignedRequests(boolean allowUnsignedRequests) {
        myState.allowUnsignedRequests = allowUnsignedRequests;
    }
}