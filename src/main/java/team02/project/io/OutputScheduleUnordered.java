package team02.project.io;

import lombok.val;
import lombok.var;
import team02.project.algorithm.Schedule;

import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Path;

public class OutputScheduleUnordered {
    private OutputScheduleUnordered() {
    }

    public static void outputGraph(Path pathName, Schedule optimalSchedule) throws IOException {
        PrintWriter writer = new PrintWriter(pathName.toString());

        writer.println("digraph \"" + pathName.getFileName() + "\" {");

        for (var task : optimalSchedule) {
            String nodeStr = "\t" + task.getTask().getId() + " [Weight=" + task.getTask().getWeight() + ", Start=" + task.getStartTime() + ", Processor=" + task.getProcessorId() + "];";
            writer.println(nodeStr);
            for (val edge : task.getTask().getIncomingEdges().entrySet()) {
                String edgeStr = "\t" + edge.getKey().getId() + " -> " + task.getTask().getId() + " [Weight=" + edge.getValue() + "];";
                writer.println(edgeStr);
            }
        }

        writer.println("}");
        writer.flush();
        writer.close();
    }
}