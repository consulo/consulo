// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.impl.http;

import consulo.builtinWebServer.http.HttpFileRegion;
import consulo.builtinWebServer.http.HttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;
import java.util.Map;

final class FileRegionResponses {
    private static final int CHUNK_SIZE = 8192;

    static void send(Channel channel, FullHttpRequest request, HttpResponse response) throws IOException {
        HttpFileRegion region = response.getFileRegion();
        assert region != null;

        FileChannel fileChannel;
        try {
            fileChannel = FileChannel.open(region.file(), StandardOpenOption.READ);
        }
        catch (NoSuchFileException ignored) {
            Responses.send(HttpResponseStatus.NOT_FOUND, channel, request);
            return;
        }

        boolean isKeepAlive;
        boolean fileWillBeClosed = false;
        try {
            io.netty.handler.codec.http.HttpResponse nettyResponse =
                new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.getCode()));

            String contentType = response.getContentType();
            if (contentType != null) {
                nettyResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            }
            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                nettyResponse.headers().set(header.getKey(), header.getValue());
            }

            HttpUtil.setContentLength(nettyResponse, region.length());
            Responses.addCommonHeaders(nettyResponse);
            isKeepAlive = Responses.addKeepAliveIfNeed(nettyResponse, request);

            channel.write(nettyResponse);
            if (!HttpMethod.HEAD.equals(request.method())) {
                if (region.count() > 0) {
                    if (channel.pipeline().get(SslHandler.class) == null) {
                        channel.write(new DefaultFileRegion(fileChannel, region.position(), region.count()));
                    }
                    else {
                        channel.write(new ChunkedNioFile(fileChannel, region.position(), region.count(), CHUNK_SIZE));
                    }
                    fileWillBeClosed = true;
                }
                if (region.suffix().length > 0) {
                    channel.write(Unpooled.wrappedBuffer(region.suffix()));
                }
            }
        }
        finally {
            if (!fileWillBeClosed) {
                fileChannel.close();
            }
        }

        ChannelFuture future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!isKeepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private FileRegionResponses() {
    }
}
