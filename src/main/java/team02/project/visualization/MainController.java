package team02.project.visualization;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.colors.Bright;
import eu.hansolo.tilesfx.colors.Dark;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import team02.project.App;
import team02.project.algorithm.Schedule;
import team02.project.algorithm.ScheduledTask;
import team02.project.algorithm.SchedulingContext;
import team02.project.algorithm.stats.AlgorithmStats;
import team02.project.algorithm.stats.AlgorithmStatsListener;
import team02.project.cli.CLIConfig;
import team02.project.graph.Graph;
import team02.project.visualization.GanttChart.ExtraData;

import javax.sound.midi.Soundbank;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static javafx.scene.paint.Color.rgb;
import static team02.project.App.*;

public class MainController implements AlgorithmStatsListener {

    @FXML
    private VBox statsBox;

    @FXML
    private VBox rootVbox;

    @FXML
    private VBox memBox;

    @FXML
    private VBox schedulesBox;

    @FXML
    private HBox allocBox;

    @FXML
    private HBox orderBox;

    @FXML
    private VBox ganttBox;

    @FXML
    private TextFlow numProFlow, inGraphFlow, outGraphFlow;

    @FXML
    private Text schedCreatedText, currentBestText, timeElapsedText;

    private Tile memoryTile;
    private Tile allocationTile;
    private Tile orderTile;
    private Tile scheduleTile;
    private GanttChart<Number,String> chart;
    private Timeline timerHandler;

    private int numSchedules;
    private double startTime;
    private double currentTime;
    private double finishTime;

    private CLIConfig config;
    
    public void injectConfig(CLIConfig config){
        this.config = config;
    }
    
