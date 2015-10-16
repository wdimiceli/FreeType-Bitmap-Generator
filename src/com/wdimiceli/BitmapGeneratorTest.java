package com.wdimiceli;

import junit.framework.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;

public class BitmapGeneratorTest {

    @Test (expected=Error.class)
    public void testMainNegativeWidth() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "width=-10"
        };
        BitmapGenerator.main(args);
    }

    @Test (expected=Error.class)
    public void testMainLargeWidth() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "width=99999"
        };
        BitmapGenerator.main(args);
    }

    @Test (expected=Error.class)
    public void testMainDuplicateWidth() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "width=512",
                "width=auto"
        };
        BitmapGenerator.main(args);
    }

    @Test (expected=Error.class)
    public void testMainBadSize() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "sizes=5,bad,10"
        };
        BitmapGenerator.main(args);
    }

    @Test (expected=Error.class)
    public void testMainNoInput() throws Exception {
        String[] args = {
                "width=512"
        };
        BitmapGenerator.main(args);
    }

    @Test (expected=Error.class)
    public void testMainNoFont() throws Exception {
        String[] args = {
                "in=asdfasdfasdf"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainWeirdCase() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputWeirdCase.png",
                "wIdTh=512"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainPOTExpansion() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputPOT.png",
                "wIdTh=auto",
                "height=auto",
                "force-pot=true",
                "sizes=32,33,34,35,36,37,38,39,40"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainExpansion() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputExpanded.png",
                "wIdTh=0",
                "height=0",
                "force-pot=false",
                "sizes=16,24,34,35,36,37,38,39,40"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainUTF16() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputUTF16.png",
                "force-pot=false",
                "sizes=16,24,34",
                "charset=UTF_16"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainISO() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputISO.png",
                "force-pot=false",
                "sizes=16,24,34",
                "charset=ISO_8859_1"
        };
        BitmapGenerator.main(args);
    }

//    @Test
//    public void testMainGiant() throws Exception {
//        String[] args = {
//                "in=assets/c_and_l.ttf",
//                "out=test/testOutputGiant.png",
//                "force-pot=false",
//                "sizes=16,24,34,38,42,44,48,52,56,58,62,68,72,75,76,78,80,82,85,88,92",
//                "charset=UTF_16"
//        };
//        BitmapGenerator.main(args);
//    }

    @Test
    public void testMainBinary() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputBinary.png",
                "metadata=binary"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainEmbedded() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutputEmbedded.png",
                "metadata=embedded"
        };
        BitmapGenerator.main(args);
    }

    @Test
    public void testMainAllArguments() throws Exception {
        String[] args = {
                "in=assets/c_and_l.ttf",
                "out=test/testOutput.png",
                "sizes=24,26",
                "width=256",
                "height=256",
                "charset=US_ASCII",
                "force-pot=false",
                "ignoreundefined=true"
        };
        BitmapGenerator.main(args);
    }
}