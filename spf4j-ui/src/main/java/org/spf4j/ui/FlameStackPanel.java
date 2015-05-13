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
package org.spf4j.ui;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.List;
import java.util.Map;
import org.spf4j.base.EqualsPredicate;
import org.spf4j.base.Pair;
import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;

/**
 *
 * @author zoly
 */
public final class FlameStackPanel extends StackPanelBase {
    private static final long serialVersionUID = 1L;

    public FlameStackPanel(final SampleNode samples) {
        super(samples);
    }

    @Override
    public int paint(final Graphics2D gr, final double width, final double rowHeight) {
        return paintNode(Method.ROOT, samples, gr,
                0, 0, (int) width, (int) rowHeight, 0);
    }

    @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
    private int paintNode(final Method method, final SampleNode node,
            final Graphics2D g2, final int x, final int py, final int width, final int height, final int depth) {
        int y = py;
        int sampleCount = node.getSampleCount();
        String val = method.toString() + '-' + sampleCount;
        setElementColor(depth, g2);
        g2.setClip(x, y, width, height);
        g2.fillRect(x, y, width, height);
        tooltipDetail.insert(new float[]{x, y}, new float[]{width, height}, Pair.of(method, sampleCount));
        g2.setPaint(Color.BLACK);
        g2.drawString(val, x, y + height - 1);
        g2.setClip(null);
        g2.setPaint(LINK_COLOR);
        g2.drawRect(x, y, width, height);
        Map<Method, SampleNode> children = node.getSubNodes();
        int result = height;
        if (children != null) {
            y += height;
            int relX = x;
            double scale = (double) width / sampleCount;
            int maxY = 0;
            for (Map.Entry<Method, SampleNode> entry : children.entrySet()) {
                SampleNode cnode = entry.getValue();
                // sampleCount -> width
                // childSampleCount -> childWidth
                int childWidth = (int) (scale * cnode.getSampleCount());
                if (childWidth > 0) {
                    maxY = Math.max(maxY, paintNode(entry.getKey(), cnode, g2, relX, y, childWidth, height, depth + 1));
                    relX += childWidth;
                }
            }
            result += maxY;
        }
        return result;
    }

    @Override
    @SuppressFBWarnings("ISB_TOSTRING_APPENDING")
    public String getDetail(final Point location) {
        List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{location.x, location.y}, new float[]{0, 0});
        if (tips.size() >= 1) {
            final Pair<Method, Integer> m = tips.get(0);
            return m.getFirst().toString() + '-' + m.getSecond();
        } else {
            return null;
        }
    }

    @Override
    public void filter() {
        List<Pair<Method, Integer>> tips = tooltipDetail.search(new float[]{xx, yy}, new float[]{0, 0});
        if (tips.size() >= 1) {
            final Method value = tips.get(0).getFirst();
            samples = samples.filteredBy(new EqualsPredicate<Method>(value));
            repaint();
        }
    }

}