    public void init() {
        Objects.requireNonNull(config);

        setUpMemoryTile();
        setUpAllocationTile();
        setUpOrderTile();
        setUpScheduleTile();
        setUpGanttBox();
        setUpStatsBox();

        // monitor and update view of memory on another thread
        Timeline memoryHandler = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            double memoryUsage = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1000000d);
            memoryTile.setValue(memoryUsage);
        }));
        memoryHandler.setCycleCount(Animation.INDEFINITE);
        memoryHandler.play();

        // initialize the value in order for setValue
        memoryTile.setValue(0);
    }

    private void setUpMemoryTile() {
        this.memoryTile = TileBuilder.create().skinType(Tile.SkinType.BAR_GAUGE)
                .unit("MB")
                .maxValue(Runtime.getRuntime().maxMemory() / (1024 * 1024))
                .threshold(Runtime.getRuntime().maxMemory() * 0.8 / (1024 * 1024))
                .gradientStops(new Stop(0, rgb(244,160,0)),
                        new Stop(0.8, Bright.RED),
                        new Stop(1.0, Dark.RED))
                .animated(true)
                .decimals(0)
                .strokeWithGradient(true)
                .thresholdVisible(true)
                .backgroundColor(Color.WHITE)
                .valueColor(rgb(244,160,0))
                .unitColor(rgb(244,160,0))
                .barBackgroundColor(rgb(242, 242, 242))
                .thresholdColor(rgb(128, 84, 1))
                .needleColor(rgb(244,160,0))
                .build();

        memBox.getChildren().addAll(buildFlowGridPane(this.memoryTile));

    }

    private void setUpAllocationTile() {
        this.allocationTile = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .chartData(new ChartData(0), new ChartData(0))
                .animated(false)
                .smoothing(true)
                .minWidth(387)
                .backgroundColor(Color.WHITE)
                .valueColor(rgb(219,68,55))
                .build();

        allocBox.getChildren().addAll(buildFlowGridPane(this.allocationTile));
    }

    private void setUpOrderTile() {
        this.orderTile = TileBuilder.create().skinType(Tile.SkinType.SMOOTH_AREA_CHART)
                .chartData(new ChartData(0), new ChartData(0))
                .animated(false)
                .smoothing(true)
                .minWidth(387)
                .backgroundColor(Color.WHITE)
                .valueColor(rgb(15,157,88))
                .build();

        orderBox.getChildren().addAll(buildFlowGridPane(this.orderTile));
    }

    private void setUpScheduleTile() {
        this.scheduleTile =  TileBuilder.create().skinType(Tile.SkinType.NUMBER)
                .titleAlignment(TextAlignment.CENTER)
                .animated(true)
                .decimals(0)
                .backgroundColor(Color.WHITE)
                .valueColor(rgb(66,133,244))
                .build();

        schedulesBox.getChildren().addAll(buildFlowGridPane(this.scheduleTile));
    }

    private void updateNumSchedules(int i){
        schedCreatedText.setText(String.valueOf(i));
    }

    private FlowGridPane buildFlowGridPane(Tile tile) {
        return new FlowGridPane(1, 1, tile);
    }

    private void setUpStatsBox(){
        numProFlow.getChildren().add(new Text(String.valueOf(config.numberOfScheduleProcessors())));

        String inputString = config.inputDOTFile();
        String outputString = config.outputDOTFile();

        inputString = inputString.substring(inputString.lastIndexOf('/')+1);
        outputString = outputString.substring(outputString.lastIndexOf('/')+1);

        inGraphFlow.getChildren().add(new Text(inputString));
        outGraphFlow.getChildren().add(new Text(outputString));
    }

    private void startTimer(){

        startTime=System.currentTimeMillis();
        timerHandler = new Timeline(new KeyFrame(Duration.seconds(0.05), new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                currentTime=System.currentTimeMillis();
                timeElapsedText.setText(""+((currentTime-startTime)/1000));
            }
        }));
        timerHandler.setCycleCount(Timeline.INDEFINITE);
        timerHandler.play();
    }

    private void stopTimer(){
        timerHandler.stop();
    }


    private void setUpGanttBox(){

        // Setting up number of processors and array of their names
        int numberPro = config.numberOfScheduleProcessors();
        String[] processors = new String[numberPro];
        for (int i = 0;i<numberPro;i++){
            processors[i]="Processor "+i;
        }

        // Setting up time (x) axis
        final NumberAxis timeAxis = new NumberAxis();
        timeAxis.setLabel("");
        timeAxis.setTickLabelFill(Color.CHOCOLATE);
        timeAxis.setMinorTickCount(1);

        // Setting up processor (y) axis
        final CategoryAxis processorAxis = new CategoryAxis();
        processorAxis.setLabel("");
        processorAxis.setTickLabelFill(Color.CHOCOLATE);
        processorAxis.setTickLabelGap(1);
        processorAxis.setCategories(FXCollections.<String>observableArrayList(Arrays.asList(processors)));

        // Setting up chart
        chart = new GanttChart<Number,String>(timeAxis,processorAxis);
        chart.setLegendVisible(false);
        chart.setBlockHeight(280/numberPro);

        chart.getStylesheets().add(getClass().getResource("/GanttChart.css").toExternalForm());
        chart.setMaxHeight(ganttBox.getPrefHeight());
        ganttBox.getChildren().add(chart);

        //TODO Remove me uwu
//        testGannt();
    }

    private void updateGannt(Schedule bestSchedule){

        int numProcessers = config.numberOfScheduleProcessors();

        // new array of series to write onto
        Series[] seriesArray = new Series[numProcessers];

        // initializing series obj
        for (int i=0;i<numProcessers;i++){
            seriesArray[i]=new Series();
        }

        // for every task in schedule, write its data onto the specific series
        for (ScheduledTask scTask:bestSchedule.getTasks()){
            int idOfTask = scTask.getProcessorId();

            XYChart.Data newData = new XYChart.Data(scTask.getStartTime(), "Processor "+ String.valueOf(idOfTask),
                    new ExtraData(scTask, "task-style"));

            seriesArray[idOfTask].getData().add(newData);

        }

        //clear and rewrite series onto the chart
        chart.getData().clear();
        for (Series series: seriesArray){
            chart.getData().add(series);
        }

        // update the best text
        currentBestText.setText(""+bestSchedule.getFinishTime());
    }

    //TODO ############# REMOVE ME
//    private void testGannt(){
//        System.out.println("Remove the test Gantt method ");
//
//        Graph graph = createGraph(Paths.get(config.inputDOTFile()));
//        SchedulingContext ctx = new SchedulingContext(graph, config.numberOfScheduleProcessors());
//        Schedule maybeOptimal = calculateSchedule(config, graph, ctx);
//        System.out.print(maybeOptimal.getFinishTime());
//        writeOutput(Paths.get(config.outputDOTFile()),ctx,maybeOptimal);
//
//        updateGannt(maybeOptimal);
//    }

    @Override
    public void update(AlgorithmStats stats) {
        // take new timestamp
        System.out.println("Allocations: " + stats.getAllocationsExpanded());
        System.out.println("Orderings: " + stats.getOrderingsExpanded());

        System.out.println("Complete schedules: " + stats.getCompleteSchedules());

        // use old timestamp and cached values to update stuff

        // cache new values

        updateGannt(stats.getCurrentBest());
    }
}
