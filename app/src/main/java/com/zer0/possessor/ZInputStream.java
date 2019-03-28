package com.zer0.possessor;

import java.io.IOException;
import java.io.InputStream;

public class ZInputStream
{
	private InputStream _is;
	
	public void init(InputStream is)
	{
		this._is = is;
	}
	
	public int read(byte[] buffer) throws IOException
	{
		return _is.read(buffer);
	}
	
	public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException
	{
		return _is.read(buffer, byteOffset, byteCount);
	}
	
	public long readLong() throws IOException
	{
		long value = 0;
		byte[] data = new byte[8];
		if (_is.read(data) == 8) {
            for (int i = 0; i < data.length; i++) {
                value |= ((long) data[i] & 0xffL) << (8 * i);
            }
        }
		return value;
	}
	
	public int readInt() throws IOException
	{
		int value = 0;
		byte[] data = new byte[4];
		if (_is.read(data) == 4) {
            for (int i = 0; i < data.length; i++) {
                value |= ((int) data[i] & 0x000000ff) << (8 * i);
            }
        }
		return value;
	}
	/*
	public byte[] read(int sz) throws IOException
	{
		byte[] data = new byte[sz];
		this._is.read(data);
		return data;
	}
	*/
	public String readBinaryString() throws IOException
	{
		int sz = readInt();
		byte[] data = new byte[sz];
		
		if (_is.read(data) == sz) {
            return new String(data);
        }
        else {
            return "";
        }
	}
	
	public void close() throws IOException
	{
		_is.close();
	}
}
