package org.spf4j.concurrent.jdbc;

import java.io.Serializable;

/*
 * CREATE TABLE HEARTBEATS (
 *  OWNER VARCHAR(255) NOT NULL,
 *  INTERVAL_MILLIS bigint(20) NOT NULL,
 *  LAST_HEARTBEAT_INSTANT_MILLIS bigint(20) NOT NULL,
 *  PRIMARY KEY (OWNER),
 *  UNIQUE KEY HEARTBEATS_PK (OWNER)
 * );
 *
 * Table description for storing heartbeats for processes. (OWNER)
 * The main purpose of thistable is to detect dead OWNERS (when their heart stops beating)
 * OWNER = String column uniquely identifying a process.
 * INTERVAL_MILLIS - the delay between heartbeats.
 * LAST_HEARTBEAT_INSTANT_MILLIS - the millis since epoch when the last heartbeat happened.
 */
public final class HeartBeatTableDesc  implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String tableName;
  private final String ownerColumn;
  private final String intervalColumn;
  private final String lastHeartbeatColumn;

  /**
   * MSSQL = DATEDIFF(ms, '1970-01-01 00:00:00', GETUTCDATE())
   * ORACLE = (SYSDATE - TO_DATE('01-01-1970 00:00:00', 'DD-MM-YYYY HH24:MI:SS')) * 24 * 3600000
   * H2 = TIMESTAMPDIFF('MILLISECOND', timestamp '1970-01-01 00:00:00', CURRENT_TIMESTAMP())
   */
  private final String currentTimeMillisFunc;

  public HeartBeatTableDesc(final String tableName, final String ownerColun,
          final String intervalColumn, final String lastHeartbeatColumn, final String currentTimeMillisFunc) {
    this.tableName = tableName;
    this.ownerColumn = ownerColun;
    this.intervalColumn = intervalColumn;
    this.lastHeartbeatColumn = lastHeartbeatColumn;
    this.currentTimeMillisFunc = currentTimeMillisFunc;
  }

  public String getTableName() {
    return tableName;
  }

  public String getOwnerColumn() {
    return ownerColumn;
  }

  public String getIntervalColumn() {
    return intervalColumn;
  }

  public String getLastHeartbeatColumn() {
    return lastHeartbeatColumn;
  }

  public String getCurrentTimeMillisFunc() {
    return currentTimeMillisFunc;
  }

  @Override
  public String toString() {
    return "HeartbeatTableDesc{" + "tableName=" + tableName + ", ownerColun=" + ownerColumn + ", intervalColumn="
            + intervalColumn + ", lastHeartbeatColumn=" + lastHeartbeatColumn + '}';
  }

}
