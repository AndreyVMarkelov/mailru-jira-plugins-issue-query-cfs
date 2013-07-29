/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf.struct;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Html JSON representation.
 *
 * @author Andrey Markelov
 */
@XmlRootElement
public class HtmlEntity
{
    @XmlElement
    private String html;

    public HtmlEntity(String html)
    {
        this.html = html;
    }

    public String getHtml()
    {
        return html;
    }

    public void setHtml(String html)
    {
        this.html = html;
    }

    @Override
    public String toString()
    {
        return ("HtmlEntity[html=" + html + "]");
    }
}
