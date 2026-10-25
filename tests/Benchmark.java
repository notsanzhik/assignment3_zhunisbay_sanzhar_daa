import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Benchmark {
    private static final int WARMUPS = 5;
    private static final int REPETITIONS = 5;

    private record Fixture(
            Map<String, Object> request,
            long optimum,
            int n,
            int m,
            long seed) {
    }

    private static Fixture generate(int n, int m, long seed) {
        Random random = new Random(seed);
        List<List<Integer>> edges = new ArrayList<>(m);
        long optimum = 0;

        for (int v = 1; v < n; v++) {
            int weight = -1 - random.nextInt(1000);
            edges.add(List.of(random.nextInt(v), v, weight));
            optimum += weight;
        }

        while (edges.size() < m) {
            int u = random.nextInt(n);
            int v = random.nextInt(n - 1);

            if (v >= u) {
                v++;
            }

            edges.add(List.of(
                    u,
                    v,
                    1 + random.nextInt(1_000_000)
            ));
        }

        Collections.shuffle(edges, random);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("n", n);
        request.put("edges", edges);

        return new Fixture(request, optimum, n, m, seed);
    }

    private static int root(int[] parent, int vertex) {
        while (vertex != parent[vertex]) {
            parent[vertex] = parent[parent[vertex]];
            vertex = parent[vertex];
        }

        return vertex;
    }

    private static int[] ids(Object result) {
        if (result instanceof int[] array) {
            return array;
        }

        if (result instanceof List<?> list) {
            int[] array = new int[list.size()];

            for (int i = 0; i < array.length; i++) {
                array[i] = ((Number) list.get(i)).intValue();
            }

            return array;
        }

        throw new AssertionError("Unsupported edge_ids representation");
    }

    private static void validate(
            Fixture fixture,
            Map<String, Object> answer) {

        long cost = ((Number) answer.get("cost")).longValue();
        int components = ((Number) answer.get("components")).intValue();

        if (cost != fixture.optimum || components != 1) {
            throw new AssertionError("Incorrect MST cost or components");
        }

        int[][] edges = Input.edges(fixture.request);
        int[] chosen = ids(answer.get("edge_ids"));

        if (chosen.length != fixture.n - 1) {
            throw new AssertionError("Wrong forest size");
        }

        int[] parent = new int[fixture.n];

        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }

        boolean[] used = new boolean[fixture.m];
        long total = 0;

        for (int id : chosen) {
            if (id < 0 || id >= edges.length || used[id]) {
                throw new AssertionError("Invalid or repeated edge ID");
            }

            used[id] = true;

            int a = root(parent, edges[id][0]);
            int b = root(parent, edges[id][1]);

            if (a == b) {
                throw new AssertionError("Cycle in forest");
            }

            parent[a] = b;
            total += edges[id][2];
        }

        if (total != fixture.optimum) {
            throw new AssertionError("Edge weight sum mismatch");
        }
    }

    private static Map<String, Object> invoke(
            String method,
            Fixture fixture) {

        return method.equals("kruskal")
                ? Algorithms.kruskal(fixture.request)
                : Algorithms.prim(fixture.request);
    }

    private static double measure(String method, Fixture fixture) {
        long start = System.nanoTime();

        Map<String, Object> result = invoke(method, fixture);

        long elapsed = System.nanoTime() - start;

        validate(fixture, result);

        return elapsed / 1_000_000.0;
    }

    private static double median(double[] timings) {
        double[] sorted = timings.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    public static void main(String[] args) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();

        int[][] sizes = {
                {1000, 4000},
                {10000, 40000},
                {50000, 200000}
        };

        for (int i = 0; i < sizes.length; i++) {
            Fixture fixture = generate(
                    sizes[i][0],
                    sizes[i][1],
                    20261024L + i
            );

            validate(fixture, invoke("kruskal", fixture));
            validate(fixture, invoke("prim", fixture));

            for (int repetition = 0; repetition < WARMUPS; repetition++) {
                String first = repetition % 2 == 0 ? "kruskal" : "prim";
                String second = repetition % 2 == 0 ? "prim" : "kruskal";

                validate(fixture, invoke(first, fixture));
                validate(fixture, invoke(second, fixture));
            }

            double[] kruskal = new double[REPETITIONS];
            double[] prim = new double[REPETITIONS];

            for (int repetition = 0; repetition < REPETITIONS; repetition++) {
                if (repetition % 2 == 0) {
                    kruskal[repetition] = measure("kruskal", fixture);
                    prim[repetition] = measure("prim", fixture);
                } else {
                    prim[repetition] = measure("prim", fixture);
                    kruskal[repetition] = measure("kruskal", fixture);
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();

            row.put("n", fixture.n);
            row.put("m", fixture.m);
            row.put("seed", fixture.seed);
            row.put("verified_cost", fixture.optimum);
            row.put("kruskal_ms", kruskal);
            row.put("kruskal_median_ms", median(kruskal));
            row.put("prim_ms", prim);
            row.put("prim_median_ms", median(prim));

            results.add(row);
        }

        Map<String, Object> report = new LinkedHashMap<>();

        report.put("java_runtime", System.getProperty("java.runtime.version"));
        report.put("java_vm", System.getProperty("java.vm.name"));
        report.put(
                "os",
                System.getProperty("os.name")
                        + " " + System.getProperty("os.version")
        );
        report.put(
                "available_processors",
                Runtime.getRuntime().availableProcessors()
        );
        report.put("max_heap_bytes", Runtime.getRuntime().maxMemory());
        report.put("timer", "System.nanoTime; milliseconds");
        report.put("warmup_calls_per_method_per_size", WARMUPS);
        report.put("measured_calls_per_method_per_size", REPETITIONS);
        report.put("results", results);

        Path output = Path.of(
                "tests", "benchmark_results.json"
        ).toAbsolutePath();

        Files.createDirectories(output.getParent());
        Files.writeString(
                output,
                Json.write(report) + System.lineSeparator()
        );

        for (Map<String, Object> row : results) {
            System.out.printf(
                    java.util.Locale.ROOT,
                    "n=%d, m=%d: Kruskal median=%.4f ms; Prim median=%.4f ms%n",
                    row.get("n"),
                    row.get("m"),
                    row.get("kruskal_median_ms"),
                    row.get("prim_median_ms")
            );
        }

        System.out.println(
                "Verified 3 graph sizes, 5 timings per method per size."
        );
        System.out.println("Saved: " + output);
    }
}