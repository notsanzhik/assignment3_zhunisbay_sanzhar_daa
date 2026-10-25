import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ExtendedTests {
    private static final int N = 100_000, M = 250_000;
    private static final Path OUTPUT = Path.of("tests").toAbsolutePath();

    private static final String SMALL = """
{"name":"ping","input":{"task":"ping"},"expected":{"status":"ready"}}
{"name":"dsu-empty","input":{"task":"dsu","n":0,"operations":[]},"expected":{"states":[]}}
{"name":"dsu-repeat-self","input":{"task":"dsu","n":5,"operations":[[0,1],[1,2],[2,0],[3,3],[3,4],[2,4]]},"expected":{"states":[[4,2],[3,3],[3,3],[3,3],[2,3],[1,5]]}}
{"name":"dsu-singleton","input":{"task":"dsu","n":1,"operations":[[0,0],[0,0]]},"expected":{"states":[[1,1],[1,1]]}}
{"name":"kruskal-empty","input":{"task":"kruskal","n":0,"edges":[]},"expected":{"cost":0,"components":0,"edge_ids":[]}}
{"name":"kruskal-isolates","input":{"task":"kruskal","n":5,"edges":[]},"expected":{"cost":0,"components":5,"edge_ids":[]}}
{"name":"kruskal-special","input":{"task":"kruskal","n":6,"edges":[[0,1,-4],[0,1,-4],[1,2,3],[0,2,3],[2,2,-999],[3,4,1000000000]]},"expected":{"cost":999999999,"components":3,"edge_ids":[0,2,5]}}
{"name":"kruskal-positive64","input":{"task":"kruskal","n":5,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000],[3,4,1000000000]]},"expected":{"cost":4000000000,"components":1,"edge_ids":[0,1,2,3]}}
{"name":"kruskal-negative64","input":{"task":"kruskal","n":5,"edges":[[0,1,-1000000000],[1,2,-1000000000],[2,3,-1000000000],[3,4,-1000000000]]},"expected":{"cost":-4000000000,"components":1,"edge_ids":[0,1,2,3]}}
{"name":"prim-empty","input":{"task":"prim","n":0,"edges":[]},"expected":{"cost":0,"components":0,"edge_ids":[]}}
{"name":"prim-isolates","input":{"task":"prim","n":5,"edges":[]},"expected":{"cost":0,"components":5,"edge_ids":[]}}
{"name":"prim-special","input":{"task":"prim","n":6,"edges":[[0,1,-4],[0,1,-4],[1,2,3],[0,2,3],[2,2,-999],[3,4,1000000000]]},"expected":{"cost":999999999,"components":3,"edge_ids":[0,2,5]}}
{"name":"prim-positive64","input":{"task":"prim","n":5,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000],[3,4,1000000000]]},"expected":{"cost":4000000000,"components":1,"edge_ids":[0,1,2,3]}}
{"name":"prim-negative64","input":{"task":"prim","n":5,"edges":[[0,1,-1000000000],[1,2,-1000000000],[2,3,-1000000000],[3,4,-1000000000]]},"expected":{"cost":-4000000000,"components":1,"edge_ids":[0,1,2,3]}}
{"name":"scc-empty","input":{"task":"scc","n":0,"edges":[]},"expected":{"component":[],"condensation":[]}}
{"name":"scc-special","input":{"task":"scc","n":6,"edges":[[0,1],[1,0],[1,2],[0,2],[2,3],[3,2],[3,3],[3,4],[3,4]]},"expected":{"component":[0,0,1,1,2,3],"condensation":[[0,1],[1,2]]}}
{"name":"scc-self-loop","input":{"task":"scc","n":1,"edges":[[0,0]]},"expected":{"component":[0],"condensation":[]}}
{"name":"topo-empty","input":{"task":"topo","n":0,"edges":[]},"expected":{"acyclic":true,"order":[]}}
{"name":"topo-finished-edge","input":{"task":"topo","n":5,"edges":[[0,2],[1,2],[1,2],[2,3]]},"expected":{"acyclic":true,"order":[4,1,0,2,3]}}
{"name":"topo-self-loop","input":{"task":"topo","n":1,"edges":[[0,0]]},"expected":{"acyclic":false,"order":[]}}
{"name":"topo-disconnected-cycle","input":{"task":"topo","n":4,"edges":[[0,1],[2,3],[3,2]]},"expected":{"acyclic":false,"order":[]}}
{"name":"dag-singleton","input":{"task":"dag","n":1,"source":0,"edges":[]},"expected":{"acyclic":true,"distance":[0],"parent_edge":[-1]}}
{"name":"dag-negative-unreachable","input":{"task":"dag","n":5,"source":0,"edges":[[0,1,3],[0,2,8],[1,2,-7],[2,3,4]]},"expected":{"acyclic":true,"distance":[0,3,-4,0,null],"parent_edge":[-1,0,2,3,-1]}}
{"name":"dag-unreachable-cycle","input":{"task":"dag","n":4,"source":0,"edges":[[0,1,2],[2,3,-1],[3,2,1]]},"expected":{"acyclic":false,"distance":[],"parent_edge":[]}}
{"name":"dag-tied-parallel","input":{"task":"dag","n":5,"source":0,"edges":[[0,1,2],[0,1,2],[0,2,1],[1,3,-3],[2,3,-2]]},"expected":{"acyclic":true,"distance":[0,2,1,-1,null],"parent_edge":[-1,0,2,3,-1]}}
{"name":"dag-positive64","input":{"task":"dag","n":5,"source":0,"edges":[[0,1,1000000000],[1,2,1000000000],[2,3,1000000000],[3,4,1000000000]]},"expected":{"acyclic":true,"distance":[0,1000000000,2000000000,3000000000,4000000000],"parent_edge":[-1,0,1,2,3]}}
{"name":"dag-negative64","input":{"task":"dag","n":5,"source":0,"edges":[[0,1,-1000000000],[1,2,-1000000000],[2,3,-1000000000],[3,4,-1000000000]]},"expected":{"acyclic":true,"distance":[0,-1000000000,-2000000000,-3000000000,-4000000000],"parent_edge":[-1,0,1,2,3]}}
{"name":"dag-nonzero-source","input":{"task":"dag","n":4,"source":1,"edges":[[0,1,-7],[1,2,-1],[3,2,-100]]},"expected":{"acyclic":true,"distance":[null,0,-1,null],"parent_edge":[-1,-1,1,-1]}}
{"name":"dag-self-loop","input":{"task":"dag","n":1,"source":0,"edges":[[0,0,-1]]},"expected":{"acyclic":false,"distance":[],"parent_edge":[]}}
{"name":"dag-reachable-cycle","input":{"task":"dag","n":3,"source":0,"edges":[[0,1,1],[1,2,-1],[2,1,1]]},"expected":{"acyclic":false,"distance":[],"parent_edge":[]}}
""";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }
    private static Map<String, Object> normalize(Map<String, Object> value) {
        return Json.parseRequest(Json.write(value));
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void save(String name, Object value) throws IOException {
        Files.writeString(OUTPUT.resolve(name), Json.write(value) + "\n");
    }
    private static String jsonl(List<Map<String, Object>> rows) {
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> row : rows) text.append(Json.write(row)).append('\n');
        return text.toString();
    }
    private static Map<String, Object> invoke(Map<String, Object> input) {
        return switch ((String) input.get("task")) {
            case "dsu" -> Algorithms.dsu(input);
            case "kruskal" -> Algorithms.kruskal(input);
            case "prim" -> Algorithms.prim(input);
            case "scc" -> Algorithms.scc(input);
            case "topo" -> Algorithms.topo(input);
            case "dag" -> Algorithms.dag(input);
            default -> throw new AssertionError("Unknown task");
        };
    }

    private static int mixedBatch(List<Map<String, Object>> cases) throws Exception {
        List<Map<String, Object>> inputs = new ArrayList<>(), expected = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int round = 0; round < 2; round++) {
            for (Map<String, Object> test : cases) {
                inputs.add(object(test.get("input")));
                expected.add(object(test.get("expected")));
                names.add(test.get("name") + "/round-" + (round + 1));
            }
        }
        String inputText = jsonl(inputs);
        Files.writeString(OUTPUT.resolve("extended.inputs.jsonl"), inputText);
        Files.writeString(OUTPUT.resolve("extended.expected.jsonl"), jsonl(expected));
        save("extended.case_names.json", names);
        InputStream previousInput = System.in;
        PrintStream previousOutput = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(inputText.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
            Main.main(new String[0]);
        } finally {
            System.out.flush();
            System.setIn(previousInput);
            System.setOut(previousOutput);
        }
        String outputText = captured.toString(StandardCharsets.UTF_8);
        Files.writeString(OUTPUT.resolve("extended.actual.jsonl"), outputText);
        List<String> lines = outputText.lines().toList();
        require(lines.size() == inputs.size(), "Incorrect number of JSON output lines");
        for (int i = 0; i < lines.size(); i++) {
            try {
                ExtendedValidation.check(inputs.get(i), Json.parseRequest(lines.get(i)), expected.get(i));
            } catch (AssertionError | RuntimeException error) {
                throw new AssertionError(names.get(i) + ": " + error.getMessage(), error);
            }
        }
        System.out.println("PASS mixed JSON batch: " + lines.size() + " requests in one Main invocation");
        return lines.size();
    }

    private static Map<String, Object> demo(String name, String input, String expected,
                                            String alternative, String invalid) {
        Map<String, Object> request = Json.parseRequest(input), want = Json.parseRequest(expected);
        Map<String, Object> good = Json.parseRequest(alternative), bad = Json.parseRequest(invalid);
        ExtendedValidation.check(request, good, want);
        boolean rejected = false;
        try {
            ExtendedValidation.check(request, bad, want);
        } catch (AssertionError error) {
            rejected = true;
        }
        require(rejected, name + ": invalid answer was accepted");
        return Map.of("name", name, "input", request, "expected", want,
                "alternative", good, "invalid", bad,
                "alternative_accepted", true, "invalid_rejected", true);
    }

    private static int witnesses() throws IOException {
        List<Map<String, Object>> reports = new ArrayList<>();
        for (String task : List.of("kruskal", "prim")) {
            reports.add(demo(task,
                    "{\"task\":\"" + task + "\",\"n\":3,\"edges\":[[0,1,1],[1,2,1],[0,2,1]]}",
                    "{\"cost\":2,\"components\":1}",
                    "{\"cost\":2,\"components\":1,\"edge_ids\":[0,2]}",
                    "{\"cost\":2,\"components\":1,\"edge_ids\":[0,0]}"));
        }
        reports.add(demo("scc",
                "{\"task\":\"scc\",\"n\":3,\"edges\":[[0,1],[1,0],[1,2],[1,2]]}",
                "{\"component\":[0,0,1]}",
                "{\"component\":[8,8,20],\"condensation\":[[8,20]]}",
                "{\"component\":[8,8,20],\"condensation\":[[8,20],[8,20]]}"));
        reports.add(demo("topo",
                "{\"task\":\"topo\",\"n\":3,\"edges\":[[0,2],[1,2]]}",
                "{\"acyclic\":true}",
                "{\"acyclic\":true,\"order\":[1,0,2]}",
                "{\"acyclic\":true,\"order\":[2,0,1]}"));
        reports.add(demo("dag",
                "{\"task\":\"dag\",\"n\":4,\"source\":0,\"edges\":[[0,1,2],[0,1,2],[0,2,1],[1,3,-3],[2,3,-2]]}",
                "{\"acyclic\":true,\"distance\":[0,2,1,-1]}",
                "{\"acyclic\":true,\"distance\":[0,2,1,-1],\"parent_edge\":[-1,1,2,4]}",
                "{\"acyclic\":true,\"distance\":[0,2,1,-1],\"parent_edge\":[-1,0,2,0]}"));
        save("extended.witnesses.json", reports);
        System.out.println("PASS semantic witnesses: " + reports.size() + " alternatives accepted, invalid answers rejected");
        return reports.size();
    }

    private static Map<String, Object> summarize(Map<String, Object> answer) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : answer.entrySet()) {
            if (entry.getValue() instanceof List<?> values) {
                Map<String, Object> array = new LinkedHashMap<>();
                array.put("length", values.size());
                if (!values.isEmpty()) {
                    array.put("first", values.get(0));
                    array.put("last", values.get(values.size() - 1));
                }
                result.put(entry.getKey(), array);
            } else result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, Object> scale(String task) {
        Map<String, Object> request = new LinkedHashMap<>(), expected = new LinkedHashMap<>();
        request.put("task", task); request.put("n", N);
        List<List<Integer>> rows = new ArrayList<>(M);
        if (task.equals("dsu")) {
            int[][] states = new int[M][2];
            for (int i = 0; i < M; i++) {
                rows.add(i < N - 1 ? List.of(i, i + 1) : List.of(0, 0));
                int merged = Math.min(i + 1, N - 1);
                states[i][0] = N - merged; states[i][1] = merged + 1;
            }
            request.put("operations", rows); expected.put("states", states);
        } else {
            boolean weighted = task.equals("kruskal") || task.equals("prim") || task.equals("dag");
            for (int i = 0; i < M; i++) {
                int u = i < N - 1 ? i : (i - (N - 1)) % (N - 1);
                if (weighted) rows.add(List.of(u, u + 1, i < N - 1 ? -1_000_000_000 : 1));
                else rows.add(List.of(u, u + 1));
            }
            request.put("edges", rows);
            switch (task) {
                case "kruskal", "prim" -> {
                    expected.put("cost", -(N - 1L) * 1_000_000_000L);
                    expected.put("components", 1);
                }
                case "scc" -> {
                    int[] groups = new int[N];
                    for (int i = 0; i < N; i++) groups[i] = i;
                    expected.put("component", groups);
                }
                case "topo" -> expected.put("acyclic", true);
                case "dag" -> {
                    request.put("source", 0); expected.put("acyclic", true);
                    Long[] distance = new Long[N];
                    for (int i = 0; i < N; i++) distance[i] = -i * 1_000_000_000L;
                    expected.put("distance", distance);
                }
                default -> throw new AssertionError("Unknown scale task");
            }
        }
        long started = System.nanoTime();
        Map<String, Object> actual = normalize(invoke(request));
        Map<String, Object> want = normalize(expected);
        ExtendedValidation.check(request, actual, want);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("task", task); report.put("n", N); report.put("items", M);
        report.put("generator", task.equals("dsu")
                ? "First n-1 chain unions, then self-unions [0,0]."
                : "First n-1 forward chain edges, then cyclically repeated forward edges. Original weighted edges have weight -1000000000; extra edges have weight 1.");
        report.put("expected_summary", summarize(want));
        report.put("actual_summary", summarize(actual));
        report.put("all_entries_validated", true);
        report.put("elapsed_ms_including_conversion_and_validation", (System.nanoTime() - started) / 1_000_000.0);
        System.out.println("PASS scale " + task + ": n=" + N + ", items=" + M);
        return report;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT);
        Files.deleteIfExists(OUTPUT.resolve("extended.summary.json"));
        List<Map<String, Object>> cases = SMALL.lines().map(Json::parseRequest).toList();
        require(cases.size() == 30, "Expected 30 small fixtures");
        int mixedCount = mixedBatch(cases);
        int witnessCount = witnesses();
        List<Map<String, Object>> scaleReports = new ArrayList<>();
        for (String task : List.of("dsu", "kruskal", "prim", "scc", "topo", "dag")) {
            scaleReports.add(scale(task));
        }
        save("extended.scale.json", scaleReports);
        save("extended.summary.json", Map.of(
                "passed", true, "small_fixtures", cases.size(),
                "mixed_requests_passed", mixedCount,
                "witness_demonstrations_passed", witnessCount,
                "full_limit_cases_passed", scaleReports.size(),
                "java_runtime", System.getProperty("java.runtime.version"),
                "max_heap_bytes", Runtime.getRuntime().maxMemory()));
        Files.writeString(OUTPUT.resolve("extended.README.txt"), """
ExtendedTests evidence (Java 17, no external libraries).
Run ExtendedTests.main() in IntelliJ, with the project root as working directory.
Compilation from the project root:
javac --release 17 -encoding UTF-8 -d build src/Java/Algorithms.java src/Java/Input.java src/Java/Json.java src/Java/Main.java tests/ExtendedTests.java
java -cp build ExtendedTests

Thirty handcrafted fixtures are repeated twice through one Main.main() invocation.
extended.inputs.jsonl, extended.expected.jsonl and extended.actual.jsonl have 60 aligned lines.
extended.case_names.json identifies those lines in execution order.
Expected answers are independently calculated; witnesses are checked semantically.
DSU expectations count merges and component sizes directly.
Special forests use -4+3+1000000000=999999999 and three components, including an isolate.
Positive and negative four-edge chains have costs +/-4000000000.
SCC expected groups are {0,1},{2,3},{4},{5}; condensation has only the two forward links.
Topological constraints are checked edge by edge; self-loops and disconnected cycles are rejected.
DAG distances follow explicit path sums; source 1 tests incoming edges and unreachable predecessors.
The source-unreachable cycle must still make the complete DAG request cyclic.
Equivalent forest edge sets, SCC labels, topological orders and DAG parents need not match exactly.
Five witness demonstrations accept alternatives and reject duplicate IDs, duplicate condensation,
invalid precedence and a parent edge ending at the wrong vertex.

Each of six scale generators uses n=100000 and exactly 250000 operations/edges.
DSU joins the chain, then self-unions: state[i]=[n-k,k+1], k=min(i+1,n-1).
The negative chain is the unique MST: cost=-(n-1)*1000000000=-99999000000000, components=1.
SCC has n singleton groups and exactly n-1 forward condensation pairs.
The topological chain forces order [0,1,...,99999].
DAG distance[v]=-v*1000000000; all parent edges must form paths to source 0.
The generators are stored in ExtendedTests.java; large results are summarized in extended.scale.json.
Every entry is validated before a scale case is marked as passed.
Scale durations include conversion and validation and are not Kruskal-Prim benchmark timings.
Only extended.summary.json with passed=true from a completed run records overall success.
""");
        System.out.println("EXTENDED TESTS PASSED: 30 fixtures, 60 mixed requests, 5 witness checks, 6 scale cases");
        System.out.println("Evidence saved in: " + OUTPUT);
    }
}


