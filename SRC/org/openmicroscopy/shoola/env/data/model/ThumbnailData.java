/*
 * org.openmicroscopy.shoola.env.data.model.ThumbnailData
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006 University of Dundee. All rights reserved.
 *
 *
 * 	This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

package org.openmicroscopy.shoola.env.data.model;


//Java imports
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

//Third-party libraries

//Application-internal dependencies

/** 
 * Holds a {@link BufferedImage} serving as a thumbnail for a given <i>OME</i>
 * image.
 *
 * @author  Jean-Marie Burel &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:j.burel@dundee.ac.uk">j.burel@dundee.ac.uk</a>
 * @author  <br>Andrea Falconi &nbsp;&nbsp;&nbsp;&nbsp;
 * 				<a href="mailto:a.falconi@dundee.ac.uk">
 * 					a.falconi@dundee.ac.uk</a>
 * @version 2.2
 * <small>
 * (<b>Internal version:</b> $Revision$ $Date$)
 * </small>
 * @since OME2.2
 */
public class ThumbnailData
    implements DataObject
{

	 /** The id of the image to which the thumbnail belong. */
    private long            userID;
    
    /** The id of the image to which the thumbnail belong. */
    private long            imageID;
    
    /** The thumbnail pixels. */
    private BufferedImage   thumbnail;

    /**
     * Creates a new instance.
     * 
     * @param imageID   The id of the image to which the thumbnail belong.
     *                  Must be positive.
     * @param thumbnail The thumbnail pixels.  Mustn't be <code>null</code>.
     * @param userID   	The id of the user the thumbnail is for.
     *                  Must be positive.
     */
    public ThumbnailData(long imageID, BufferedImage thumbnail, long userID)
    {
        if (imageID <= 0) 
            throw new IllegalArgumentException("Non-positive image id: "+
                                               imageID+".");
        if (thumbnail == null)
            throw new NullPointerException("No thumbnail.");
        this.imageID = imageID;
        this.thumbnail = thumbnail;
        this.userID = userID;
    }
    
    /**
     * Creates a new instance.
     * 
     * @param imageID   The id of the image to which the thumbnail belong.
     *                  Must be positive.
     * @param thumbnail The thumbnail pixels.  Mustn't be <code>null</code>.
     */
    public ThumbnailData(long imageID, BufferedImage thumbnail)
    {
        if (imageID <= 0) 
            throw new IllegalArgumentException("Non-positive image id: "+
                                               imageID+".");
        if (thumbnail == null)
            throw new NullPointerException("No thumbnail.");
        this.imageID = imageID;
        this.thumbnail = thumbnail;
    }
    
    /**
     * Clones this object.
     * This is a deep-copy, the thumbnail pixels are cloned too.
     * 
     * @see org.openmicroscopy.shoola.env.data.model.DataObject#makeNew()
     */
    public DataObject makeNew()
    {
        BufferedImage pixClone = new BufferedImage(
                                        thumbnail.getWidth(),
                                        thumbnail.getHeight(), 
                                        thumbnail.getType());
        Graphics2D g2D = pixClone.createGraphics();
        g2D.drawImage(thumbnail, null, 0, 0); 
        return new ThumbnailData(imageID, pixClone);
    }

    /**
     * Returns the id of the user.
     * 
     * @return See above.
     */
    public long getUserID() { return userID; }
    
    /**
     * Returns the id of the image to which the thumbnail belong.
     * 
     * @return See above.
     */
    public long getImageID() { return imageID; }
    
    /**
     * Returns the thumbnail pixels.
     * 
     * @return See above.
     */
    public BufferedImage getThumbnail() { return thumbnail; }
    
}
