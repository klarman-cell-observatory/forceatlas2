# Multithreaded [Gephi](https://gephi.org/) Force Atlas2 Layout in 2 or 3-d

## Features

- Parallel Barnes-Hut tree building
- Computes total distance that all points move at each iteration in order to determine when the algorithm has converged
- Parallel force, attraction, and speed computations
- Option to rebuild Barnes-Hut tree every n iterations
- Option to update Barnes-Hut tree centers every n iterations
- Command line interface 

## Installation
Download gephi-toolkit-0.9.2-all.jar and forceatlas2.jar from https://github.com/klarman-cell-observatory/forceatlas2/releases


## Command Line Usage

```
java -Djava.awt.headless=true -Xmx8g -cp forceatlas2.jar:gephi-toolkit-0.9.2-all.jar kco.forceatlas2.Main flags 
```

where flags are

Flag | Description | Default Value
--- | --- | ---
--input | Input graph in one of Gephi input file formats https://gephi.org/users/supported-graph-formats/ |
--output | Output file | 
--nsteps | Number of iterations. Mutually exclusive with --targetChangePerNode | 
--targetChangePerNode | Target distance change per node before stop the algorithm. Mutually exclusive with --nsteps | 
--targetSteps | Maximum number of iterations before stopping the algoritm. This option is together with --targetChangePerNode | 10000
--2d | Whether to produce a 2d layout | false
--directed | Whether input graph is directed | false
--nthreads | Number of threads to use. | All cores
--format | Output file format. One of csv, gdf, gexf, gml, graphml, pajek, txt | txt
--coords | Tab separated file containing initial coordinates with headers id, x, y, and, z | 
--seed | Seed for random number generation for initial node position | timestamp
--barnesHutSplits | Rounds of splits to use for Barnes-Hut tree building. Number of regions after splitting is 4^barnesHutSplits for 2D and 8^barnesHutSplits for 3D | 
--barnesHutTheta | Theta of the Barnes Hut optimization | 1.2
--barnesHutUpdateIter | Update Barnes-Hut tree every barnesHutUpdateIter iterations | 1
--updateCenter | Update Barnes-Hut region centers when not rebuilding Barnes-Hut tree | false
--jitterTolerance  | How much swinging you allow. Above 1 discouraged. Lower gives less speed and more precision. | 1.0
--linLogMode | Switch ForceAtlas' model from lin-lin to lin-log (tribute to Andreas Noack). Makes clusters more tight. | false
--scalingRatio | How much repulsion you want. More makes a more sparse graph | 2.0 if # nodes >= 100, otherwise 10.0
--gravity | Attracts nodes to the center | 1.0
--strongGravityMode | A stronger gravity law | false
--outboundAttractionDistribution | Distributes attraction along outbound edges. Hubs attract less and thus are pushed to the borders. | false


## Example Datasets
[Gephi example datasets](https://github.com/gephi/gephi/wiki/Datasets)

