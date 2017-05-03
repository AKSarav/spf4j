
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
package org.spf4j.perf.impl.chart;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jfree.chart.axis.NumberTickUnit;
import org.joda.time.format.DateTimeFormatter;

/**
 * This class although implements Serializable is not implemented correctly to serialize. Since I do not intend to
 * serialize this, I will skip the proper implementation.
 *
 * @author zoly
 */
@SuppressFBWarnings({"NFF_NON_FUNCTIONAL_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"})
class TimestampTickUnitImpl extends NumberTickUnit {

  private static final long serialVersionUID = 0L;
  private final long[] timestamps;
  private final long stepMillis;
  private final transient DateTimeFormatter formatter;

  TimestampTickUnitImpl(final double size,
          final long[] timestamps, final long stepMillis, final DateTimeFormatter formatter) {
    super(size);
    this.timestamps = timestamps;
    this.formatter = formatter;
    this.stepMillis = stepMillis;
  }

  @Override
  public String valueToString(final double value) {
    int ival = (int) Math.round(value);
    long val;
    if (ival >= timestamps.length) {
      val = timestamps[timestamps.length - 1] + stepMillis * (ival - timestamps.length + 1);
    } else if (ival < 0) {
      val = timestamps[0] + ival * stepMillis;
    } else {
      val = timestamps[ival];
    }
    return formatter.print(val);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 89 * hash + java.util.Arrays.hashCode(this.timestamps);
    hash = 89 * hash + (int) (this.stepMillis ^ (this.stepMillis >>> 32));
    return 89 * hash + (this.formatter != null ? this.formatter.hashCode() : 0);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final TimestampTickUnitImpl other = (TimestampTickUnitImpl) obj;
    if (!java.util.Arrays.equals(this.timestamps, other.timestamps)) {
      return false;
    }
    if (this.stepMillis != other.stepMillis) {
      return false;
    }
    return !(this.formatter != other.formatter
            && (this.formatter == null || !this.formatter.equals(other.formatter)));
  }

}
