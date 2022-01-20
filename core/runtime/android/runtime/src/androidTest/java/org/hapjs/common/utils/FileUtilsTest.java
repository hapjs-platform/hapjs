/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.utils;

import static org.junit.Assert.assertEquals;


import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FileUtilsTest {

    @Test
    public void rmRF() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        File dir = appContext.getDir("test", Context.MODE_PRIVATE);
        assertEquals(true, dir.exists());
        File a = new File(dir, "a");
        writeFile(a);
        assertEquals(true, a.exists());
        File subDir = new File(dir, "sub");
        subDir.mkdirs();
        assertEquals(true, subDir.exists());
        File b = new File(subDir, "b");
        writeFile(b);
        assertEquals(true, b.exists());
        File emptySubDir = new File(subDir, "empty");
        emptySubDir.mkdirs();
        assertEquals(true, emptySubDir.exists());
        FileUtils.rmRF(dir);
        assertEquals(false, dir.exists());
    }

    private void writeFile(File a) throws IOException {
        OutputStream os = new FileOutputStream(a);
        os.write(1);
        os.close();
    }
}
