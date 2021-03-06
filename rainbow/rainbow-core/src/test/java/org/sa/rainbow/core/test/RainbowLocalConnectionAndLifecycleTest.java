package org.sa.rainbow.core.test;

import java.io.File;
import java.io.IOException;

public class RainbowLocalConnectionAndLifecycleTest extends RainbowConnectionAndLifecycleTest {

    @Override
    protected void configureTestProperties () throws IOException {
        File basePath = new File (System.getProperty ("user.dir"));
        File testMasterDir = new File (basePath, "src/test/resources/RainbowTest/local");
        System.setProperty ("user.dir", testMasterDir.getCanonicalPath ());
    }



}
