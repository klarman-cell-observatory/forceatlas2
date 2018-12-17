package org.gephi.layout.plugin.forceAtlas2_3d;

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

    private static void addArg(String flag, String description, boolean required, Object defaultValue) {
        argsMap.put("--" + flag.toLowerCase(), new Arg(flag, description, required, "" + defaultValue));
    }

    private static void addArg(String flag, String description, boolean required) {
        argsMap.put("--" + flag.toLowerCase(), new Arg(flag, description, required, null));
    }

    private static String getArg(String flag) {
        Arg a = argsMap.get("--" + flag.toLowerCase());
        return a != null ? a.value : null;
    }

    public static void main(String[] args) throws IOException {
        addArg("input", "Input graph in one of Gephi input file formats https://gephi.org/users/supported-graph-formats/", true);
        addArg("output", "Output file", true);
        addArg("nsteps", "Number of iterations", false, 1000);
        addArg("barnesHutOptimize", "Whether to use Barnes-Hut optimization (true or false)", false, true);
        addArg("undirected", "Whether input graph is undirected", false, true);
        addArg("nthreads", "Number of threads to use. If not specified will use all cores", false);
        addArg("barnesHutSplits", "Number of splits to use for Barnes-Hut tree building. Number of threads used is 8 to the power barnesHutSplits", false);
        addArg("format", "Output file format. One of csv, gdf, gexf, gml, graphml, pajek, txt", false);
        addArg("coords", "Tab separated file containing initial coordinates with headers id, x, y, and, z", false);
        addArg("barnesHutTheta", " Theta of the Barnes Hut optimization", false);
        addArg("jitterTolerance", "How much swinging you allow. Above 1 discouraged. Lower gives less speed and more precision.", false);
        addArg("linLogMode", "Switch ForceAtlas' model from lin-lin to lin-log (tribute to Andreas Noack). Makes clusters more tight.", false);
        addArg("scalingRatio", "How much repulsion you want. More makes a more sparse graph", false);
        addArg("gravity", "Attracts nodes to the center", false);
        addArg("strongGravityMode", "A stronger gravity law", false);
        addArg("outboundAttractionDistribution", "Distributes attraction along outbound edges. Hubs attract less and thus are pushed to the borders.", false);
        addArg("seed", "Seed for random number generation for initial node positions", false);
        addArg("barnesHutUpdateIter", "Update Barnes-Hut tree every barnesHutUpdateIter iterations", false);
        addArg("updateCenter", "Update Barnes-Hut region centers every updateCenter iterations when not rebuilding Barnes-Hut tre", false);

        for (int i = 0; i < args.length; i++) {
            Arg a = argsMap.get(args[i].toLowerCase());
            if (a == null) {
                System.err.println("Unknown argument " + args[i]);
                continue;
            }
            String value = args[++i];
            a.value = value;
        }

        Long seed = null;
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
        int nsteps = Integer.parseInt(getArg("nsteps"));
        boolean barnesHutOptimize = getArg("barnesHutOptimize").equalsIgnoreCase("true");
        if (getArg("barnesHutSplits") != null) {
            barnesHutSplits = Integer.parseInt(getArg("barnesHutSplits"));
        }
        if (getArg("nthreads") != null) {
            threadCount = Integer.parseInt(getArg("nthreads"));
        }

        if (getArg("barnesHutTheta") != null) {
            barnesHutTheta = Double.parseDouble(getArg("barnesHutTheta"));
        } else if (getArg("jitterTolerance") != null) {
            jitterTolerance = Double.parseDouble(getArg("jitterTolerance"));
        } else if (getArg("linLogMode") != null) {
            linLogMode = getArg("linLogMode").equalsIgnoreCase("true");
        } else if (getArg("scalingRatio") != null) {
            scalingRatio = Double.parseDouble(getArg("scalingRatio"));
        } else if (getArg("gravity") != null) {
            gravity = Double.parseDouble(getArg("gravity"));
        } else if (getArg("strongGravityMode") != null) {
            strongGravityMode = getArg("strongGravityMode").equalsIgnoreCase("true");
        } else if (getArg("outboundAttractionDistribution") != null) {
            outboundAttractionDistribution = getArg("outboundAttractionDistribution").equalsIgnoreCase("true");
        } else if (getArg("seed") != null) {
            seed = Long.parseLong(getArg("seed"));
        } else if (getArg("format") != null) {
            formats.add(getArg("format"));
        } else if (getArg("barnesHutUpdateIter") != null) {
            barnesHutUpdateIter = Integer.parseInt(getArg("barnesHutUpdateIter"));
        } else if (getArg("updateCenter") != null) {
            updateCenter = getArg("updateCenter").equalsIgnoreCase("true");
        } else if (getArg("coords") != null) {
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
        if (getArg("undirected").equals("true")) {
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.UNDIRECTED);
            g = graphModel.getUndirectedGraph();
        } else {
            container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);
            g = graphModel.getDirectedGraph();
        }
        Graph graph = graphModel.getGraph();
        importController.process(container, new DefaultProcessor(), workspace);
        org.gephi.layout.plugin.forceAtlas2_3d.ForceAtlas2 layout = new org.gephi.layout.plugin.forceAtlas2_3d.ForceAtlas2(null);
        layout.setGraphModel(graphModel);
        Random random = seed != null ? new Random(seed) : new Random();

        for (Node node : graph.getNodes()) {
            node.setX((float) ((0.01 + random.nextDouble()) * 1000) - 500);
            node.setY((float) ((0.01 + random.nextDouble()) * 1000) - 500);
            node.setZ((float) ((0.01 + random.nextDouble()) * 1000) - 500);
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
            while ((s = br.readLine()) != null) {
                String[] tokens = s.split(sep);
                String id = tokens[idIndex];
                Node n = idToNode.get(id);
                if (n != null) {
                    n.setX(Float.parseFloat(tokens[xIndex]));
                    n.setY(Float.parseFloat(tokens[yIndex]));
                    n.setZ(Float.parseFloat(tokens[zIndex]));
                } else {
                    System.err.println(id + " not found");
                }
            }
            br.close();
        }


        layout.setBarnesHutOptimize(barnesHutOptimize);
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
        final PrintWriter distanceWriter = new PrintWriter(new FileWriter(output + ".distances.txt"));
        distanceWriter.print("step\tdistance\n");

        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                _layout.endAlgo();
                writeOutput(_g, true, _formats, _output);
                distanceWriter.close();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        int lastPercent = 0;
        for (int i = 0; i < nsteps; i++) {
            layout.goAlgo();

            double distance = layout.getDistance();
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
        if (nsteps > 0) {
            System.out.println();
        }
        layout.endAlgo();
//        Runtime.getRuntime().removeShutdownHook(shutdownThread);
//        writeOutput(g, is3d, formats, output);
    }

    private static class Arg {
        String flag;
        String description;
        boolean required;
        String defaultValue;
        String value;

        private Arg(String flag, String description, boolean required, String defaultValue) {
            this.flag = flag;
            this.description = description;
            this.required = required;
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
