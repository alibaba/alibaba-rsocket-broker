/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.rsocket.transport.netty;

import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.uri.UriHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link UriHandler} that creates {@link WebsocketClientTransport}s and {@link
 * WebsocketServerTransport}s.
 */
public final class WebsocketUriHandler implements UriHandler {

    private static final List<String> SCHEME = Arrays.asList("ws", "wss", "http", "https");

    @Override
    public Optional<ClientTransport> buildClient(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        if (SCHEME.stream().noneMatch(scheme -> scheme.equals(uri.getScheme()))) {
            return Optional.empty();
        }

        return Optional.of(WebsocketClientTransport.create(uri));
    }

    @Override
    public Optional<ServerTransport<?>> buildServer(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        if (SCHEME.stream().noneMatch(scheme -> scheme.equals(uri.getScheme()))) {
            return Optional.empty();
        }

        int port = isSecure(uri) ? getPort(uri, 443) : getPort(uri, 80);

        return Optional.of(WebsocketServerTransport.create(uri.getHost(), port));
    }

    /**
     * Returns the port of a URI. If the port is unset (i.e. {@code -1}) then returns the {@code
     * defaultPort}.
     *
     * @param uri         the URI to extract the port from
     * @param defaultPort the default to use if the port is unset
     * @return the port of a URI or {@code defaultPort} if unset
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static int getPort(URI uri, int defaultPort) {
        Objects.requireNonNull(uri, "uri must not be null");
        return uri.getPort() == -1 ? defaultPort : uri.getPort();
    }

    /**
     * Returns whether the URI has a secure schema. Secure is defined as being either {@code wss} or
     * {@code https}.
     *
     * @param uri the URI to examine
     * @return whether the URI has a secure schema
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public static boolean isSecure(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");
        return uri.getScheme().equals("wss") || uri.getScheme().equals("https");
    }
}
