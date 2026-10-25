# Assignment 3: Graph Algorithms

## 1. Implementation and interface

The implementation uses Java 17 and the standard library.
The selected language in submission.json is Java.

The source files are located in src/Java:

- Main.java: UTF-8 JSON Lines input and output.
- Input.java: conversion of request fields into Java values.
- Json.java: JSON parsing and serialization.
- Algorithms.java: the six required algorithms and route reconstruction.

Vertices and original edge IDs are numbered from zero.
An edge ID is its position in the original input array.
Sorting and adjacency construction preserve these IDs.

Main processes multiple requests in one process and prints one JSON result
per request. The ping request returns {"status":"ready"}.

All mutable algorithm state is created inside each request.
No graph, visited array, heap, DSU or distance array is shared between requests.

## 2. Disjoint Set Union

### Method and invariant

Each component is represented by a rooted tree.
The root stores the component size.

find follows parent pointers and then compresses the traversed path.
union attaches the smaller component root to the larger one.

The maintained invariants are:

- Two vertices have the same root exactly when they are connected by
  previously processed union operations.
- The size stored at a root equals its component size.
- components is the current number of components.
- largest is the size of the largest component.

Initially, every vertex forms a singleton component.
Merging two different roots combines exactly their two components,
decreases the component count by one and updates the largest size.
A repeated union or self-union leaves these values unchanged.

The output records the state after every operation.
For n = 0, there are no operations and the result is an empty array.

### Complexity

For n vertices and q operations:

- Time: O(n + q alpha(n)) amortized.
- Memory: O(n + q), including converted operations and output states.

Here alpha is the inverse Ackermann function.

## 3. Kruskal minimum spanning forest

### Method and correctness

The algorithm sorts original edge IDs by weight, using the ID to break ties.
It processes these edges in increasing weight order.

An edge is selected exactly when its endpoints belong to different DSU
components. Therefore, selected edges never form a cycle.

At each accepted edge, consider the cut between its current DSU component
and the remaining vertices. No lighter edge crossing this cut could have
been skipped: it would have joined different components when processed.
The selected edge is therefore safe by the minimum spanning tree cut property.

Repeating this argument produces a minimum spanning tree in every connected
component, hence a minimum spanning forest of the whole graph.

The DSU component count includes isolated vertices.
Self-loops are rejected because their endpoints already have the same root.
Parallel, negative and equal-weight edges are supported.
An empty graph produces cost 0, no selected edges and 0 components.

### Complexity

For n vertices and m edges:

- Time: O(n + m log(m + 1)).
- Memory: O(n + m), including edge conversion, sorting and the result.

## 4. Prim minimum spanning forest

### Method and correctness

Prim builds an undirected adjacency structure and uses a priority queue.
It starts a new traversal from every vertex that is still unvisited.

Within a component, the visited vertices form a connected tree.
The queue contains candidate edges added from visited vertices.
Candidates whose targets have already been visited are discarded.

The smallest remaining candidate crosses from the current visited set to
an unvisited vertex. It is a minimum-weight edge across this cut and is
safe by the cut property.

Each accepted edge adds exactly one new vertex, so no cycle is introduced.
When the queue contains no useful candidate, the connected component is
complete. Restarting from the next unvisited vertex handles disconnected
graphs and isolated vertices.

Self-loops never add a new vertex.
Parallel edges and negative or equal weights are supported.
The implementation is independent of Kruskal.

### Complexity

The implementation uses a lazy priority queue containing up to O(m)
candidates. Each adjacency entry is examined once.

- Time: O(n + m log(m + 1)).
- Memory: O(n + m).

## 5. Kosaraju strongly connected components

### Method and correctness

The first DFS pass visits every vertex in the original directed graph
and records vertices when their DFS frames finish.

The second pass visits the transposed graph in decreasing order of
first-pass finishing time.

For different SCCs with an original edge from C to D, the maximum finishing
time in C is greater than the maximum finishing time in D.
Consequently, the next unassigned component chosen for the second pass
cannot reach another unassigned component in the transposed graph.

Within an SCC, every vertex remains reachable from every other vertex
after transposition. Therefore, each second-pass traversal labels exactly
one SCC.

