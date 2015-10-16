package com.wdimiceli;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

/**
 * Created by Wes on 7/21/2014.
 */
public class BitmapRenderer {
    private BufferedImage output;
    private SkylineBottomLeft skyline;

    public Point putRaster(Raster raster) {
        Point retval = null;
        Point placement = skyline.place(raster.getWidth(), raster.getHeight());
        if (placement != null) {
            output.getRaster().setRect(placement.x, placement.y, raster);
            retval = placement;
        }
        return retval;
    }

    public void saveToFile(File file) throws IOException, SecurityException {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        ImageIO.write(output, "png", file);
    }

    public void expand(int newWidth, int newHeight) {
        WritableRaster newRaster = output.getRaster().createCompatibleWritableRaster(newWidth, newHeight);
        BufferedImage newImage = new BufferedImage(output.getColorModel(), newRaster, output.getColorModel().isAlphaPremultiplied(), null);
        newImage.getGraphics().drawImage(output, 0, 0, null);
        newImage.flush();
        output.flush();
        output = newImage;
        skyline.expand(newWidth, newHeight);
    }

    public void shrink(int newWidth, int newHeight) {
        WritableRaster newRaster = output.getRaster().createCompatibleWritableRaster(newWidth, newHeight);
        BufferedImage newImage = new BufferedImage(output.getColorModel(), newRaster, output.getColorModel().isAlphaPremultiplied(), null);
        newImage.getGraphics().drawImage(output, 0, 0, null);
        newImage.flush();
        output.flush();
        output = newImage;
        skyline.shrink(newWidth, newHeight);
    }

    public int getWidth() {
        return output.getWidth();
    }

    public int getHeight() {
        return output.getHeight();
    }

    public Point trimmedSize() {
        return skyline.boundingBox();
    }

    /*
    returns the size in bytes of encoding area available
     */
    public int bytesAvailbleForEncoding() {
        return bytesAvailbleForEncoding(output.getWidth(), output.getHeight());
    }
    public int bytesAvailbleForEncoding(int width, int height) {
        int skylineFilledPixels = skyline.filledArea();
        return (width * height - skylineFilledPixels) + (int)Math.floor(skylineFilledPixels / 8);
    }

    /*
    encodes one byte for each pixel into the area given (x,y,width,height)

    starts at offset and writes count bytes

    returns the number of bytes encoded
     */
    private int encodeBytes(int areaX, int areaY, int width, int height, int offset, int count, byte[] bytes) {
        WritableRaster raster = output.getRaster();
        int bytesWritten = 0;
        int areaSize = width*height;
        for (int i = 0; i < count && i < areaSize; i++, bytesWritten++) {
            byte b = bytes[offset + i];
            int y = areaY + (int) Math.floor(i / width);
            int x = areaX + (i % width);
            raster.setSample(x, y, 0, b);
            bytesWritten++;
        }
        return bytesWritten;
    }

    /*
    encodes one bit for each pixel using the LSB into the area given (x,y,width,height)

    starts at offset and writes count bytes
    since we're only writing one bit per pixel, this requires an area of count*8 pixels

    returns the number of bytes (NOT BITS) encoded
     */
    private int encodeBits(int areaX, int areaY, int width, int height, int offset, int count, byte[] bytes) {
        WritableRaster raster = output.getRaster();
        int bytesWritten = 0;
        int areaSize = width*height;
        for (int i = 0; i < count && i < areaSize; i++, bytesWritten++) {
            byte b = bytes[offset+i];
            for (int bitIndex = 7; bitIndex >= 0; bitIndex--) {
                int bitMask = (int) Math.pow(2, bitIndex);
                int bitSet = (b & bitMask) > 0 ? 1 : 0;
                int pixelNumber = i * 8 + (7 - bitIndex);
                int y = areaY + (int) Math.floor(pixelNumber / width);
                int x = areaX + (pixelNumber % width);
                int sample = raster.getSample(x, y, 0);
                sample = (sample & Integer.MAX_VALUE - 1) | bitSet;
                raster.setSample(x, y, 0, sample);
            }
        }
        return bytesWritten;
    }

    /*
    encodes each byte using the LSB of pixels below the skyline
        then uses the area above the skyline

    returns the number of bytes encoded -- see bytesAvailableForEncoding()
     */
    public int encode(byte[] bytes) {
        int bytesEncoded = 0;
        if (bytes.length > 0) {
            //these are the skyline stripes - height corresponds to area not filled by the skyline
            List<Rectangle> stripes = skyline.getSkyline();
            //encode stripe information in the first row for the decoder
            //  a one indicates the end of a stripe
            byte[] stripeInformation = new byte[output.getWidth()];
            for (Rectangle r : stripes) {
                //we have to encode skyline boundary, so make sure this stripe has at least 16 bits of info
                //otherwise we need to skip it
                if (r.width*(r.y-1) < 16) {
                    //fill with ones, the decoder will skip across 0-width stripes
                    Arrays.fill(stripeInformation, r.x, r.x + r.width - 1, (byte) 1);
                } else {
                    //fill the length of the stripe with zeroes...
                    Arrays.fill(stripeInformation, r.x, r.x + r.width - 2, (byte) 0);
                    //...and a one on the end...
                    stripeInformation[r.x + r.width - 1] = 1;
                }
            }
            //...and throw that info into the image
            encodeBits(0, 0, output.getWidth(), 1, 0, stripeInformation.length, stripeInformation);
            for (Rectangle r : stripes) {
                //below skyline
                int bytesBelowSkyline = (int) Math.floor(r.y * r.width / 8);
                int bytesToEncode = Math.min(bytes.length - bytesEncoded, bytesBelowSkyline);
                if (bytesToEncode <= 0) {
                    break;
                }
                //the y value is a one because we don't want to stomp on the strip info
                encodeBits(r.x, 1, r.width, r.y, bytesEncoded, bytesToEncode, bytes);
                bytesEncoded += bytesToEncode;
                //above skyline
                int bytesAboveSkyline = r.height * r.width;
                bytesToEncode = Math.min(bytes.length - bytesEncoded, bytesAboveSkyline);
                if (bytesToEncode <= 0) {
                    break;
                }
                encodeBytes(r.x, r.y, r.width, r.height, bytesEncoded, bytesToEncode, bytes);
                bytesEncoded += bytesToEncode;
            }
        }
        return bytesEncoded;
    }

    public BitmapRenderer(int width, int height) {
        output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        skyline = new SkylineBottomLeft(width, height);
    }
}
