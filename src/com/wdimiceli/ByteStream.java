package com.wdimiceli;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by Wes on 7/26/2014.
 */
public class ByteStream {
    private ByteArrayOutputStream outputStream;
    private DeflaterOutputStream deflater;
    private DataOutputStream out;
    private ByteBuffer bb;

    public ByteStream write(int value, int bytes) throws IOException {
        assert bytes <= 4 && bytes > 0;
        bb.putInt(0, value);
        out.write(bb.array(), 4-bytes, bytes);
        return this;
    }

    public ByteStream write(char value, int bytes) throws IOException {
        assert bytes <= 2 && bytes > 0;
        bb.putChar(0, value);
        out.write(bb.array(), 2-bytes, bytes);
        return this;
    }

    public byte[] close() throws IOException {
        out.close();
        return outputStream.toByteArray();
    }

    public ByteStream(int capacity) {
        outputStream = new ByteArrayOutputStream(capacity);
        deflater = new DeflaterOutputStream(outputStream);
        out = new DataOutputStream(deflater);
        bb = ByteBuffer.allocate(4);
    }
}
