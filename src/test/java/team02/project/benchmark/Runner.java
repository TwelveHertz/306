package team02.project.benchmark;

import lombok.var;
import team02.project.algorithm.SequentialBranchBoundAlgorithm;
import team02.project.algorithm.solnspace.ao.AOSolutionSpace;
import team02.project.benchmark.AlgorithmBenchmark.Result;

import java.util.Optional;

public class Runner {
    public static void main(String[] args) {

        System.out.println("Starting...");

        var loader = new TestGraphLoader("/testPackages/all10nodes.csv");

        var benchmark = new AlgorithmBenchmark(() -> new SequentialBranchBoundAlgorithm(new AOSolutionSpace(), Optional.empty()));

        int total = 0;
        for (var testGraph : loader) {
            Result result = benchmark.run(testGraph.getFile(), testGraph.getNumProcessors());
            total += result.getTimeTaken();
            System.out.print(testGraph.getName());
            print(testGraph.getOptimal(), result.getScheduleLength(), result.getTimeTaken());
        }
        System.out.println("Total time: " + total + "ms");
    }

    private static void print(int expectedLength, int actualLength, long timeTaken) {
        System.out.println(
                "\t"
                + ((expectedLength == actualLength) ? "Optimal" : "Not Optimal")
                + "\t"
                + "Expected: " + expectedLength
                + "\t"
                + "Actual: " + actualLength
                + "\t"
                + timeTaken
                + "ms"
        );
    }

}
