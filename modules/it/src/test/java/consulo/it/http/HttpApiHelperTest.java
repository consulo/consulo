/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import consulo.http.client.HttpApiHelper;
import consulo.http.client.HttpStatusErrorException;
import consulo.http.graphql.CachingGraphQLQueryLoader;
import consulo.http.graphql.GraphQLApiHelper;
import consulo.http.graphql.GraphQLDataDeserializer;
import consulo.http.graphql.GraphQLErrorException;
import consulo.http.graphql.GraphQLRequestPagination;
import consulo.http.graphql.dto.GraphQLConnectionDTO;
import consulo.http.graphql.dto.GraphQLCursorPageInfoDTO;
import consulo.http.graphql.dto.GraphQLErrorDTO;
import consulo.http.graphql.dto.GraphQLResponseDTO;
import consulo.http.json.JsonDataDeserializer;
import consulo.http.json.JsonDataSerializer;
import consulo.http.json.JsonHttpApiHelper;
import consulo.http.page.ApiPageUtil;
import consulo.http.page.LinkHttpHeaderValue;
import consulo.it.HeadlessApplicationExtension;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(HeadlessApplicationExtension.class)
public class HttpApiHelperTest {
    private static final Logger LOG = Logger.getInstance(HttpApiHelperTest.class);
    private static final Gson GSON = new Gson();

    private HttpServer myServer;
    private URI myBase;

    @BeforeEach
    public void startServer() throws IOException {
        myServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        myServer.start();
        myBase = URI.create("http://127.0.0.1:" + myServer.getAddress().getPort());
    }

    @AfterEach
    public void stopServer() {
        myServer.stop(0);
    }

