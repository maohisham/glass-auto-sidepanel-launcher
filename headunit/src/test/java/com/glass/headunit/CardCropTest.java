package com.glass.headunit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CardCropTest {

    @Test
    public void aTallerPhotoIsCroppedFromTheMiddleOfItsHeight() {
        // 4000 x 3000 (4:3) into a 5:3 card: keep the full width, 2400 of the height, from 300 down
        assertArrayEquals(new int[]{0, 300, 4000, 2400}, CardCrop.rect(4000, 3000, 5f / 3f));
    }

    @Test
    public void aSquarePhotoKeepsTheFullWidth() {
        assertArrayEquals(new int[]{0, 600, 3000, 1800}, CardCrop.rect(3000, 3000, 5f / 3f));
    }

    @Test
    public void aWiderPhotoIsCroppedFromTheMiddleOfItsWidth() {
        // 6000 x 2000 is 3:1, wider than 5:3: keep the full height and 3333 of the width
        int[] r = CardCrop.rect(6000, 2000, 5f / 3f);
        assertEquals(1333, r[0]);
        assertEquals(0, r[1]);
        assertEquals(3333, r[2]);
        assertEquals(2000, r[3]);
    }

    @Test
    public void aPhotoThatAlreadyFitsIsKeptWhole() {
        assertArrayEquals(new int[]{0, 0, 1500, 900}, CardCrop.rect(1500, 900, 5f / 3f));
    }

    @Test
    public void theSampleSizeShrinksABigPhotoButStaysAtLeastTheTargetWidth() {
        assertEquals(2, CardCrop.sampleSize(4000, 1200));    // 4000/2 = 2000 >= 1200, 4000/4 = 1000 is too small
        assertEquals(1, CardCrop.sampleSize(1500, 1200));
        assertEquals(4, CardCrop.sampleSize(5000, 1200));    // 5000/4 = 1250
        assertEquals(1, CardCrop.sampleSize(800, 1200));     // never enlarge
    }

    @Test
    public void theCardShapeIsFiveToThree() {
        assertEquals(5f / 3f, CardCrop.ASPECT, 0.0001f);
    }

    @Test
    public void thePictureFileForACarIsNamedByItsId() {
        assertEquals("car_myvi.jpg", CardCrop.fileName("myvi"));
        assertEquals("car_tiggo.jpg", CardCrop.fileName("tiggo"));
    }
}
