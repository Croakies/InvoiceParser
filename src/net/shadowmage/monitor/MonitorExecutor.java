package net.shadowmage.monitor;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public interface MonitorExecutor {

   void OnFileRead(File var1, File var2, File var3, Logger var4);

   List getParsedFiles();
}
