/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

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
     * Private constructor.
     */
    private Utils() {}
}
