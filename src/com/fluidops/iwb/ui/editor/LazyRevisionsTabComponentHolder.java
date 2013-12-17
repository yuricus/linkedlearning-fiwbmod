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

package com.fluidops.iwb.ui.editor;

import java.util.List;

import com.fluidops.ajax.components.FButton;
import com.fluidops.ajax.components.FContainer;
import com.fluidops.ajax.components.FHTML;
import com.fluidops.ajax.components.FTextDiff;
import com.fluidops.iwb.api.EndpointImpl;
import com.fluidops.iwb.ui.editor.RevisionTable.WikiRevisionWithRevNumber;
import com.fluidops.iwb.user.UserManager;
import com.fluidops.iwb.user.UserManager.UIComponent;
import com.fluidops.iwb.user.UserManager.ValueAccessLevel;
import com.fluidops.iwb.wiki.Wikimedia;
import com.fluidops.textdiff.TextDiff2.DiffView;
import com.fluidops.util.StringUtil;


/**
 * The lazy loaded {@link SemWikiComponentHolder} for the revision tab. This
 * tab shows the {@link RevisionTable}, and if applicable buttons to trigger
 * certain operations (e.g. deletions, restore, comparison). The availability
 * of the operations depends on the {@link ValueAccessLevel} of the
 * particular {@link SemWiki} instance.
 * 
 * @author as
 * @see RevisionTable
 */
public class LazyRevisionsTabComponentHolder extends SemWikiComponentHolder {

	private RevisionTable revisionTable;
	private FHTML revisionCompare;
	
	public LazyRevisionsTabComponentHolder(SemWiki semWiki) {
		super(semWiki);
	}
	
	@Override
	protected void initializeView(FContainer container) {
		
		// set revision access rights
		UserManager userManager = EndpointImpl.api().getUserManager();
		ValueAccessLevel accessLevel = semWiki.getAccessLevel();
        boolean allowDeleteRevisions = userManager.hasUIComponentAccess(UIComponent.WIKIVIEW_REVISIONDELETE_BUTTON,null)
                && (accessLevel!=null && accessLevel.compareTo(ValueAccessLevel.WRITE_LIMITED)>=0);
        boolean allowRestoreRevisions = userManager.hasUIComponentAccess(UIComponent.WIKIVIEW_REVISIONRESTORE_BUTTON,null)
                && (accessLevel!=null && accessLevel.compareTo(ValueAccessLevel.WRITE_LIMITED)>=0);
        
        
        // revision table
		revisionTable = new RevisionTable("revTable", semWiki, semWiki.getSubject(), allowDeleteRevisions, allowRestoreRevisions);
		container.add(revisionTable);
		
		// show revision comparison controls if there is more than 1 revision
		if (revisionTable.getNumberOfRevisions() > 1) {
			// showDiffButton
			container.add(createShowRevisionDifferencButton());
			
			// revision compare placeholder
			revisionCompare = new FHTML("revCompare");
			container.add(revisionCompare);
		}
	}
	
	private FButton createShowRevisionDifferencButton() {
		return new FButton("showdiff", "Show Difference") {
			@Override
			public void onClick() {

				List<WikiRevisionWithRevNumber> selectedRevs = revisionTable.getSelectedObjects();

				if (selectedRevs.size() != 2) {
					getPage().getPopupWindowInstance().showInfo("Select exactly two revisions to compare");
					return;
				}				

				WikiRevisionWithRevNumber revisionNew = selectedRevs.get(0);
				WikiRevisionWithRevNumber revisionOld = selectedRevs.get(1);

				String contentOld = Wikimedia.getWikiStorage()
						.getWikiContent(semWiki.getSubject(), revisionOld.rev);
				String contentNew = Wikimedia.getWikiStorage()
						.getWikiContent(semWiki.getSubject(), revisionNew.rev);

				// TODO: find a better solution here for cutting of leading
				// and trailing quotes
				contentNew = StringUtil
						.removeBeginningAndEndingQuotes(contentNew);
				contentOld = StringUtil
						.removeBeginningAndEndingQuotes(contentOld);

				FTextDiff ftd = new FTextDiff("myTextDiff");
				ftd.setTexts(contentNew, contentOld);
				ftd.setView(DiffView.COMPACT);
				ftd.setRevisionTitles("Revision " + revisionOld.revNumber,
						"Revision " + revisionNew.revNumber);

				String diff = "<br/>" + "<br/>"
						+ "<b>Difference between selected revisions:</b>"
						+ "<br/>"
						+ "<div class = 'revDiff' style='overflow:auto'>"
						+ ftd.render() + "</div>";

				revisionCompare.setValue(diff);
			}
		};
	}
}