/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011-2014 Basis Technology Corp.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.autopsy.ingest.ModuleContentEvent;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.Directory;
import org.sleuthkit.datamodel.FileSystem;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.VolumeSystem;

/**
 * Makes the children nodes / keys for a given content object. Has knowledge
 * about the structure of the directory tree and what levels should be ignored.
 * TODO consider a ContentChildren child factory
 */
class ContentChildren extends AbstractContentChildren<Content> {

    private static final Logger logger = Logger.getLogger(ContentChildren.class.getName());

    private final Content parent;

    ContentChildren(Content parent) {
        super(); //initialize lazy behavior
        this.parent = parent;
        /* These are in here versus addNotify because addNotify() is not 
         * called for a file that does not have children when it is created (.i.e. a zip file). */
        Case.addPropertyChangeListener(pcl);
        IngestManager.getInstance().addIngestModuleEventListener(pcl);
    }

    private final PropertyChangeListener pcl = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String eventType = evt.getPropertyName();

            // See if the new file is a child of ours
            if (eventType.equals(IngestManager.IngestModuleEvent.CONTENT_CHANGED.toString())) {
                if ((evt.getOldValue() instanceof ModuleContentEvent) == false) {
                    return;
                }
                ModuleContentEvent moduleContentEvent = (ModuleContentEvent) evt.getOldValue();
                if ((moduleContentEvent.getSource() instanceof Content) == false) {
                    return;
                }
                Content newContent = (Content) moduleContentEvent.getSource();
                try {
                    Content parentOfContent = newContent.getParent();
                    // The new file is my child, so refresh our children.
                    if (parentOfContent.getId() == parent.getId()) {
                        // @@@ REMOVED FOR TESTING refreshKeys();
                    }

                    // See if my child has a new child.  This is needed because
                    // directory tree needs to now show my child if it is a parent.
                    // DirectoryTreeFilterChildren does not show nodes if they do
                    // not have children.
                    Content p2 = parentOfContent.getParent();
                    if ((p2 != null) && (p2.getId() == parent.getId())) {
                        refreshKeys(parentOfContent);
                    }
                } catch (TskCoreException ex) {
                    // @@@ TODO
                }
            } else if (eventType.equals(Case.Events.CURRENT_CASE.toString())) {
                // case was closed. Remove listeners so that we don't get called with a stale case handle
                if (evt.getNewValue() == null) {
                    removeNotify();
                }
            }
        }
    };

    /**
     * Get the children of the Content object based on what we want to display.
     * As an example, we don't display the direct children of VolumeSystems or
     * FileSystems. We hide some of the levels in the tree. This method takes
     * care of that and returns the children we want to display
     *
     * @param parent
     * @return
     */
    private static List<Content> getDisplayChildren(Content parent) {
        // what does the content think its children are?
        List<Content> tmpChildren;
        try {
            tmpChildren = parent.getChildren();
        } catch (TskCoreException ex) {
            logger.log(Level.WARNING, "Error getting Content children.", ex); //NON-NLS
            tmpChildren = Collections.emptyList();
        }

        // Cycle through the list and make a new one based
        // on what we actually want to display. 
        List<Content> children = new ArrayList<>();
        for (Content c : tmpChildren) {
            if (c instanceof VolumeSystem) {
                children.addAll(getDisplayChildren(c));
            } else if (c instanceof FileSystem) {
                children.addAll(getDisplayChildren(c));
            } else if (c instanceof Directory) {
                Directory dir = (Directory) c;
                /* For root directories, we want to return their contents.
                 * Special case though for '.' and '..' entries, because they should
                 * not have children (and in fact don't in the DB).  Other drs
                 * get treated as files and added as is. */
                if ((dir.isRoot()) && (dir.getName().equals(".") == false)
                        && (dir.getName().equals("..") == false)) {
                    children.addAll(getDisplayChildren(dir));
                } else {
                    children.add(c);
                }
            } else {
                children.add(c);
            }
        }
        return children;
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        refreshKeys(null);
    }

    /**
     * Reset the keys for the latest set of children this node has. 
     * @param key Key that needs an explicit refresh because we think 
     * it's state dramatically changed. Or null to do basic resetting. 
     */
    private void refreshKeys(Content key) {
        List<Content> children = getDisplayChildren(parent);
        setKeys(children);
        // @@@ THis was added as part of testing the content added refresh work.
        // Not sure it is really needed or not. 
        if (key != null) {
            refreshKey(key); // TEST 
        }
    }

    @Override
    protected void removeNotify() {
        super.removeNotify();
        Case.removePropertyChangeListener(pcl);
        IngestManager.getInstance().removeIngestModuleEventListener(pcl);
        setKeys(new ArrayList<>());
    }
}
