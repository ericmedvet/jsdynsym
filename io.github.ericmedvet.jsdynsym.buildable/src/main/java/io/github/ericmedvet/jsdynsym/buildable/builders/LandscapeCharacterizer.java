package io.github.ericmedvet.jsdynsym.buildable.builders;

import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.FormattedNamedFunction;
import io.github.ericmedvet.jsdynsym.control.Simulation;
import io.github.ericmedvet.jsdynsym.control.SingleAgentTask;
import io.github.ericmedvet.jsdynsym.control.navigation.NavigationEnvironment;
import io.github.ericmedvet.jsdynsym.core.DynamicalSystem;
import io.github.ericmedvet.jsdynsym.core.numerical.ann.MultiLayerPerceptron;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LandscapeCharacterizer {
  record Pair(String environment, String builder) {
  }

  record Range(double min, double max) {
  }

  // Start parameters settings
  private final static String CSV_PATH = "/home/melsalib/Downloads/navigationFitnessSamples.csv";
  private final static long SEED = 0;
  private final static int N_POINTS = 10;
  private final static int N_NEIGHBORS = 10;
  private final static int N_SAMPLES = 10;
  private final static double SEGMENT_LENGTH = 1;
  private final static Range GENOTYPE_BOUNDS = new Range(-3, 3);
  private final static List<Pair> PROBLEMS = List.of(
      // Per grafico innerLayerRatio (1, 2, 3, 4, 5) con nOfSensors=7 e Barrier=M_BARRIER
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 1)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 2)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 4)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 5)"),

      // Per grafico nOfSensors (3, 5, 7, 9, 11) con innerLayerRatio=3 e Barrier=M_BARRIER
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 3)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 5)", "ds.num.mlp(innerLayerRatio = 3)"),
      //new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 9)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 11)", "ds.num.mlp(innerLayerRatio = 3)"),

      // Per grafico barrier (XS_BARRIER, S_BARRIER, M_BARRIER, L_BARRIER, XL_BARRIER) con innerLayerRatio=3 e nOfSensors=7
      new Pair("ds.e.navigation(arena = XS_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = S_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      //new Pair("ds.e.navigation(arena = M_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = L_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)"),
      new Pair("ds.e.navigation(arena = XL_BARRIER; nOfSensors = 7)", "ds.num.mlp(innerLayerRatio = 3)")
  );
  private final static List<String> FITNESS_FUNCTIONS = List.of(
      "ds.e.n.avgD()",
      "ds.e.n.minD()",
      "ds.e.n.finalD()"
  );
  // End parameters settings

  private final static NamedBuilder<Object> BUILDER = NamedBuilder.fromDiscovery();

  @SuppressWarnings("unchecked")
  private static double[] getFitnessValues(Pair problem, double[] mlpWeights) {
    NavigationEnvironment environment = (NavigationEnvironment) BUILDER.build(problem.environment);
    MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>) NamedBuilder.fromDiscovery().build(problem.builder))
        .apply(environment.nOfOutputs(), environment.nOfInputs());
    SingleAgentTask<DynamicalSystem<double[], double[], ?>, double[], double[], NavigationEnvironment.State> task =
        SingleAgentTask.fromEnvironment(environment, new double[2], new DoubleRange(0, 60), 1/60d);
    mlp.setParams(mlpWeights);
    Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>> outcome =
        task.simulate(mlp);
    return FITNESS_FUNCTIONS.stream()
        .mapToDouble(s -> ((FormattedNamedFunction<Simulation.Outcome<SingleAgentTask.Step<double[], double[], NavigationEnvironment.State>>, Double>) BUILDER.build(s)).apply(outcome))
        .toArray();
  }

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws FileNotFoundException {

    PrintStream ps = new PrintStream(CSV_PATH);
    String header = "ENVIRONMENT;BUILDER;POINT_INDEX;NEIGHBOR_INDEX;SAMPLE_INDEX;SEGMENT_LENGTH;" + String.join(";", FITNESS_FUNCTIONS);
    ps.println(header);
    ExecutorService executorService =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    Random random = new Random(SEED);

    for (Pair problem : PROBLEMS) {
      NavigationEnvironment environment = (NavigationEnvironment) BUILDER.build(problem.environment);
      MultiLayerPerceptron mlp = ((NumericalDynamicalSystems.Builder<MultiLayerPerceptron, ?>) NamedBuilder.fromDiscovery().build(problem.builder))
          .apply(environment.nOfOutputs(), environment.nOfInputs());
      int genotypeLength = mlp.getParams().length;
      for (int point = 0; point < N_POINTS; point++) {
        double[] centralGenotype = IntStream.range(0, mlp.getParams().length)
            .mapToDouble(i -> GENOTYPE_BOUNDS.min() + random.nextDouble() * (GENOTYPE_BOUNDS.max() - GENOTYPE_BOUNDS.min()))
            .toArray();

        // Calculate and store the centralGenotype fitness for the current point once and for all here
        double[] centralGenotypeFitnessValues = getFitnessValues(problem, centralGenotype);
        int finalPoint = point;
        executorService.submit(() -> {
          double[] fitnessValues = getFitnessValues(problem, centralGenotypeFitnessValues);
          for (int neighbor = 0; neighbor < N_NEIGHBORS; neighbor++) {
            StringBuilder line = new StringBuilder();
            line.append("%s;%s;%d;%d;%d;%.3f;"
                .formatted(
                    problem.environment,
                    problem.builder,
                    finalPoint,
                    neighbor,
                    0,
                    SEGMENT_LENGTH
                )
            );
            line.append(Arrays.stream(fitnessValues)
                .mapToObj(value -> String.format("%.5f", value))
                .collect(Collectors.joining(";")));
            ps.println(line);
            System.out.println(header.replace(';', '\t'));
            System.out.println(line.toString().replace(';', '\t'));
          }
        });

        for (int neighbor = 0; neighbor < N_NEIGHBORS; neighbor++) {
          double[] randomVector = IntStream.range(0, genotypeLength) // Extracts component with a Gaussian distribution to have uniformity on the sphere
              .mapToDouble(i -> GENOTYPE_BOUNDS.min() + random.nextGaussian() * (GENOTYPE_BOUNDS.max() - GENOTYPE_BOUNDS.min()))
              .toArray();
          double randomVector_norm = Math.sqrt(Arrays.stream(randomVector).
              boxed().
              mapToDouble(element -> element * element).
              sum());
          double[] neighborGenotype = IntStream.range(0, genotypeLength).
              mapToDouble(i -> (randomVector[i] / randomVector_norm) * SEGMENT_LENGTH + centralGenotype[i])
              .toArray();
          double[] sampleStep = IntStream.range(0, genotypeLength).
              mapToDouble(i -> (neighborGenotype[i] - centralGenotype[i]) / (N_SAMPLES - 1))
              .toArray();

          int finalNeighbor = neighbor;
          for (int sample = 1; sample < N_SAMPLES; sample++) {
            int finalSample = sample;
            executorService.submit(() -> {
              StringBuilder line = new StringBuilder();
              line.append("%s;%s;%d;%d;%d;%.3f;"
                  .formatted(
                      problem.environment,
                      problem.builder,
                      finalPoint,
                      finalNeighbor,
                      finalSample,
                      SEGMENT_LENGTH
                  )
              );
              double[] sampleGenotype = Arrays.stream(sampleStep)
                  .boxed()
                  .mapToDouble(s -> s * finalSample)
                  .toArray();
              double[] fitnessValues = getFitnessValues(problem, sampleGenotype);
              line.append(Arrays.stream(fitnessValues)
                  .mapToObj(value -> String.format("%.5f", value))
                  .collect(Collectors.joining(";")));
              ps.println(line);
              System.out.println(line.toString().replace(';', '\t'));
            });
          }

        }
      }
    }
    executorService.shutdown();
    boolean terminated = false;
    while (!terminated) {
      try {
        terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        // ignore
      }
    }
    System.out.println("Done");
    ps.close();
  }
}
