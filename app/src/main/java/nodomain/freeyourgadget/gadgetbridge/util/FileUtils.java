/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti, Felix Konstantin Maurer, JohnnySun, José Rebelo,
    Petr Vaněk, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import xyz.tenseventyseven.fresh.Application;
import xyz.tenseventyseven.fresh.AppEnvironment;

public class FileUtils {
    // Don't use slf4j here -- would be a bootstrapping problem
    private static final String TAG = "FileUtils";

    private static final List<String> KNOWN_PACKAGES = Arrays.asList(
            "nodomain.freeyourgadget.gadgetbridge",
            "nodomain.freeyourgadget.gadgetbridge.nightly",
            "nodomain.freeyourgadget.gadgetbridge.nightly_nopebble",
            "com.espruino.gadgetbridge.banglejs",
            "com.espruino.gadgetbridge.banglejs.nightly"
    );

    /**
     * Copies the the given sourceFile to destFile, overwriting it, in case it exists.
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("Does not exist: " + sourceFile.getAbsolutePath());
        }
        try (FileInputStream in = new FileInputStream(sourceFile); FileOutputStream out = new FileOutputStream(destFile)) {
            copyFile(in, out);
        }
    }

    private static void copyFile(FileInputStream sourceStream, FileOutputStream destStream) throws IOException {
        try (FileChannel fromChannel = sourceStream.getChannel(); FileChannel toChannel = destStream.getChannel()) {
            fromChannel.transferTo(0, fromChannel.size(), toChannel);
        }
    }

    /**
     * Copies the contents of the given input stream to the destination file.
     * @param inputStream the contents to write. Note: the caller has to close the input stream!
     * @param destFile the file to write to
     * @throws IOException
     */
    public static void copyStreamToFile(InputStream inputStream, File destFile) throws IOException {
        try (FileOutputStream fout = new FileOutputStream(destFile)) {
            byte[] buf = new byte[4096];
            while (inputStream.available() > 0) {
                int bytes = inputStream.read(buf);
                fout.write(buf, 0, bytes);
            }
        }
    }

