package com.glass.headunit;

import static org.junit.Assert.*;

import org.junit.Test;

public class PhotoGridTest {
    @Test public void narrowPanelTwoColumns() { assertEquals(2, PhotoGrid.columns(300)); }
    @Test public void fullScreenManyColumns() { assertEquals(8, PhotoGrid.columns(1250)); }
    @Test public void neverFewerThanTwo() { assertEquals(2, PhotoGrid.columns(0)); }
    @Test public void emptyText() { assertEquals("No pictures on this unit yet. Download one, or use Other apps…", PhotoGrid.EMPTY); }
}
