/*
 * Created by Andrey Markelov 29-08-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.LinkedList;
import java.util.List;

/**
 * This structure keeps information for rendering all plugIn custom fields for administration page.
 * 
 * @author Andrey Markelov
 */
public class CfData
{
    /**
     * Linked fields.
     */
    private final List<QueryFieldStruct> linkedFields;

    /**
     * Linker fields.
     */
    private final List<QueryFieldStruct> linkerFields;

    /**
     * Multi linker fields.
     */
    private final List<QueryFieldStruct> multiFields;

    /**
     * Constructor.
     */
    public CfData()
    {
        this.linkerFields = new LinkedList<QueryFieldStruct>();
        this.multiFields = new LinkedList<QueryFieldStruct>();
        this.linkedFields = new LinkedList<QueryFieldStruct>();
    }

    public void addLinkedField(QueryFieldStruct qfs)
    {
        linkedFields.add(qfs);
    }

    public void addLinkerField(QueryFieldStruct qfs)
    {
        linkerFields.add(qfs);
    }

    public void addMultiFields(QueryFieldStruct qfs)
    {
        multiFields.add(qfs);
    }

    public List<QueryFieldStruct> getLinkedFields()
    {
        return linkedFields;
    }

    public List<QueryFieldStruct> getLinkerFields()
    {
        return linkerFields;
    }

    public List<QueryFieldStruct> getMultiFields()
    {
        return multiFields;
    }

    @Override
    public String toString()
    {
        return "CfData[linkerFields=" + linkerFields + ", linkedFields=" + linkedFields +
            ", multiFields=" + multiFields + "]";
    }
}
