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
package org.spf4j.ds;

import java.util.Set;
import javax.annotation.Nullable;
import org.spf4j.base.Pair;

/**
 * a graph interface.
 * @author zoly
 */
public interface Graph<V, E> {
    
    VertexEdges<V, E> getEdges(V vertice);
        
    Pair<V, V> getVertices(E edge);
    
    @Nullable
    E getEdge(V from, V to);
    
    @Nullable
    Set<V> getVertices();
    
    void remove(V vertice);
    
    boolean contains(V vertice);
    
    Graph<V, E> copy();
    
}
