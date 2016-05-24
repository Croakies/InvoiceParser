/*
 * Decompiled with CFR 0_114.
 */
package net.shadowmage.monitor;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public interface MonitorExecutor {
    public void OnFileRead(File var1, File var2, File var3, Logger var4);

    public List<File> getParsedFiles();
}

