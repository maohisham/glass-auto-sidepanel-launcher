package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChangedTest {
    @Test public void sameTextIsNotAChange() { assertFalse(Changed.text("Asar in 1h 06m", "Asar in 1h 06m")); }
    @Test public void newTextIsAChange() { assertTrue(Changed.text("Asar in 1h 06m", "Asar in 1h 05m")); }
    @Test public void spansDoNotHideTheSameWords() { assertFalse(Changed.text(new StringBuilder("3:05"), "3:05")); }
    @Test public void emptyAndNull() { assertFalse(Changed.text(null, "")); assertFalse(Changed.text("", null)); assertTrue(Changed.text(null, "x")); }
    @Test public void noticesByKey() { assertFalse(Changed.key("a|b", "a|b")); assertTrue(Changed.key("a|b", "a|c")); assertTrue(Changed.key(null, "a")); }
}