    @Test
    public void gzipBodyIsDecoded() throws Exception {
        AtomicReference<String> acceptEncoding = new AtomicReference<>();
        myServer.createContext("/text", exchange -> {
            acceptEncoding.set(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
            ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
            try (GZIPOutputStream out = new GZIPOutputStream(gzipped)) {
                out.write("hello".getBytes(StandardCharsets.UTF_8));
            }
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            send(exchange, 200, "text/plain; charset=utf-8", gzipped.toByteArray());
        });

        HttpApiHelper helper = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        HttpRequest request = await(helper.request(myBase.resolve("/text"))).GET().build();
        String body = await(helper.sendAndReadBody(request, (stream, mimeType, charset) -> new String(stream.readAllBytes(), charset)));

        assertEquals("hello", body);
        assertEquals("gzip", acceptEncoding.get());
    }

    @Test
    public void errorStatusCarriesTheBody() throws Exception {
        myServer.createContext("/missing", exchange -> send(exchange, 404, "text/plain", "not here".getBytes(StandardCharsets.UTF_8)));

        HttpApiHelper helper = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        HttpRequest request = await(helper.request(myBase.resolve("/missing"))).GET().build();
        ExecutionException error = assertThrows(ExecutionException.class,
            () -> helper.sendAndReadBody(request, (stream, mimeType, charset) -> "unexpected").get(10, TimeUnit.SECONDS));

        HttpStatusErrorException statusError = assertInstanceOf(HttpStatusErrorException.class, error.getCause());
        assertEquals(404, statusError.getStatusCode());
        assertEquals("not here", statusError.getBody());
    }

    @Test
    public void jsonValueAndList() throws Exception {
        myServer.createContext("/user", exchange -> sendJson(exchange, "{\"login\":\"vistall\",\"id\":1}"));
        myServer.createContext("/users", exchange -> sendJson(exchange, "[{\"login\":\"a\",\"id\":1},{\"login\":\"b\",\"id\":2}]"));

        HttpApiHelper http = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        JsonHttpApiHelper json = JsonHttpApiHelper.create(LOG, http, new GsonSerializer(), new GsonDeserializer());

        User user = await(json.loadJsonValueByClass(await(http.request(myBase.resolve("/user"))).GET().build(), User.class)).body();
        assertEquals("vistall", user.login);
        assertEquals(1, user.id);

        List<User> users = await(json.loadJsonListByClass(await(http.request(myBase.resolve("/users"))).GET().build(), User.class)).body();
        assertEquals(2, users.size());
        assertEquals("b", users.get(1).login);
    }

    @Test
    public void graphQlQueryIncludesFragmentsAndMapsThePath() throws Exception {
        AtomicReference<String> sentQuery = new AtomicReference<>();
        myServer.createContext("/graphql", exchange -> {
            JsonObject request = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject();
            sentQuery.set(request.get("query").getAsString());
            sendJson(exchange, "{\"data\":{\"viewer\":{\"login\":\"vistall\",\"id\":2}}}");
        });

        Map<String, String> resources = Map.of(
            "graphql/query/viewer.graphql", "query {\n  viewer {\n    ...user\n  }\n}",
            "graphql/fragment/user.graphql", "fragment user on User {\n  login\n  ...node\n}",
            "graphql/fragment/node.graphql", "fragment node on Node {\n  id\n}"
        );
        CachingGraphQLQueryLoader loader = new CachingGraphQLQueryLoader(path -> {
            String text = resources.get(path);
            return text == null ? null : new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        });

        HttpApiHelper http = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        GraphQLApiHelper graphQL = GraphQLApiHelper.create(LOG, http, new GsonSerializer(), new GsonGraphQLDeserializer());
        HttpRequest request = await(graphQL.query(myBase.resolve("/graphql"), () -> load(loader, "graphql/query/viewer.graphql")));
        User viewer = await(graphQL.loadResponseByClass(request, User.class, "viewer")).body();

        assertEquals("vistall", viewer.login);
        assertEquals(2, viewer.id);
        String query = sentQuery.get();
        assertTrue(query.indexOf("fragment node") < query.indexOf("fragment user"), query);
        assertTrue(query.indexOf("fragment user") < query.indexOf("query {"), query);
    }

    @Test
    public void graphQlErrorsFail() throws Exception {
        myServer.createContext("/graphql", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendJson(exchange, "{\"errors\":[{\"message\":\"Bad credentials\"}]}");
        });

        HttpApiHelper http = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        GraphQLApiHelper graphQL = GraphQLApiHelper.create(LOG, http, new GsonSerializer(), new GsonGraphQLDeserializer());
        HttpRequest request = await(graphQL.query(myBase.resolve("/graphql"), () -> "query { viewer { login } }"));
        ExecutionException error = assertThrows(ExecutionException.class,
            () -> graphQL.loadResponseByClass(request, User.class, "viewer").get(10, TimeUnit.SECONDS));

        GraphQLErrorException graphQLError = assertInstanceOf(GraphQLErrorException.class, error.getCause());
        assertEquals("Bad credentials", graphQLError.getErrors().get(0).getMessage());
    }

    @Test
    public void pagesFollowTheLinkHeader() throws Exception {
        myServer.createContext("/items", exchange -> {
            boolean first = !"page=2".equals(exchange.getRequestURI().getQuery());
            if (first) {
                exchange.getResponseHeaders().add("Link", "<" + myBase + "/items?page=2>; rel=\"next\", <" + myBase + "/items?page=2>; rel=\"last\"");
            }
            sendJson(exchange, first ? "[\"a\",\"b\"]" : "[\"c\"]");
        });

        HttpApiHelper http = HttpApiHelper.create(LOG, HttpClient::newHttpClient);
        JsonHttpApiHelper json = JsonHttpApiHelper.create(LOG, http, new GsonSerializer(), new GsonDeserializer());
        List<String> items = new ArrayList<>();
        await(ApiPageUtil.collectPagesByLinkHeader(myBase.resolve("/items"),
            uri -> http.request(uri).thenCompose(builder -> json.loadJsonListByClass(builder.GET().build(), String.class)),
            response -> items.addAll(response.body())));

        assertEquals(List.of("a", "b", "c"), items);
    }

