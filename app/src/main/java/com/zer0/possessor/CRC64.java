package com.zer0.possessor;

public class CRC64
{
	private static final long POLY = 0xc96c5795d7870f42L;
    private static final long[][] LOOKUPTABLE;
    
    static {
    	long crc;
        LOOKUPTABLE = new long[8][256];
        for (int n = 0; n < 256; ++n) {
        	crc = n;
        	for (int k = 0; k < 8; ++k) {
        		if ((crc & 1) == 1) {
        			crc = POLY ^ (crc >>> 1);
        		}
        		else {
        			crc = crc >>> 1;
        		}
            }
        	LOOKUPTABLE[0][n] = crc;
        }
        
        for (int n = 0; n < 256; ++n) {
            crc = LOOKUPTABLE[0][n];
            for (int k = 1; k < 8; ++k) {
                crc = LOOKUPTABLE[0][(int)(crc & 0xff)] ^ (crc >>> 8);
                LOOKUPTABLE[k][n] = crc;
            }
        }
    }
    
    private static long dataToLong(final byte[] data)
    {
    	long value = 0;
    	for (int i = 0; i < data.length; i++) {
    	   value |= ((long)data[i] & 0xffL) << (8 * i);
    	}
    	return value;
    }
    
    public static long checksum(final byte[] data)
    {
    	long crc = 0;
    	int len = data.length;
    	int pos = 0;
    	
        crc = ~crc;
        //while (len != 0 && (data[pos] & 7) != 0) {
//        	crc = LOOKUPTABLE[0][(int)((crc ^ (data[pos++])) & 0xff)] ^ (crc >>> 8);
  //      	--len;
    //    }

        while (len >= 8) {
            crc ^= dataToLong(new byte[] {data[pos], data[pos+1], data[pos+2], data[pos+3], data[pos+4], data[pos+5], data[pos+6], data[pos+7]});
            crc = LOOKUPTABLE[7][(int)(crc & 0x000000ff)] ^
        		LOOKUPTABLE[6][(int)((crc >>> 8) & 0x000000ff)] ^
        		LOOKUPTABLE[5][(int)((crc >>> 16) & 0x000000ff)] ^
        		LOOKUPTABLE[4][(int)((crc >>> 24) & 0x000000ff)] ^
        		LOOKUPTABLE[3][(int)((crc >>> 32) & 0x000000ff)] ^
        		LOOKUPTABLE[2][(int)((crc >>> 40) & 0x000000ff)] ^
        		LOOKUPTABLE[1][(int)((crc >>> 48) & 0x000000ff)] ^
        		LOOKUPTABLE[0][(int)((crc >>> 56) & 0x000000ff)];
            pos += 8;
            len -= 8;
        }
        while (len != 0) {
            crc = LOOKUPTABLE[0][(int)((crc ^ data[pos++]) & 0x000000ff)] ^ (crc >>> 8);
            --len;
        }
        return ~crc;
    }
}
