/*
Copyright 2008-2011 Gephi
Authors : Mathieu Jacomy <mathieu.jacomy@gmail.com>
Website : http://www.gephi.org

This file is part of Gephi.

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright 2011 Gephi Consortium. All rights reserved.

The contents of this file are subject to the terms of either the GNU
General Public License Version 3 only ("GPL") or the Common
Development and Distribution License("CDDL") (collectively, the
"License"). You may not use this file except in compliance with the
License. You can obtain a copy of the License at
http://gephi.org/about/legal/license-notice/
or /cddl-1.0.txt and /gpl-3.0.txt. See the License for the
specific language governing permissions and limitations under the
License.  When distributing the software, include this License Header
Notice in each file and include the License files at
/cddl-1.0.txt and /gpl-3.0.txt. If applicable, add the following below the
License Header, with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted [year] [name of copyright owner]"

If you wish your version of this file to be governed by only the CDDL
or only the GPL Version 3, indicate your decision by adding
"[Contributor] elects to include this software in this distribution
under the [CDDL or GPL Version 3] license." If you do not indicate a
single choice of license, a recipient has the option to distribute
your version of this file under either the CDDL, the GPL Version 3 or
to extend the choice of license to its licensees as provided above.
However, if you add GPL Version 3 code and therefore, elected the GPL
Version 3 license, then the option applies only if the new code is
made subject to such option by the copyright holder.

Contributor(s):

Portions Copyrighted 2011 Gephi Consortium.
 */
package kco.forceatlas2;

import org.gephi.graph.spi.LayoutData;

/**
 * Data stored in Nodes and used by ForceAtlas2
 *
 * @author Mathieu Jacomy
 */
public interface ForceAtlas2LayoutData extends LayoutData {


    public double getDx();

    public void setDx(double dx);

    public double getDy();

    public void setDy(double dy);

    public double getDz();

    public void setDz(double dz);

    public double getOld_dx();

    public void setOld_dx(double old_dx);

    public double getOld_dy();

    public void setOld_dy(double old_dy);

    public double getOld_dz();

    public void setOld_dz(double old_dz);

    public double getMass();

    public void setMass(double mass);

    // synchronized augment functions, only used for updating attraction force
    public void augmentDx(double ddx);

    public void augmentDy(double ddy);

    public void augmentDz(double ddz);
}
