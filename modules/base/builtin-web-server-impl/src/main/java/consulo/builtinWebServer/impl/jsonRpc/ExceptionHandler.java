package consulo.builtinWebServer.impl.jsonRpc;

public interface ExceptionHandler {
    /**
     * @param e Exception while encode message (on send)
     */
    void exceptionCaught(Throwable e);
}
