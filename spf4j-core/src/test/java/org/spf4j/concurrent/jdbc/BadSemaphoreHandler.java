package org.spf4j.concurrent.jdbc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.h2.jdbcx.JdbcDataSource;

/**
 *
 * @author zoly
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class BadSemaphoreHandler {

  static {
    System.setProperty("spf4j.heartbeat.intervalMillis", "2000"); // 2 second heartbeat
  }

  public static void main(final String[] args) throws InterruptedException, TimeoutException {

    String connectionString = args[0];
    String semaphoreName = args[1];
    JdbcDataSource ds = new JdbcDataSource();
    ds.setURL(connectionString);
    ds.setUser("sa");
    ds.setPassword("sa");
    JdbcSemaphore semaphore = new JdbcSemaphore(ds, semaphoreName, 3);
    semaphore.acquire(1, 1L, TimeUnit.SECONDS);
    System.exit(0);
  }

}
