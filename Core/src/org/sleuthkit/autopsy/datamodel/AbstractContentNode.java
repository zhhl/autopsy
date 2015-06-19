/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.datamodel;

import java.util.List;
import java.util.logging.Level;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskException;

/**
 * Interface class that all Data nodes inherit from. Provides basic information
 * such as ID, parent ID, etc.
 *
 * @param <T> type of wrapped Content
 */
public abstract class AbstractContentNode<T extends Content> extends ContentNode {

    /**
     * Underlying Sleuth Kit Content object
     */
    T content;
    private static final Logger logger = Logger.getLogger(AbstractContentNode.class.getName());

    /**
     * Handles aspects that depend on the Content object
     *
     * @param content Underlying Content instances
     */
    AbstractContentNode(T content) {
        //TODO consider child factory for the content children
        super(new ContentChildren(content), Lookups.singleton(content));
        this.content = content;
        //super.setName(ContentUtils.getSystemName(content));
        super.setName("content_" + Long.toString(content.getId())); //NON-NLS
    }
    
    /**
     * Return the content data associated with this node
     * @return the content object wrapped by this node
     */
    public T getContent() {
        return content;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException(
                NbBundle.getMessage(this.getClass(), "AbstractContentNode.exception.cannotChangeSysName.msg"));
    }

    @Override
    public String getName() {
        return super.getName();
    }

    /**
     * Return true if the underlying content object has children Useful for lazy
     * loading.
     *
     * @return true if has children
     */
    public boolean hasContentChildren() {
        boolean hasChildren = false;

        if (content != null) {
            try {
                hasChildren = content.hasChildren();
            } catch (TskCoreException ex) {
                logger.log(Level.SEVERE, "Error checking if the node has children, for content: " + content, ex); //NON-NLS
            }
        }

        return hasChildren;
    }

    /**
     * Return ids of children of the underlying content. The ids can be treated
     * as keys - useful for lazy loading.
     *
     * @return list of content ids of children content.
     */
    public List<Long> getContentChildrenIds() {
        List<Long> childrenIds = null;

        if (content != null) {
            try {
                childrenIds = content.getChildrenIds();
            } catch (TskCoreException ex) {
                logger.log(Level.SEVERE, "Error getting children ids, for content: " + content, ex); //NON-NLS
            }
        }

        return childrenIds;

    }

    /**
     * Return children of the underlying content.
     *
     * @return list of content children content.
     */
    public List<Content> getContentChildren() {
        List<Content> children = null;

        if (content != null) {
            try {
                children = content.getChildren();
            } catch (TskCoreException ex) {
                logger.log(Level.SEVERE, "Error getting children, for content: " + content, ex); //NON-NLS
            }
        }

        return children;

    }
    
     /**
     * Get count of the underlying content object children.
     * 
     * Useful for lazy
     * loading.
     *
     * @return content children count
     */
    public int getContentChildrenCount() {
        int childrenCount = -1;

        if (content != null) {
            try {
                childrenCount = content.getChildrenCount();
            } catch (TskCoreException ex) {
                logger.log(Level.SEVERE, "Error checking node content children count, for content: " + content, ex); //NON-NLS
            }
        }

        return childrenCount;
    }


    /**
     * Reads the content of this node (of the underlying content object).
     *
     * @param buf buffer to read into
     * @param offset the starting offset in the content object
     * @param len the length to read
     * @return the bytes read
     * @throws TskException exception thrown if the requested part of content could not be read
     */
    public int read(byte[] buf, long offset, long len) throws TskException {
        return content.read(buf, offset, len);
    }
    
    // @@@ main reason for overriding these while testing various refresh options
    // is to force the comparision to consider if the node has children or not. 
    // That way when we call setKeys(), for a file that was already there, a
    // refresh will be forced. 
     @Override
    public boolean equals(Object o) {
        if ((o instanceof AbstractContentNode) == false){
            return false;
        }
        Content oContent = ((AbstractContentNode)o).getContent();
        if (oContent.getId() != content.getId())
            return false;
        
        
        // @@@ Strangely, the node that has new derived children always 
        // comes in here thinking that it always had children.  I never saw 
        // a situation where we had two nodes for the same file, but one had children
        // (i.e. the new node after the derived files were added) and one had
        // no children (i.e. the node that was created before the file had children). 
        // code always seems to have. One theory for this is from here: 
        // http://bits.netbeans.org/8.0/javadoc/org-openide-nodes/org/openide/nodes/FilterNode.Children.html.
        /* FilterNode.Children is not well suited to cases where you need to insert additional nodes at the beginning 
                or end of the list, or where you may need to merge together multiple original children lists, or 
                reorder them, etc. That is because the keys are of type Node, one for each original child, and 
                the keys are reset during addNotify(), filterChildrenAdded(org.openide.nodes.NodeMemberEvent), 
                filterChildrenRemoved(org.openide.nodes.NodeMemberEvent), and 
                filterChildrenReordered(org.openide.nodes.NodeReorderEvent), so it is not trivial to use different 
                keys: you would need to override addNotify (calling super first!) and the other three update methods. 
                For such complex cases you will do better by creating your own Children.Keys subclass, setting 
                keys that are useful to you, and keeping a NodeListener on the original node to handle changes.
          By 'resetting' the keys, I"m not sure if that means that they reload them and therefore during the reload 
          the newly reloaded nodes think they have children and always did have children.  I tried to override those,
          but it didn't quite work - they were never called.  This could use some further work in DirectoryTreeFilterChildren. 
        
        */
        try {
            return (oContent.hasChildren() == content.hasChildren());
        } catch (TskCoreException ex) {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int hashCode = 22;
        hashCode = hashCode * 51 + (int)content.getId(); 
        try {
            if (content.hasChildren())
                hashCode = hashCode + 1;
        } catch (TskCoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        return hashCode;
    }
}
