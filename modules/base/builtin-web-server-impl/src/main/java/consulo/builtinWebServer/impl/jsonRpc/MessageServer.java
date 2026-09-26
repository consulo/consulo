package consulo.builtinWebServer.impl.jsonRpc;

public interface MessageServer {
    void messageReceived(Client client, CharSequence message);
}
