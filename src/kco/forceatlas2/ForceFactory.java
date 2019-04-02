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

/**
 * Generates the forces on demand, here are all the formulas for attraction and
 * repulsion.
 *
 * @author Mathieu Jacomy
 */
public class ForceFactory {

    public static ForceFactory builder = new ForceFactory();

    private ForceFactory() {
    }

    public RepulsionForce buildRepulsion(boolean adjustBySize, double coefficient) {
        if (adjustBySize) {
            return new linRepulsion_antiCollision(coefficient);
        } else {
            return new linRepulsion(coefficient);
        }
    }

    public RepulsionForce getStrongGravity(double coefficient) {
        return new strongGravity(coefficient);
    }

    public AttractionForce buildAttraction(boolean logAttraction, boolean distributedAttraction, boolean adjustBySize, double coefficient) {
        if (adjustBySize) {
            if (logAttraction) {
                if (distributedAttraction) {
                    return new logAttraction_degreeDistributed_antiCollision(coefficient);
                } else {
                    return new logAttraction_antiCollision(coefficient);
                }
            } else {
                if (distributedAttraction) {
                    return new linAttraction_degreeDistributed_antiCollision(coefficient);
                } else {
                    return new linAttraction_antiCollision(coefficient);
                }
            }
        } else {
            if (logAttraction) {
                if (distributedAttraction) {
                    return new logAttraction_degreeDistributed(coefficient);
                } else {
                    return new logAttraction(coefficient);
                }
            } else {
                if (distributedAttraction) {
                    return new linAttraction_massDistributed(coefficient);
                } else {
                    return new linAttraction(coefficient);
                }
            }
        }
    }

    public abstract class AttractionForce {

        public abstract void apply(Node n1, Node n2, double e); // Model for node-node attraction (e is for edge weight if needed)
    }

    public abstract class RepulsionForce {

        public abstract void apply(Node n1, Node n2);           // Model for node-node repulsion

        public abstract void apply(Node n, Region r);           // Model for Barnes Hut approximation

        public abstract void apply(Node n, double g);           // Model for gravitation (anti-repulsion)

        public abstract void apply_BH(Node n, Node o);             // Model for node-node repulsion in quadtree (BH), do not update both n and o
    }

    /*
     * Repulsion force: Linear
     */
    private class linRepulsion extends RepulsionForce {

        private double coefficient;

        public linRepulsion(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance

            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * n1Layout.getMass() * n2Layout.getMass() / distance / distance;

                n1Layout.setDx(n1Layout.getDx() + xDist * factor);
                n1Layout.setDy(n1Layout.getDy() + yDist * factor);
                n1Layout.setDz(n1Layout.getDz() + zDist * factor);

                n2Layout.setDx(n2Layout.getDx() - xDist * factor);
                n2Layout.setDy(n2Layout.getDy() - yDist * factor);
                n2Layout.setDz(n2Layout.getDz() - zDist * factor);
            }
        }

        @Override
        public void apply(Node n, Region r) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();

            // Get the distance
            double xDist = n.x() - r.getMassCenterX();
            double yDist = n.y() - r.getMassCenterY();
            double zDist = n.z() - r.getMassCenterZ();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * r.getMass() / distance / distance;

