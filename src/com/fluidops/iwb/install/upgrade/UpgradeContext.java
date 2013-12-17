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

package com.fluidops.iwb.install.upgrade;

import org.openrdf.repository.Repository;

/**
 * Contextual information required for {@link UpgradeHandler}s.
 * 
 * @author as
 *
 */
public class UpgradeContext {

	private final Repository mainRepository;
	private final Repository historyRepository;
	
	/**
	 * @param mainRepository
	 * @param historyRepository
	 */
	public UpgradeContext(Repository mainRepository,
			Repository historyRepository) {
		super();
		this.mainRepository = mainRepository;
		this.historyRepository = historyRepository;
	}

	/**
	 * @return the mainRepository
	 */
	public Repository getMainRepository() {
		return mainRepository;
	}

	/**
	 * @return the historyRepository or null (if this is not available)
	 */
	public Repository getHistoryRepository() {
		return historyRepository;
	}
	
	
	
}
