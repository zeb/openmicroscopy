/*
 * org.openmicroscopy.shoola.agents.browser.colormap.ColorMapUI
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

/*------------------------------------------------------------------------------
 *
 * Written by:    Jeff Mellen <jeffm@alum.mit.edu>
 *
 *------------------------------------------------------------------------------
 */
package org.openmicroscopy.shoola.agents.browser.colormap;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openmicroscopy.ds.st.CategoryGroup;

/**
 * The UI for the colormap.
 * 
 * @author Jeff Mellen, <a href="mailto:jeffm@alum.mit.edu">jeffm@alum.mit.edu</a><br>
 * <b>Internal version:</b> $Revision$ $Date$
 * @version 2.2
 * @since OME2.2
 */
public class ColorMapUI extends JInternalFrame
                        implements ColorMapModelListener,
                                   ColorMapGroupListener
{
    private ColorMapModel model;
    private ColorMapGroupBar groupBar;
    private ColorMapListUI listUI;
    
    public ColorMapUI()
    {
        init();
    }
    
    public ColorMapUI(ColorMapModel model)
    {
        this.model = model;
        init();
        groupBar.setCategoryTree(model.getTree());
    }
    
    public void init()
    {
        groupBar = new ColorMapGroupBar();
        groupBar.addListener(this);
    }
    
    public void reset()
    {
        // TODO perform reset operation here
    }
    
    /**
     * @see org.openmicroscopy.shoola.agents.browser.colormap.ColorMapModelListener#modelChanged(org.openmicroscopy.shoola.agents.browser.colormap.ColorMapModel)
     */
    public void modelChanged(ColorMapModel model)
    {
        // TODO Auto-generated method stub

    }
    
    /**
     * @see org.openmicroscopy.shoola.agents.browser.colormap.ColorMapGroupListener#groupSelected(org.openmicroscopy.ds.st.CategoryGroup)
     */
    public void groupSelected(CategoryGroup group)
    {
        // TODO change List UI
    }
    
    /**
     * @see org.openmicroscopy.shoola.agents.browser.colormap.ColorMapGroupListener#groupsDeselected()
     */
    public void groupsDeselected()
    {
        // TODO change List UI
    }
    
    private void buildUI()
    {
        Container container = getContentPane();
        
        container.setLayout(new BorderLayout(2,2));
        container.add(groupBar,BorderLayout.NORTH);
    }

}