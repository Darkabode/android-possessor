package com.zer0.possessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZOutputStream extends ByteArrayOutputStream
{
	public ZOutputStream()
	{
		super(0);
	}
	
	public void writeData(byte[] buffer) throws IOException
	{
		this.write(buffer);
	}
	
	public void writeData(byte[] buffer, int offset, int len)
	{
		this.write(buffer, offset, len);
	}
	
	public void writeLong(long value)
	{
		byte[] buff = new byte[8];
		buff[0] = (byte)value;
		buff[1] = (byte)(value >>> 8);
		buff[2] = (byte)(value >>> 16);
		buff[3] = (byte)(value >>> 24);
		buff[4] = (byte)(value >>> 32);
		buff[5] = (byte)(value >>> 40);
		buff[6] = (byte)(value >>> 48);
		buff[7] = (byte)(value >>> 56);
		write(buff, 0, 8);
	}
	
	public void writeInt(int value)
	{
		byte[] buff = new byte[4];
		buff[0] = (byte)value;
		buff[1] = (byte)(value >>> 8);
		buff[2] = (byte)(value >>> 16);
		buff[3] = (byte)(value >>> 24);
		write(buff, 0, 4);
	}
	
	public void writeBinaryString(String str)
	{
        byte[] data = str.getBytes();
		writeInt(data.length);
		if (data.length > 0) {
			write(data, 0, data.length);
		}
	}
	
	public void writeTwoBinaryStrings(String strKey, String strValue)
	{
		writeBinaryString(strKey);
		writeBinaryString(strValue);
	}

    public void writeWholeStream(ZOutputStream s) throws IOException
    {
        write(s.toByteArray());
    }
	
	public byte[] toByteArray()
	{
		return super.toByteArray();
	}
}
