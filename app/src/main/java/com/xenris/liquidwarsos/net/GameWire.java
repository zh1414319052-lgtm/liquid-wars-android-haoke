// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/** Original wire format: unsigned byte payload size, then big-endian int32 values. */
public final class GameWire {
    public static final int MAX_ARGS = 63;
    private GameWire() {}

    public static int[] read(InputStream input) throws IOException {
        int bytes = input.read();
        if (bytes == -1) return null;
        if (bytes == 0 || bytes % 4 != 0 || bytes > MAX_ARGS * 4)
            throw new IOException("Invalid game frame size");
        byte[] payload = new byte[bytes];
        int offset = 0;
        while (offset < bytes) {
            int count = input.read(payload, offset, bytes - offset);
            if (count < 0) throw new EOFException("Truncated game frame");
            if (count == 0) {
                int one = input.read();
                if (one < 0) throw new EOFException("Truncated game frame");
                payload[offset++] = (byte) one;
            } else offset += count;
        }
        int[] result = new int[bytes / 4];
        ByteBuffer.wrap(payload).asIntBuffer().get(result);
        return result;
    }

    public static void write(OutputStream output, int argc, int[] args) throws IOException {
        if (argc < 1 || argc > MAX_ARGS || args == null || args.length < argc)
            throw new IOException("Invalid game argument count");
        byte[] packet = new byte[1 + argc * 4];
        packet[0] = (byte) (argc * 4);
        ByteBuffer.wrap(packet, 1, argc * 4).asIntBuffer().put(args, 0, argc);
        synchronized (output) {
            output.write(packet);
            output.flush();
        }
    }
}
