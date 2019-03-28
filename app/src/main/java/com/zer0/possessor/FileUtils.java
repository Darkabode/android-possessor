package com.zer0.possessor;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils
{
    public static final int ORDER_NAME = 0;
    public static final int ORDER_DATE = 1;
    public static final int ORDER_SIZE = 2;

    private List<File> _externalRootPaths;
    private File _internalRootPath;
    private final Context _context;

    public FileUtils(Context context)
    {
        _context = context;
        // set default configuration
        _externalRootPaths = new ArrayList<File>();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

                StorageManager storageManager = (StorageManager)_context.getSystemService(Context.STORAGE_SERVICE);
                final Method method = storageManager.getClass().getMethod("getVolumePaths");
                final String[] strList = (String[]) method.invoke(storageManager);

                for (String path : strList) {
                    final File file = new File(path);
                    if (!FileUtils.doesFileExistInList(_externalRootPaths, file)) {
                        _externalRootPaths.add(file);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (_externalRootPaths.size() == 0) {
            _externalRootPaths.add(new File(Environment.getExternalStorageDirectory().getAbsolutePath()));
        }
        _internalRootPath = new File(Environment.getRootDirectory().getAbsolutePath());
    }

    public List<File> getExternalRootPaths()
    {
        return _externalRootPaths;
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[2048];
        int readed;
        while ((readed = in.read(buffer)) != -1) {
            out.write(buffer, 0, readed);
        }
    }

    private static void dirChecker(String destPath, String dir)
    {
        File f = new File(destPath + "/" + dir);

        if (!f.isDirectory()) {
            f.mkdirs();
        }
    }

    public static boolean extractFiles(String sourcePath, String relPath, String destPath)
    {
        boolean result = false;

        try  {
            FileInputStream fin = new FileInputStream(sourcePath);
            ZipInputStream zin = new ZipInputStream(fin);
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                if (ze.getName().startsWith(relPath)) {
                    if(ze.isDirectory()) {
                        dirChecker(destPath, ze.getName());
                    }
                    else {
                        File outFile = new File(destPath, ze.getName());
                        if (outFile.exists()) {
                            outFile.delete();
                        }
                        FileOutputStream fout = new FileOutputStream(outFile.getAbsolutePath());
                        copyStream(zin, fout);

                        zin.closeEntry();
                        fout.flush();
                        fout.close();
                        fout = null;
                    }
                }
            }
            result = true;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean saveFile(String filePath, byte[] data)
    {
        boolean result = false;
        int remainSize = data.length;
        int offset = 0;
        try {
            OutputStream fos = new BufferedOutputStream(new FileOutputStream(filePath));
            while (remainSize > 0) {
                int neededSize = Math.min(1024, remainSize);
                fos.write(data, offset, neededSize);
                remainSize -= neededSize;
                offset += neededSize;
            }
            fos.flush();
            fos.close();
            fos = null;

            result = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean doesFileExistInList(List<File> fileList, File newFile)
    {
        if (newFile == null || fileList == null || !newFile.exists() || !newFile.isDirectory() || !newFile.canRead() || (newFile.getTotalSpace() <= 0) || newFile.getName().equalsIgnoreCase("tmp") || fileList.contains(newFile)) {
            // File Is Null Or List Is Null
            return true;
        }

        // Make Sure The File Isn't In The List As A Link Of Some Sort
        // More Of An In Depth Look
        for (File file : fileList) {
            if (file.getFreeSpace() == newFile.getFreeSpace() && file.getUsableSpace() == newFile.getUsableSpace()) {
                // Same Free/Usable Space
                // Must Be Same Files
                return true;
            }
        }

        return false;
    }

    public static boolean createDirectory(String path)
    {
        // Check if the directory already exist
        if (isDirectoryExists(path)) {
            throw new RuntimeException(""/*"The direcory already exist. You can't override the existing one. Use createDirectory(String path, boolean override)"*/);
        }

        File directory = new File(path);

        // Create a new directory
        return directory.mkdirs();
    }

    public static boolean createDirectory(String path, boolean override) {
        // If override==false, then don't override
        if (!override) {
            return isDirectoryExists(path) || createDirectory(path);
        }

        // Check if directory exists. If yes, then delete all directory
        if (isDirectoryExists(path)) {
            deleteRecursive(new File(path));
        }

        // Create new directory
        boolean wasCreated = createDirectory(path);
        // If directory is already exist then wasCreated=false
        if (!wasCreated) {
            throw new RuntimeException(""/*"Couldn't create new direcory"*/);
        }

        return true;
    }

    public static boolean deleteRecursive(File fileOrDir)
    {
        // If the directory exists then delete
        if (fileOrDir.exists()) {
            File[] files = fileOrDir.listFiles();
            if (files == null) {
                return true;
            }
            // Run on all sub files and folders and delete them
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteRecursive(f);
                }
                else {
                    f.delete();
                }
            }
        }
        return fileOrDir.delete();
    }

    public static boolean isDirectoryExists(String path)
    {
        File f = new File(path);
        return (f.exists() && f.isDirectory());
    }

    public static boolean createFile(String path, String content)
    {
        return createFile(path, content.getBytes());
    }

    public static boolean createFile(String path, byte[] content)
    {
        try {
            OutputStream stream = new FileOutputStream(new File(path));

            stream.write(content);
            stream.flush();
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(""/*"Failed to create", e*/);
        }
        return true;
    }

    public static boolean createFile(String path, Bitmap bitmap)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return createFile(path, byteArray);
    }

    public static boolean deleteFile(String path)
    {
        File file = new File(path);
        return file.delete();
    }

    public static boolean isFileExist(String path)
    {
        File f = new File(path);
        return (f.exists() && f.isFile());
    }

    public static byte[] readFile(String path)
    {
        final FileInputStream stream;
        try {
            stream = new FileInputStream(new File(path));
            return readFile(stream);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(""/*"Failed to read file to input stream", e*/);
        }
    }

    public static String readTextFile(String path)
    {
        byte[] bytes = readFile(path);
        return new String(bytes);
    }

    public static void appendFile(String path, String content)
    {
        appendFile(path, content.getBytes());
    }

    public static void appendFile(String path, byte[] bytes)
    {
        if (!isFileExist(path)) {
            throw new RuntimeException(""/*"Impossible to append content, because such file doesn't exist"*/);
        }

        try {
            FileOutputStream stream = new FileOutputStream(new File(path), true);
            stream.write(bytes);
            stream.flush();
            stream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(""/*"Failed to append content to file", e*/);
        }
    }

    public static List<File> getNestedFiles(String path)
    {
        File file = new File(path);
        List<File> out = new ArrayList<File>();
        getDirectoryFilesImpl(file, out);
        return out;
    }

    public static List<File> getFiles(String path, final String matchRegex, int orderType)
    {
        File file = new File(path);
        List<File> out = null;
        if (matchRegex != null) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String fileName) {
                    return fileName.matches(matchRegex);
                }
            };
            File[] files = file.listFiles(filter);
            if (files != null) {
                out = Arrays.asList(files);
            }
        }
        else {
            File[] files = file.listFiles();
            if (files != null) {
                out = Arrays.asList(files);
            }
        }
        if (orderType != -1) {
            Collections.sort(out, getFileComparator(orderType));
        }
        return out;
    }

    private static Comparator<File> getFileComparator(int orderType) {
        switch (orderType) {
            case ORDER_NAME:
                return new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return lhs.getName().compareTo(rhs.getName());
                    }
                };
            case ORDER_DATE:
                return new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return (int) (rhs.lastModified() - lhs.lastModified());
                    }
                };
            case ORDER_SIZE:
                return new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        return (int) (lhs.length() - rhs.length());
                    }
                };
            default:
                break;
        }
        return null;
    }

    public static void rename(File file, String newName)
    {
        String name = file.getName();
        String newFullName = file.getAbsolutePath().replaceAll(name, newName);
        File newFile = new File(newFullName);
        file.renameTo(newFile);
    }

    private static void closeQuietly(Closeable closeable)
    {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copy(File file, String path)
    {
        if (!file.isFile()) {
            return;
        }

        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(file);
            outStream = new FileOutputStream(new File(path));
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        finally {
            closeQuietly(inStream);
            closeQuietly(outStream);
        }
    }

    public static void move(File file, String path)
    {
        copy(file, path);
        file.delete();
    }

    private static class ImmutablePair<T, S> implements Serializable {
        private static final long serialVersionUID = 40;

        public final T element1;
        public final S element2;

        public ImmutablePair() {
            element1 = null;
            element2 = null;
        }

        public ImmutablePair(T element1, S element2) {
            this.element1 = element1;
            this.element2 = element2;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ImmutablePair)) {
                return false;
            }

            Object object1 = ((ImmutablePair<?, ?>) object).element1;
            Object object2 = ((ImmutablePair<?, ?>) object).element2;

            return element1.equals(object1) && element2.equals(object2);
        }

        @Override
        public int hashCode()
        {
            return element1.hashCode() << 16 + element2.hashCode();
        }
    }

    protected static byte[] readFile(final FileInputStream stream)
    {
        class Reader extends Thread {
            byte[] array = null;
        }

        Reader reader = new Reader() {
            public void run() {
                LinkedList<ImmutablePair<byte[], Integer>> chunks = new LinkedList<ImmutablePair<byte[], Integer>>();

                // read the file and build chunks
                int size = 0;
                int globalSize = 0;
                do {
                    try {
                        int chunkSize = 8192;
                        // read chunk
                        byte[] buffer = new byte[chunkSize];
                        size = stream.read(buffer, 0, chunkSize);
                        if (size > 0) {
                            globalSize += size;

                            // add chunk to list
                            chunks.add(new ImmutablePair<byte[], Integer>(buffer, size));
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        // very bad
                    }
                } while (size > 0);

                try {
                    stream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    // very bad
                }

                array = new byte[globalSize];

                // append all chunks to one array
                int offset = 0;
                for (ImmutablePair<byte[], Integer> chunk : chunks) {
                    // flush chunk to array
                    System.arraycopy(chunk.element1, 0, array, offset, chunk.element2);
                    offset += chunk.element2;
                }
            }
        };

        reader.start();
        try {
            reader.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(""/*"Failed on reading file from storage while the locking Thread", e*/);
        }

        return reader.array;
    }

    /**
     * Get all files under the directory
     *
     * @param directory
     * @param out
     * @return
     */
    private static void getDirectoryFilesImpl(File directory, List<File> out)
    {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        getDirectoryFilesImpl(file, out);
                    } else {
                        out.add(file);
                    }
                }
            }
        }
    }
}