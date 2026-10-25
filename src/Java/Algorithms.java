import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public final class Algorithms {
    private Algorithms() { }

    private static final class DisjointSet {
        final int[] parent, size;
        int components, largest;

        DisjointSet(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;

            for (int v = 0; v < n; v++) {
                parent[v] = v;
                size[v] = 1;
            }
        }

        int find(int vertex) {
            int root = vertex;

            while (parent[root] != root) {
                root = parent[root];
            }

            while (parent[vertex] != vertex) {
                int previous = parent[vertex];
                parent[vertex] = root;
                vertex = previous;
            }

            return root;
        }

        boolean union(int u, int v) {
            int a = find(u);
            int b = find(v);

            if (a == b) {
                return false;
            }

            if (size[a] < size[b]) {
                int swap = a;
                a = b;
                b = swap;
            }

            parent[b] = a;
            size[a] += size[b];
            largest = Math.max(largest, size[a]);
            components--;

            return true;
        }
    }

    private static final class Adjacency {
        final int[] head, next, to, edgeId;
        int used;

        Adjacency(int n, int capacity) {
            head = new int[n];
            Arrays.fill(head, -1);
            next = new int[capacity];
            to = new int[capacity];
            edgeId = new int[capacity];
        }

        void add(int from, int destination, int id) {
            next[used] = head[from];
            to[used] = destination;
            edgeId[used] = id;
            head[from] = used++;
        }
    }

    private static Adjacency directed(
            int n, int[][] edges, boolean transpose) {

        Adjacency graph = new Adjacency(n, edges.length);

        for (int id = 0; id < edges.length; id++) {
            int[] edge = edges[id];

            graph.add(
                    edge[transpose ? 1 : 0],
                    edge[transpose ? 0 : 1],
                    id);
        }

        return graph;
    }

    private static int[] finishOrder(
            Adjacency graph, boolean detectCycles) {

        int n = graph.head.length;
        int count = 0;

        byte[] state = new byte[n];
        int[] vertices = new int[n];
        int[] nextArc = new int[n];
        int[] finished = new int[n];

        for (int start = 0; start < n; start++) {
            if (state[start] != 0) {
                continue;
            }

            int top = 0;
            vertices[top] = start;
            nextArc[top] = graph.head[start];
            state[start] = 1;

            while (top >= 0) {
                int u = vertices[top];
                int arc = nextArc[top];

                if (arc == -1) {
                    state[u] = 2;
                    finished[count++] = u;
                    top--;
                    continue;
                }

                nextArc[top] = graph.next[arc];
                int v = graph.to[arc];

                if (detectCycles && state[v] == 1) {
                    return null;
                }

                if (state[v] == 0) {
                    state[v] = 1;
                    vertices[++top] = v;
                    nextArc[top] = graph.head[v];
                }
            }
        }

        return finished;
    }

    private static void reverse(int[] values) {
        for (int left = 0, right = values.length - 1;
             left < right;
             left++, right--) {

            int swap = values[left];
            values[left] = values[right];
            values[right] = swap;
        }
    }

    static Map<String, Object> dsu(Map<String, Object> input) {
        DisjointSet sets = new DisjointSet(Input.vertexCount(input));
        int[][] operations = Input.operations(input);
        int[][] states = new int[operations.length][2];

        for (int i = 0; i < operations.length; i++) {
            sets.union(operations[i][0], operations[i][1]);
            states[i][0] = sets.components;
            states[i][1] = sets.largest;
        }

        return Map.of("states", states);
    }

    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        Integer[] order = new Integer[edges.length];

        for (int id = 0; id < order.length; id++) {
            order[id] = id;
        }

        Arrays.sort(
                order,
                Comparator.<Integer>comparingInt(id -> edges[id][2])
                        .thenComparingInt(id -> id));

        DisjointSet sets = new DisjointSet(n);
        List<Integer> chosen = new ArrayList<>();
        long cost = 0;

        for (int id : order) {
            if (sets.union(edges[id][0], edges[id][1])) {
                chosen.add(id);
                cost += (long) edges[id][2];
            }
        }

        return Map.of(
                "cost", cost,
                "edge_ids", chosen,
                "components", sets.components);
    }

    private record Candidate(int weight, int edgeId, int target) { }

    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        Adjacency graph = new Adjacency(n, 2 * edges.length);

        for (int id = 0; id < edges.length; id++) {
            graph.add(edges[id][0], edges[id][1], id);
            graph.add(edges[id][1], edges[id][0], id);
        }

        boolean[] visited = new boolean[n];

        PriorityQueue<Candidate> heap = new PriorityQueue<>(
                Comparator.comparingInt(Candidate::weight)
                        .thenComparingInt(Candidate::edgeId));

        List<Integer> chosen = new ArrayList<>();
        long cost = 0;
        int components = 0;

        for (int start = 0; start < n; start++) {
            if (visited[start]) {
                continue;
            }

            components++;
            int u = start;

            while (true) {
                visited[u] = true;

                for (int arc = graph.head[u];
                     arc != -1;
                     arc = graph.next[arc]) {

                    int v = graph.to[arc];
                    int id = graph.edgeId[arc];

                    if (!visited[v]) {
                        heap.add(new Candidate(edges[id][2], id, v));
                    }
                }

                Candidate next = null;

                while (!heap.isEmpty()) {
                    Candidate candidate = heap.remove();

                    if (!visited[candidate.target]) {
                        next = candidate;
                        break;
                    }
                }

                if (next == null) {
                    break;
                }

                chosen.add(next.edgeId);
                cost += (long) next.weight;
                u = next.target;
            }
        }

        return Map.of(
                "cost", cost,
                "edge_ids", chosen,
                "components", components);
    }

    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        Adjacency graph = directed(n, edges, false);
        Adjacency transpose = directed(n, edges, true);

        int[] finished = finishOrder(graph, false);
        int[] component = new int[n];
        int[] stack = new int[n];

        Arrays.fill(component, -1);
        int label = 0;

        for (int i = n - 1; i >= 0; i--) {
            int start = finished[i];

            if (component[start] != -1) {
                continue;
            }

            int size = 0;
            stack[size++] = start;
            component[start] = label;

            while (size > 0) {
                int u = stack[--size];

                for (int arc = transpose.head[u];
                     arc != -1;
                     arc = transpose.next[arc]) {

                    int v = transpose.to[arc];

                    if (component[v] == -1) {
                        component[v] = label;
                        stack[size++] = v;
                    }
                }
            }

            label++;
        }

        Set<Long> seen = new HashSet<>();
        List<int[]> condensation = new ArrayList<>();

        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];

            long key = ((long) from << 32) | (to & 0xffffffffL);

            if (from != to && seen.add(key)) {
                condensation.add(new int[]{from, to});
            }
        }

        return Map.of(
                "component", component,
                "condensation", condensation);
    }

    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        int[] order = finishOrder(directed(n, edges, false), true);

        if (order == null) {
            return Map.of(
                    "acyclic", false,
                    "order", new int[0]);
        }

        reverse(order);

        return Map.of(
                "acyclic", true,
                "order", order);
    }

    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);

        Adjacency graph = directed(n, edges, false);
        int[] order = finishOrder(graph, true);

        if (order == null) {
            return Map.of(
                    "acyclic", false,
                    "distance", new Long[0],
                    "parent_edge", new int[0]);
        }

        reverse(order);

        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];

        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;

        for (int u : order) {
            if (distance[u] == null) {
                continue;
            }

            for (int arc = graph.head[u];
                 arc != -1;
                 arc = graph.next[arc]) {

                int v = graph.to[arc];
                int id = graph.edgeId[arc];

                long candidate = distance[u] + (long) edges[id][2];

                if (distance[v] == null || candidate < distance[v]) {
                    distance[v] = candidate;
                    parentEdge[v] = id;
                }
            }
        }

        return Map.of(
                "acyclic", true,
                "distance", distance,
                "parent_edge", parentEdge);
    }

    public static List<Integer> reconstructPath(
            int source,
            int target,
            int[][] edges,
            int[] parentEdge) {

        List<Integer> route = new ArrayList<>();
        int current = target;

        while (current != source) {
            int id = parentEdge[current];

            if (id == -1) {
                return Collections.emptyList();
            }

            if (id < 0
                    || id >= edges.length
                    || edges[id][1] != current) {

                throw new IllegalArgumentException("Invalid parent edge");
            }

            route.add(current);

            if (route.size() >= parentEdge.length) {
                throw new IllegalArgumentException("Cycle in parent edges");
            }

            current = edges[id][0];
        }

        route.add(source);
        Collections.reverse(route);

        return route;
    }
}