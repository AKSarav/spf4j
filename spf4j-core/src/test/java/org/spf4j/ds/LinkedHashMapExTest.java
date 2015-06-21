
package org.spf4j.ds;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author zoly
 */
public final class LinkedHashMapExTest {


    @Test
    public void testSomeMethod() {
        LinkedMap<Integer, Integer> map = new LinkedHashMapEx(2);
        map.put(10, 10);
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        assertEquals(10, map.keySet().iterator().next().intValue());
        assertEquals(3, map.getLastEntry().getKey().intValue());
        assertEquals(3, map.pollLastEntry().getKey().intValue());
        assertEquals(2, map.pollLastEntry().getKey().intValue());
        assertEquals(1, map.pollLastEntry().getKey().intValue());
        assertEquals(10, map.pollLastEntry().getKey().intValue());
        assertEquals(null, map.pollLastEntry());

    }

}
