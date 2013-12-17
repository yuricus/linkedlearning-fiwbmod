/*
 * Copyright (C) 2008-2013, fluid Operations AG
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.fluidops.iwb.api.solution;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;

public class CompositeInstallationResult implements InstallationResult {
    
    private static final long serialVersionUID = 6478726685374437528L;
    private static final Function<InstallationResult, InstallationStatus> getInstallationStatusFunction = 
            new Function<InstallationResult, InstallationStatus>()
            {
                @Override
                public InstallationStatus apply(InstallationResult result)
                {
                    return result.getInstallationStatus();
                }
            };
    private Map<String, InstallationResult> handlerToResult = newHashMap();

    @Override
    public InstallationStatus getInstallationStatus()
    {
    	if (handlerToResult.isEmpty()) {
    		return InstallationStatus.INSTALLED_NOTHING;
    	}
    	
        return aggregate(transform(handlerToResult.values(), getInstallationStatusFunction));
    }
    
    private static InstallationStatus aggregate(Collection<InstallationStatus> coll) {
        Iterator<InstallationStatus> i = coll.iterator();
        InstallationStatus candidate = i.next();
        boolean restartRequired = candidate.isRestartRequired();
        boolean success = candidate.isSuccess();
        
        while (i.hasNext()) {
        	InstallationStatus next = i.next();
        	restartRequired = restartRequired || next.isRestartRequired();
        	success = success && next.isSuccess();
            if (next.compareTo(candidate) > 0)
                candidate = next;
        }
        if(!success && restartRequired) {
        	return InstallationStatus.INSTALLED_PARTIALLY_WITH_ERRORS_RESTART_REQUIRED;
        } else {
        	return candidate;
        }
    }

    public void addResultForHandler(SolutionHandler<? extends InstallationResult> solutionHandler,
            InstallationResult solutionContext)
    {
    	addResultForHandler(solutionHandler.toString(), solutionContext);
    }
    
    public void addResultForHandler(String solutionHandler,
            InstallationResult solutionContext) {
        handlerToResult.put(solutionHandler, solutionContext);
    }

    @Override
    public List<Exception> getErrors()
    {
        List<Exception> allExceptions = newArrayList();
        for (InstallationResult result : handlerToResult.values())
        {
            allExceptions.addAll(result.getErrors());
        }
        return allExceptions;
    }
    
    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        output.append("Status: " + getInstallationStatus());
        for (Entry<String, InstallationResult> entry : handlerToResult.entrySet())
        {
            String solutionHandler = entry.getKey();
            InstallationResult result = entry.getValue();
            if(!result.getInstallationStatus().isSuccess())
                output.append(String.format("%n%s: %s", solutionHandler, result.getErrors()));
            if(result.getInstallationStatus().isRestartRequired())
                output.append(String.format("%n%s: requires a restart", solutionHandler));
        }
        return output.toString();
    }
}