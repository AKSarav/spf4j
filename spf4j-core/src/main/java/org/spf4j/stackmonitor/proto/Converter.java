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
package org.spf4j.stackmonitor.proto;

import org.spf4j.stackmonitor.Method;
import org.spf4j.stackmonitor.SampleNode;
import org.spf4j.stackmonitor.proto.gen.ProtoSampleNodes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zoly
 */
public final class Converter {
    
    private Converter() { }
    
    public static ProtoSampleNodes.Method fromMethodToProto(final Method m) {
        return ProtoSampleNodes.Method.newBuilder().setMethodName(m.getMethodName())
                .setDeclaringClass(m.getDeclaringClass()).build();
    }
    
    public static ProtoSampleNodes.SampleNode fromSampleNodeToProto(final SampleNode node) {
        
        ProtoSampleNodes.SampleNode.Builder resultBuilder
                = ProtoSampleNodes.SampleNode.newBuilder().setCount(node.getSampleCount());
        
        Map<Method, SampleNode> subNodes = node.getSubNodes();
        if (subNodes != null) {
           for (Map.Entry<Method, SampleNode> entry : subNodes.entrySet()) {
               resultBuilder.addSubNodes(
               ProtoSampleNodes.SamplePair.newBuilder().setMethod(fromMethodToProto(entry.getKey())).
                        setNode(fromSampleNodeToProto(entry.getValue())).build());
           }
        }
        return resultBuilder.build();
    }
    
    
    
    public static SampleNode  fromProtoToSampleNode(final ProtoSampleNodes.SampleNodeOrBuilder node) {
        
        Map<Method, SampleNode> subNodes = null;
        
        List<ProtoSampleNodes.SamplePair> sns =  node.getSubNodesList();
        if (sns != null) {
            subNodes = new HashMap<Method, SampleNode>();
            for (ProtoSampleNodes.SamplePair pair : sns) {
                final ProtoSampleNodes.Method method = pair.getMethod();
                subNodes.put(new Method(pair.getMethod().getDeclaringClass(), method.getMethodName()),
                        fromProtoToSampleNode(pair.getNode()));
            }
        }
        return new SampleNode(node.getCount(), subNodes);
        
        
    }
    
    
}
