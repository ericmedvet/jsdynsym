/*-
 * ========================LICENSE_START=================================
 * jsdynsym-control
 * %%
 * Copyright (C) 2023 - 2025 Eric Medvet
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package io.github.ericmedvet.jsdynsym.control.pong;

import io.github.ericmedvet.jnb.datastructure.DoubleRange;
import io.github.ericmedvet.jnb.datastructure.Pair;
import io.github.ericmedvet.jsdynsym.control.HomogeneousBiEnvironment;
import io.github.ericmedvet.jsdynsym.control.pong.PongEnvironment.State;
import io.github.ericmedvet.jsdynsym.core.numerical.MultivariateRealFunction;
import io.github.ericmedvet.jsdynsym.core.numerical.NumericalDynamicalSystem;
import io.github.ericmedvet.jviz.core.geometry.Point;
import io.github.ericmedvet.jviz.core.geometry.Rectangle;
import io.github.ericmedvet.jviz.core.geometry.Segment;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;

public class PongEnvironment implements HomogeneousBiEnvironment<double[], double[], State, NumericalDynamicalSystem<?>> {

  private final Configuration configuration;
  private State state;
  private double previousTime;
  private final Rectangle arena;

  public PongEnvironment(Configuration configuration) {
    this.configuration = configuration;
    this.arena = new Rectangle(new Point(0.0, configuration.arenaYLength), new Point(configuration.arenaXLength, 0.0));
    reset();
  }

  public record Configuration(
      DoubleRange racketsInitialYRange,
      double racketsLength,
      double racketsMaxDeltaY,
      double ballInitialVelocity,
      double ballMaxVelocity,
      DoubleRange ballInitialAngleRange,
      double ballAccelerationRate,
      double maxPercentageAngleAdjustment,
      double arenaXLength,
      double arenaYLength,
      double precision,
      RandomGenerator randomGenerator
  ) {
    public static final Configuration DEFAULT = new Configuration(
        new DoubleRange(22, 28),
        5,
        0.5,
        20,
        50,
        new DoubleRange(-Math.PI / 8, Math.PI / 8),
        1.1,
        0.1,
        60,
        50,
        1e-3,
        new Random()
    );
  }

  public record State(
      Configuration configuration, RacketState lRacketState, RacketState rRacketState, BallState ballState
  ) {}

  public enum Side {
    LEFT, RIGHT
  }

  private enum ArenaObject {
    L_RACKET, R_RACKET, ARENA_UPPER_EDGE, ARENA_LOWER_EDGE, NONE
  }

  public record RacketState(double yCenter, int nOfBallCollisions, double score, Side side) {
    RacketState updatePosition(double yCenter) {
      return new RacketState(yCenter, this.nOfBallCollisions, this.score, this.side);
    }

    RacketState incrementNofBallCollisions() {
      return new RacketState(this.yCenter, this.nOfBallCollisions + 1, this.score, this.side);
    }

    RacketState incrementScore() {
      return new RacketState(this.yCenter, this.nOfBallCollisions, this.score + 1, this.side);
    }
  }

  public record BallState(Point position, Point velocity, int nOfCollisions) {
    BallState deepCopy() {
      return new BallState(this.position, this.velocity, this.nOfCollisions);
    }

    BallState rotate(Point centerOfRotation, double angle) {
      Point ballCenterRotated = this.position.rotate(centerOfRotation, angle);
      Point ballVelocityRotated = this.velocity.rotate(centerOfRotation, angle);
      return new BallState(ballCenterRotated, ballVelocityRotated, this.nOfCollisions);
    }
  }

  private Pair<double[], double[]> getNormalizedRacketObservations() {
    BallState flippedBallState = flippedHorizontalAxisReferenceFrame(state.ballState);
    return new Pair<>(
        new double[]{state.lRacketState.yCenter / configuration.arenaYLength, state.ballState.position()
            .x() / configuration.arenaXLength, state.ballState.position()
                .y() / configuration.arenaYLength, state.ballState.velocity()
                    .x() / configuration.ballMaxVelocity, state.ballState.velocity()
                        .y() / configuration.ballMaxVelocity, state.rRacketState.yCenter / configuration.arenaYLength,
        },
        new double[]{state.rRacketState.yCenter / configuration.arenaYLength, flippedBallState.position()
            .x() / configuration.arenaXLength, flippedBallState.position()
                .y() / configuration.arenaYLength, flippedBallState.velocity()
                    .x() / configuration.ballMaxVelocity, flippedBallState.velocity()
                        .y() / configuration.ballMaxVelocity, state.lRacketState.yCenter / configuration.arenaYLength,
        }
    );
  }

  public int nOfInputsPerAgent() {
    return 1;
  }

  public int nOfObservationsPerAgent() {
    return getNormalizedRacketObservations().first().length;
  }

  @Override
  public NumericalDynamicalSystem<?> exampleAgent() {
    return MultivariateRealFunction.from(
        o -> new double[nOfInputsPerAgent()],
        nOfObservationsPerAgent(),
        nOfInputsPerAgent()
    );
  }

  @Override
  public double[] defaultObservation() {
    return new double[nOfObservationsPerAgent()];
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public void reset() {
    state = new State(
        configuration,
        new RacketState(
            configuration.racketsInitialYRange.denormalize(configuration.randomGenerator.nextDouble()),
            0,
            0,
            Side.LEFT
        ),
        new RacketState(
            configuration.racketsInitialYRange.denormalize(configuration.randomGenerator.nextDouble()),
            0,
            0,
            Side.RIGHT
        ),
        new BallState(
            new Point(configuration.arenaXLength / 2.0, configuration.arenaYLength / 2.0),
            randomBallVelocity(),
            0
        )
    );
    previousTime = 0.0;
  }

  @Override
  public Pair<double[], double[]> step(double t, Pair<double[], double[]> normalizedActions) {
    if (normalizedActions.first().length != nOfInputsPerAgent()) {
      throw new IllegalArgumentException(
          "Left agent action has wrong number of elements: %d found, %d expected"
              .formatted(normalizedActions.first().length, nOfInputsPerAgent())
      );
    }
    if (normalizedActions.second().length != nOfInputsPerAgent()) {
      throw new IllegalArgumentException(
          "Right agent action has wrong number of elements: %d found, %d expected"
              .formatted(normalizedActions.second().length, nOfInputsPerAgent())
      );
    }
    // clip and denormalize inputs
    double lAction = DoubleRange.SYMMETRIC_UNIT.clip(normalizedActions.first()[0]) * configuration.racketsMaxDeltaY;
    double rAction = DoubleRange.SYMMETRIC_UNIT.clip(normalizedActions.second()[0]) * configuration.racketsMaxDeltaY;
    // update time
    double deltaTime = t - previousTime;
    previousTime = t;
    // update rackets states
    RacketState updatedLRacketState = updateRacketPosition(state.lRacketState, lAction);
    RacketState updatedRRacketState = updateRacketPosition(state.rRacketState, rAction);
    // update ball state
    BallState previousBallState = state.ballState;
    BallState updatedBallState = updateBallState(previousBallState, deltaTime);
    // manage collisions if any
    Segment ballTrajectory;
    boolean collisionIsPossible = true;
    boolean resetStateAfterPoint = false;
    ArenaObject lastCollidingObject = ArenaObject.NONE;
    while (collisionIsPossible) {
      // are considered in the next step
      ballTrajectory = getAsSegment(previousBallState, updatedBallState);
      Optional<Point> lRacketCollision = ballTrajectory.intersection(
          getAsSegment(updatedLRacketState),
          configuration.precision
      );
      Optional<Point> rRacketCollision = ballTrajectory.intersection(
          getAsSegment(updatedRRacketState),
          configuration.precision
      );
      List<Point> arenaHorizontalEdgesCollisions = arena.horizontalEdgesIntersections(
          ballTrajectory,
          configuration.precision
      );
      Point arenaHorizontalEdgesCollision = arenaHorizontalEdgesCollisions
          .isEmpty() ? null : arenaHorizontalEdgesCollisions.getFirst();
      ArenaObject closestCollidingArenaObject = getClosestCollidingArenaObject(
          lRacketCollision,
          rRacketCollision,
          arenaHorizontalEdgesCollision,
          previousBallState,
          updatedBallState,
          lastCollidingObject
      );
      switch (closestCollidingArenaObject) {
        case NONE:
          if (updatedBallState.position.x() < 0) {
            updatedRRacketState = updatedRRacketState.incrementScore();
            resetStateAfterPoint = true;
          } else if (updatedBallState.position.x() > configuration.arenaXLength) {
            updatedLRacketState = updatedLRacketState.incrementScore();
            resetStateAfterPoint = true;
          }
          collisionIsPossible = false;
          break;
        case L_RACKET:
          if (lRacketCollision.isPresent()) {
            updatedLRacketState = updatedLRacketState.incrementNofBallCollisions();
            Pair<BallState, BallState> lBallStates = racketsCollision(
                lRacketCollision.get(),
                updatedBallState,
                updatedLRacketState,
                lAction
            );
            updatedBallState = lBallStates.first();
            previousBallState = lBallStates.second();
            lastCollidingObject = ArenaObject.L_RACKET;
          }
          break;
        case R_RACKET:
          if (rRacketCollision.isPresent()) {
            updatedRRacketState = updatedRRacketState.incrementNofBallCollisions();
            Pair<BallState, BallState> rBallStates = racketsCollision(
                rRacketCollision.get(),
                updatedBallState,
                updatedRRacketState,
                rAction
            );
            updatedBallState = rBallStates.first();
            previousBallState = rBallStates.second();
            lastCollidingObject = ArenaObject.R_RACKET;
          }
          break;
        case ARENA_UPPER_EDGE:
          assert arenaHorizontalEdgesCollision != null;
          double offsetAbove = configuration.arenaYLength - updatedBallState.position.y();
          Point bouncedBallVelocityUE = new Point(
              updatedBallState.velocity().x(),
              -updatedBallState.velocity().y()
          );
          Point bouncedBallPositionUE = new Point(
              updatedBallState.position.x(),
              configuration.arenaYLength + offsetAbove
          );
          updatedBallState = new BallState(
              bouncedBallPositionUE,
              bouncedBallVelocityUE,
              updatedBallState.nOfCollisions + 1
          );
          previousBallState = new BallState(
              arenaHorizontalEdgesCollision,
              updatedBallState.velocity(),
              updatedBallState.nOfCollisions + 1
          );
          lastCollidingObject = ArenaObject.ARENA_UPPER_EDGE;
          break;
        case ARENA_LOWER_EDGE:
          assert arenaHorizontalEdgesCollision != null;
          double offsetBelow = updatedBallState.position.y();
          Point bouncedBallVelocityLE = new Point(
              updatedBallState.velocity().x(),
              -updatedBallState.velocity().y()
          );
          Point bouncedBallPositionLE = new Point(updatedBallState.position.x(), -offsetBelow);
          updatedBallState = new BallState(
              bouncedBallPositionLE,
              bouncedBallVelocityLE,
              updatedBallState.nOfCollisions + 1
          );
          previousBallState = new BallState(
              arenaHorizontalEdgesCollision,
              updatedBallState.velocity(),
              updatedBallState.nOfCollisions + 1
          );
          lastCollidingObject = ArenaObject.ARENA_LOWER_EDGE;
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + closestCollidingArenaObject);
      }
    }
    // update state
    if (resetStateAfterPoint) {
      resetSetAfterPoint(updatedLRacketState, updatedRRacketState, updatedBallState);
    } else {
      updateState(updatedBallState, updatedLRacketState, updatedRRacketState);
    }
    // return pair of observations
    return getNormalizedRacketObservations();
  }

  private ArenaObject getClosestCollidingArenaObject(
      Optional<Point> lRacketCollision,
      Optional<Point> rRacketCollision,
      Point arenaHorizontalEdgesCollision,
      BallState previousBallState,
      BallState updatedBallState,
      ArenaObject lastCollidingObject
  ) {
    Point previousBallPosition = previousBallState.position;
    ArenaObject closestCollidingArenaObject = ArenaObject.NONE;
    double closestDistance = Double.MAX_VALUE;
    if (lRacketCollision.isPresent() && lastCollidingObject != ArenaObject.L_RACKET) {
      double distance = previousBallPosition.distance(lRacketCollision.get());
      if (distance < closestDistance) {
        closestDistance = distance;
        closestCollidingArenaObject = ArenaObject.L_RACKET;
      }
    }
    if (rRacketCollision.isPresent() && lastCollidingObject != ArenaObject.R_RACKET) {
      double distance = previousBallPosition.distance(rRacketCollision.get());
      if (distance < closestDistance) {
        closestDistance = distance;
        closestCollidingArenaObject = ArenaObject.R_RACKET;
      }
    }
    if (arenaHorizontalEdgesCollision != null) {
      double distance = previousBallPosition.distance(arenaHorizontalEdgesCollision);
      if (distance < closestDistance) {
        double offsetAbove = configuration.arenaYLength - updatedBallState.position.y();
        double offsetBelow = updatedBallState.position.y();
        if (offsetAbove <= configuration.precision && lastCollidingObject != ArenaObject.ARENA_UPPER_EDGE) {
          closestCollidingArenaObject = ArenaObject.ARENA_UPPER_EDGE;
        } else if (offsetBelow <= configuration.precision && lastCollidingObject != ArenaObject.ARENA_LOWER_EDGE) {
          closestCollidingArenaObject = ArenaObject.ARENA_LOWER_EDGE;
        }
      }
    }
    return closestCollidingArenaObject;
  }

  private Pair<BallState, BallState> racketsCollision(
      Point racketCollisionPoint,
      BallState updatedBallState,
      RacketState updatedRacketState,
      double racketAction
  ) {
    Point collisionPointRRF = toRacketReferenceFrame(racketCollisionPoint, updatedRacketState);
    BallState updatedBallStateRRF = toRacketReferenceFrame(updatedBallState, updatedRacketState);
    double deltaX = updatedBallStateRRF.position.x() - collisionPointRRF.x();
    Point increasedBallVelocity = updatedBallStateRRF.velocity().scale(configuration.ballAccelerationRate);
    if (increasedBallVelocity.magnitude() > configuration.ballMaxVelocity) {
      increasedBallVelocity = new Point(increasedBallVelocity.direction()).scale(configuration.ballMaxVelocity);
    }
    BallState bouncedBallState = new BallState(
        new Point(collisionPointRRF.x() - deltaX, updatedBallStateRRF.position.y()),
        new Point(-increasedBallVelocity.x(), increasedBallVelocity.y()),
        updatedBallStateRRF.nOfCollisions + 1
    );
    double collisionAngle = bouncedBallState.position.getRotationAngle(racketCollisionPoint);
    double anglePercentageCorrection = DoubleRange.SYMMETRIC_UNIT.clip(
        racketAction / configuration.racketsMaxDeltaY
    ) * configuration.maxPercentageAngleAdjustment;
    double correctionAngle;
    if (collisionAngle >= 0) {
      correctionAngle = -collisionAngle * anglePercentageCorrection;
    } else {
      correctionAngle = collisionAngle * anglePercentageCorrection;
    }
    bouncedBallState = bouncedBallState.rotate(collisionPointRRF, correctionAngle);
    BallState previousBallState = new BallState(
        new Point(
            collisionPointRRF.x() + (bouncedBallState.position.x() - collisionPointRRF.x()) * configuration.precision,
            collisionPointRRF.y() + (bouncedBallState.position.y() - collisionPointRRF.y()) * configuration.precision
        ),
        bouncedBallState.velocity(),
        bouncedBallState.nOfCollisions
    );
    return new Pair<>(
        toArenaReferenceFrame(bouncedBallState, updatedRacketState),
        toArenaReferenceFrame(previousBallState, updatedRacketState)
    );
  }

  private Point toRacketReferenceFrame(Point point, RacketState racketState) {
    if (racketState.side.equals(Side.LEFT)) {
      return new Point(point.x(), point.y() - racketState.yCenter);
    } else {
      return new Point(configuration.arenaXLength - point.x(), point.y() - racketState.yCenter);
    }
  }

  // The racket reference frame is centered in the racket center with the x-axis pointing the center of the arena and
  // the y-axis pointing upwards
  private BallState toRacketReferenceFrame(BallState ballState, RacketState racketState) {
    BallState ballStateFlippedReferenceFrame = ballState.deepCopy();
    if (racketState.side.equals(Side.RIGHT)) {
      ballStateFlippedReferenceFrame = flippedHorizontalAxisReferenceFrame(ballStateFlippedReferenceFrame);
    }
    return new BallState(
        new Point(
            ballStateFlippedReferenceFrame.position.x(),
            ballStateFlippedReferenceFrame.position.y() - racketState.yCenter
        ),
        ballStateFlippedReferenceFrame.velocity,
        ballStateFlippedReferenceFrame.nOfCollisions
    );
  }

  private BallState toArenaReferenceFrame(BallState ballState, RacketState racketState) {
    BallState ballStateFlippedReferenceFrame = ballState.deepCopy();
    if (racketState.side.equals(Side.RIGHT)) {
      ballStateFlippedReferenceFrame = flippedHorizontalAxisReferenceFrame(ballStateFlippedReferenceFrame);
    }
    return new BallState(
        new Point(
            ballStateFlippedReferenceFrame.position.x(),
            ballStateFlippedReferenceFrame.position.y() + racketState.yCenter
        ),
        ballStateFlippedReferenceFrame.velocity,
        ballStateFlippedReferenceFrame.nOfCollisions
    );
  }

  private BallState flippedHorizontalAxisReferenceFrame(BallState ballState) {
    return new BallState(
        new Point(configuration.arenaXLength() - ballState.position.x(), ballState.position.y()),
        new Point(-ballState.velocity.x(), ballState.velocity.y()),
        ballState.nOfCollisions
    );
  }

  private Segment getAsSegment(BallState previousBallState, BallState nextBallState) {
    return new Segment(previousBallState.position, nextBallState.position);
  }

  private Segment getAsSegment(RacketState racketState) {
    if (racketState.side == Side.LEFT) {
      return new Segment(
          new Point(0, racketState.yCenter - configuration.racketsLength / 2),
          new Point(0, racketState.yCenter + configuration.racketsLength / 2)
      );
    } else {
      return new Segment(
          new Point(configuration.arenaXLength, racketState.yCenter - configuration.racketsLength / 2),
          new Point(configuration.arenaXLength, racketState.yCenter + configuration.racketsLength / 2)
      );
    }
  }

  private BallState updateBallState(BallState ballState, double deltaTime) {
    double deltaX = ballState.velocity.x() * deltaTime;
    double deltaY = ballState.velocity.y() * deltaTime;
    Point deltaPosition = new Point(deltaX, deltaY);
    return new BallState(ballState.position.sum(deltaPosition), ballState.velocity, ballState.nOfCollisions);
  }

  private RacketState updateRacketPosition(RacketState racketState, double deltaY) {
    double updatedY = racketState.yCenter + deltaY;
    DoubleRange racketsValidMargin = new DoubleRange(
        configuration.racketsLength / 2,
        configuration.arenaYLength - configuration.racketsLength / 2
    );
    updatedY = racketsValidMargin.clip(updatedY);
    return racketState.updatePosition(updatedY);
  }

  private void resetSetAfterPoint(
      RacketState updatedLRacketState,
      RacketState updatedRRacketState,
      BallState ballState
  ) {
    state = new State(
        configuration,
        new RacketState(
            configuration.racketsInitialYRange.denormalize(configuration.randomGenerator.nextDouble()),
            updatedLRacketState.nOfBallCollisions,
            updatedLRacketState.score,
            updatedLRacketState.side
        ),
        new RacketState(
            configuration.racketsInitialYRange.denormalize(configuration.randomGenerator.nextDouble()),
            updatedRRacketState.nOfBallCollisions,
            updatedRRacketState.score,
            updatedRRacketState.side
        ),
        new BallState(
            new Point(configuration.arenaXLength / 2.0, configuration.arenaYLength / 2.0),
            randomBallVelocity(),
            ballState.nOfCollisions
        )
    );
  }

  private void updateState(BallState ballState, RacketState lRacketState, RacketState rRacketState) {
    state = new State(configuration, lRacketState, rRacketState, ballState);
  }

  private Point randomBallVelocity() {
    double ballInitialAngle = configuration.ballInitialAngleRange.denormalize(
        configuration.randomGenerator.nextDouble()
    );
    boolean flipBallVelocity = configuration.randomGenerator.nextBoolean();
    if (flipBallVelocity) {
      ballInitialAngle = ballInitialAngle + Math.PI;
    }
    double ballInitialXV = Math.cos(ballInitialAngle) * configuration.ballInitialVelocity;
    double ballInitialYV = Math.sin(ballInitialAngle) * configuration.ballInitialVelocity;
    return new Point(ballInitialXV, ballInitialYV);
  }
}