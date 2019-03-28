package com.zer0.possessor;

public class Arc4
{
    public static byte[] encrypt(final byte[] data, final byte[] key)
    {
    	final byte[] S = new byte[256];
	    final byte[] T = new byte[256];
    	
	    final int keylen = key.length;
        for (int i = 0; i < 256; i++) {
            S[i] = (byte) i;
            T[i] = key[i % keylen];
        }
        int i, j = 0;
        for (i = 0; i < 256; i++) {
            j = (j + S[i] + T[i]) & 0xFF;
            byte temp = S[i];
            S[i] = S[j];
            S[j] = temp;
        }
        
        final byte[] ciphertext = new byte[data.length];
        i = 0;
        j = 0;
        int k, t;
        for (int counter = 0; counter < data.length; counter++) {
            i = (i + 1) & 0xFF;
            j = (j + S[i]) & 0xFF;
            byte temp = S[i];
            S[i] = S[j];
            S[j] = temp;
            t = (S[i] + S[j]) & 0xFF;
            k = S[t];
            ciphertext[counter] = (byte)(data[counter] ^ k);
        }
        return ciphertext;
    }
}
