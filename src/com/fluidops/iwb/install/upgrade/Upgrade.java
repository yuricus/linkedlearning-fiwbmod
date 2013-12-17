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

import com.fluidops.iwb.Global;

/**
 * Convenience class for statically accessing available upgrade handlers,
 * e.g. from groovy scripts
 * 
 * @author as
 *
 */
public class Upgrade {

	
	/**
	 * Upgrade procedure for historical repositories using 
	 * {@link HistoryRepositoryUpgradeHandler}
	 * 
	 */
	public static void upgradeHistoryRepositoryData() {
		new HistoryRepositoryUpgradeHandler().doUpgrade(createContext());
	}
	
	/**
	 * Create a fresh {@link UpgradeContext} using the current
	 * system configuration
	 * 
	 * @return
	 */
	private static UpgradeContext createContext() {
		return new UpgradeContext(Global.repository, Global.historyRepository);
	}
}