    /**
     * Copies the contents of the given string to the destination file.
     * @param string the contents to write.
     * @param dst the file to write to
     * @throws IOException
     */
    public static void copyStringToFile(String string, File dst, String mode) throws IOException {
        boolean append = true;
        if (!Objects.equals(mode, "append")) append = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dst, append))) {
            writer.write(string);
        }
    }

    /**
     * Copies the contents of the given file to the destination output stream.
     * @param src the file from which to read.
     * @param dst the output stream that is written to. Note: the caller has to close the output stream!
     * @throws IOException
     */
    public static void copyFileToStream(File src, OutputStream dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            byte[] buf = new byte[4096];
            while(in.available() > 0) {
                int bytes = in.read(buf);
                dst.write(buf, 0, bytes);
            }
        }
    }

    public static void copyURItoFile(Context ctx, Uri uri, File destFile) throws IOException {
        if (uri.getPath().equals(destFile.getPath())) {
            return;
        }

        ContentResolver cr = ctx.getContentResolver();
        InputStream in = cr.openInputStream(uri);
        if (in == null) {
            throw new IOException("unable to open input stream: " + uri);
        }
        try (InputStream fin = new BufferedInputStream(in)) {
            copyStreamToFile(fin, destFile);
        }
    }

    /**
     * Copies the content of a file to an uri,
     * which for example was retrieved using the storage access framework.
     * @param context the application context.
     * @param src the file from which the content should be copied.
     * @param dst the destination uri.
     * @throws IOException
     */
    public static void copyFileToURI(Context context, File src, Uri dst) throws IOException {
        OutputStream out = context.getContentResolver().openOutputStream(dst);
        if (out == null) {
            throw new IOException("Unable to open output stream for " + dst.toString());
        }
        try (OutputStream bufOut = new BufferedOutputStream(out)) {
            copyFileToStream(src, bufOut);
        }
    }

    /**
     * Returns the textual contents of the given file. The contents is expected to be
     * in UTF-8 encoding.
     * @param file the file to read
     * @return the file contents as a newline-delimited string
     * @throws IOException
     * @see #getStringFromFile(File, String)
     */
    public static String getStringFromFile(File file) throws IOException {
        return getStringFromFile(file, StandardCharsets.UTF_8.name());
    }

    /**
     * Returns the textual contents of the given file. The contents will be interpreted using the
     * given encoding.
     * @param file the file to read
     * @return the file contents as a newline-delimited string
     * @throws IOException
     * @see #getStringFromFile(File)
     */
    public static String getStringFromFile(File file, String encoding) throws IOException {
        FileInputStream fin = new FileInputStream(file);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fin, encoding))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Returns the existing external storage dir. The directory is guaranteed to
     * exist and to be writable.
     *
     * @throws IOException when the directory is not available
     */
    public static File getExternalFilesDir() throws IOException {
        List<File> dirs = getWritableExternalFilesDirs();
        for (File dir : dirs) {
            if (canWriteTo(dir)) {
                return dir;
            }
        }
        throw new IOException("no writable external directory found");
    }

    /**
     * Returns a File object representing the "child" argument, but relative
     * to the Android "external files directory" (e.g. /sdcard).
     * It doesn't matter whether child shall represent a file or a directory.
     * The parent directory will automatically be created, if necessary.
     * @param child the path to become relative to the external files directory
     * @throws IOException
     * @see #getExternalFilesDir()
     */
    public static File getExternalFile(String child) throws IOException {
        File file = new File(getExternalFilesDir(), child);
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create directory " + file.getParent());
        }
        return file;
    }


    private static boolean canWriteTo(File dir) {
        File file = new File(dir, "gbtest");
        try {
            FileOutputStream test = new FileOutputStream(file);
            try {
                test.close();
            } catch (IOException e) {
                // ignore
            }
            file.delete();
            return true;
        } catch (FileNotFoundException e) {
            GB.log("Cannot write to directory: " + dir.getAbsolutePath(), GB.INFO, e);
            return false;
        }
    }

    /**
     * Returns a list of directories to write to. The list is sorted by priority,
     * i.e. the first directory should be preferred, the last one is the least
     * preferred one.
     * <p/>
     * Note that the directories may not exist, so it is not guaranteed that you
     * can actually write to them. But when created, they *should* be writable.
     *
     * @return the list of writable directories
     * @throws IOException
     */
    @NonNull
    private static List<File> getWritableExternalFilesDirs() throws IOException {
        Context context = Application.getContext();
        File[] dirs;
        try {
            dirs = context.getExternalFilesDirs(null);
        } catch (NullPointerException | UnsupportedOperationException ex) {
            // workaround for robolectric 3.1.2 not implementing getExternalFilesDirs()
            // https://github.com/robolectric/robolectric/issues/2531
            File dir = context.getExternalFilesDir(null);
            if (dir != null) {
                dirs = new File[] { dir };
            } else {
                throw ex;
            }
        }
        if (dirs == null) {
            throw new IOException("Unable to access external files dirs: null");
        }
        List<File> result = new ArrayList<>(dirs.length);
        if (dirs.length == 0) {
            throw new IOException("Unable to access external files dirs: 0");
        }
        for (int i = 0; i < dirs.length; i++) {
            File dir = dirs[i];
            if (dir == null) {
                continue;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                GB.log("Unable to create directories: " + dir.getAbsolutePath(), GB.INFO, null);
                continue;
            }

            if (!AppEnvironment.env().isLocalTest()) { // don't do this with robolectric
                final String storageState = Environment.getExternalStorageState(dir);
                if (!Environment.MEDIA_MOUNTED.equals(storageState)) {
                    GB.log("ignoring '" +  storageState + "' external storage dir: " + dir, GB.INFO, null);
                    continue;
                }
            }
            result.add(dir); // add last
        }
        return result;
    }

    /**
     * Reads the contents of the given InputStream into a byte array, but does not
     * read more than maxLen bytes. If the stream provides more than maxLen bytes,
     * an IOException is thrown.
     *
     * @param in     the stream to read from
     * @param maxLen the maximum number of bytes to read/return
     * @return the bytes read from the InputStream
     * @throws IOException when reading failed or when maxLen was exceeded
     */
    public static byte[] readAll(InputStream in, long maxLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(8192, in.available()));
        byte[] buf = new byte[8192];
        int read;
        long totalRead = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
            totalRead += read;
            if (totalRead > maxLen) {
                throw new IOException("Too much data to read into memory. Got already " + totalRead);
            }
        }
        return out.toByteArray();
    }

    public static boolean deleteRecursively(File dir) {
        if (!dir.exists()) {
            return true;
        }
        if (dir.isFile()) {
            return dir.delete();
        }
        for (File sub : dir.listFiles()) {
            if (!deleteRecursively(sub)) {
                return false;
            }
        }
        return dir.delete();
    }

    public static File createTempDir(String prefix) throws IOException {
        File parent = new File(System.getProperty("java.io.tmpdir", "/tmp"));
        for (int i = 1; i < 100; i++) {
            String name = prefix + (int) (Math.random() * 100000);
            File dir = new File(parent, name);
            if (dir.mkdirs()) {
                return dir;
            }
        }
        throw new IOException("Cannot create temporary directory in " + parent);
    }

    /**
     * Replaces some wellknown invalid characters in the given filename
     * to underscrores.
     * @param name the file name to make valid
     * @return the valid file name
     */
    public static String makeValidFileName(String name) {
        return name.replaceAll("[\0/:\\r\\n\\\\]", "_");
    }

    /**
     *Returns extension of a file
     * @param file string filename
     */
    public static String getExtension(String file) {
        int i = file.lastIndexOf('.');
        String extension = "";
        if (i > 0) {
            extension = file.substring(i + 1);
        }
        return extension;
    }

    /**
     * Returns a Uri referencing a temporary file with the contents of the given asset
     * @param assetPath relative path to the assets file
     * @param context current context for getting AssetManager
     * @return Uri that points to the created temporary file
     * @throws IOException thrown when a file could not be created or opened
     */
    public static Uri getUriForAsset(String assetPath, Context context) throws IOException {
        File tempFile = File.createTempFile("tmpfile" + System.currentTimeMillis(), null);
        tempFile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tempFile);
        InputStream asset = context.getAssets().open(assetPath);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = asset.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
        return Uri.fromFile(tempFile);
    }

    /**
     * When migrating the database between Gadgetbridge versions or phones, we may end up with the
     * wrong path persisted in the database. Attempt to find the file in the current external data.
     *
     * @return the fixed file path, if it exists, null otherwise
     */
    @Nullable
    public static File tryFixPath(final File file) {
        if (file == null || (file.isFile() && file.canRead())) {
            return file;
        }

        File externalFilesDir;
        try {
            externalFilesDir = getExternalFilesDir();
        } catch (final IOException e) {
            return null;
        }

        final String absolutePath = file.getAbsolutePath();
        for (final String knownPackage : KNOWN_PACKAGES) {
            final int i = absolutePath.indexOf(knownPackage);
            if (i < 0) {
                continue;
            }

            // We found the gadgetbridge package in the path!
            String relativePath = absolutePath.substring(i + knownPackage.length() + 1);
            if (relativePath.startsWith("files/")) {
                relativePath = relativePath.substring(6);
            }
            final File fixedFile = new File(externalFilesDir, relativePath);
            if (fixedFile.exists()) {
                return fixedFile;
            }
        }

        return null;
    }
}