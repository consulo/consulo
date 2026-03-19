package consulo.remoteServer.runtime;

import consulo.localize.LocalizeValue;
import consulo.remoteServer.localize.RemoteServerLocalize;

/**
 * @author nik
 */
public enum ConnectionStatus {
    DISCONNECTED(RemoteServerLocalize.connectionstatusDisconnected()),
    CONNECTED(RemoteServerLocalize.connectionstatusConnected()),
    CONNECTING(RemoteServerLocalize.connectionstatusConnecting());

    private final LocalizeValue myPresentableText;

    ConnectionStatus(LocalizeValue presentableText) {
        myPresentableText = presentableText;
    }

    public LocalizeValue getPresentableText() {
        return myPresentableText;
    }
}
