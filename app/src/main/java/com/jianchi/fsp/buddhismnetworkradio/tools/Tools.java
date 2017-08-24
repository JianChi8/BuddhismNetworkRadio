package com.jianchi.fsp.buddhismnetworkradio.tools;

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by fsp on 17-8-17.
 */

public class Tools {
    /// <summary>
    ///解析html成 普通文本
    /// </summary>
    /// <param name="str">string</param>
    /// <returns>string</returns>
    public static String Decode(String str)
    {
        str = str.replace("<br>","\n");
        str = str.replace("&gt;",">");
        str = str.replace("&lt;","<");
        str = str.replace("&nbsp;"," ");
        str = str.replace("&quot;","\"");
        str = str.replace("&#039;","'");
        str = str.replace("&amp;","&");
        return str;
    }


    private String readRawFile(int rawId, Context context)
    {
        String tag = "readRawFile";
        String content=null;
        Resources resources=context.getResources();
        InputStream is=null;
        try{
            is=resources.openRawResource(rawId);
            byte buffer[]=new byte[is.available()];
            is.read(buffer);
            content=new String(buffer);
            MyLog.i(tag, "read:"+content);
        }
        catch(IOException e)
        {
            MyLog.e(tag, e.getMessage());
        }
        finally
        {
            if(is!=null)
            {
                try{
                    is.close();
                }catch(IOException e)
                {
                    MyLog.e(tag, e.getMessage());
                }
            }
        }
        return content;
    }
}
