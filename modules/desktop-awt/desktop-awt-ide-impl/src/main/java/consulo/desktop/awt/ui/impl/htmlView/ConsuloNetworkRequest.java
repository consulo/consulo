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

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.geometry.size.FloatSize;
import com.github.weisj.jsvg.parser.SVGLoader;
import org.cobraparser.ua.ImageResponse;
import org.cobraparser.ua.NetworkRequest;
import org.cobraparser.ua.NetworkRequestListener;
import org.cobraparser.ua.UserAgentContext;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

/**
 * @author VISTALL
 * @since 2025-01-21
 */
public class ConsuloNetworkRequest implements NetworkRequest {
    private final List<NetworkRequestListener> listeners = new CopyOnWriteArrayList<>();

    private volatile int readyState = STATE_UNINITIALIZED;
    private volatile int status = 0;
    private volatile byte[] responseBytes;
    private volatile URL requestUrl;
    private volatile String classpathResource;
    private volatile boolean async;

    @Override
    public int getReadyState() {
        return readyState;
    }

    @Override
    public String getResponseText() {
        byte[] b = responseBytes;
        return b == null ? null : new String(b);
    }

    @Override
    public Document getResponseXML() {
        return null;
    }

    @Override
    public ImageResponse getResponseImage() {
        byte[] b = responseBytes;
        if (b == null) {
            return new ImageResponse(ImageResponse.State.error, null);
        }
        if (isSvgContent(b)) {
            return renderSvg(b);
        }
        try {
            Image img = ImageIO.read(new ByteArrayInputStream(b));
            if (img == null) {
                return new ImageResponse(ImageResponse.State.error, null);
            }
            return new ImageResponse(ImageResponse.State.loaded, img);
        }
        catch (IOException e) {
            return new ImageResponse(ImageResponse.State.error, null);
        }
    }

    private static boolean isSvgContent(byte[] bytes) {
        int checkLen = Math.min(bytes.length, 512);
        String head = new String(bytes, 0, checkLen, java.nio.charset.StandardCharsets.UTF_8);
        return head.contains("<svg");
    }

    private static ImageResponse renderSvg(byte[] bytes) {
        try {
            SVGDocument doc = new SVGLoader().load(new ByteArrayInputStream(bytes));
            if (doc == null) {
                return new ImageResponse(ImageResponse.State.error, null);
            }
            FloatSize size = doc.size();
            int w = Math.max(1, (int) Math.ceil(size.width));
            int h = Math.max(1, (int) Math.ceil(size.height));
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            var g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            doc.render(null, g, new ViewBox(0, 0, w, h));
            g.dispose();
            return new ImageResponse(ImageResponse.State.loaded, img);
        }
        catch (Exception e) {
            return new ImageResponse(ImageResponse.State.error, null);
        }
    }

    @Override
    public byte[] getResponseBytes() {
        return responseBytes;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getStatusText() {
        return status == 200 ? "OK" : String.valueOf(status);
    }

    @Override
    public void abort() {
        setReadyState(STATE_ABORTED);
    }

    @Override
    public String getAllResponseHeaders(List<String> excludedHeadersLowerCase) {
        return "";
    }

    @Override
    public String getResponseHeader(String headerName) {
        return null;
    }

    @Override
    public void open(String method, String url) throws IOException {
        if (url.startsWith("classpath:")) {
            this.classpathResource = url.substring("classpath:".length());
            setReadyState(STATE_LOADING);
            return;
        }
        open(method, new URL(url), true);
    }

    @Override
    public void open(String method, URL url) throws IOException {
        open(method, url, true);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag) throws IOException {
        this.requestUrl = url;
        this.async = asyncFlag;
        setReadyState(STATE_LOADING);
    }

    @Override
    public void open(String method, String url, boolean asyncFlag) throws IOException {
        if (url.startsWith("classpath:")) {
            this.classpathResource = url.substring("classpath:".length());
            this.async = asyncFlag;
            setReadyState(STATE_LOADING);
            return;
        }
        open(method, new URL(url), asyncFlag);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName) throws IOException {
        open(method, url, asyncFlag);
    }

    @Override
    public void open(String method, URL url, boolean asyncFlag, String userName, String password) throws IOException {
        open(method, url, asyncFlag);
    }

    @Override
    public void send(String content, UserAgentContext.Request requestType) throws IOException {
        if (classpathResource != null || !async) {
            execute();
        }
        else {
            ForkJoinPool.commonPool().execute(this::execute);
        }
    }

    private void execute() {
        String cp = classpathResource;
        if (cp != null) {
            try (InputStream in = getClass().getResourceAsStream(cp)) {
                if (in != null) {
                    responseBytes = in.readAllBytes();
                    status = 200;
                }
                else {
                    status = 404;
                }
            }
            catch (Exception e) {
                status = 0;
                responseBytes = null;
            }
            setReadyState(STATE_COMPLETE);
            return;
        }
        try {
            URL url = requestUrl;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 Cobra/1.0");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.connect();
            status = conn.getResponseCode();
            try (InputStream in = conn.getInputStream()) {
                responseBytes = in.readAllBytes();
            }
        }
        catch (Exception e) {
            status = 0;
            responseBytes = null;
        }
        setReadyState(STATE_COMPLETE);
    }

    @Override
    public void addNetworkRequestListener(NetworkRequestListener listener) {
        listeners.add(listener);
    }

    @Override
    public Optional<URL> getURL() {
        if (classpathResource != null) {
            URL u = getClass().getResource(classpathResource);
            return Optional.ofNullable(u);
        }
        return Optional.ofNullable(requestUrl);
    }

    @Override
    public boolean isAsnyc() {
        return async;
    }

    @Override
    public void addRequestedHeader(String header, String value) {
        // Not needed for test purposes
    }

    private void setReadyState(int state) {
        this.readyState = state;
        for (NetworkRequestListener l : listeners) {
            l.readyStateChanged(null);
        }
    }
}
