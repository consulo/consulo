/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.desktop.awt.ui.impl.htmlView;

import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.collection.impl.map.LinkedHashMap;
import consulo.util.io.StreamUtil;
import consulo.util.io.URLUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import org.cobraparser.ua.*;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

/**
 * @author VISTALL
 * @since 2025-01-21
 */
public class ConsuloNetworkRequest implements NetworkRequest {
    private static final Logger LOG = Logger.getInstance(ConsuloNetworkRequest.class);

    private String myMethod;
    private URL myURL;
    private boolean myAsyncFlag;
    private String myUserName;
    private String myPassword;

    private byte[] myBytes;

    private ImageResponse myImageResponse;

    private EventDispatcher<NetworkRequestListener> myListenerDispatcher = EventDispatcher.create(NetworkRequestListener.class);

    private Map<String, String> myHeaders = new LinkedHashMap<>();

    private int myState = NetworkRequest.STATE_UNINITIALIZED;

    @Override
    public int getReadyState() {
        return myState;
    }

    @Override
    public String getResponseText() {
        if (myBytes != null) {
            return new String(myBytes, StandardCharsets.UTF_8);
        }
        return null;
    }

    @Override
    public Document getResponseXML() {
        return null;
    }

    @Override
    public ImageResponse getResponseImage() {
        return myImageResponse;
    }

    @Override
    public byte[] getResponseBytes() {
        return myBytes;
    }

    @Override
    public int getStatus() {
        if (myBytes != null) {
            return HttpURLConnection.HTTP_OK;
        }

        return 0;
    }

    @Override
    public String getStatusText() {
        if (myBytes != null) {
            return "OK";
        }
        return null;
    }

    @Override
    public void abort() {
    }

    @Override
    public String getAllResponseHeaders(List<String> excludedHeadersLowerCase) {
        return null;
    }

    @Override
    public String getResponseHeader(String headerName) {
        return myHeaders.get(headerName);
    }

    private URL toURL(String url) throws IOException {
        try {
            return new URI(url).toURL();
        }
        catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void open(String method, String url) throws IOException {
        open(method, toURL(url));
    }

    @Override
    public void open(String method, URL url) throws IOException {
        open(method, url, true);
    }

    @Override
    public void open(String method, String url, boolean asyncFlag) throws IOException {
        open(method, toURL(url), true);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag) throws IOException {
        open(method, url, asyncFlag, null);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName) throws IOException {
        open(method, url, asyncFlag, null, null);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName, String password) throws IOException {
        myMethod = method;
        myURL = url;
        myAsyncFlag = asyncFlag;
        myUserName = userName;
        myPassword = password;
    }

    @Override
    public void send(String content, UserAgentContext.Request requestType) throws IOException {
        if (URLUtil.FILE_PROTOCOL.equals(myURL.getProtocol())) {
            if (myAsyncFlag) {
                ForkJoinPool.commonPool().execute(() -> loadFile(requestType));
            }
            else {
                loadFile(requestType);
            }
        }
    }

    private void loadFile(UserAgentContext.Request requestType) {
        myState = NetworkRequest.STATE_LOADING;

        myListenerDispatcher.getMulticaster().readyStateChanged(new NetworkRequestEvent(this, myState));

        if (requestType.kind == UserAgentContext.RequestKind.Image) {
            myImageResponse = new ImageResponse(ImageResponse.State.loading, null);
        }

        try (InputStream inputStream = URLUtil.openStream(myURL)) {
            byte[] bytes = StreamUtil.loadFromStream(inputStream);

            myBytes = bytes;

            myState = NetworkRequest.STATE_LOADED;

            myListenerDispatcher.getMulticaster().readyStateChanged(new NetworkRequestEvent(this, myState));

            if (requestType.kind == UserAgentContext.RequestKind.Image) {
                try {
                    BufferedImage image = ImageIO.read(new UnsyncByteArrayInputStream(myBytes));
                    if (image != null) {
                        myImageResponse = new ImageResponse(ImageResponse.State.loaded, image);
                    } else {
                        myImageResponse = new ImageResponse(ImageResponse.State.error, null);
                    }
                }
                catch (IOException e) {
                    myImageResponse = new ImageResponse(ImageResponse.State.error, null);
                    
                    LOG.warn(e);
                }
            }

            myState = NetworkRequest.STATE_COMPLETE;

            myListenerDispatcher.getMulticaster().readyStateChanged(new NetworkRequestEvent(this, myState));
        }
        catch (IOException e) {
            if (requestType.kind == UserAgentContext.RequestKind.Image) {
                myImageResponse = new ImageResponse(ImageResponse.State.error, null);
            }

            LOG.warn(e);
        }
    }

    @Override
    public void addNetworkRequestListener(NetworkRequestListener listener) {
        myListenerDispatcher.addListener(listener);
    }

    @Override
    public Optional<URL> getURL() {
        return Optional.ofNullable(myURL);
    }

    @Override
    public boolean isAsnyc() {
        return myAsyncFlag;
    }

    @Override
    public void addRequestedHeader(String header, String value) {
        myHeaders.put(header, value);
    }
}