Finally, every original edge is examined.
Edges inside one SCC are omitted.
Edges between different SCCs are converted into directed label pairs.
A hash set removes repeated pairs.

The pair encoding uses separate 32-bit portions of a long value, preserving
both component labels.

Isolated vertices receive separate labels.
Loops and duplicate edges do not change the SCC partition.
For n = 0, both output arrays are empty.

### Complexity

- Expected time: O(n + m), including hash-based condensation deduplication.
- Memory: O(n + m), including both graph directions and the output.

## 6. DFS topological ordering

### Method and correctness

DFS uses three vertex states:

- 0: unvisited.
- 1: active on the current DFS stack.
- 2: finished.

An edge to an active vertex closes a directed cycle.
An edge to a finished vertex does not by itself indicate a cycle.

All components are checked.
If a cycle is found, the result is acyclic = false and an empty order.

Otherwise, the finishing order is reversed.
For every edge u -> v, v finishes before u:
an unvisited v is fully explored before u finishes, while an already
finished v has finished earlier.
Thus, reversing the finishing order places u before v.

Every vertex appears exactly once.
A self-loop is detected as a cycle.
An empty graph is acyclic and has an empty order.

### Complexity

- Time: O(n + m).
- Memory: O(n + m).

## 7. DAG shortest paths

### Method and correctness

The algorithm first performs the same cycle-detecting DFS over the entire
graph. A cycle in a component unreachable from the source is also rejected.

For a valid DAG, vertices are processed in topological order.
The source distance is 0; other distances initially contain null.

Before a vertex is processed, all its predecessors have already been
processed. Every reachable incoming edge has therefore already offered
its candidate distance. By induction over the topological order, the
vertex's distance is its shortest distance from the source.

Outgoing edges are relaxed only from reachable vertices.
Negative weights are valid because the graph has no directed cycles.

Whenever a shorter distance is found, parent_edge stores the original ID
of the edge producing that distance.

The source keeps parent_edge = -1.
Unreachable vertices keep distance = null and parent_edge = -1.
Equal shortest paths may produce different valid parent arrays.

### Complexity

- Time: O(n + m).
- Memory: O(n + m).

## 8. Route reconstruction

reconstructPath(source, target, edges, parentEdge) follows parent edges
backward from the target and reverses the collected vertices.

Each parent edge must have a valid original ID and end at the current
vertex. Its starting endpoint becomes the next vertex in the backward walk.

For valid parent arrays, this reconstructs the directed path from source
to target.

The chosen conventions are:

- Reachable target: the vertex sequence from source to target.
- Target equal to source: [source].
- Unreachable target: an empty list.

Invalid edge IDs, inconsistent edge directions and cyclic parent chains
are rejected.

For a reachable route containing L vertices, reconstruction takes O(L)
time and O(L) result memory.

### Executed helper tests

GraphAlgorithmsTest.java directly calls the helper five times.
The saved output is tests/route-helper.actual.txt.

For the first graph, the original edges are:

0: 0 -> 1, weight 3
1: 0 -> 2, weight 8
2: 1 -> 2, weight -7
3: 2 -> 3, weight 4

The independently expected distances are [0, 3, -4, 0, null].

| Case | Returned route | Selected edge IDs | Weight |
|---|---|---|---|
| Reachable target 3 | [0, 1, 2, 3] | [0, 2, 3] | 0 |
| Source target 0 | [0] | [] | 0 |
| Unreachable target 4 | [] | [] | Unreachable |

The reachable route has weight 3 - 7 + 4 = 0.

For the equal-path and parallel-edge graph, the original edges are:

0: 0 -> 1, weight 2
1: 0 -> 1, weight 2
2: 0 -> 2, weight 1
3: 1 -> 3, weight -3
4: 2 -> 3, weight -2

The independently expected distances are [0, 2, 1, -1, null].

| Case | Returned route | Selected edge IDs | Weight |
|---|---|---|---|
| Actual DAG parents | [0, 1, 3] | [1, 3] | -1 |
| Alternative valid parents | [0, 2, 3] | [2, 4] | -1 |

The alternative parent array is [-1, 0, 2, 4, -1].

The tests check route endpoints, original edge IDs, edge direction and
total weight. They also compare complete distance arrays against fixed,
independently calculated expectations.

