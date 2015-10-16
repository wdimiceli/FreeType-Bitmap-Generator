package com.wdimiceli;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wes on 7/21/2014.
 */
public class BitStream {
    private ByteBuffer bytes;
    //private ArrayList<Byte> bytes;
    private int lastByte;
    private int remainingBits;
    private static int int2byteMask = 0x000000FF;
    public int bitsRecorded;

    private int significantBytes(int significantBits) {
        return (int)Math.ceil(significantBits/8);
    }

    public void outChar(char binary) {
        bytes.putChar(binary);
    }

    public void out(int binary, int maxSignificance) {
        bitsRecorded += maxSignificance;
        int shiftRight = maxSignificance - remainingBits;
        if (shiftRight > 0) {
            lastByte = lastByte | (binary >> shiftRight);
        } else {
            lastByte = lastByte | (binary << (-shiftRight));
        }
        if (maxSignificance > remainingBits) {
            bytes.put((byte)(lastByte&int2byteMask));
            remainingBits =  8 - shiftRight;
            lastByte = (binary<<remainingBits)&0x0000FFFF&(~(int)(Math.pow(2,remainingBits)-1));
        } else if (maxSignificance == remainingBits) {
            bytes.put((byte)(lastByte&int2byteMask));
            remainingBits = 8;
        } else {
            remainingBits -= maxSignificance;
        }
    }

    public ByteBuffer close() {
        if (lastByte != 0) {
            bytes.put((byte)(lastByte&int2byteMask));
        }
        return bytes;
    }

    public BitStream (int capacity) {
        //bytes = new ArrayList<Byte>(capacity);
        bytes = ByteBuffer.allocate(capacity);
        remainingBits = 8;
        lastByte = 0;
        bitsRecorded = 0;
    }
}
