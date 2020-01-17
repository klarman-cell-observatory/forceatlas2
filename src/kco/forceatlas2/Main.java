package kco.forceatlas2;

import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.spi.Layout;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.io.*;
import java.util.*;


public class Main {

    private static Map<String, Arg> argsMap = new LinkedHashMap<>();

    private static void writeOutput(Graph g, boolean is3d, Set<String> formats, String output) {
        try {
            // ExporterCSV, ExporterDL, ExporterGDF, ExporterGEXF, ExporterGML, ExporterGraphML, ExporterPajek, ExporterVNA, PDFExporter, PNGExporter, SVGExporter
            ExportController ec = Lookup.getDefault().lookup(ExportController.class);
            for (String format : formats) {
                if (format.equals("txt")) {
                    PrintWriter pw = new PrintWriter(new FileWriter(output + (output.toLowerCase().endsWith("." + format) ? "" : "." + format)));
                    pw.print("id\tx\ty" + (is3d ? "\tz" : "") + "\n");
                    for (Node n : g.getNodes()) {
                        pw.print(n.getId());
                        pw.print("\t");
                        pw.print(n.x());
                        pw.print("\t");
                        pw.print(n.y());
                        if (is3d) {
                            pw.print("\t");
                            pw.print(n.z());
                        }
                        pw.print("\n");
                    }
                    pw.close();
                } else {
                    ec.exportFile(new File(output + (output.toLowerCase().endsWith("." + format) ? "" : "." + format)), ec.getExporter(format));
                }
            }
        } catch (IOException x) {
            x.printStackTrace();
            System.exit(1);
        }
    }

    private static void addArg(String flag, String description, boolean not_boolean, Object defaultValue) {
        argsMap.put("--" + flag.toLowerCase(), new Arg(flag, description, not_boolean, "" + defaultValue));
    }

    private static void addArg(String flag, String description, boolean not_boolean) {
        argsMap.put("--" + flag.toLowerCase(), new Arg(flag, description, not_boolean, null));
    }

    private static String getArg(String flag) {
        Arg a = argsMap.get("--" + flag.toLowerCase());
        return a != null ? a.value : null;
    }

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();

        addArg("input", "Input graph in one of Gephi input file formats https://gephi.org/users/supported-graph-formats/", true);
        addArg("output", "Output file", true);
        addArg("nsteps", "Number of iterations. Mutually exclusive with --targetChangePerNode", true);
        addArg("targetChangePerNode", "Maximum change per node to stop the algorithm. Mutually exclusive with --nsteps", true);
        addArg("targetSteps", "Maximum number of iterations before stopping the algoritm. This option is together with --targetChangePerNode", true, 10000);
        addArg("2d", "Generate a 2d layout", false, false);
        addArg("useAltSpeed", "Use alternative speed calculation, which is documented in the ForceAtlas2 paper.", false, false);
        addArg("directed", "Whether input graph is undirected", false, false);
        addArg("nthreads", "Number of threads to use. If not specified will use all cores", true);
        addArg("format", "Output file format. One of csv, gdf, gexf, gml, graphml, pajek, txt", true);
        addArg("coords", "Tab separated file containing initial coordinates with headers id, x, y, and, z", true);
        addArg("seed", "Seed for random number generation for initial node positions", true);
        addArg("barnesHutSplits", "Rounds of splits to use for Barnes-Hut tree building. Number of regions after splitting is 4^barnesHutSplits for 2D and 8^barnesHutSplits for 3D", true);
        addArg("barnesHutTheta", " Theta of the Barnes Hut optimization", true);
        addArg("barnesHutUpdateIter", "Update Barnes-Hut tree every barnesHutUpdateIter iterations", true);
        addArg("updateCenter", "Update Barnes-Hut region centers when not rebuilding Barnes-Hut tree", false, false);
        addArg("jitterTolerance", "How much swinging you allow. Above 1 discouraged. Lower gives less speed and more precision.", true);
        addArg("linLogMode", "Switch ForceAtlas' model from lin-lin to lin-log (tribute to Andreas Noack). Makes clusters more tight.", true);
        addArg("scalingRatio", "How much repulsion you want. More makes a more sparse graph", true);
        addArg("gravity", "Attracts nodes to the center", true);
        addArg("strongGravityMode", "A stronger gravity law", true);
        addArg("outboundAttractionDistribution", "Distributes attraction along outbound edges. Hubs attract less and thus are pushed to the borders.", true);

        for (int i = 0; i < args.length; i++) {
            Arg a = argsMap.get(args[i].toLowerCase());
            if (a == null) {
                System.err.println("Unknown argument " + args[i]);
                System.exit(1);
            }
            String value = a.not_boolean ? args[++i] : "true";
            a.value = value;
        }