All five helper tests passed.

## 9. Deep graphs, numeric range and state

All graph DFS traversals use explicit arrays as stacks.
Each suspended DFS frame stores its next adjacency position.
This avoids recursive call-stack overflow and repeated adjacency scans.

DSU find is iterative as well.

Costs, candidate distances and reconstructed route sums use Java long.
Under the assignment limits, a simple path or forest contains at most
99999 edges with absolute weight at most 1000000000.
Its absolute total is therefore below 10^14, within the signed 64-bit range.

Unreachable distances use null rather than a numeric sentinel.
In particular, -1 remains a valid finite distance.

Algorithm state is request-local, so later requests start with fresh
arrays, sets, queues and counters.

## 10. Current test evidence

The ping request returned {"status":"ready"}.

The six requests in tests/smoke.inputs.jsonl were executed consecutively
in the same Main process.

Their expected results are independently justified as follows:

- DSU: component states are [[3,2],[2,3],[2,3],[2,3]].
  The third operation repeats an existing connection; the fourth is a self-union.
- Kruskal and Prim: original edges 1 and 2 have total cost 2 + 3 = 5.
  Vertex 3 is isolated, so the component count is 2.
- SCC: the groups are {0,1}, {2} and {3}, with consecutive directed links
  between these groups.
- Topological ordering: vertices 0 and 1 precede 2, and 2 precedes 3.
- DAG: the distances are [0,3,-4,0,null], with parent edges [-1,0,2,3,-1].

The observed results matched these expectations.
Edge order, SCC labels and valid topological orders need not be identical
between different correct implementations.

The saved helper output confirms five successful reconstruction tests.
The benchmark also validates every generated spanning tree.

ExtendedTests.java was subsequently executed successfully in IntelliJ using
-Xms64m -Xmx384m. Its saved summary reports:

- 30 handcrafted fixtures, repeated twice through one Main.main invocation.
- 60 mixed JSON Lines requests passed, checking independence between requests.
- Five semantic demonstrations accepted alternative valid answers and rejected
  deliberately invalid witnesses for Kruskal, Prim, SCC, topo and DAG.
- Six full-limit cases passed, one for each required algorithm, with n = 100000
  and exactly 250000 edges or union operations per case.

The aligned inputs, independently specified expected answers and actual results
are saved in tests/extended.inputs.jsonl, tests/extended.expected.jsonl and
tests/extended.actual.jsonl. Case names are in tests/extended.case_names.json.
Witness demonstrations, scale summaries and final counts are saved in
tests/extended.witnesses.json, tests/extended.scale.json and
tests/extended.summary.json. The generators and expected-value reasoning are
included in ExtendedTests.java and tests/extended.README.txt.

The scale DSU joins a chain and then repeats self-unions. Scale weighted graphs
contain a negative chain followed by positive parallel edges; the independently
known minimum forest cost is -99999000000000. The directed chain forces a unique
topological order, one SCC per vertex and distances -v * 1000000000 from source 0.
These cases verify deep traversals and 64-bit totals. Scale durations include
conversion and validation; they are separate from the benchmark measurements.

The official setup and all 36 public algorithm cases passed locally.
The official repeated batch also passed: two complete batches in one process.
Saved logs are tests/public-setup.actual.txt and tests/public-checks.actual.txt.

## 11. Kruskal–Prim benchmark

### Generator and validation

Both methods receive the same generated graph object for each size.
Graph generation occurs outside the measured interval.

The deterministic generator first creates a random spanning tree with
strictly negative weights, then adds strictly positive edges and shuffles
all edge rows.

The negative tree is the unique minimum spanning tree: every other spanning
tree omits at least one negative tree edge and replaces the omitted edges
with positive edges, increasing the total cost.

Its generated weight sum supplies an independent optimum.

Before warm-up, and after every warm-up and measured call, the validator
checks:

- The known optimal cost.
- Exactly one connected component.
- Exactly n - 1 distinct valid original edge IDs.
- No cycle among selected edges.
- The exact sum of selected edge weights.

Validation is outside the timed interval.

### Measurement procedure

Each method receives five warm-up calls and five measured calls per size.
There is also one initial validation call per method.
The method running first alternates between repetitions.

