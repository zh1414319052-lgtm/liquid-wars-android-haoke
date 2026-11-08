// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class GameTransport {
    private GameTransport() {}
    public interface Connection extends Closeable {
        InputStream input() throws IOException;
        OutputStream output() throws IOException;
    }
    public interface Listener extends Closeable { Connection accept() throws IOException; }
    public interface ListenerFactory {
        Listener open() throws IOException;
        default int clientLimit() { return 5; }
    }
    public interface Connector extends Closeable {
        Connection connect() throws IOException;
        default long timeoutMillis() { return 5000; }
    }
    public static void close(Closeable value) {
        if (value != null) try { value.close(); } catch (IOException ignored) { }
    }
}