                nLayout.setDx(nLayout.getDx() + xDist * factor);
                nLayout.setDy(nLayout.getDy() + yDist * factor);
                nLayout.setDz(nLayout.getDz() + zDist * factor);
            }
        }

        @Override
        public void apply(Node n, double g) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();

            // Get the distance
            double xDist = n.x();
            double yDist = n.y();
            double zDist = n.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * g / distance;

                nLayout.setDx(nLayout.getDx() - xDist * factor);
                nLayout.setDy(nLayout.getDy() - yDist * factor);
                nLayout.setDz(nLayout.getDz() - zDist * factor);
            }
        }

        @Override
        public void apply_BH(Node n, Node o) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();
            ForceAtlas2LayoutData oLayout = o.getLayoutData();

            // Get the distance
            double xDist = n.x() - o.x();
            double yDist = n.y() - o.y();
            double zDist = n.z() - o.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * oLayout.getMass() / distance / distance;

                nLayout.setDx(nLayout.getDx() + xDist * factor);
                nLayout.setDy(nLayout.getDy() + yDist * factor);
                nLayout.setDz(nLayout.getDz() + zDist * factor);
            }
        }
    }

    /*
     * Repulsion force: Strong Gravity (as a Repulsion Force because it is easier)
     */
    private class linRepulsion_antiCollision extends RepulsionForce {

        private double coefficient;

        public linRepulsion_antiCollision(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n1.size() - n2.size();

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * n1Layout.getMass() * n2Layout.getMass() / distance / distance;

                n1Layout.setDx(n1Layout.getDx() + xDist * factor);
                n1Layout.setDy(n1Layout.getDy() + yDist * factor);
                n1Layout.setDz(n1Layout.getDz() + zDist * factor);

                n2Layout.setDx(n2Layout.getDx() - xDist * factor);
                n2Layout.setDy(n2Layout.getDy() - yDist * factor);
                n2Layout.setDz(n2Layout.getDz() - zDist * factor);

            } else if (distance < 0) {
                double factor = 100 * coefficient * n1Layout.getMass() * n2Layout.getMass();

                n1Layout.setDx(n1Layout.getDx() + xDist * factor);
                n1Layout.setDy(n1Layout.getDy() + yDist * factor);
                n1Layout.setDz(n1Layout.getDz() + zDist * factor);

                n2Layout.setDx(n2Layout.getDx() - xDist * factor);
                n2Layout.setDy(n2Layout.getDy() - yDist * factor);
                n2Layout.setDz(n2Layout.getDz() - zDist * factor);
            }
        }

        @Override
        public void apply(Node n, Region r) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();

            // Get the distance
            double xDist = n.x() - r.getMassCenterX();
            double yDist = n.y() - r.getMassCenterY();
            double zDist = n.z() - r.getMassCenterZ();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * r.getMass() / distance / distance;

                nLayout.setDx(nLayout.getDx() + xDist * factor);
                nLayout.setDy(nLayout.getDy() + yDist * factor);
                nLayout.setDz(nLayout.getDz() + zDist * factor);
            } else if (distance < 0) {
                double factor = -coefficient * nLayout.getMass() * r.getMass() / distance;

                nLayout.setDx(nLayout.getDx() + xDist * factor);
                nLayout.setDy(nLayout.getDy() + yDist * factor);
                nLayout.setDz(nLayout.getDz() + zDist * factor);
            }
        }

        @Override
        public void apply(Node n, double g) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();

            // Get the distance
            double xDist = n.x();
            double yDist = n.y();
            double zDist = n.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * g / distance;

                nLayout.setDx(nLayout.getDx() - xDist * factor);
                nLayout.setDy(nLayout.getDy() - yDist * factor);
                nLayout.setDz(nLayout.getDz() - zDist * factor);
            }
        }

        @Override
        public void apply_BH(Node n, Node o) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();
            ForceAtlas2LayoutData oLayout = o.getLayoutData();
            // Get the distance
            double xDist = n.x() - o.x();
            double yDist = n.y() - o.y();
            double zDist = n.z() - o.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n.size() - o.size();

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * oLayout.getMass() / distance / distance;

                nLayout.setDx(nLayout.getDx() + xDist * factor);
                nLayout.setDy(nLayout.getDy() + yDist * factor);
                nLayout.setDz(nLayout.getDz() + zDist * factor);
            }
        }
    }

    private class strongGravity extends RepulsionForce {

        private double coefficient;

        public strongGravity(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2) {
            // Not Relevant
        }

        @Override
        public void apply(Node n, Region r) {
            // Not Relevant
        }

        @Override
        public void apply(Node n, double g) {
            ForceAtlas2LayoutData nLayout = n.getLayoutData();

            // Get the distance
            double xDist = n.x();
            double yDist = n.y();
            double zDist = n.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = coefficient * nLayout.getMass() * g;

                nLayout.setDx(nLayout.getDx() - xDist * factor);
                nLayout.setDy(nLayout.getDy() - yDist * factor);
                nLayout.setDz(nLayout.getDz() - zDist * factor);
            }
        }

        @Override
        public void apply_BH(Node n, Node o) {
            // Not Relevant
        }
    }

    /*
     * Attraction force: Linear
     */
    private class linAttraction extends AttractionForce {

        private double coefficient;

        public linAttraction(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();

            // NB: factor = force / distance
            double factor = -coefficient * e;

            // if (n1.getId().equals("10812")) {
            //     System.out.println("before (" + n2.getId() + ") " + n1Layout.getDx() + " " + n1Layout.getDy());
            // }
            n1Layout.augmentDx(xDist * factor);
            n1Layout.augmentDy(yDist * factor);
            n1Layout.augmentDz(zDist * factor);
            // if (n1.getId().equals("10812")) {
            //     System.out.println("after(" + n2.getId() +  ") " + n1Layout.getDx() + " " + n1Layout.getDy());
            // }

            // if (n2.getId().equals("10812")) {
            //     System.out.println("before (" + n1.getId() + ") " + n2Layout.getDx() + " " + n2Layout.getDy());
            // }
            n2Layout.augmentDx(- xDist * factor);
            n2Layout.augmentDy(- yDist * factor);
            n2Layout.augmentDz(- zDist * factor);
            // if (n2.getId().equals("10812")) {
            //     System.out.println("after (" + n1.getId() + ") " + n2Layout.getDx() + " " + n2Layout.getDy());
            // }
        }
    }

    /*
     * Attraction force: Linear, distributed by mass (typically, degree)
     */
    private class linAttraction_massDistributed extends AttractionForce {

        private double coefficient;

        public linAttraction_massDistributed(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();

            // NB: factor = force / distance
            double factor = -coefficient * e / n1Layout.getMass();

            n1Layout.augmentDx(xDist * factor);
            n1Layout.augmentDy(yDist * factor);
            n1Layout.augmentDz(zDist * factor);

            n2Layout.augmentDx(- xDist * factor);
            n2Layout.augmentDy(- yDist * factor);
            n2Layout.augmentDz(- zDist * factor);
        }
    }

    /*
     * Attraction force: Logarithmic
     */
    private class logAttraction extends AttractionForce {

        private double coefficient;

        public logAttraction(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {

                // NB: factor = force / distance
                double factor = -coefficient * e * Math.log(1 + distance) / distance;

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }

    /*
     * Attraction force: Linear, distributed by Degree
     */
    private class logAttraction_degreeDistributed extends AttractionForce {

        private double coefficient;

        public logAttraction_degreeDistributed(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

            if (distance > 0) {

                // NB: factor = force / distance
                double factor = -coefficient * e * Math.log(1 + distance) / distance / n1Layout.getMass();

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }

    /*
     * Attraction force: Linear, with Anti-Collision
     */
    private class linAttraction_antiCollision extends AttractionForce {

        private double coefficient;

        public linAttraction_antiCollision(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n1.size() - n2.size();

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = -coefficient * e;

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }

    /*
     * Attraction force: Linear, distributed by Degree, with Anti-Collision
     */
    private class linAttraction_degreeDistributed_antiCollision extends AttractionForce {

        private double coefficient;

        public linAttraction_degreeDistributed_antiCollision(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n1.size() - n2.size();

            if (distance > 0) {
                // NB: factor = force / distance
                double factor = -coefficient * e / n1Layout.getMass();

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }

    /*
     * Attraction force: Logarithmic, with Anti-Collision
     */
    private class logAttraction_antiCollision extends AttractionForce {

        private double coefficient;

        public logAttraction_antiCollision(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n1.size() - n2.size();

            if (distance > 0) {

                // NB: factor = force / distance
                double factor = -coefficient * e * Math.log(1 + distance) / distance;

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }

    /*
     * Attraction force: Linear, distributed by Degree, with Anti-Collision
     */
    private class logAttraction_degreeDistributed_antiCollision extends AttractionForce {

        private double coefficient;

        public logAttraction_degreeDistributed_antiCollision(double c) {
            coefficient = c;
        }

        @Override
        public void apply(Node n1, Node n2, double e) {
            ForceAtlas2LayoutData n1Layout = n1.getLayoutData();
            ForceAtlas2LayoutData n2Layout = n2.getLayoutData();

            // Get the distance
            double xDist = n1.x() - n2.x();
            double yDist = n1.y() - n2.y();
            double zDist = n1.z() - n2.z();
            double distance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) - n1.size() - n2.size();

            if (distance > 0) {

                // NB: factor = force / distance
                double factor = -coefficient * e * Math.log(1 + distance) / distance / n1Layout.getMass();

                n1Layout.augmentDx(xDist * factor);
                n1Layout.augmentDy(yDist * factor);
                n1Layout.augmentDz(zDist * factor);

                n2Layout.augmentDx(- xDist * factor);
                n2Layout.augmentDy(- yDist * factor);
                n2Layout.augmentDz(- zDist * factor);
            }
        }
    }
}