/** Semantic answer checks; expected optima/partitions/distances come from the fixtures. */
final class ExtendedValidation {
    private ExtendedValidation() { }
    private record Pair(long from, long to) { }

    static void check(Map<String, Object> request, Map<String, Object> answer,
                      Map<String, Object> expected) {
        try {
            require(answer != null, "answer must be an object");
            switch ((String) request.get("task")) {
                case "ping" -> require(Objects.equals(answer.get("status"), expected.get("status")), "ping status");
                case "dsu" -> require(sameNumbers(answer.get("states"), expected.get("states")), "DSU states");
                case "kruskal", "prim" -> forest(request, answer, expected);
                case "scc" -> scc(request, answer, expected);
                case "topo" -> topo(request, answer, expected);
                case "dag" -> dag(request, answer, expected);
                default -> throw new AssertionError("unknown task");
            }
        } catch (RuntimeException error) {
            throw new AssertionError("malformed answer: " + error.getClass().getSimpleName(), error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static long number(Object value) {
        require(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long,
                "expected a signed 64-bit integer");
        return ((Number) value).longValue();
    }
    private static int integer(Object value) { return Math.toIntExact(number(value)); }
    private static List<?> list(Object value) {
        require(value instanceof List<?>, "expected an array");
        return (List<?>) value;
    }
    private static boolean sameNumbers(Object actual, Object expected) {
        if (expected instanceof List<?> right) {
            List<?> left = list(actual);
            if (left.size() != right.size()) return false;
            for (int i = 0; i < left.size(); i++)
                if (!sameNumbers(left.get(i), right.get(i))) return false;
            return true;
        }
        return number(actual) == number(expected);
    }
    private static boolean acyclic(Map<String, Object> answer, Map<String, Object> expected) {
        Object actual = answer.get("acyclic");
        require(actual instanceof Boolean && actual.equals(expected.get("acyclic")), "acyclic flag");
        return (Boolean) actual;
    }
    private static int root(int[] parent, int vertex) {
        while (parent[vertex] != vertex) {
            parent[vertex] = parent[parent[vertex]];
            vertex = parent[vertex];
        }
        return vertex;
    }

    private static void forest(Map<String, Object> x, Map<String, Object> y, Map<String, Object> e) {
        int n = integer(x.get("n")), components = integer(y.get("components"));
        long cost = number(y.get("cost"));
        require(cost == number(e.get("cost")) && components == integer(e.get("components")), "forest optimum/components");
        require(components >= 0 && components <= n, "forest component range");
        List<?> edges = list(x.get("edges")), ids = list(y.get("edge_ids"));
        require(ids.size() == n - components, "forest edge count");
        int[] parent = new int[n], size = new int[n];
        for (int v = 0; v < n; v++) { parent[v] = v; size[v] = 1; }
        Set<Integer> used = new HashSet<>();
        long total = 0;
        for (Object value : ids) {
            int id = integer(value);
            require(id >= 0 && id < edges.size() && used.add(id), "forest edge ID/range/duplicate");
            List<?> edge = list(edges.get(id));
            int a = root(parent, integer(edge.get(0))), b = root(parent, integer(edge.get(1)));
            require(a != b, "forest cycle or self-loop");
            if (size[a] < size[b]) { int swap = a; a = b; b = swap; }
            parent[b] = a; size[a] += size[b];
            total = Math.addExact(total, number(edge.get(2)));
        }
        require(total == cost, "forest witness cost");
        for (Object value : edges) {
            List<?> edge = list(value);
            require(root(parent, integer(edge.get(0))) == root(parent, integer(edge.get(1))), "forest does not span an input component");
        }
    }

    private static void scc(Map<String, Object> x, Map<String, Object> y, Map<String, Object> e) {
        int n = integer(x.get("n"));
        List<?> labels = list(y.get("component")), reference = list(e.get("component"));
        require(labels.size() == n && reference.size() == n, "SCC label count");
        Map<Long, Long> forward = new HashMap<>(), reverse = new HashMap<>();
        long[] component = new long[n];
        for (int v = 0; v < n; v++) {
            long label = number(labels.get(v)), ref = number(reference.get(v));
            require(label >= 0, "SCC labels must be nonnegative");
            Long oldRef = forward.putIfAbsent(label, ref), oldLabel = reverse.putIfAbsent(ref, label);
            require((oldRef == null || oldRef == ref) && (oldLabel == null || oldLabel == label), "SCC partition");
            component[v] = label;
        }
        Set<Pair> wanted = new HashSet<>(), actual = new HashSet<>();
        for (Object value : list(x.get("edges"))) {
            List<?> edge = list(value);
            long from = component[integer(edge.get(0))], to = component[integer(edge.get(1))];
            if (from != to) wanted.add(new Pair(from, to));
        }
        for (Object value : list(y.get("condensation"))) {
            List<?> edge = list(value);
            require(edge.size() == 2, "condensation pair length");
            require(actual.add(new Pair(number(edge.get(0)), number(edge.get(1)))), "duplicate condensation pair");
        }
        require(actual.equals(wanted), "exact SCC condensation");
    }

    private static void topo(Map<String, Object> x, Map<String, Object> y, Map<String, Object> e) {
        boolean valid = acyclic(y, e);
        List<?> order = list(y.get("order"));
        if (!valid) { require(order.isEmpty(), "cyclic topological order must be empty"); return; }
        int n = integer(x.get("n"));
        require(order.size() == n, "topological order length");
        int[] position = new int[n]; Arrays.fill(position, -1);
        for (int i = 0; i < n; i++) {
            int vertex = integer(order.get(i));
            require(vertex >= 0 && vertex < n && position[vertex] == -1, "topological permutation");
            position[vertex] = i;
        }
        for (Object value : list(x.get("edges"))) {
            List<?> edge = list(value);
            require(position[integer(edge.get(0))] < position[integer(edge.get(1))], "topological precedence");
        }
    }

    private static void dag(Map<String, Object> x, Map<String, Object> y, Map<String, Object> e) {
        boolean valid = acyclic(y, e);
        List<?> distance = list(y.get("distance")), parents = list(y.get("parent_edge"));
        if (!valid) { require(distance.isEmpty() && parents.isEmpty(), "cyclic DAG arrays must be empty"); return; }
        int n = integer(x.get("n")), source = integer(x.get("source"));
        List<?> reference = list(e.get("distance")), edges = list(x.get("edges"));
        require(distance.size() == n && parents.size() == n && reference.size() == n, "DAG array lengths");
        Long[] dist = new Long[n];
        int[] predecessor = new int[n]; Arrays.fill(predecessor, -1);
        for (int v = 0; v < n; v++) {
            Object actual = distance.get(v), expected = reference.get(v);
            require(expected == null ? actual == null : actual != null && number(actual) == number(expected), "DAG shortest distance");
            if (actual != null) dist[v] = number(actual);
        }
        require(dist[source] != null && dist[source] == 0L, "source distance");
        for (int v = 0; v < n; v++) {
            int id = integer(parents.get(v));
            if (v == source || dist[v] == null) { require(id == -1, "source/unreachable parent"); continue; }
            require(id >= 0 && id < edges.size(), "DAG parent edge ID");
            List<?> edge = list(edges.get(id));
            int u = integer(edge.get(0));
            require(integer(edge.get(1)) == v && dist[u] != null, "DAG parent edge direction/reachability");
            require(Math.addExact(dist[u], number(edge.get(2))) == dist[v], "DAG parent edge distance");
            predecessor[v] = u;
        }
        // Memoise verified suffixes: every reachable vertex is processed at most twice.
        byte[] state = new byte[n]; state[source] = 2;
        int[] chain = new int[n];
        for (int v = 0; v < n; v++) {
            if (dist[v] == null || state[v] == 2) continue;
            int current = v, length = 0;
            while (state[current] == 0) {
                state[current] = 1; chain[length++] = current;
                current = predecessor[current];
                require(current >= 0, "DAG parent chain fails to reach source");
            }
            require(state[current] == 2, "DAG parent cycle");
            while (length > 0) state[chain[--length]] = 2;
        }
    }
}
