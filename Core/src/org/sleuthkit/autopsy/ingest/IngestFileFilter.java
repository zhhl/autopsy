/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sleuthkit.autopsy.ingest;

import java.util.ArrayList;
import java.util.List;
import org.sleuthkit.datamodel.AbstractFile;

/**
 *
 */
class IngestFileFilter {
    private final List<Rule> rules = new ArrayList<>();
    
    void add(String extension, String path) {
        rules.add(new Rule(extension, path));
    }
    
    boolean match(AbstractFile file) {
        if (rules.isEmpty())
            return true;
        
        for (Rule rule : rules) {
            if (rule.match(file))
                return true;
        }
        return false;
    }
    
    class Rule {
        private String extension;
        private final String path;

        Rule(String extension, String path) {
            if (extension != null) {
                this.extension = extension.toLowerCase();
                if (this.extension.startsWith(".") == false) 
                    this.extension = "." + this.extension;
            }
            else {
                this.extension = null;
            }
            
            if (path != null)
                this.path = path.toLowerCase();
            else
                this.path = null;
        }
        
        boolean match (AbstractFile file) {
            if (extension != null && file.getName().toLowerCase().endsWith(extension) == false)
                return false;
            else if (path != null && file.getParentPath().toLowerCase().contains(path) == false)
                return false;
            return true;
        }
    }
}