    @Test
    public void graphQlPagesFoldToList() throws Exception {
        List<Integer> nodes = await(ApiPageUtil.<Integer>foldGQLPagesToList(pagination -> {
            String cursor = pagination.getAfterCursor();
            GraphQLConnectionDTO<Integer> page = cursor == null
                ? new GraphQLConnectionDTO<>(new GraphQLCursorPageInfoDTO(null, false, "c1", true), List.of(1, 2))
                : new GraphQLConnectionDTO<>(new GraphQLCursorPageInfoDTO("c1", true, "c2", false), List.of(3));
            return CompletableFuture.completedFuture(page);
        }));

        assertEquals(List.of(1, 2, 3), nodes);
        assertEquals(Map.of("pageSize", 100), withoutNulls(new GraphQLRequestPagination().asParameters()));
    }

    @Test
    public void linkHeaderParsesGitHubFormat() {
        LinkHttpHeaderValue value = LinkHttpHeaderValue.parse(
            "<https://api.github.com/user/repos?page=3>; rel=\"next\", <https://api.github.com/user/repos?page=50>; rel=\"last\"");

        assertEquals("https://api.github.com/user/repos?page=3", value.nextLink());
        assertEquals("https://api.github.com/user/repos?page=50", value.lastLink());
        assertNull(value.firstLink());
    }

    private static String load(CachingGraphQLQueryLoader loader, String path) {
        try {
            return loader.loadQuery(path);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> withoutNulls(Map<String, @Nullable Object> map) {
        Map<String, Object> result = new java.util.HashMap<>();
        map.forEach((key, value) -> {
            if (value != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        send(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    public static class User {
        public String login = "";
        public int id;
    }

    private static class GsonSerializer implements JsonDataSerializer {
        @Override
        public byte[] toJsonBytes(Object content) {
            return GSON.toJson(content).getBytes(StandardCharsets.UTF_8);
        }
    }

    private static class GsonDeserializer implements JsonDataDeserializer {
        @Override
        public <T> @Nullable T fromJson(Reader bodyReader, Class<T> clazz) {
            return GSON.fromJson(bodyReader, clazz);
        }

        @Override
        public <T> @Nullable T fromJson(Reader bodyReader, Class<T> clazz, Class<?>... classArgs) {
            return GSON.fromJson(bodyReader, TypeToken.getParameterized(clazz, classArgs).getType());
        }

        @Override
        public <T> @Nullable T readJson(InputStream stream, Charset charset, Class<T> clazz) {
            return fromJson(new InputStreamReader(stream, charset), clazz);
        }

        @Override
        public <T> @Nullable T readJson(InputStream stream, Charset charset, Class<T> clazz, Class<?>... classArgs) {
            return fromJson(new InputStreamReader(stream, charset), clazz, classArgs);
        }
    }

    private static class GsonGraphQLDeserializer implements GraphQLDataDeserializer {
        @Override
        public <T> GraphQLResponseDTO<@Nullable T, GraphQLErrorDTO> readAndMapGQLResponse(Reader bodyReader,
                                                                                         String[] pathFromData,
                                                                                         Class<T> clazz) {
            JsonObject root = JsonParser.parseReader(bodyReader).getAsJsonObject();
            T data = null;
            JsonElement element = root.get("data");
            for (String segment : pathFromData) {
                element = element == null || element.isJsonNull() ? null : element.getAsJsonObject().get(segment);
            }
            if (element != null && !element.isJsonNull()) {
                data = GSON.fromJson(element, clazz);
            }
            List<GraphQLErrorDTO> errors = null;
            if (root.has("errors")) {
                errors = new ArrayList<>();
                for (JsonElement error : root.getAsJsonArray("errors")) {
                    errors.add(new GraphQLErrorDTO(error.getAsJsonObject().get("message").getAsString()));
                }
            }
            return new GraphQLResponseDTO<>(data, errors);
        }

        @Override
        public <T> GraphQLResponseDTO<@Nullable T, GraphQLErrorDTO> readAndMapGQLResponse(InputStream stream,
                                                                                         Charset charset,
                                                                                         String[] pathFromData,
                                                                                         Class<T> clazz) {
            return readAndMapGQLResponse(new InputStreamReader(stream, charset), pathFromData, clazz);
        }
    }
}
