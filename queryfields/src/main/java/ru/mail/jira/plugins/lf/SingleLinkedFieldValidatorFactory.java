/*
 * Created by Andrey Markelov 29-11-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins.lf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginValidatorFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ValidatorDescriptor;

/**
 * Factory for validator that checks linked issues status.
 *
 * @author Andrey Markelov
 */
public class SingleLinkedFieldValidatorFactory
    extends AbstractWorkflowPluginFactory
    implements WorkflowPluginValidatorFactory
{
    /**
     * Constructor.
     */
    public SingleLinkedFieldValidatorFactory() {}

    @Override
    public Map<String, ?> getDescriptorParams(
        Map<String, Object> validatorParams)
    {
        Map<String, Object> map = new HashMap<String, Object>();

        if (validatorParams != null && validatorParams.containsKey("jql"))
        {
            map.put("jql", extractSingleParam(validatorParams, "jql"));
        }
        else
        {
            map.put("jql", "");
        }

        if (validatorParams != null && validatorParams.containsKey("invalid_statuses"))
        {
            String[] statuses = (String[])validatorParams.get("invalid_statuses");
            StringBuilder sb = new StringBuilder();
            if (statuses != null)
            {
                for (String status : statuses)
                {
                    sb.append(status).append("&");
                }
            }
            map.put("invalid_statuses", sb.toString());
        }
        else
        {
            map.put("invalid_statuses", "");
        }

        return map;
    }

    /**
     * Get parameter from descriptor.
     */
    private String getParam(AbstractDescriptor descriptor, String param)
    {
        if (!(descriptor instanceof ValidatorDescriptor))
        {
            throw new IllegalArgumentException("Descriptor must be a ValidatorDescriptor.");
        }

        ValidatorDescriptor validatorDescriptor = (ValidatorDescriptor) descriptor;
        String value = (String) validatorDescriptor.getArgs().get(param);

        if (value!=null && value.trim().length() > 0)
        {
            return value;
        }
        else
        {
            return "";
        }
    }

    /**
     * Get collection parameters.
     */
    private Set<String> getSetParams(
        AbstractDescriptor descriptor,
        String param)
    {
        Set<String> params = new TreeSet<String>();

        String paramStr = getParam(descriptor, param);
        if (paramStr != null)
        {
            StringTokenizer st = new StringTokenizer(paramStr, "&");
            while (st.hasMoreTokens())
            {
                String token = st.nextToken();
                if (token.length() > 0)
                {
                    params.add(token.trim());
                }
            }
        }

        return params;
    }

    /**
     * Get list of statuses.
     */
    private Map<String, String> getStatuses()
    {
        Map<String, String> res = new TreeMap<String, String>();

        Collection<Status> statuses = ComponentAccessor.getConstantsManager().getStatusObjects();
        if (statuses != null)
        {
            for (Status status : statuses)
            {
                res.put(status.getId(), status.getName());
            }
        }

        return res;
    }

    @Override
    protected void getVelocityParamsForEdit(
        Map<String, Object> velocityParams,
        AbstractDescriptor descriptor)
    {
        velocityParams.put("statuses", getStatuses());
        velocityParams.put("jql", getParam(descriptor, "jql"));
        velocityParams.put("invalid_statuses", getSetParams(descriptor, "invalid_statuses"));
    }

    @Override
    protected void getVelocityParamsForInput(
        Map<String, Object> velocityParams)
    {
        velocityParams.put("statuses", getStatuses());
        velocityParams.put("jql", "");
        velocityParams.put("invalid_statuses", new TreeSet<String>());
    }

    @Override
    protected void getVelocityParamsForView(
        Map<String, Object> velocityParams,
        AbstractDescriptor descriptor)
    {
        StringBuilder realStatuses = new StringBuilder();
        Set<String> statuses = getSetParams(descriptor, "invalid_statuses");
        for (String status : statuses)
        {
            Status statusObj = ComponentAccessor.getConstantsManager().getStatusObject(status);
            if (statusObj != null)
            {
                if (realStatuses.length() > 0)
                {
                    realStatuses.append(", ");
                }
                realStatuses.append(statusObj.getName());
            }
        }

        velocityParams.put("jql", getParam(descriptor, "jql"));
        velocityParams.put("invalid_statuses", getSetParams(descriptor, "invalid_statuses"));
        velocityParams.put("realStatuses", realStatuses.toString());
    }
}
