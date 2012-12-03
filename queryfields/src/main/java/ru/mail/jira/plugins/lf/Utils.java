/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/**
 * PlugIn utility methods.
 * 
 * @author Andrey Markelov
 */
public class Utils
{
    /**
     * Get base URL from HTTP request.
     */
    public static String getBaseUrl(HttpServletRequest req)
    {
        return (req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getContextPath());
    }

    /**
     * Check that string is not empty and not null.
     */
    public static boolean isValidStr(String str)
    {
        return (str != null && str.length() > 0);
    }

    /**
     * Convert string list to string.
     */
    public static String listToString(List<String> list)
    {
        StringBuilder sb = new StringBuilder();

        if (list != null)
        {
            for (String item : list)
            {
                sb.append(item).append("&");
            }
        }

        return sb.toString();
    }

    /**
     * Convert string to string list.
     */
    public static List<String> stringToList(String str)
    {
        List<String> list = new ArrayList<String>();

        if (str != null)
        {
            StringTokenizer st = new StringTokenizer(str, "&");
            while (st.hasMoreTokens())
            {
                list.add(st.nextToken());
            }
        }

        return list;
    }

    /**
     * Private constructor.
     */
    private Utils() {}
}