        int nsteps = 0;
        double targetChangePerNode = 0.0;
        int targetSteps = 0;

        Long seed = null;
        boolean is3d = true;
        boolean useAltSpeed = false;
        int threadCount = Runtime.getRuntime().availableProcessors();
        Double barnesHutTheta = null;
        Double jitterTolerance = null;
        Boolean linLogMode = null;
        Double scalingRatio = null;
        Boolean strongGravityMode = null;
        Double gravity = null;
        Boolean outboundAttractionDistribution = null;
        Integer barnesHutUpdateIter = null;
        Set<String> formats = new HashSet<>();
        File coordsFile = null;
        Boolean updateCenter = false;
        Integer barnesHutSplits = null;


        File file = new File(getArg("input"));
        if (!file.exists()) {
            System.err.println(file + " not found.");
            System.exit(1);
        }

        String output = getArg("output");
        
        if (getArg("nsteps") != null) {
            nsteps = Integer.parseInt(getArg("nsteps"));
        }

        if (getArg("targetChangePerNode") != null) {
            targetChangePerNode = Double.parseDouble(getArg("targetChangePerNode"));
            targetSteps = Integer.parseInt(getArg("targetSteps"));
        }

        if (nsteps == 0 && targetChangePerNode == 0.0) {
            System.err.println("Either --nsteps or --targetChangePerNode must be set!");
            System.exit(1);            
        }

        if (nsteps > 0 && targetChangePerNode > 0.0) {
            System.err.println("--nsteps and --targetChangePerNode are mutually exclusive!");
            System.exit(1);
        }

        if (getArg("barnesHutSplits") != null) {
            barnesHutSplits = Integer.parseInt(getArg("barnesHutSplits"));
        }
        
        if (getArg("nthreads") != null) {
            threadCount = Integer.parseInt(getArg("nthreads"));
        }

        if (getArg("barnesHutTheta") != null) {
            barnesHutTheta = Double.parseDouble(getArg("barnesHutTheta"));
        }
        
        if (getArg("jitterTolerance") != null) {
            jitterTolerance = Double.parseDouble(getArg("jitterTolerance"));
        }
        
        if (getArg("linLogMode") != null) {
            linLogMode = getArg("linLogMode").equalsIgnoreCase("true");
        }
        
        if (getArg("scalingRatio") != null) {
            scalingRatio = Double.parseDouble(getArg("scalingRatio"));
        }
        
        if (getArg("gravity") != null) {
            gravity = Double.parseDouble(getArg("gravity"));
        }
        
        if (getArg("strongGravityMode") != null) {
            strongGravityMode = getArg("strongGravityMode").equalsIgnoreCase("true");
        }
        
        if (getArg("outboundAttractionDistribution") != null) {
            outboundAttractionDistribution = getArg("outboundAttractionDistribution").equalsIgnoreCase("true");
        }
        
        if (getArg("seed") != null) {
            seed = Long.parseLong(getArg("seed"));
        }
        
        if (getArg("format") != null) {
            formats.add(getArg("format"));
        }
        
        if (getArg("barnesHutUpdateIter") != null) {
            barnesHutUpdateIter = Integer.parseInt(getArg("barnesHutUpdateIter"));
        }
        
        updateCenter = getArg("updateCenter").equalsIgnoreCase("true");

        is3d = !getArg("2d").equalsIgnoreCase("true");
        useAltSpeed = getArg("useAltSpeed").equalsIgnoreCase("true");

        if (getArg("coords") != null) {
            coordsFile = new File(getArg("coords"));
            if (!coordsFile.exists()) {
                System.err.println(coordsFile + " not found.");
                System.exit(1);
            }
        }

