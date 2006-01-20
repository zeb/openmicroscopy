/*
 * org.openmicroscopy.shoola.agents.treemng.browser.BrowserControl
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

package org.openmicroscopy.shoola.agents.treeviewer.browser;


//Java imports
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;


//Third-party libraries

//Application-internal dependencies
import org.openmicroscopy.shoola.agents.treeviewer.actions.CloseAction;
import org.openmicroscopy.shoola.agents.treeviewer.actions.CollapseAction;
import org.openmicroscopy.shoola.agents.treeviewer.actions.FilterAction;
import org.openmicroscopy.shoola.agents.treeviewer.actions.FilterMenuAction;
import org.openmicroscopy.shoola.agents.treeviewer.actions.SortAction;
import org.openmicroscopy.shoola.agents.treeviewer.actions.SortByDateAction;
import pojos.CategoryData;
import pojos.DatasetData;

/** 
 * The Browser's Controller.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
class BrowserControl
    implements ChangeListener, PropertyChangeListener
{

    /** Identifies the Collapse action in the Actions menu. */
    static final Integer     COLLAPSE = new Integer(0);
    
    /** Identifies the Close action in the Actions menu. */
    static final Integer     CLOSE = new Integer(1);
    
    /** Identifies the Sort action in the Actions menu. */
    static final Integer     SORT = new Integer(2);
    
    /** Identifies the Sort by Date action in the Actions menu. */
    static final Integer     SORT_DATE = new Integer(3);
    
    /** Identifies the Filter in Dataset action in the Actions menu. */
    static final Integer     FILTER_IN_DATASET = new Integer(4);
    
    /** Identifies the Filter in Category action in the Actions menu. */
    static final Integer     FILTER_IN_CATEGORY = new Integer(5);
    
    /** Identifies the Filter Menu action in the Actions menu. */
    static final Integer     FILTER_MENU = new Integer(6);
       
    /** 
     * Reference to the {@link Browser} component, which, in this context,
     * is regarded as the Model.
     */
    private Browser     model;
    
    /** Reference to the View. */
    private BrowserUI   view;
    
    /** Maps actions ids onto actual <code>Action</code> object. */
    private Map			actionsMap;
    
    /** Helper method to create all the UI actions. */
    private void createActions()
    {
        actionsMap.put(COLLAPSE, new CollapseAction(model));
        actionsMap.put(CLOSE, new CloseAction(model));
        actionsMap.put(SORT, new SortAction(model));
        actionsMap.put(SORT_DATE, new SortByDateAction(model));
        actionsMap.put(FILTER_IN_DATASET, new FilterAction(model,
                                            Browser.DATASET_CONTAINER));
        actionsMap.put(FILTER_IN_CATEGORY, new FilterAction(model,
                							Browser.CATEGORY_CONTAINER));
        actionsMap.put(FILTER_MENU, new FilterMenuAction(model));
    }
    
    /**
     * Loads the children of nodes contained in the specified in collection.
     * 
     * @param nodes The collection of nodes.
     */
    private void filterNodes(Set nodes)
    {
        if (nodes == null || nodes.size() == 0) return;
        if (model.getBrowserType() != Browser.IMAGES_EXPLORER) return;
        //We should be in the ready state.
        DefaultTreeModel dtm = (DefaultTreeModel) 
                                view.getTreeDisplay().getModel();
        TreeImageDisplay root = (TreeImageDisplay) dtm.getRoot();
        root.removeAllChildrenDisplay() ;
        view.loadAction(root);
        model.loadData(nodes);
    }
    
    /**
     * Creates a new instance.
     * The {@link #initialize(BrowserUI) initialize} method 
     * should be called straight after to link this Controller to the other 
     * MVC components.
     * 
     * @param model  Reference to the {@link Browser} component, which, in 
     *               this context, is regarded as the Model.
     *               Mustn't be <code>null</code>.
     */
    BrowserControl(Browser model)
    {
        if (model == null) throw new NullPointerException("No model.");
        this.model = model;
        actionsMap = new HashMap();
        createActions();
    }
    
    /**
     * Links this Controller to its Model and its View.
     * 
     * @param view   Reference to the View. Mustn't be <code>null</code>.
     */
    void initialize(BrowserUI view)
    {
        if (view == null) throw new NullPointerException("No view.");
        this.view = view;
        model.addPropertyChangeListener(this);
    }

    /**
     * Reacts to tree expansion events.
     * 
     * @param display The selected node.
     * @param expanded 	<code>true</code> if the node is expanded,
     * 					<code>false</code> otherwise.
     */
    void onNodeNavigation(TreeImageDisplay display, boolean expanded)
    {
        int state = model.getState();
        if ((state == Browser.LOADING_DATA) ||
             (state == Browser.LOADING_LEAVES) || 
             (state == Browser.COUNTING_ITEMS)) return;
        Object ho = display.getUserObject();
        model.setSelectedDisplay(display); 
        if (!expanded) return;
        if ((ho instanceof DatasetData) || (ho instanceof CategoryData)) {
            if (display.getChildrenDisplay().size() == 0) {
                view.loadAction(display);
                model.loadLeaves();
            }    
        } else {
            DefaultTreeModel dtm = (DefaultTreeModel) 
                            view.getTreeDisplay().getModel();
            TreeImageDisplay root = (TreeImageDisplay) dtm.getRoot();
            if (root.equals(display) && root.getChildrenDisplay().size() == 0) {
                view.loadAction(root);
                model.loadData();
            }
        }
    }
    
    /**
     * Reacts to click events on the tree.
     * 
     * @param popupTrigger  <code>true</code> is the event is the popup menu
     *                      trigger event for the platform, <code>false</code>
     *                      otherwise.
     */
    void onClick(boolean popupTrigger)
    {
        Object node = view.getTreeDisplay().getLastSelectedPathComponent();
        if (!(node instanceof TreeImageDisplay)) return;
        model.setSelectedDisplay((TreeImageDisplay) node);
        if (popupTrigger) model.showPopupMenu();
    }
    
    /**
     * Returns the action corresponding to the specified id.
     * 
     * @param id One of the flags defined by this class.
     * @return The specified action.
     */
    Action getAction(Integer id) { return (Action) actionsMap.get(id); }
    
    /**
     * Detects when the {@link Browser} is ready and then registers for
     * property change notification.
     * @see ChangeListener#stateChanged(ChangeEvent)
     */
    public void stateChanged(ChangeEvent e)
    {
        int state = model.getState();
        switch (state) {
            case Browser.LOADING_DATA:
                break;
            case Browser.LOADING_LEAVES:
                break;
            case Browser.READY:
                
                break;
            case Browser.DISCARDED:
                break;
        }
    }

    /**
     * Reacts to {@link Browser} property changes.
     * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent pce)
    {
        String name = pce.getPropertyName();
        if (name.equals(Browser.FILTER_NODES_PROPERTY)) 
            filterNodes((Set) pce.getNewValue());
        else if (name.equals(Browser.HIERARCHY_ROOT_PROPERTY))
            model.refreshTree();
    }
    
}
