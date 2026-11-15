package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class StepperTest {
    @Test public void stepsUp() { assertEquals(115, Stepper.next(110, 1, 5, 40, 180)); }
    @Test public void stepsDown() { assertEquals(105, Stepper.next(110, -1, 5, 40, 180)); }
    @Test public void clampsAtTheEnds() { assertEquals(180, Stepper.next(180, 1, 5, 40, 180)); assertEquals(10, Stepper.next(10, -1, 5, 10, 100)); }
    @Test public void snapsOffGridValues() { assertEquals(115, Stepper.next(112, 1, 5, 40, 180)); assertEquals(110, Stepper.next(112, -1, 5, 40, 180)); }
    @Test public void outOfRangeComesBackIn() { assertEquals(180, Stepper.next(200, 1, 5, 40, 180)); assertEquals(40, Stepper.next(20, -1, 5, 40, 180)); }
    @Test public void stepOfOne() { assertEquals(71, Stepper.next(70, 1, 1, 50, 80)); assertEquals(50, Stepper.next(50, -1, 1, 50, 80)); }
}
