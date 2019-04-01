package org.icpclive.backend.player.widgets.locator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by icpclive on 4/1/2019.
 */
public class LocatorsData {
    public static List<LocatorCamera> locatorCameras;

    static {
        Properties properties = new Properties();
        try {
            InputStream resource = LocatorsData.class.getResourceAsStream("/mainscreen.properties");
            if (resource == null) {
                throw new AssertionError("/mainscreen.properties not found");
            }
            properties.load(resource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String camerasProperty = properties.getProperty("locator.cameras");
        String locatorInputsProperty = properties.getProperty("locator.inputfiles");
        String locatorCoordinatesProperty = properties.getProperty("locator.coordinates");
        if (camerasProperty == null) {
            throw new AssertionError("locator.cameras expected in properties");
        }
        if (locatorInputsProperty == null) {
            throw new AssertionError("locator.inputfiles expected in properties");
        }
        if (locatorCoordinatesProperty == null)  {
            throw new AssertionError("locator.coordinates expected in properties");
        }
        String[] cameraIPs = camerasProperty.split(",");
        String[] locatorInputs = locatorInputsProperty.split(",");
        String[] locatorCoordinates = locatorCoordinatesProperty.split(",");
        if (cameraIPs.length != locatorInputs.length || cameraIPs.length != locatorCoordinates.length) {
            throw new AssertionError("locator.cameras, locator.inputfiles and locator.coordinates must be of the same length");
        }
        locatorCameras = new ArrayList<>();
        for (int i = 0; i < cameraIPs.length; i++) {
            locatorCameras.add(new LocatorCamera(cameraIPs[i], new File(locatorInputs[i]),
                    new File(locatorCoordinates[i]), i));
        }
    }
}
