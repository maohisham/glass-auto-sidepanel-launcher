package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChecklistSummaryTest {
    @Test public void allSet() { assertEquals("✔ All set", ChecklistSummary.line(0)); assertFalse(ChecklistSummary.dot(0)); }
    @Test public void counts() { assertEquals("1 permission missing", ChecklistSummary.line(1)); assertEquals("3 permissions missing", ChecklistSummary.line(3)); assertTrue(ChecklistSummary.dot(1)); }
}
