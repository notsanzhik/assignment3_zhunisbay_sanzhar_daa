import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class GraphAlgorithmsTest {
    private static Map<String, Object> solve(int n, int source, int[][] edges) {
        List<List<Integer>> rows = new ArrayList<>();

        for (int[] edge : edges) {
            rows.add(List.of(edge[0], edge[1], edge[2]));
        }

        return Algorithms.dag(
                Map.of("n", n, "source", source, "edges", rows)
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkDistances(
            Map<String, Object> answer,
            Long[] expected) {

        require(
                Boolean.TRUE.equals(answer.get("acyclic")),
                "Expected a DAG"
        );

        Long[] actual = (Long[]) answer.get("distance");

        require(
                Arrays.equals(actual, expected),
                "Incorrect distances: " + Arrays.toString(actual)
        );
    }

    private static void checkRoute(
            String name,
            int source,
            int target,
            int[][] edges,
            int[] parents,
            Long expectedWeight) {

        List<Integer> route =
                Algorithms.reconstructPath(source, target, edges, parents);

        List<Integer> edgeIds = new ArrayList<>();
        long weight = 0;

        if (expectedWeight == null) {
            require(
                    route.isEmpty(),
                    name + ": unreachable route must be empty"
            );

            require(
                    parents[target] == -1,
                    name + ": unreachable parent must be -1"
            );

            System.out.println(
                    "PASS " + name + ": route=[] (unreachable)"
            );
            return;
        }

        require(!route.isEmpty(), name + ": route must not be empty");
        require(route.get(0) == source, name + ": wrong start vertex");

        require(
                route.get(route.size() - 1) == target,
                name + ": wrong end vertex"
        );

        for (int i = 1; i < route.size(); i++) {
            int from = route.get(i - 1);
            int to = route.get(i);
            int id = parents[to];

            require(
                    id >= 0 && id < edges.length,
                    name + ": invalid edge ID"
            );

            require(
                    edges[id][0] == from && edges[id][1] == to,
                    name + ": edge direction does not match the route"
            );

            edgeIds.add(id);
            weight += (long) edges[id][2];
        }

        require(
                weight == expectedWeight.longValue(),
                name + ": incorrect route weight"
        );

        System.out.println(
                "PASS " + name
                        + ": route=" + route
                        + ", edge_ids=" + edgeIds
                        + ", weight=" + weight
        );
    }

    public static void main(String[] args) {
        int[][] edges = {
                {0, 1, 3},
                {0, 2, 8},
                {1, 2, -7},
                {2, 3, 4}
        };

        Map<String, Object> answer = solve(5, 0, edges);

        checkDistances(
                answer,
                new Long[]{0L, 3L, -4L, 0L, null}
        );

        int[] parents = (int[]) answer.get("parent_edge");

        checkRoute("reachable", 0, 3, edges, parents, 0L);
        checkRoute("source", 0, 0, edges, parents, 0L);
        checkRoute("unreachable", 0, 4, edges, parents, null);

        int[][] tied = {
                {0, 1, 2},
                {0, 1, 2},
                {0, 2, 1},
                {1, 3, -3},
                {2, 3, -2}
        };

        answer = solve(5, 0, tied);

        checkDistances(
                answer,
                new Long[]{0L, 2L, 1L, -1L, null}
        );

        parents = (int[]) answer.get("parent_edge");

        checkRoute(
                "tied_parallel_actual",
                0, 3, tied, parents, -1L
        );

        checkRoute(
                "tied_alternative",
                0, 3, tied,
                new int[]{-1, 0, 2, 4, -1},
                -1L
        );

        System.out.println("Route reconstruction tests passed: 5/5");
    }
}