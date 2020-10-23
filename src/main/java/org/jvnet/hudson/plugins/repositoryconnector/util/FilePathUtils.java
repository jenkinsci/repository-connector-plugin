package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.io.Files;

import hudson.FilePath;

public class FilePathUtils {

    public static File copyToLocal(FilePath filePath) throws IOException, InterruptedException {
        if (!filePath.exists()) {
            throw new IOException("file [" + filePath.getRemote() + "] does not exist!");
        }

        File local = createTempFile(filePath);
        filePath.copyTo(createOutputStream(local));

        return local;
    }

    static OutputStream createOutputStream(File file) throws IOException {
        return Files.newOutputStreamSupplier(file).getOutput();
    }

    static File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix + "-", "." + suffix);
        // always delete on exit but try to delete sooner...
        file.deleteOnExit();

        return file;
    }

    private static File createTempFile(FilePath filePath) throws IOException {
        String name = filePath.getName();
        int index = name.indexOf('.');

        String prefix = name.substring(0, index - 1);
        String suffix = name.substring(index + 1);

        return createTempFile(prefix, suffix);
    }
}
