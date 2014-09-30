/*
 * Copyright (c) 2001, Zoltan Farkas All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.spf4j.concurrent;

import com.google.common.io.BaseEncoding;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Unique ID Generator Based on the assumptions:
 * 1. host IP address is used for uniqueness
 * 
 * @author zoly
 */
public final class UIDGenerator {
    
    private final Sequence sequence;

    private final String base;
    
    private final int maxSize;
    
    public UIDGenerator(final Sequence sequence) {
        this.sequence = sequence;
       
        try {
            StringBuilder sb = new StringBuilder().append(
                            BaseEncoding.base64().encode(
                                    NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress()))
                            .append('@');
            appendUnsignedString(sb, org.spf4j.base.Runtime.PID, 5);
            sb.append('T');
            appendUnsignedString(sb, System.currentTimeMillis() / 1000, 5);
            sb.append('>');
            base = sb.toString();
        } catch (UnknownHostException | SocketException ex) {
            throw new RuntimeException(ex);
        }
        maxSize = base.length() + 16;
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    public String next() {
        StringBuilder result = new StringBuilder(maxSize);
        result.append(base);
        appendUnsignedString(result, sequence.next(), 5);
        return result.toString();
    }
    
    private static final char[] DIGITS = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };
    
    private static void appendUnsignedString(final StringBuilder sb, final long nr, final int shift) {
        long i = nr;
        char[] buf = new char[64];
        int charPos = 64;
        int radix = 1 << shift;
        long mask = radix - 1;
        do {
            buf[--charPos] = DIGITS[(int) (i & mask)];
            i >>>= shift;
        } while (i != 0);
        sb.append(buf, charPos, (64 - charPos));
    }
    
}
