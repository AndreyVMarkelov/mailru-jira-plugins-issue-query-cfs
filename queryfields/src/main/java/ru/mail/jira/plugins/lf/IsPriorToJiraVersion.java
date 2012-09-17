/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Jira version condition.
 * 
 * @author Andrey Markelov
 */
public class IsPriorToJiraVersion
    implements Condition
{
    private int maxMajorVersion;

    private int maxMinorVersion;

    private int majorVersion;

    private int minorVersion;
 
    public IsPriorToJiraVersion(
        ApplicationProperties applicationProperties)
    {
        String versionString = applicationProperties.getVersion();
        String versionRegex = "^(\\d+)\\.(\\d+)";
        Pattern versionPattern = Pattern.compile(versionRegex);
        Matcher versionMatcher = versionPattern.matcher(versionString);
        versionMatcher.find();
        majorVersion = Integer.decode(versionMatcher.group(1));
        minorVersion = Integer.decode(versionMatcher.group(2));
    }
 
    public void init(
        final Map<String, String> paramMap)
    throws PluginParseException
    {
        maxMajorVersion = Integer.decode(paramMap.get("majorVersion"));
        maxMinorVersion = Integer.decode(paramMap.get("minorVersion"));
    }
 
    public boolean shouldDisplay(
        final Map<String, Object> context)
    {
        return (majorVersion < maxMajorVersion) || (majorVersion == maxMajorVersion) && (minorVersion < maxMinorVersion);
    }
}
