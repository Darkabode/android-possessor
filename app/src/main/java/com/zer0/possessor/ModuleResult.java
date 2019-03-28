package com.zer0.possessor;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import android.util.Base64;

public class ModuleResult
{
    private final int status;
    private final int messageType;
    private boolean _keepCallback = false;
    private String strMessage;
    private String encodedMessage;
    private List<ModuleResult> multipartMessages;

    public ModuleResult(Status status)
    {
        this(status, ModuleResult.StatusMessages[status.ordinal()]);
    }

    public ModuleResult(Status status, String message)
    {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_STRING;
        this.strMessage = message;
    }

    public ModuleResult(Status status, JSONArray message)
    {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_JSON;
        encodedMessage = message.toString();
    }

    public ModuleResult(Status status, JSONObject message)
    {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_JSON;
        encodedMessage = message.toString();
    }

    public ModuleResult(Status status, int i)
    {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_NUMBER;
        this.encodedMessage = ""+i;
    }

    public ModuleResult(Status status, byte[] data)
    {
        this(status, data, false);
    }

    public ModuleResult(Status status, byte[] data, boolean binaryString)
    {
        this.status = status.ordinal();
        this.messageType = binaryString ? MESSAGE_TYPE_BINARYSTRING : MESSAGE_TYPE_ARRAYBUFFER;
        this.encodedMessage = Base64.encodeToString(data, Base64.NO_WRAP);
    }
    // The keepCallback and status of multipartMessages are ignored.
    public ModuleResult(Status status, List<ModuleResult> multipartMessages) {
        this.status = status.ordinal();
        this.messageType = MESSAGE_TYPE_MULTIPART;
        this.multipartMessages = multipartMessages;
    }
    
    public void setKeepCallback(boolean b)
    {
        _keepCallback = b;
    }

    public int getStatus()
    {
        return status;
    }

    public int getMessageType()
    {
        return messageType;
    }

    public String getMessage()
    {
        if (encodedMessage == null) {
            encodedMessage = JSONObject.quote(strMessage);
        }
        return encodedMessage;
    }

    public int getMultipartMessagesSize() {
        return multipartMessages.size();
    }

    public ModuleResult getMultipartMessage(int index) {
        return multipartMessages.get(index);
    }

    /**
     * If messageType == MESSAGE_TYPE_STRING, then returns the message string.
     * Otherwise, returns null.
     */
    public String getStrMessage()
    {
        return strMessage;
    }

    public boolean getKeepCallback()
    {
        return _keepCallback;
    }

    public static final int MESSAGE_TYPE_STRING = 1;
    public static final int MESSAGE_TYPE_JSON = 2;
    public static final int MESSAGE_TYPE_NUMBER = 3;
    public static final int MESSAGE_TYPE_BOOLEAN = 4;
    public static final int MESSAGE_TYPE_NULL = 5;
    public static final int MESSAGE_TYPE_ARRAYBUFFER = 6;
    // Use BINARYSTRING when your string may contain null characters.
    // This is required to work around a bug in the platform :(.
    public static final int MESSAGE_TYPE_BINARYSTRING = 7;
    public static final int MESSAGE_TYPE_MULTIPART = 8;

    public static String[] StatusMessages = new String[] {
        "No result",
        "OK",
        "Class not found",
        "Illegal access",
        "Instantiation error",
        "Malformed url",
        "IO error",
        "Invalid action",
        "JSON error",
        "Error"
    };

    public enum Status {
        NO_RESULT,
        OK,
        CLASS_NOT_FOUND_EXCEPTION,
        ILLEGAL_ACCESS_EXCEPTION,
        INSTANTIATION_EXCEPTION,
        MALFORMED_URL_EXCEPTION,
        IO_EXCEPTION,
        INVALID_ACTION,
        JSON_EXCEPTION,
        ERROR
    }
}
