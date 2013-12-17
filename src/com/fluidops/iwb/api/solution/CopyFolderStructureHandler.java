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

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fluidops.iwb.api.solution.InstallationResult.InstallationStatus;
import com.fluidops.util.GenUtil;
import com.fluidops.util.StringUtil;

public class CopyFolderStructureHandler extends AbstractFailureHandlingHandler
{
    private final static FileFilter EXCLUDE_SVN = new FileFilter()
    {
        @Override
        public boolean accept(File pathname)
        {
            return !pathname.getName().equals(".svn");
        }
    };
    private final String rootRelPath;
    private final File applicationRoot;
    private final FileFilter fileFilter;
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath)
    {
        this(applicationRoot, rootRelPath, InstallationStatus.INSTALLED_SUCCESSFULLY);
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, String wildcardPattern)
    {
    	this(applicationRoot, rootRelPath, new WildcardFileFilter(wildcardPattern));
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, FileFilter fileFilter)
    {
    	this(applicationRoot, rootRelPath, fileFilter, InstallationStatus.INSTALLED_SUCCESSFULLY);
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, InstallationStatus successStatus)
    {
    	this(applicationRoot, rootRelPath, EXCLUDE_SVN, successStatus);
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, String wildcardPattern, InstallationStatus successStatus)
    {
    	this(applicationRoot, rootRelPath, new WildcardFileFilter(wildcardPattern), successStatus);
    }
    
    public CopyFolderStructureHandler(File applicationRoot, String rootRelPath, FileFilter fileFilter, InstallationStatus successStatus)
    {
        super(successStatus);
        this.applicationRoot = applicationRoot;
        this.rootRelPath = rootRelPath;
		this.fileFilter = fileFilter;
    }
    
    @Override boolean installIgnoreExceptions(File solutionDir)
    {
        File solutionFolderDir = (StringUtil.isNotNullNorEmpty(rootRelPath) ? new File(solutionDir, rootRelPath) : solutionDir);
        File applicationFolderDir = (StringUtil.isNotNullNorEmpty(rootRelPath) ? new File(this.applicationRoot, rootRelPath) : this.applicationRoot);
        if(!solutionFolderDir.exists()) return false;
        GenUtil.copyFolder(solutionFolderDir, applicationFolderDir, fileFilter);
        return true;
    }
}
