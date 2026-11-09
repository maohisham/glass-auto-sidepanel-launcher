package com.glass.headunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

/**
 * The Home entry must be a thin starter, so that the panel itself stays an ordinary app: a task that is started
 * through a Home entry is a home-type task, which is left out of Recents and cannot be one half of a split.
 */
public class HomeManifestTest {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";
    private static Element root;

    @BeforeClass
    public static void parse() throws Exception {
        javax.xml.parsers.DocumentBuilderFactory f = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        root = f.newDocumentBuilder().parse(new File("src/main/AndroidManifest.xml")).getDocumentElement();
    }

    private static Element named(String tag, String name) {
        NodeList l = root.getElementsByTagName(tag);
        for (int i = 0; i < l.getLength(); i++) {
            Element e = (Element) l.item(i);
            if (name.equals(e.getAttributeNS(ANDROID, "name"))) return e;
        }
        return null;
    }

    private static boolean hasHomeCategory(Element e) {
        NodeList cats = e.getElementsByTagName("category");
        for (int i = 0; i < cats.getLength(); i++) {
            if ("android.intent.category.HOME".equals(((Element) cats.item(i)).getAttributeNS(ANDROID, "name"))) return true;
        }
        return false;
    }

    @Test
    public void theHomeEntryExistsAndCarriesTheHomeIntent() {
        Element alias = named("activity-alias", ".HomeAlias");
        assertNotNull(alias);
        assertTrue(hasHomeCategory(alias));
    }

    @Test
    public void theHomeEntryStartsTheThinStarterNotThePanel() {
        assertEquals(".HomeActivity", named("activity-alias", ".HomeAlias").getAttributeNS(ANDROID, "targetActivity"));
    }

    @Test
    public void theHomeEntryStaysOffUntilTheUserTurnsItOn() {
        assertEquals("false", named("activity-alias", ".HomeAlias").getAttributeNS(ANDROID, "enabled"));
    }

    @Test
    public void thePanelItselfNeverHandlesHome() {
        assertFalse(hasHomeCategory(named("activity", ".PanelActivity")));
    }

    @Test
    public void theStarterLeavesNothingInRecentsAndStaysOutOfThePanelsTask() {
        Element starter = named("activity", ".HomeActivity");
        assertNotNull(starter);
        assertEquals("true", starter.getAttributeNS(ANDROID, "excludeFromRecents"));
        assertTrue(starter.hasAttributeNS(ANDROID, "taskAffinity"));
        assertEquals("", starter.getAttributeNS(ANDROID, "taskAffinity"));
    }

    @Test
    public void theStarterIsReachedOnlyThroughTheHomeEntry() {
        Element starter = named("activity", ".HomeActivity");
        assertEquals(0, starter.getElementsByTagName("intent-filter").getLength());
    }
}