System.nanoTime measures elapsed time, converted to milliseconds.
The median is the third value after sorting the five measurements.

The measured interval includes input-edge conversion, internal graph and
data-structure construction, sorting or heap operations, and result construction.

It excludes compilation, graph generation, JSON parsing, console/file output
and result validation. The measurement boundary is the same for both methods.

No explicit garbage collection is requested.

### Environment

- Execution: IntelliJ IDEA, Java 17.
- Java runtime: 17.0.20+8.
- VM: OpenJDK 64-Bit Server VM.
- OS reported by Java: Windows 11, version 10.0.
- CPU: Intel Core i5-12450HX.
- Physical cores: 8.
- Logical processors: 12.
- Installed RAM reported by the system: 16873545728 bytes.
- Maximum JVM heap reported during this run: 4219469824 bytes.

The complete measurements and runtime metadata are saved in
tests/benchmark_results.json.

### Actual measurements

All times are milliseconds.

| n | m | Seed | Method | Five measurements | Median |
|---:|---:|---:|---|---|---:|
| 1000 | 4000 | 20261024 | Kruskal | 1.6993, 1.6099, 1.9011, 1.4206, 1.3786 | 1.6099 |
| 1000 | 4000 | 20261024 | Prim | 3.9127, 1.3219, 1.2681, 1.4951, 1.1376 | 1.3219 |
| 10000 | 40000 | 20261025 | Kruskal | 16.1370, 15.9857, 15.0782, 15.6809, 15.1709 | 15.6809 |
| 10000 | 40000 | 20261025 | Prim | 16.2704, 15.4474, 15.4992, 16.4536, 16.2802 | 16.2704 |
| 50000 | 200000 | 20261026 | Kruskal | 88.2281, 90.5964, 90.4151, 85.3544, 87.1104 | 88.2281 |
| 50000 | 200000 | 20261026 | Prim | 117.5745, 112.0207, 102.3767, 105.2994, 109.2533 | 109.2533 |

The independently verified minimum costs were:

- n = 1000: -482119.
- n = 10000: -4990151.
- n = 50000: -25017545.

There were 30 measured calls in total.

### Interpretation and limitations

Prim had the lower median on the smallest graph.
Kruskal had the lower median on the two larger graphs.

Both methods became slower as the graph size increased.
Their practical constants differ because Kruskal sorts edges and performs
DSU operations, while Prim builds adjacency lists and maintains a heap.

Individual measurements vary because of JIT compilation, allocation,
garbage collection and operating-system scheduling.
Taking a median reduces sensitivity to an isolated slow measurement.

The experiment uses three sparse connected graphs with m = 4n, one seed
per size and a special weight distribution with a known optimum.
It does not establish which method is faster for every graph family.
Five measured repetitions are a small sample.

The measurements are consistent with both submitted implementations having
O(n + m log(m + 1)) time, but finite timings do not prove asymptotic bounds.

## 12. Build, run and test commands

Run these commands from the project root with JDK 17 selected.

PowerShell compilation:

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac --release 17 -encoding UTF-8 -d build src/Java/Main.java src/Java/Input.java src/Java/Json.java src/Java/Algorithms.java tests/GraphAlgorithmsTest.java tests/Benchmark.java tests/ExtendedTests.java
```

Run the JSON Lines program:

```powershell
java -cp build Main
```

Run the saved smoke inputs:

```powershell
Get-Content tests/smoke.inputs.jsonl | java -cp build Main
```

Run the route reconstruction tests:

```powershell
java -cp build GraphAlgorithmsTest
```

Reproduce the benchmark and save its new measurements:

```powershell
java -cp build Benchmark
```

Run the extended suite with the same memory limits as the successful IntelliJ run:

```powershell
java -Xms64m -Xmx384m -cp build ExtendedTests
```

The equivalent IntelliJ runs are Main, GraphAlgorithmsTest, Benchmark and
ExtendedTests. ExtendedTests uses VM options -Xms64m -Xmx384m.

With the included template tools, Python 3.12+ and Java 17 configured in the
terminal, run the official checks:

```powershell
python tools/run.py --setup
python tools/run.py --check
```

The official setup, all 36 public cases and the repeated mixed batch have passed
locally. GitHub Actions must also complete successfully for the submitted commit.