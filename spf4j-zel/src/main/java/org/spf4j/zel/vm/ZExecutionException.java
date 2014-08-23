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
package org.spf4j.zel.vm;

public final class ZExecutionException extends Exception {

    private static final long serialVersionUID = 8823469923479284L;

    public ZExecutionException(final String message, final Exception e,
            final String context) {
        super(message, e);
        this.context = context;
    }

    public ZExecutionException(final String message, final Exception e) {
        super(message, e);
        this.context = null;
    }


    public ZExecutionException(final Exception e) {
        super(e);
        this.context = null;
    }
    
      public ZExecutionException(final String msg) {
        super(msg);
        this.context = null;
    }
    

    /**
     * the execution context in which the exception happened
     */
    private final String context;

    /**
     * the execution context where the exception happened
     *
     * @return
     */
    public String getContext() {
        return context;
    }

}
