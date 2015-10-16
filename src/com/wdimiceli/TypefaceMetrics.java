package com.wdimiceli;

import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by Wes on 7/21/2014.
 */
public class TypefaceMetrics {
    private HashMap<Integer, HashMap<Character, GlyphInfo> > glyphSets;

    public static class GlyphInfo implements JSONAware{
        public final char character;
        public HashMap<Character, Integer> kerningTable;
        public int horiBearingY;
        public int horiAdvance;
        public int x;
        public int y;
        public int width;
        public int height;

        public String toJSONString() {
            JSONObject obj = new JSONObject();
            obj.put("horiBearingY", new Integer(horiBearingY));
            obj.put("horiAdvance", new Integer(horiAdvance));
            obj.put("x", new Integer(x));
            obj.put("y", new Integer(y));
            obj.put("width", new Integer(width));
            obj.put("height", new Integer(height));
            obj.put("kerningTable", new JSONObject(kerningTable));
            return obj.toJSONString();
        }

        public void recordKerning(char rhchar, int distance) {
            kerningTable.put(rhchar, distance);
        }

        public GlyphInfo(char in_character) {
            character = in_character;
            kerningTable = new HashMap<Character, Integer>();
        }
    }

    //calculate the max number of bits that we need to contain this value
    private int calculatePrecision(int value) {
        int retval = 1;
        int i = 1;
        while (i < value) {
            i *= 2;
            retval++;
        }
        return retval;
    }

    private int calculatePrecisionBytes(int value) {
        return (int) Math.ceil(calculatePrecision(value)/8.0);
    }

    public byte[] getBinary() throws IOException {
        //1 byte - number of sizes
        //1 byte - sizing precision in bytes [SP]
        //* size list ---
        //SP bytes - pixel size
        //* size list ---
        //2 bytes - number of glyphs
        //1 byte - location precision in bytes [LP]
        //* glyph list ---
        //2 bytes - char code UTF-16
        //SP bytes - horizontal bearing Y
        //SP bytes - horizontal advance
        //LP bytes - x
        //LP bytes - Y
        //LP bytes - width
        //LP bytes - height
        //2 bytes - kern table size
        //* kern list ---
        //2 bytes - char code UTF-16
        //SP bytes - kerning
        //* kern list ---
        //* glyph list ---

        //start with 32k
        ByteStream bs = new ByteStream(1024*32);

        //number of pixel sizes
        try {
            bs.write(glyphSets.size(), 1);

            //number of bits we need to contain the font metric data
            //assume this won't exceed an order of magnitude beyond the largest pixel size
            int sizingPrecision = 0;
            for (int size : glyphSets.keySet()) {
                sizingPrecision = Math.max(sizingPrecision, size);
            }
            sizingPrecision = calculatePrecisionBytes(sizingPrecision);
            bs.write(sizingPrecision, 1);

            //output each pixel size
            for (int size : glyphSets.keySet()) {
                bs.write(size, sizingPrecision);
            }

            for (int size : glyphSets.keySet()) {
                HashMap<Character, GlyphInfo> glyphs = glyphSets.get(size);
                //number of glyphs in this set - write the last two bytes
                bs.write(glyphs.size(), 2);
                int locationPrecision = 0;
                //grab the largest coordinate or rectangle size to estimate the
                //precision we need to store bitmap coordinate info
                for (GlyphInfo info : glyphs.values()) {
                    locationPrecision = Math.max(info.x, locationPrecision);
                    locationPrecision = Math.max(info.y, locationPrecision);
                    locationPrecision = Math.max(info.width, locationPrecision);
                    locationPrecision = Math.max(info.height, locationPrecision);
                }
                locationPrecision = calculatePrecisionBytes(locationPrecision);
                bs.write(locationPrecision, 1);

                for (Character glyphChar : glyphs.keySet()) {
                    GlyphInfo info = glyphs.get(glyphChar);

                    //char code
                    bs.write(glyphChar, 2);
                    //glyph informations
                    bs.write(info.horiBearingY, sizingPrecision);
                    bs.write(info.horiAdvance, sizingPrecision);
                    bs.write(info.x, locationPrecision);
                    bs.write(info.y, locationPrecision);
                    bs.write(info.width, locationPrecision);
                    bs.write(info.height, locationPrecision);

                    //now we count up each entry in the kerning table, not counting ones that are zero
                    int kerntablesize = 0;
                    for (int kerning : info.kerningTable.values()) {
                        if (kerning > 0) {
                            kerntablesize++;
                        }
                    }
                    bs.write(kerntablesize, 2);

                    //output each of the values in the kerning table, again skipping blank entries
                    for (char kernChar : info.kerningTable.keySet()) {
                        bs.write(kernChar, 2);
                        int kerningValue = info.kerningTable.get(kernChar);
                        if (kerningValue > 0) {
                            bs.write(kerningValue, sizingPrecision);
                        }
                    }
                }
            }
            return bs.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public void saveBinary(String filename) throws Exception {
        File file = new File(filename);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream out = new FileOutputStream(file);

        byte[] bytes = getBinary();
        System.out.println(String.format("Writing font data: %d bytes", bytes.length));
        out.write(bytes);
        out.flush();
        out.close();
    }

    public void saveJson(String filename) throws Exception {
        String jsonText = JSONValue.toJSONString(glyphSets);
        PrintWriter writer = new PrintWriter(filename, "UTF-8");
        writer.println(jsonText);
        writer.close();
    }

    public void recordGlyph(int pixelSize, GlyphInfo glyph) {
        if (!glyphSets.containsKey(pixelSize)) {
            glyphSets.put(pixelSize, new HashMap<Character, GlyphInfo>());
        }
        glyphSets.get(pixelSize).put(glyph.character, glyph);
    }

    public TypefaceMetrics() {
        glyphSets = new HashMap<Integer, HashMap<Character, GlyphInfo> >();
    }
}
