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

import org.gephi.graph.api.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Barnes Hut optimization
 *
 * @author Mathieu Jacomy
 */
public class Region {

    private double mass;
    private double massCenterX;
    private double massCenterY;
    private double massCenterZ;
    private double size;
    private List<Node> nodes;
    private List<Region> subregions = new ArrayList<>();
    private final boolean is3d;

    public Region(Node[] nodes, boolean is3d) {
        this.nodes = new ArrayList<>();
        this.nodes.addAll(Arrays.asList(nodes));
        this.is3d = is3d;
        updateMassAndGeometry();

    }

    public Region(ArrayList<Node> nodes, boolean is3d) {
        this.nodes = new ArrayList<>(nodes);
        this.is3d = is3d;
        updateMassAndGeometry();
    }

    void updateMassAndGeometry() {
        if (nodes.size() > 1) {
            // Compute Mass
            mass = 0;
            double massSumX = 0;
            double massSumY = 0;
            double massSumZ = 0;
            for (Node n : nodes) {
                ForceAtlas2LayoutData nLayout = n.getLayoutData();
                mass += nLayout.getMass();
                massSumX += n.x() * nLayout.getMass();
                massSumY += n.y() * nLayout.getMass();
                massSumZ += is3d ? n.z() * nLayout.getMass() : 0;
            }
            massCenterX = massSumX / mass;
            massCenterY = massSumY / mass;
            massCenterZ = is3d ? massSumZ / mass : 0;

            // Compute size
            size = Double.MIN_VALUE;
            for (Node n : nodes) {
                double z = is3d ? (n.z() - massCenterZ) * (n.z() - massCenterZ) : 0;
                double distance = Math.sqrt((n.x() - massCenterX) * (n.x() - massCenterX) + (n.y() - massCenterY) * (n.y() - massCenterY) + z);
                size = Math.max(size, 2 * distance);
            }
        }
    }

    public void updateAllMassAndGeometry(boolean recursive) {
        this.updateMassAndGeometry();
        if (recursive && nodes.size() > 1) {
            for (Region subregion : subregions) {
                subregion.updateAllMassAndGeometry(recursive);
            }
        }
    }

    private List<ArrayList<Node>> partitionZ(ArrayList<Node> nodesToAdd) {
        ArrayList<Node> frontNodes = new ArrayList<>();
        ArrayList<Node> backNodes = new ArrayList<>();
        for (Node n : nodesToAdd) {
            ArrayList<Node> nodesLine = (n.z() < massCenterZ) ? (frontNodes) : (backNodes);
            nodesLine.add(n);
        }
        return Arrays.asList(frontNodes, backNodes);
    }

    private void addSubRegions(List<ArrayList<Node>> lists) {
        for (ArrayList<Node> nodesToAdd : lists) {
            addSubRegions(nodesToAdd);
        }
    }

    private void addSubRegions(ArrayList<Node> nodesToAdd) {
        if (nodesToAdd.size() > 0) {
            if (nodesToAdd.size() < nodes.size()) {
                Region subregion = new Region(nodesToAdd, is3d);
                subregions.add(subregion);
            } else {
                for (Node n : nodesToAdd) {
                    ArrayList<Node> oneNodeList = new ArrayList<>();
                    oneNodeList.add(n);
                    Region subregion = new Region(oneNodeList, is3d);
                    subregions.add(subregion);
                }
            }
        }
    }

    public void buildSubRegions(boolean recursive) {
        if (is3d) {
            this.buildSubRegions3D(recursive);
        } else {
            this.buildSubRegions2D(recursive);
        }
    }

    private void buildSubRegions2D(boolean recursive) {
        if (nodes.size() > 1) {
            ArrayList<Node> leftNodes = new ArrayList<>();
            ArrayList<Node> rightNodes = new ArrayList<>();
            for (Node n : nodes) {
                ArrayList<Node> nodesColumn = (n.x() < massCenterX) ? (leftNodes) : (rightNodes);
                nodesColumn.add(n);
            }

            ArrayList<Node> topleftNodes = new ArrayList<>();
            ArrayList<Node> bottomleftNodes = new ArrayList<>();
            for (Node n : leftNodes) {
                ArrayList<Node> nodesLine = (n.y() < massCenterY) ? (topleftNodes) : (bottomleftNodes);
                nodesLine.add(n);
            }

            ArrayList<Node> bottomrightNodes = new ArrayList<>();
            ArrayList<Node> toprightNodes = new ArrayList<>();
            for (Node n : rightNodes) {
                ArrayList<Node> nodesLine = (n.y() < massCenterY) ? (toprightNodes) : (bottomrightNodes);
                nodesLine.add(n);
            }

            addSubRegions(topleftNodes);
            addSubRegions(bottomleftNodes);
            addSubRegions(bottomrightNodes);
            addSubRegions(toprightNodes);

            if (recursive) {
                for (Region subregion : subregions) {
                    subregion.buildSubRegions(recursive);
                }
            }
        }
    }

    private void buildSubRegions3D(boolean recursive) {
        if (nodes.size() > 1) {
            ArrayList<Node> leftNodes = new ArrayList<>();
            ArrayList<Node> rightNodes = new ArrayList<>();
            for (Node n : nodes) {
                ArrayList<Node> nodesColumn = (n.x() < massCenterX) ? (leftNodes) : (rightNodes);
                nodesColumn.add(n);
            }

            ArrayList<Node> topleftNodes = new ArrayList<>();
            ArrayList<Node> bottomleftNodes = new ArrayList<>();
            for (Node n : leftNodes) {
                ArrayList<Node> nodesLine = (n.y() < massCenterY) ? (topleftNodes) : (bottomleftNodes);
                nodesLine.add(n);
            }

            ArrayList<Node> bottomrightNodes = new ArrayList<>();
            ArrayList<Node> toprightNodes = new ArrayList<>();
            for (Node n : rightNodes) {
                ArrayList<Node> nodesLine = (n.y() < massCenterY) ? (toprightNodes) : (bottomrightNodes);
                nodesLine.add(n);
            }

            addSubRegions(partitionZ(topleftNodes));
            addSubRegions(partitionZ(bottomleftNodes));
            addSubRegions(partitionZ(bottomrightNodes));
            addSubRegions(partitionZ(toprightNodes));

            if (recursive) {
                for (Region subregion : subregions) {
                    subregion.buildSubRegions(recursive);
                }
            }
        }
    }

    public void applyForce(Node n, ForceFactory.RepulsionForce Force, double theta) {
        if (nodes.size() < 2) {
            Node regionNode = nodes.get(0);
            Force.apply_BH(n, regionNode);
        } else {
            double distance = Math.sqrt((n.x() - massCenterX) * (n.x() - massCenterX) + (n.y() - massCenterY) * (n.y() - massCenterY) + (n.z() - massCenterZ) * (n.z() - massCenterZ));
            if (distance * theta > size) {
                Force.apply(n, this);
            } else {
                for (Region subregion : subregions) {
                    subregion.applyForce(n, Force, theta);
                }
            }
        }
    }

    public List<Region> getSubregions() {
        return subregions;
    }

    public double getMass() {
        return mass;
    }


    public double getMassCenterX() {
        return massCenterX;
    }

    public double getMassCenterY() {
        return massCenterY;
    }

    public double getMassCenterZ() {
        return massCenterZ;
    }
}
