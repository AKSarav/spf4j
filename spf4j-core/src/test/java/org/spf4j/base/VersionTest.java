package org.spf4j.base;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class VersionTest {

    @Test
    public void testVersion() {
        Version version1 = new Version("1.u1.3");
        Version version2 = new Version("1.u10.3");
        Assert.assertTrue(version1.compareTo(version2) < 0);
        System.out.println("version1" + version1);
        Assert.assertEquals(Integer.valueOf(3), version1.getComponents()[3]);
        Version javaVersion = new Version(org.spf4j.base.Runtime.JAVA_VERSION);
        System.out.println("version1" + javaVersion + ", " + javaVersion.getImage());
        Assert.assertTrue(javaVersion.compareTo(new Version ("1.6.0_1")) > 0);
    }
     
}
