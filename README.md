# Gephi Force Atlas2 in 3-d

## Features

- Parallel Barnes-Hut tree building
- Computes total distance that all points move at each iteration in order to determine when the algorithm has converged
- Parallel force, attraction, and speed computations
- Option to rebuild Barnes-Hut tree every n iterations
- Option to update Barnes-Hut tree centers every n iterations


- Command line interface 


## Command Line Usage

```
java -Djava.awt.headless=true -Xmx8g -cp forceatlas2-3d.jar:gephi-toolkit-0.9.2-all.jar org.gephi.layout.plugin.forceAtlas2_3d.Main flags 
```

where flags are

Flag | Description | Default Value
--- | --- | ---
--input | Input graph in one of Gephi input file formats https://gephi.org/users/supported-graph-formats/ |
--output | Output file | 
--nsteps | Number of iterations | 1000
--barnesHutOptimize | Whether to use Barnes-Hut optimization (true or false) | true
--undirected | Whether input graph is undirected | true
--threads | Number of threads to use. | All cores
--format | Output file format. One of csv, gdf, gexf, gml, graphml, pajek, txt | txt
--barnesHutSplits | Number of splits to use for Barnes-Hut tree building. Number of threads used is 8 to the power barnesHutSplits | 1
--coords | Tab separated file containing initial coordinates with headers id, x, y, and, z | 
--seed | Seed for random number generation for initial node position | timestamp
--barnesHutUpdateIter | Update Barnes-Hut tree every barnesHutUpdateIter iterations | 1
--updateCenter | Update Barnes-Hut region centers every updateCenter iterations when not rebuilding Barnes-Hut tree | 
--barnesHutTheta | Theta of the Barnes Hut optimization | 1.2
--jitterTolerance  | How much swinging you allow. Above 1 discouraged. Lower gives less speed and more precision. | 1.0
--linLogMode | Switch ForceAtlas' model from lin-lin to lin-log (tribute to Andreas Noack). Makes clusters more tight. | false
--scalingRatio | How much repulsion you want. More makes a more sparse graph | 2.0 if # nodes >= 100, otherwise 10.0
--gravity | Attracts nodes to the center | 1.0
--strongGravityMode | A stronger gravity law | false
--outboundAttractionDistribution | Distributes attraction along outbound edges. Hubs attract less and thus are pushed to the borders. | false
       
