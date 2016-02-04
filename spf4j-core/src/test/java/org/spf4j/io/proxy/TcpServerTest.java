
package org.spf4j.io.proxy;

import org.spf4j.io.tcp.proxy.ProxyClientHandler;
import org.spf4j.io.tcp.TcpServer;
import com.google.common.io.ByteStreams;
import com.google.common.net.HostAndPort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.base.AbstractRunnable;
import org.spf4j.concurrent.DefaultScheduler;

/**
 *
 * @author zoly
 */
public class TcpServerTest {


    @Test(timeout = 10000)
    public void testProxy() throws IOException, InterruptedException {
        String testSite = "charizard.homeunix.net";
        ForkJoinPool pool = new ForkJoinPool(1024);
        try (TcpServer server = new TcpServer(pool,
                new ProxyClientHandler(HostAndPort.fromParts(testSite, 80), 10000, 5000),
                1976, 10)) {
            server.runInBackground();
            server.waitForStared();
            byte [] originalContent = readfromSite("http://" + testSite);
            long start = System.currentTimeMillis();
            originalContent = readfromSite("http://" + testSite);
            long time1 = System.currentTimeMillis();
            byte [] proxiedContent = readfromSite("http://localhost:1976");
            long time2 = System.currentTimeMillis();
            System.out.println("Direct = " + (time1 - start) + " ms, proxied = " + (time2 - time1));
            Assert.assertArrayEquals(originalContent, proxiedContent);
        }
    }

    @Test(expected = java.net.SocketException.class, timeout = 60000)
    public void testTimeout() throws IOException, InterruptedException {
        String testSite = "10.10.10.10";
        ForkJoinPool pool = new ForkJoinPool(1024);
        try (TcpServer server = new TcpServer(pool,
                new ProxyClientHandler(HostAndPort.fromParts(testSite, 80), 10000, 5000),
                1977, 10)) {
            server.runInBackground();
            server.waitForStared();
            readfromSite("http://localhost:1977"); // results in a socket exception after 5 seconds
            Assert.fail("Should timeout");
        }
    }

    @Test(expected = java.net.SocketException.class, timeout = 10000)
    public void testKill() throws IOException, InterruptedException {
        String testSite = "10.10.10.10";
        ForkJoinPool pool = new ForkJoinPool(1024);
        try (TcpServer server = new TcpServer(pool,
                new ProxyClientHandler(HostAndPort.fromParts(testSite, 80), 10000, 10000),
                1977, 10)) {
            server.runInBackground();
            server.waitForStared();
            DefaultScheduler.INSTANCE.schedule(new AbstractRunnable(true) {
                @Override
                public void doRun() throws IOException {
                    server.close();
                }
            }, 2, TimeUnit.SECONDS);
            readfromSite("http://localhost:1977"); // results in a socket exception after 5 seconds
            Assert.fail("Should timeout");
        }
    }


    private static byte [] readfromSite(String siteUrl) throws IOException {
        URL url = new URL(siteUrl);
        InputStream stream = url.openStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteStreams.copy(stream, bos);
        return bos.toByteArray();
    }

}
