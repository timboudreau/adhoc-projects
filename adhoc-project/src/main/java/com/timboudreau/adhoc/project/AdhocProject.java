/* Copyright (C) 2013 Tim Boudreau

 Permission is hereby granted, free of charge, to any person obtaining a copy 
 of this software and associated documentation files (the "Software"), to 
 deal in the Software without restriction, including without limitation the 
 rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
 sell copies of the Software, and to permit persons to whom the Software is 
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package com.timboudreau.adhoc.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Icon;
import javax.swing.text.Document;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import static org.netbeans.spi.project.ActionProvider.COMMAND_COPY;
import static org.netbeans.spi.project.ActionProvider.COMMAND_DELETE;
import static org.netbeans.spi.project.ActionProvider.COMMAND_MOVE;
import static org.netbeans.spi.project.ActionProvider.COMMAND_RENAME;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.netbeans.spi.project.MoveOperationImplementation;
import org.netbeans.spi.project.ProjectConfiguration;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.netbeans.spi.project.ui.support.ProjectActionPerformer;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
public class AdhocProject implements Project,
        FileOwnerQueryImplementation, ProjectConfiguration,
        ProjectActionPerformer, ActionProvider, LogicalViewProvider /*, CodeStylePreferences.Provider */ {

    private FileObject dir;
    private final ProjectState state;
    private final AuxPropertiesImpl aux = new AuxPropertiesImpl();
    private final CopyMoveRenameDelete ops = new CopyMoveRenameDelete();
    private final EncQueryImpl encodingQuery;
    public static final String CUSTOMIZE_COMMAND = "customize";
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);
    private final PI info = new PI();
    private final AdhocProjectCustomizerProvider customizer = new AdhocProjectCustomizerProvider(this);
    public static final String TYPE_NAME = "com-timboudreau-adhoc-project";

    public AdhocProject(FileObject dir, ProjectState state) throws IOException {
        this.encodingQuery = new EncQueryImpl();
        this.dir = dir;
        this.state = state;
    }

    @Override
    public FileObject getProjectDirectory() {
        return dir;
    }

    @Override
    public Lookup getLookup() {
        return Lookups.fixed(this, aux, encodingQuery, ops, info, customizer);
    }

    @Override
    public String getDisplayName() {
        return info.getDisplayName();
    }

    private class PI implements ProjectInformation {

        private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

        @Override
        public String getName() {
            return dir.getName();
        }

        @Override
        public String getDisplayName() {
            try {
                Preferences p = preferences(false);
                if (p == null) {
                    return getName();
                }
                String s = p.get("name", getName());
                return s.trim().isEmpty() ? getName() : s.trim();
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
            }
            return getName();
        }

        @Override
        public Icon getIcon() {
            return ImageUtilities.loadImageIcon("com/timboudreau/adhoc/project/adhoc.png", false);
        }

        @Override
        public Project getProject() {
            return AdhocProject.this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener pl) {
            supp.addPropertyChangeListener(pl);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener pl) {
            supp.removePropertyChangeListener(pl);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener pl) {
        supp.addPropertyChangeListener(pl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pl) {
        supp.removePropertyChangeListener(pl);
    }

    @Override
    public boolean enable(Project prjct) {
        return prjct.getLookup().lookup(AdhocProject.class) != null;
    }

    @Override
    public void perform(Project prjct) {
    }

    @Override
    public String[] getSupportedActions() {
        return new String[]{
            COMMAND_DELETE, COMMAND_MOVE, COMMAND_COPY, COMMAND_RENAME, null, CUSTOMIZE_COMMAND
        };
    }

    @Override
    public void invokeAction(String string, Lookup lkp) throws IllegalArgumentException {
        switch (string) {
            case COMMAND_MOVE:
                DefaultProjectOperations.performDefaultMoveOperation(this);
                break;
            case COMMAND_DELETE:
                DefaultProjectOperations.performDefaultDeleteOperation(this);
                break;
            case COMMAND_COPY:
                DefaultProjectOperations.performDefaultCopyOperation(this);
                break;
            case COMMAND_RENAME:
                DialogDescriptor.InputLine line = new DialogDescriptor.InputLine("New Name", "Rename Project");
                if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(line))) {
                    try {
                        String txt = line.getInputText().trim();
                        if (info.getName().equals(txt) || txt.isEmpty() || txt.indexOf('\\') >= 0 || txt.indexOf(':') >= 0 || txt.indexOf('/') >= 0 || txt.indexOf(';') >= 0) {
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Bad characters in name"));
                            return;
                        }
                        FileObject parent = getProjectDirectory().getParent();
                        if (parent.getFileObject(txt) != null) {
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                                    "A folder with that name already exists\n" + txt));
                            return;
                        }
                        FileObject fo = getProjectDirectory();
                        FileLock lock = fo.lock();
                        Preferences old = preferences(true);
                        try {
                            fo.rename(lock, txt, null);
                            FileObject nue = parent.getFileObject(txt);
                            assert nue != null;
                            AdhocProjectFactory.mark(nue);
                            dir = nue;
                            Preferences nuPrefs = preferences(true);
                            copyPreferences(old, nuPrefs);
                        } finally {
                            lock.releaseLock();
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (BackingStoreException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                break;
            case CUSTOMIZE_COMMAND:
                customizer.showCustomizer();
                break;
        }
    }

    @Override
    public boolean isActionEnabled(String string, Lookup lkp) throws IllegalArgumentException {
        switch (string) {
            case COMMAND_MOVE:
            case COMMAND_DELETE:
            case COMMAND_COPY:
            case COMMAND_RENAME:
                return getProjectDirectory().isValid() && getProjectDirectory().canRead() && getProjectDirectory().canWrite();
            case CUSTOMIZE_COMMAND:
                return true;

        }
        //do nothing
        return false;
    }

    public void setEncoding(Charset charset) {
        try {
            String name = charset == null ? "UTF-8" : charset.name();
            preferences(true).put("charset", name);
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public Charset getEncoding() {
        try {
            Preferences p = preferences(false);
            if (p != null) {
                String name = p.get("charset", "UTF-8");
                return Charset.forName(name);
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        return Charset.forName("UTF-8");
    }

    private String prefsNodeName() {
        String nodeName = ";;" + getProjectDirectory().getPath().replace(
                '/', '_').replace('\\', '_');
        return nodeName;
    }

    private String prefsNodeNameAfterRename(String fn) {
        String s = ";;" + getProjectDirectory().getParent().getPath().replace('/', '_').replace('\\', '_');
        s += '/' + fn.replace('/', '_').replace('\\', '_');
        return s;
    }

    private void copyPreferences(Preferences orig, Preferences into) throws BackingStoreException {
        for (String s : orig.keys()) {
            into.put(s, orig.get(s, null));
        }
        for (String kid : orig.childrenNames()) {
            Preferences child = orig.node(kid);
            Preferences newChild = into.node(kid);
            copyPreferences(child, newChild);
        }
    }

    Preferences preferences(boolean create) throws BackingStoreException {
        Preferences prefs = NbPreferences.forModule(AdhocProjectNode.class);
        String n = prefsNodeName();
        if (prefs.nodeExists(n) || create) {
            Preferences forProject = prefs.node(n);
            return forProject;
        }
        return null;
    }

    void clearFavorites() {
        try {
            Preferences forProject = preferences(false);
            if (forProject != null) {
                forProject.clear();
                forProject.removeNode();
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    void saveFavorites(Iterable<Favorite> items) {
        try {
            Preferences forProject = preferences(true);
            for (Favorite fav : items) {
                Preferences forFile = forProject.node(fav.path().
                        replace('/', '_').replace('\\', '_'));
                forFile.put("name", fav.path());
                forFile.putInt("value", fav.count);
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public List<Favorite> favorites() {
        List<Favorite> favorites = new ArrayList<>();
        try {
            Preferences forProject = preferences(false);
            if (forProject != null) {
                String[] s = forProject.childrenNames();
                for (String ch : s) {
                    Preferences forFile = forProject.node(ch);
                    if (forFile != null) {
                        int val = forFile.getInt("value", 0);
                        String name = forFile.get("name", null);
                        if (val > 0 && name != null) {
                            Favorite fav = new Favorite(val, name);
                            favorites.add(fav);
                        }
                    }
                }
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
        Collections.sort(favorites);
        return favorites;
    }

//    @Override
    public Preferences forFile(FileObject fo, String string) {
        try {
            Preferences p = preferences(true);
            return p.node("__formatting").node(string);
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
            return NbPreferences.forModule(AdhocProject.class);
        }
    }

//    @Override
    public Preferences forDocument(Document dcmnt, String string) {
        return forFile(null, string);
    }

    @Override
    public Project getOwner(URI uri) {
        FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(new File(uri)));
        return fo == null ? null : getOwner(fo);
    }

    @Override
    public Project getOwner(FileObject fo) {
        FileObject check = fo;
        do {
            if (AdhocProjectFactory.check(check)) {
                if (check.equals(getProjectDirectory())) {
                    return this;
                } else {
                    return null;
                }
            }
            check = check.getParent();
        } while (check != null);
        return null;
    }

    public int getMaxFavorites() {
        try {
            return preferences(true).getInt("maxFavorites", 20);
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
            return 20;
        }
    }

    public int getFavoriteUsageCount() {
        try {
            return preferences(true).getInt("favoriteUsageCount", 1);
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
            return 1;
        }
    }

    public void setFavoriteUsageCount(int val) {
        try {
            int old = getFavoriteUsageCount();
            if (old != val) {
                preferences(true).putInt("favoriteUsageCount", val);
                supp.firePropertyChange("favoriteUsageCount", old, val);
                refreshFavorites();
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void setMaxFavorites(int val) {
        try {
            int old = getMaxFavorites();
            if (old != val) {
                preferences(true).putInt("maxFavorites", val);
                refreshFavorites();
                supp.firePropertyChange("maxFavorites", old, val);
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void setDisplayName(String name) {
        try {
            String old = getDisplayName();
            if (!old.equals(name)) {
                preferences(true).put("name", name);
                supp.firePropertyChange(ProjectInformation.PROP_DISPLAY_NAME, old, name);
                info.supp.firePropertyChange(ProjectInformation.PROP_DISPLAY_NAME, old, name);
            }
        } catch (BackingStoreException bse) {
            Exceptions.printStackTrace(bse);
        }
    }

    private class CopyMoveRenameDelete implements CopyOperationImplementation, DeleteOperationImplementation, MoveOperationImplementation {

        @Override
        public void notifyCopying() throws IOException {
            //do nothing
        }

        @Override
        public void notifyCopied(Project prjct, File file, String string) throws IOException {
            //do nothing
            AdhocProject p = prjct.getLookup().lookup(AdhocProject.class);
            if (p != null) {
                try {
                    Preferences originalPreferences = p.preferences(false);
                    if (originalPreferences != null) {
                        Preferences myPreferences = preferences(true);
                        copyPreferences(originalPreferences, myPreferences);
                    }
                } catch (BackingStoreException ex) {
                    throw new IOException(ex);
                }
            }
        }

        @Override
        public List<FileObject> getMetadataFiles() {
            return Collections.emptyList();
        }

        @Override
        public List<FileObject> getDataFiles() {
            List<FileObject> all = new ArrayList<>();
            return traverse(getProjectDirectory(), all);
        }

        private List<FileObject> traverse(FileObject base, List<FileObject> all) {
            for (FileObject fo : base.getChildren()) {
                all.add(fo);
                if (fo.isFolder() && !all.contains(fo)) {
                    traverse(fo, all);
                }
            }
            return all;
        }

        @Override
        public void notifyDeleting() throws IOException {
            //do nothing
        }

        @Override
        public void notifyDeleted() throws IOException {
            try {
                Preferences p = preferences(false);
                if (p != null) {
                    p.removeNode();
                }
            } catch (BackingStoreException ex) {
                throw new IOException(ex);
            }
        }

        @Override
        public void notifyMoving() throws IOException {
            //do nothing
        }

        @Override
        public void notifyMoved(Project prjct, File file, String string) throws IOException {
            notifyCopied(prjct, file, string);
        }
    }

    private class EncQueryImpl extends FileEncodingQueryImplementation {

        @Override
        public Charset getEncoding(FileObject fo) {
            return AdhocProject.this.getEncoding();
        }
    }

    private class AuxPropertiesImpl implements AuxiliaryProperties {

        private Preferences p(boolean shared) {
            if (!shared) {
                try {
                    return preferences(true).node("__aux");
                } catch (BackingStoreException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return NbPreferences.forModule(AuxPropertiesImpl.class);
        }

        @Override
        public String get(String string, boolean bln) {
            return p(bln).get(string, null);
        }

        @Override
        public void put(String string, String string1, boolean bln) {
            Preferences prefs = p(bln);
            if (string1 == null) {
                prefs.remove(string);
            } else {
                prefs.put(string, string1);
            }
        }

        @Override
        public Iterable<String> listKeys(boolean bln) {
            try {
                return Arrays.asList(p(bln).childrenNames());
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
                return Collections.emptySet();
            }
        }
    }
    private Reference<AdhocProjectNode> logicalView;

    @Override
    public Node createLogicalView() {
        AdhocProjectNode view = logicalView == null ? null
                : logicalView.get();
        if (view == null) {
            try {
                view = new AdhocProjectNode(this);
                logicalView = new WeakReference<>(view);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return view == null ? Node.EMPTY : view;
    }

    void refreshFavorites() {
        AdhocProjectNode view = logicalView == null ? null
                : logicalView.get();

        if (view != null) {
            view.refreshFavorites();
        }
    }

    @Override
    public Node findPath(Node node, Object o) {
        AdhocProjectNode view = logicalView == null ? null
                : logicalView.get();
        if (view != null) {
            view.findPath(node, o);
        }
        return null;
    }
}