        if (formats.size() == 0) {
            formats.add("txt");
        }

        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        Workspace workspace = pc.getCurrentWorkspace();
        ImportController importController = Lookup.getDefault().lookup(ImportController.class);
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        Container container = importController.importFile(file);
        Graph g;
        if (!getArg("directed").equalsIgnoreCase("true")) {
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
            g = graphModel.getUndirectedGraph();
        } else {
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);
            g = graphModel.getDirectedGraph();
        }
        importController.process(container, new DefaultProcessor(), workspace);
        ForceAtlas2 layout = new ForceAtlas2(null, is3d, useAltSpeed);
        layout.setGraphModel(graphModel);
        Random random = seed != null ? new Random(seed) : new Random();

        int num_nodes = 0;
        for (Node node : g.getNodes()) {
            ++num_nodes;
            node.setX((float) ((0.01 + random.nextDouble()) * 1000) - 500);
            node.setY((float) ((0.01 + random.nextDouble()) * 1000) - 500);
            if (is3d) {
                node.setZ((float) ((0.01 + random.nextDouble()) * 1000) - 500);
            } else {
                node.setZ(0);
            }
        }

        if (coordsFile != null) {
            Map<Object, Node> idToNode = new HashMap<>();
            for (Node n : g.getNodes()) {
                idToNode.put(n.getId(), n);

            }
            BufferedReader br = new BufferedReader(new FileReader(coordsFile));
            String sep = "\t";
            String s = br.readLine();
            for (String test : new String[]{"\t", ","}) {
                if (s.indexOf(test) != -1) {
                    sep = test;
                    break;
                }
            }
            List<String> header = Arrays.asList(s.split(sep));
            int idIndex = header.indexOf("id");
            int xIndex = header.indexOf("x");
            int yIndex = header.indexOf("y");
            int zIndex = header.indexOf("z");
            boolean setZ = zIndex != -1 && is3d;
            while ((s = br.readLine()) != null) {
                String[] tokens = s.split(sep);
                String id = tokens[idIndex];
                Node n = idToNode.get(id);
                if (n != null) {
                    n.setX(Float.parseFloat(tokens[xIndex]));
                    n.setY(Float.parseFloat(tokens[yIndex]));
                    if (setZ) {
                        n.setZ(Float.parseFloat(tokens[zIndex]));
                    }
                } else {
                    System.err.println(id + " not found");
                }
            }
            br.close();
        }

        if (barnesHutTheta != null) {
            layout.setBarnesHutTheta(barnesHutTheta);
        }
        if (jitterTolerance != null) {
            layout.setJitterTolerance(jitterTolerance);
        }
        if (linLogMode != null) {
            layout.setLinLogMode(linLogMode);
        }
        if (scalingRatio != null) {
            layout.setScalingRatio(scalingRatio);
        }
        if (strongGravityMode != null) {
            layout.setStrongGravityMode(strongGravityMode);
        }
        if (gravity != null) {
            layout.setGravity(gravity);
        }
        if (outboundAttractionDistribution != null) {
            layout.setOutboundAttractionDistribution(outboundAttractionDistribution);
        }
        layout.setThreadsCount(threadCount);
        if (barnesHutUpdateIter != null) {
            layout.setUpdateBarnesHutIter(barnesHutUpdateIter);
        }
        if (updateCenter != null) {
            layout.setUpdateCenter(updateCenter);
        }
        if (barnesHutSplits != null) {
            layout.setBarnesHutSplits(barnesHutSplits);
        }


        layout.initAlgo();
        
        final Set<String> _formats = formats;
        final String _output = output;
        final Graph _g = g;
        final Layout _layout = layout;
        final boolean _is3d = is3d;
        final PrintWriter distanceWriter = (nsteps > 0 ? new PrintWriter(new FileWriter(output + ".distances.txt")) : null);

        if (nsteps > 0) distanceWriter.print("step\tdistance\n");

        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                _layout.endAlgo();
                writeOutput(_g, _is3d, _formats, _output);
                if (distanceWriter != null) distanceWriter.close();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        if (nsteps > 0) {
            int lastPercent = 0;
            double distance;

            for (int i = 0; i < nsteps; i++) {
                layout.goAlgo();

                distance = layout.getDistance();
                distanceWriter.print(i);
                distanceWriter.print("\t");
                distanceWriter.print(distance);
                distanceWriter.print("\n");
                distanceWriter.flush();

                int percent = (int) Math.floor(100 * (i + 1.0) / nsteps);
                if (percent != lastPercent) {
                    System.out.print("*");
                    lastPercent = percent;
                    if (percent % 25 == 0) {
                        System.out.println(percent + "%");
                    }
                }
            }            
        } else {
            nsteps = 0;
            double changePerNode;

            do {
                ++nsteps;
                layout.goAlgo();
                changePerNode = layout.getDistance() / num_nodes;
                if (nsteps % 100 == 0) System.out.println(nsteps + " iterations, change_per_node = " + changePerNode);
            } while (nsteps == 1 || changePerNode > targetChangePerNode && nsteps < targetSteps);

            System.out.println("Finished in " + nsteps + " iterations, change_per_node = " + changePerNode);
        }

        Runtime.getRuntime().removeShutdownHook(shutdownThread);

        layout.endAlgo();
        writeOutput(g, is3d, formats, output);
        if (distanceWriter != null) distanceWriter.close();

        long endTime = System.currentTimeMillis();
        System.out.println("Time = " + (endTime - startTime) / 1000.0 + "s");
    }

    private static class Arg {
        String flag;
        String description;
        boolean not_boolean;
        String defaultValue;
        String value;

        private Arg(String flag, String description, boolean not_boolean, String defaultValue) {
            this.flag = flag;
            this.description = description;
            this.not_boolean = not_boolean;
            this.defaultValue = defaultValue;
            if (defaultValue != null) {
                this.value = defaultValue;
            }
        }

        public String toString() {
            return flag;
        }
    }
}
