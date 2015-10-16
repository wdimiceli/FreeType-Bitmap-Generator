package com.wdimiceli;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;

import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class BitmapGenerator {
    //load a typeface from a FreeType-supported file
    private static FreeType.Face loadFile(FreeType freetype, File file) throws Exception {
        FreeType.Face face = freetype.loadFile(file.getPath());
        if (face != null) {
            int numGlyphs = face.getNumGlyphs();
            System.out.println("Found " + String.valueOf(numGlyphs) + " glyphs in this file.");
        } else {
            throw new Exception("Failed to load file: " + file.toString());
        }
        return face;
    }

    /*
    renders a glyph bitmap and returns a grayscale raster

    will throw an exception if FreeType fails to render the glyph
    returns null if the glyph has no bitmap data
     */
    private static Raster renderGlyph(FreeType.GlyphSlot slot) throws Exception {
        Raster retval = null;
        if (!FreeType.renderGlyph(slot, FreeType.FT_RENDER_MODE_NORMAL)) {
            throw new Exception("FreeType failed to render glyph");
        }
        FreeType.Bitmap bitmap = slot.getBitmap();
        ByteBuffer buffer = bitmap.getBuffer();

        int rows = bitmap.getRows();
        int pitch = bitmap.getPitch();
        //if our glyph was rendered to a non-zero size, transfer the pixels to a raster
        if (rows > 0 && pitch > 0) {
            DataBufferByte databuffer = new DataBufferByte(rows * pitch);
            buffer.get(databuffer.getData());
            int[] offsets = {0};
            //the samplemodel describes the FreeType bitmap so we can transfer
            SampleModel sampleModel = new ComponentSampleModel(
                    databuffer.getDataType(), //byte
                    bitmap.getWidth(),
                    bitmap.getRows(),
                    1, //number of components
                    bitmap.getPitch(), //bytes in each row (pitch vs width)
                    offsets //byte offset to each component
            );
            retval = Raster.createRaster(sampleModel, databuffer, new java.awt.Point(0, 0));
        }
        return retval;
    }

    private static FreeType.GlyphSlot loadGlyph(FreeType.Face face, int index) throws Exception{
        if (!FreeType.loadGlyph(face, index, 0)) {
            throw new Exception("Failed to load glyph with index: " + String.valueOf(index));
        }
        return face.getGlyph();
    }

    /*
    returns the next-highest power of two integer from the input parameter
     */
    private static int nearestPowerOfTwo(int number) {
        int retval = 1;
        while (retval < number) {
            retval *= 2;
        }
        return retval;
    }

    private static int expandDimension(int originalSize, int newRectDimension, boolean restrictPowerOfTwo) {
        //simple heuristic: expand by 5 times the amount we're missing
        //note that the skyline algorithm should have some extra leeway
        //  so we don't want to be stingy
        int minWidth = originalSize + newRectDimension*3;
        if (restrictPowerOfTwo) {
            originalSize = nearestPowerOfTwo(minWidth);
        } else {
            originalSize = minWidth;
        }
        return originalSize;
    }

    /*
    returns a mapping between character codes and glyph indicies into the typeface
    the generator uses the result as a comprehensive list of glyphs to render

    takes a string containing all the characters that we will map (and subsequently render)

    set skipUndefinedChars to true to automatically skip glyphs that the typeface does not contain
        otherwise, the generation will raise an exception and abort
     */
    private static HashMap<Character, Integer> getGlyphIndices(FreeType.Face face, String charSet, boolean skipUndefinedChars) throws Exception {
        HashMap<Character, Integer> glyphIndices = new HashMap<Character, Integer>();
        for (int i = 0; i < charSet.length(); i++) {
            char character = charSet.charAt(i);
            int glyphIndex = FreeType.getCharIndex(face, character);
            //will be true if the typeface doesn't have this character
            if (glyphIndex == 0) {
                String message = "Typeface does not define a glyph for character: " + String.valueOf(character);
                if (skipUndefinedChars) {
                    //this usually spams the output, let's just print the final tally
                    //System.err.println(message + ", skipping...");
                } else {
                    throw new Exception(message);
                }
            } else {
                glyphIndices.put(character, glyphIndex);
            }
        }
        return glyphIndices;
    }

    //Thanks to Michael Borgwardt
    //http://stackoverflow.com/questions/2578233/how-do-i-get-the-set-of-all-letters-in-java-clojure
    private static String buildCharSet(Charset set)
    {
        CharsetEncoder ce = set.newEncoder();
        StringBuilder result = new StringBuilder();
        for(char c=0; c<Character.MAX_VALUE; c++)
        {
            if(ce.canEncode(c) && Character.isLetter(c))
            {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static final String WIDTH_ARG = "width";
    public static final String HEIGHT_ARG = "height";
    public static final String OUT_ARG = "out";
    public static final String IN_ARG = "in";
    public static final String SIZES_ARG = "sizes";
    public static final String IGNORE_UNDEFINED_ARG = "ignoreUndefined";
    public static final String CHARSET_ARG = "charset";
    public static final String POWEROFTWO_ARG = "force-pot";
    public static final String METADATA_ARG = "metadata";

    private static void printUsage() {
        System.out.println("Parses a typeface file and outputs glyphs in a packed bitmap");
        System.out.println("Arguments must be of the form 'argument=value'");
        System.out.println("\tEXAMPLE: width=256 height=256 charset=US_ASCII sizes=16,18,32 out=render.png in=font.ttf");
    }

    public static void main (String[] args) {
        CmdLineArgs argProcessor = new CmdLineArgs();

        //auto makes the sizes 0, which tells us to do auto sizing - we start at 32 and work up from there
        HashMap<String, Integer> autoChoice = new HashMap<String, Integer>();
        autoChoice.put("auto", 0);

        //these are the charsets available to us - could add more later but these are standard Java sets
        HashMap<String, Charset> charsets = new HashMap<String, Charset>();
        charsets.put("US_ASCII", StandardCharsets.US_ASCII);
        charsets.put("ISO_8859_1", StandardCharsets.ISO_8859_1);
        charsets.put("UTF_16", StandardCharsets.UTF_16);

        String[] metadataChoices = {"json", "binary", "embedded"};

        argProcessor.registerArgument(new CmdLineArgs.Argument(
                WIDTH_ARG,
                "horizontal width of output bitmap",
                new CmdLineArgs.IntegerChoiceArgumentHandler(0, 8192, autoChoice),
                "auto"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                HEIGHT_ARG,
                "vertical height of output bitmap",
                new CmdLineArgs.IntegerChoiceArgumentHandler(0, 8192, autoChoice),
                "auto"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                OUT_ARG,
                "output filename",
                new CmdLineArgs.PathArgumentHandler(false),
                "RenderedFont.png"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                IN_ARG,
                "path to input font",
                new CmdLineArgs.PathArgumentHandler(true)
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                SIZES_ARG,
                "comma delimited set of font pixel sizes to render",
                new CmdLineArgs.DelimitedArgumentHandler(",", 255, new CmdLineArgs.IntegerRangeArgumentHandler(8, 128)),
                "16,24,28,32"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                IGNORE_UNDEFINED_ARG,
                "skip any glyphs in the character set that aren't defined in the font",
                new CmdLineArgs.BooleanArgumentHandler(),
                "true"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                POWEROFTWO_ARG,
                "restrict output bitmap to power-of-two sizes",
                new CmdLineArgs.BooleanArgumentHandler(),
                "false"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                CHARSET_ARG,
                "name of the charset to render",
                new CmdLineArgs.ChoiceArgumentHandler(charsets.keySet()),
                "US_ASCII"
        ));
        argProcessor.registerArgument(new CmdLineArgs.Argument(
                METADATA_ARG,
                "metadata output type",
                new CmdLineArgs.ChoiceArgumentHandler(metadataChoices),
                metadataChoices[0]
        ));

        //early out for no-arg situations
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            printUsage();
            System.out.println("---- Arguments ----");
            System.out.println(argProcessor.toUsageString());
            System.exit(0);
        }

        try {
            argProcessor.parseArgs(args);
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }

        //pull all the args back in after we've processed and verified
        int outputWidth = (Integer) argProcessor.getValue(WIDTH_ARG);
        int outputHeight = (Integer) argProcessor.getValue(HEIGHT_ARG);
        //String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
        String charSet = (String) argProcessor.getValue(CHARSET_ARG);
        charSet = buildCharSet(charsets.get(charSet));
        File outFile = (File) argProcessor.getValue(OUT_ARG);
        File inFile = (File) argProcessor.getValue(IN_ARG);
        boolean ignoreUndefinedCharacters = (Boolean) argProcessor.getValue(IGNORE_UNDEFINED_ARG);
        boolean restrictPowerOfTwo = (Boolean) argProcessor.getValue(POWEROFTWO_ARG);
        ArrayList<Integer> sizes = (ArrayList) argProcessor.getValue(SIZES_ARG);
        String metadataEncoding = (String) argProcessor.getValue(METADATA_ARG);

        boolean autoWidth = false;
        boolean autoHeight = false;
        //set these as our starting sizes
        if (outputWidth == 0) {
            autoWidth = true;
            outputWidth = 32;
        }
        if (outputHeight == 0) {
            autoHeight = true;
            outputHeight = 32;
        }

        int glyphsDrawn = 0;
        FreeType freetype = new FreeType();
        try {
            FreeType.Face face = loadFile(freetype, inFile);
            //map the charset indices to the indicies in the font
            HashMap<Character, Integer> glyphIndices = getGlyphIndices(face, charSet, ignoreUndefinedCharacters);
            if (glyphIndices.size() < charSet.length()) {
                System.err.println(String.format("Skipping %d characters...", charSet.length()-glyphIndices.size()));
            }
            BitmapRenderer renderer = new BitmapRenderer(outputWidth, outputHeight);
            TypefaceMetrics record = new TypefaceMetrics();

            for (int size : sizes) {
                //pretty major functionc call here - tells FreeType that we're gonna start working in a new size
                FreeType.setPixelSizes(face, 0, size);
                for (char character : glyphIndices.keySet()) {
                    int glyphIndex = glyphIndices.get(character);
                    //I'm PRETTY SURE this needs to be called after setting a new size even though we've rendered this glyph before
                    FreeType.GlyphSlot slot = loadGlyph(face, glyphIndex);
                    //this raster is the image data that we get from FreeType
                    Raster glyphRaster = renderGlyph(slot);
                    if (glyphRaster != null) {
                        Point p = renderer.putRaster(glyphRaster);
                        //the point will be null if the skyline algorithm couldn't place the glyph anywhere
                        //we'll need to either expand our area or just abort and output what we've got
                        if (p == null) {
                            //if the user doesn't want to expand, just abort
                            if (!autoHeight && !autoWidth) {
                                System.err.println("Failed to render glyph!  Out of room.");
                                break;
                            } else {
                                //try to expand in either or both directions
                                if (autoWidth) {
                                    outputWidth = expandDimension(outputWidth, glyphRaster.getWidth(), restrictPowerOfTwo);
                                    //throw an error if we're getting too big
                                    if (outputWidth > 8192) {
                                        System.err.println("Output bitmap is too large!  Aborting.");
                                        break;
                                    }
                                }
                                if (autoHeight) {
                                    outputHeight = expandDimension(outputHeight, glyphRaster.getHeight(), restrictPowerOfTwo);
                                    if (outputHeight > 8192) {
                                        System.err.println("Output bitmap is too large!  Aborting.");
                                        break;
                                    }
                                }
                                renderer.expand(outputWidth, outputHeight);
                                //this should be a guaranteed success after resizing
                                p = renderer.putRaster(glyphRaster);
                                if (p == null) {
                                    throw new Error("Failed to render glyph!  Unknown Error.");
                                }
                            }
                        }
                        FreeType.GlyphMetrics metrics = slot.getMetrics();

                        TypefaceMetrics.GlyphInfo info = new TypefaceMetrics.GlyphInfo(character);
                        info.x = p.x;
                        info.y = p.y;
                        info.width = glyphRaster.getWidth();
                        info.height = glyphRaster.getHeight();
                        //FreeType keeps its sizes in 26.6 format, so do a quick divide here
                        info.horiAdvance = Math.round(metrics.getHoriAdvance() / 64);
                        info.horiBearingY = Math.round(metrics.getHoriBearingY() / 64);
                        record.recordGlyph(size, info);

                        //build the kerning table between this glyph and every other glyph in the charset
                        if (FreeType.hasKerning(face)) {
                            for (char rhcharacter : glyphIndices.keySet()) {
                                int kerning = FreeType.getKerning(face, glyphIndex, glyphIndices.get(rhcharacter), FreeType.FT_KERNING_DEFAULT);
                                if (kerning > 0) {
                                    info.recordKerning(rhcharacter, kerning);
                                }
                            }
                        }

                        glyphsDrawn++;
                    }
                }
            }

            System.out.println("Glyphs drawn: " + String.valueOf(glyphsDrawn));

            //special case for embedded fonts - we do a bunch of resizing magic here to
            //  make sure there are enough pixels for all the metadata
            if (metadataEncoding.equalsIgnoreCase("embedded")) {
                byte[] bytes = record.getBinary();
                Point trimSize = renderer.trimmedSize();
                int bytesAvailable = renderer.bytesAvailbleForEncoding(trimSize.x, trimSize.y);
                //this is the difference - if it's negative we're short on pixels ans need to add more
                int bytesNeeded = bytes.length - bytesAvailable;
                if (bytesNeeded > 0) {
                    //abort if we're not resizing
                    if (!autoHeight && autoWidth) {
                        throw new Exception("Not enough room embed the metadata.  Please specify a larger bitmap or use auto sizing.");
                    }
                    //expand height first (and only height if possible)
                    if (autoHeight) {
                        //add this many rows to the bitmap
                        int rowsNeeded = (int) Math.ceil(bytesNeeded/renderer.getWidth());
                        //this is here just so we get a POT size if necessary
                        outputHeight = expandDimension(outputHeight, rowsNeeded, restrictPowerOfTwo);
                        //abort if things are getting out of hand
                        if (outputHeight > 8192) {
                            throw new Exception("Output bitmap is too large!  Aborting.");
                        }
                    }
                    //don't resize the width unless we have to
                    //  this is basically the same as the height above
                    if (autoWidth && !autoHeight) {
                        int colsNeeded = (int) Math.ceil(bytesNeeded/renderer.getHeight());
                        outputWidth = expandDimension(outputWidth, colsNeeded, restrictPowerOfTwo);
                        //throw an error if we're getting too big
                        if (outputWidth > 8192) {
                            throw new Exception("Output bitmap is too large!  Aborting.");
                        }
                    }
                    renderer.shrink(outputWidth, outputHeight);
                } else {
                    //if we have enough bytes in the minimum trimmed size, just trim and go
                    renderer.shrink(trimSize.x, trimSize.y);
                }
                renderer.encode(bytes);
            } else {
                if (metadataEncoding.equalsIgnoreCase("json")) {
                    record.saveJson(outFile + ".json");
                } else if (metadataEncoding.equalsIgnoreCase("binary")) {
                    record.saveBinary(outFile + ".fontdata");
                }
                //get trimmed size, ensure POT if necessary
                Point trimSize = renderer.trimmedSize();
                if (restrictPowerOfTwo) {
                    trimSize.x = nearestPowerOfTwo(trimSize.x);
                    trimSize.y = nearestPowerOfTwo(trimSize.y);
                }
                //don't resize if the sizes are the same
                //  this is fairly probable when we're doing POT sizing
                if (outputWidth != trimSize.x || outputHeight != trimSize.y) {
                    renderer.shrink(trimSize.x, trimSize.y);
                }
            }
            renderer.saveToFile(outFile);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error(e.getMessage());
            //System.err.println(e.getMessage());
        }
        freetype.close();
    }
}
