package consulo.builtinWebServer.impl.webSocket;

import java.util.concurrent.TimeUnit;

public final class WebSocketServerOptions {
    private int myHeartbeatDelay = (int) TimeUnit.SECONDS.toMillis(25);

    private int myCloseTimeout = 60 * 1000;

    public int getHeartbeatDelay() {
        return myHeartbeatDelay;
    }

    public int getCloseTimeout() {
        return myCloseTimeout;
    }

    public WebSocketServerOptions heartbeatDelay(int value) {
        myHeartbeatDelay = value;
        return this;
    }

    public WebSocketServerOptions closeTimeout(int value) {
        myCloseTimeout = value;
        return this;
    }
}
