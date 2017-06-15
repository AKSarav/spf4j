/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.io;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressFBWarnings("PREDICTABLE_RANDOM")
public class WriterOutputStreamTest {
    private static final String TEST_STRING = "\u00e0 peine arriv\u00e9s nous entr\u00e2mes dans sa chambre";
    private static final String LARGE_TEST_STRING;

    static {
        final StringBuilder buffer = new StringBuilder();
        for (int i=0; i<100; i++) {
            buffer.append(TEST_STRING);
        }
        LARGE_TEST_STRING = buffer.toString();
    }

    private void assertTestWithSingleByteWrite(final String testString, final String charsetName) throws IOException {
        final byte[] bytes = testString.getBytes(charsetName);
        final StringWriter writer = new StringWriter();
        final WriterOutputStream out = new WriterOutputStream(writer, charsetName);
        for (final byte b : bytes) {
            out.write(b);
        }
        out.close();
        assertEquals(testString, writer.toString());
    }

    private void assertTestWithBufferedWrite(final String testString, final String charsetName) throws IOException {
        final byte[] expected = testString.getBytes(charsetName);
        final StringWriter writer = new StringWriter();
        final WriterOutputStream out = new WriterOutputStream(writer, charsetName);
        int offset = 0;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        while (offset < expected.length) {
            final int length = Math.min(Math.abs(rnd.nextInt() % 128), expected.length - offset);
            out.write(expected, offset, length);
            offset += length;
        }
        out.close();
        assertEquals(testString, writer.toString());
    }

    @Test
    public void testUTF8WithSingleByteWrite() throws IOException {
        assertTestWithSingleByteWrite(TEST_STRING, "UTF-8");
    }

    @Test
    public void testLargeUTF8WithSingleByteWrite() throws IOException {
        assertTestWithSingleByteWrite(LARGE_TEST_STRING, "UTF-8");
    }

    @Test
    public void testUTF8WithBufferedWrite() throws IOException {
        assertTestWithBufferedWrite(TEST_STRING, "UTF-8");
    }

    @Test
    public void testLargeUTF8WithBufferedWrite() throws IOException {
        assertTestWithBufferedWrite(LARGE_TEST_STRING, "UTF-8");
    }

    @Test
    public void testUTF16WithSingleByteWrite() throws IOException {
        try {
            assertTestWithSingleByteWrite(TEST_STRING, "UTF-16");
        } catch (UnsupportedOperationException e){
            if (!System.getProperty("java.vendor").contains("IBM")){
                fail("This test should only throw UOE on IBM JDKs with broken UTF-16");
            }
        }
    }

    @Test
    public void testUTF16WithBufferedWrite() throws IOException {
        try {
            assertTestWithBufferedWrite(TEST_STRING, "UTF-16");
        } catch (UnsupportedOperationException e) {
            if (!System.getProperty("java.vendor").contains("IBM")) {
                fail("This test should only throw UOE on IBM JDKs with broken UTF-16");
            }
        }
    }

    @Test
    public void testUTF16BEWithSingleByteWrite() throws IOException {
        assertTestWithSingleByteWrite(TEST_STRING, "UTF-16BE");
    }

    @Test
    public void testUTF16BEWithBufferedWrite() throws IOException {
        assertTestWithBufferedWrite(TEST_STRING, "UTF-16BE");
    }

    @Test
    public void testUTF16LEWithSingleByteWrite() throws IOException {
        assertTestWithSingleByteWrite(TEST_STRING, "UTF-16LE");
    }

    @Test
    public void testUTF16LEWithBufferedWrite() throws IOException {
        assertTestWithBufferedWrite(TEST_STRING, "UTF-16LE");
    }


    @Test
    public void testFlush() throws IOException {
        final StringWriter writer = new StringWriter();
        final WriterOutputStream out = new WriterOutputStream(writer, "us-ascii", 1024);
        out.write("abc".getBytes(StandardCharsets.US_ASCII));
        assertEquals(0, writer.getBuffer().length());
        out.flush();
        assertEquals("abc", writer.toString());
        out.close();
    }


    @Test
    public void testFlush2() throws IOException {
        final StringWriter writer = new StringWriter();
        final AppendableOutputStream out = new AppendableOutputStream(writer, "us-ascii", 1024);
        out.write("abc".getBytes(StandardCharsets.US_ASCII));
        assertEquals(0, writer.getBuffer().length());
        out.flush();
        assertEquals("abc", writer.toString());
        out.close();
    }

}
