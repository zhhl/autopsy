/*
 * Autopsy Forensic Browser
 *
 * Copyright 2014 Basis Technology Corp.
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
