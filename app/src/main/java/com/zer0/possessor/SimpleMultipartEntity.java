package com.zer0.possessor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SimpleMultipartEntity
{
    private String _boundary = null;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    boolean isSetLast = false;
    boolean isSetFirst = false;

    public SimpleMultipartEntity()
    {
        _boundary = StringUtils.randomString(30, false);
    }

    public void writeFirstBoundaryIfNeeds()
    {
        if (!isSetFirst) {
            try {
                out.write(("--" + _boundary + "\r\n").getBytes());
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }

        isSetFirst = true;
    }

    public void writeLastBoundaryIfNeeds()
    {
        if (isSetLast) {
            return;
        }

        try {
            out.write(("\r\n--" + _boundary + "--\r\n").getBytes());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }

        isSetLast = true;
    }

    public void addPart(final String key, final String value)
    {
        writeFirstBoundaryIfNeeds();
        try {
            out.write(("Content-Disposition: form-data; name=\"" +key+"\"\r\n\r\n").getBytes());
            out.write(value.getBytes());
            out.write(("\r\n--" + _boundary + "\r\n").getBytes());
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public void addPart(final String paramName, final String fileName, final InputStream fin, String type, final boolean isLast)
    {
        writeFirstBoundaryIfNeeds();
        try {
            type = "Content-Type: "+type+"\r\n";
            out.write(("Content-Disposition: form-data; name=\""+ paramName +"\"; filename=\"" + fileName + "\"\r\n").getBytes());
            out.write(type.getBytes());
            out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());

            final byte[] tmp = new byte[4096];
            int l;
            while ((l = fin.read(tmp)) != -1) {
                out.write(tmp, 0, l);
            }
            if(!isLast)
                out.write(("\r\n--" + _boundary + "\r\n").getBytes());
            out.flush();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                fin.close();
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    public long getContentLength()
    {
        writeLastBoundaryIfNeeds();
        return out.toByteArray().length;
    }
 
    public String getContentType()
    {
        return "multipart/form-data; boundary=" + _boundary;
    }
 
    public void writeTo(final OutputStream outstream) throws IOException
    {
        outstream.write(out.toByteArray());
    }
}