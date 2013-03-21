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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Tim Boudreau
 */
public class AdhocProjectNode extends FilterNode implements LogicalViewProvider, PropertyChangeListener {

    private List<Runnable> onRefreshFavorites = new ArrayList<>();
    private final AdhocProject prj;
    private final FavoritesNode accessed;
    private final SourcesNode sources;

    public AdhocProjectNode(AdhocProject prj) throws DataObjectNotFoundException {
        this(prj, DataObject.find(prj.getProjectDirectory()));
    }

    AdhocProjectNode(AdhocProject prj, DataObject dob) throws DataObjectNotFoundException {
        super(dob.getNodeDelegate(), new Children.Array(), new ProxyLookup(Lookups.fixed(prj), dob.getLookup()));
        this.prj = prj;
        getChildren().add(new Node[]{accessed =
            new FavoritesNode(prj, onRefreshFavorites),
            sources = new SourcesNode(prj, onRefreshFavorites)});
        prj.addPropertyChangeListener(WeakListeners.propertyChange(this, prj));
    }

    void refreshFavorites() {
        for (Runnable r : onRefreshFavorites) {
            r.run();
        }
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{
            CommonProjectActions.newFileAction(),
            null,
            CommonProjectActions.copyProjectAction(),
            CommonProjectActions.renameProjectAction(),
            CommonProjectActions.moveProjectAction(),
            CommonProjectActions.deleteProjectAction(),
            null,
            CommonProjectActions.setAsMainProjectAction(),
            CommonProjectActions.closeProjectAction(),
            null,
            CommonProjectActions.customizeProjectAction(),};
    }

    @Override
    public String getDisplayName() {
        return prj.getDisplayName();
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("com/timboudreau/adhoc/project/adhoc.png", false);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public Node createLogicalView() {
        return this;
    }

    @Override
    public String getShortDescription() {
        return "Ad-Hoc Project in " + prj.getProjectDirectory().getPath();
    }

    @Override
    public Node findPath(Node root, Object target) {
        if (target instanceof Node) {
            target = ((Node) target).getLookup().lookup(DataObject.class);
        }
        if (target instanceof DataObject) {
            target = ((DataObject) target).getPrimaryFile();
        }
        if (target instanceof FileObject) {
            FileObject t = (FileObject) target;
            return recurseFindChild(Collections.<Node>singleton(sources), t, 0);
        }
        return null;
    }

    private Node recurseFindChild(Iterable<Node> folders, FileObject target, int depth) {
        FileObject par = target.getParent();
        List<Node> next = new ArrayList<>();
        for (Node fld : folders) {
            DataObject dob = fld.getLookup().lookup(DataObject.class);
            if (dob != null) {
                if (par.equals(dob.getPrimaryFile())) {
                    for (Node nn : fld.getChildren().getNodes(true)) {
                        DataObject d1 = nn.getLookup().lookup(DataObject.class);
                        if (d1 != null && d1.getPrimaryFile().equals(target)) {
                            return nn;
                        } else if (d1 instanceof DataFolder) {
                            next.add(nn);
                        }
                    }
                } else {
                    for (Node nn : fld.getChildren().getNodes(true)) {
                        DataFolder nx = nn.getLookup().lookup(DataFolder.class);
                        if (nx != null) {
                            next.add(nn);
                        }
                    }
                }
            }
        }
        if (!next.isEmpty() && depth < 10) {
            return recurseFindChild(next, target, depth + 1);
        }
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce) {
        if (pce != null) {
            if (ProjectInformation.PROP_DISPLAY_NAME.equals(pce.getPropertyName())) {
                fireDisplayNameChange(pce.getOldValue() + "", pce.getNewValue() + "");
            } else {
                refreshFavorites();
            }
        } else {
            refreshFavorites();
        }
    }

    private static class WeakRunnable implements Runnable {

        private final Reference<Runnable> ref;

        WeakRunnable(Runnable r) {
            ref = new WeakReference<>(r);
        }

        @Override
        public void run() {
            Runnable r = ref.get();
            if (r != null) {
                r.run();
            }
        }
    }

    static class FavoritesNode extends AbstractNode {

        private final AdhocProject prj;
        private final List<Runnable> run;

        FavoritesNode(AdhocProject prj, List<Runnable> run) {
            super(Children.create(new FavoritesChildren(prj, run), true), prj.getLookup());
            this.prj = prj;
            this.run = run;
            setDisplayName("Favorites");
            setName("Favorites");
            setIconBaseWithExtension("com/timboudreau/adhoc/project/fav.png");
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{new ClearAction()};
        }

        @Override
        public Action getPreferredAction() {
            return null;
        }

        private class ClearAction extends AbstractAction {

            ClearAction() {
                putValue(NAME, "Clear Favorites");
            }

            @Override
            public void actionPerformed(ActionEvent ae) {
                prj.clearFavorites();
                for (Runnable r : run) {
                    r.run();
                }
            }
        }

        final static class FavoritesChildren extends ChildFactory<Favorite> implements Runnable {

            private final AdhocProject prj;

            @SuppressWarnings("LeakingThisInConstructor")
            FavoritesChildren(AdhocProject prj, List<Runnable> run) {
                this.prj = prj;
                run.add(new WeakRunnable(this));
            }

            @Override
            protected boolean createKeys(List<Favorite> list) {
                List<Favorite> all = prj.favorites();
                int max = prj.getMaxFavorites();

                if (all.size() > max) {
                    all = all.subList(0, max);
                }
                int min = prj.getFavoriteUsageCount();
                for (Iterator<Favorite> it = all.iterator(); it.hasNext();) {
                    if (it.next().count < min) {
                        it.remove();
                    }
                }
                list.addAll(all);
                return true;
            }

            @Override
            protected Node createNodeForKey(Favorite favorite) {
                if (favorite.count <= 0) {
                    // Things that were removed
                    return null;
                }
                FileObject fo = prj.getProjectDirectory().getFileObject(favorite.path());
                if (fo != null && fo.isValid()) {
                    try {
                        boolean h = !prj.getProjectDirectory().equals(fo.getParent());
                        FN fn = new FN(DataObject.find(fo).getNodeDelegate(), h, this, prj);
                        fn.setDesc(favorite.path());
                        return fn;
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                return null;
            }

            @Override
            public void run() {
                refresh(false);
            }
        }
    }

    static class FN extends FilterNode {

        private final boolean useHtml;
        private final FavoritesNode.FavoritesChildren ch;
        private final AdhocProject prj;

        public FN(Node original, boolean useHtml, FavoritesNode.FavoritesChildren ch, AdhocProject prj) {
            super(original, original.getChildren() == Children.LEAF ? Children.LEAF : new FilterNode.Children(original));
            this.prj = prj;
            this.ch = ch;
            this.useHtml = useHtml;
            disableDelegation(DELEGATE_SET_SHORT_DESCRIPTION);
            disableDelegation(DELEGATE_GET_SHORT_DESCRIPTION);
            disableDelegation(DELEGATE_GET_NAME);
            disableDelegation(DELEGATE_SET_NAME);
            disableDelegation(DELEGATE_DESTROY);
            setName(original.getName());
        }

        void setDesc(String desc) {
            setShortDescription(desc);
        }

        @Override
        public Action[] getActions(boolean context) {
            Action[] result = super.getActions(context);
            Action[] nue = new Action[result.length + 2];
            System.arraycopy(result, 0, nue, 0, result.length);
            nue[nue.length - 1] = new RemoveFromFavorites();
            return nue;
        }

        private class RemoveFromFavorites extends AbstractAction {

            RemoveFromFavorites() {
                putValue(NAME, "Remove from Favorites");
            }

            @Override
            public void actionPerformed(ActionEvent ae) {
                FileObject fo = getLookup().lookup(DataObject.class).getPrimaryFile();
                String rp = FileUtil.getRelativePath(prj.getProjectDirectory(), fo);
                if (rp != null) {
                    Iterable<Favorite> favs = prj.favorites();
                    boolean found = false;
                    for (Favorite f : favs) {
                        if (f.path().equals(rp)) {
                            found = true;
                            f.count = -4;
                            break;
                        }
                    }
                    if (found) {
                        prj.saveFavorites(favs);
                        FN.this.ch.run();
                    }
                }
            }
        }

        @Override
        public String getHtmlDisplayName() {
            if (!useHtml) {
                return super.getHtmlDisplayName();
            }
            DataObject dob = getOriginal().getLookup().lookup(DataObject.class);
            if (dob != null) {
                StringBuilder sb = new StringBuilder(getDisplayName());
                sb.append(" <font color=\"!controlShadow\">");
                sb.append('(');
                sb.append(dob.getPrimaryFile().getParent().getName());
                sb.append(')');
                return sb.toString();
            } else {
                return super.getHtmlDisplayName();
            }
        }
    }

    private static class SourcesNode extends FilterNode {

        public SourcesNode(AdhocProject prj, List<Runnable> run) throws DataObjectNotFoundException {
            this(prj, DataObject.find(prj.getProjectDirectory()), run);
            disableDelegation(DELEGATE_SET_DISPLAY_NAME);
            disableDelegation(DELEGATE_SET_NAME);
            disableDelegation(DELEGATE_GET_NAME);
            disableDelegation(DELEGATE_GET_DISPLAY_NAME);
            setDisplayName("Sources");
            setName("Sources");
        }

        SourcesNode(AdhocProject prj, DataObject dob, List<Runnable> run) {
            super(dob.getNodeDelegate(),
                    new ProxyOpenFilterChildren(prj, dob.getNodeDelegate(), run),
                    new ProxyLookup(prj.getLookup(), dob.getLookup()));
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage("com/timboudreau/adhoc/project/AdhocProject.png", false);
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        static class ProxyOpenFilterChildren extends FilterNode.Children {

            private final AdhocProject prj;
            private final List<Runnable> runs;

            public ProxyOpenFilterChildren(AdhocProject prj, Node or, List<Runnable> runs) {
                super(or);
                this.runs = runs;
                this.prj = prj;
            }

            @Override
            protected Node[] createNodes(Node key) {
                return new Node[]{new FN(key)};
            }

            private void updateFavorites(Node node) {
                DataObject dob = node.getLookup().lookup(DataObject.class);
                if (dob != null) {
                    String relPath = FileUtil.getRelativePath(prj.getProjectDirectory(), dob.getPrimaryFile());
                    if (relPath != null) {
                        Set<Favorite> it = new HashSet<>(prj.favorites());
                        boolean found = false;
                        for (Favorite item : it) {
                            if (item.relPath.equals(relPath)) {
                                item.count++;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            it.add(new Favorite(1, relPath));
                        }
                        prj.saveFavorites(it);
                        for (Runnable r : this.runs) {
                            r.run();
                        }
                    }
                }
            }

            private class FN extends FilterNode {

                public FN(Node original) {
                    super(original, original.getChildren() == Children.LEAF
                            ? Children.LEAF
                            : new ProxyOpenFilterChildren(prj, original, runs));
                }

                @Override
                public Action[] getActions(boolean context) {
                    Action[] result = super.getActions(context); //To change body of generated methods, choose Tools | Templates.
                    if (result.length > 0) {
                        Action[] nue = new Action[result.length + 2];
                        System.arraycopy(result, 0, nue, 0, result.length);
                        nue[0] = wrapAction(result[0]);
                        nue[nue.length - 1] = new PutInFavoritesAction();
                        return nue;
                    }
                    return new Action[]{new PutInFavoritesAction()};
                }

                @Override
                public Action getPreferredAction() {
                    Action a = super.getPreferredAction();
                    return a == null ? null : wrapAction(super.getPreferredAction());
                }

                class PutInFavoritesAction extends AbstractAction {

                    PutInFavoritesAction() {
                        putValue(NAME, "Put In Favorites");
                    }

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        updateFavorites(FN.this);
                    }
                }

                private Action wrapAction(Action a) {
                    if (a instanceof ContextAwareAction) {
                        ContextAwareAction aa = (ContextAwareAction) a;
                        return new WrapperAction(aa);
                    }
                    return new SimpleWrapper(a);
                }

                private class SimpleWrapper extends AbstractAction {

                    private final Action delegate;

                    public SimpleWrapper(Action delegate) {
                        this.delegate = delegate;
                    }

                    @Override
                    public Object getValue(String string) {
                        return delegate.getValue(string);
                    }

                    @Override
                    public void putValue(String string, Object o) {
                        delegate.putValue(string, o);
                    }

                    @Override
                    public void setEnabled(boolean bln) {
                        delegate.setEnabled(bln);
                    }

                    @Override
                    public boolean isEnabled() {
                        return delegate.isEnabled();
                    }

                    @Override
                    public void addPropertyChangeListener(PropertyChangeListener pl) {
                        delegate.addPropertyChangeListener(pl);
                    }

                    @Override
                    public void removePropertyChangeListener(PropertyChangeListener pl) {
                        delegate.removePropertyChangeListener(pl);
                    }

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        delegate.actionPerformed(ae);
                        updateFavorites(FN.this);
                    }
                }

                private class WrapperAction extends AbstractAction implements ContextAwareAction {

                    private final ContextAwareAction delegate;

                    public WrapperAction(ContextAwareAction delegate) {
                        this.delegate = delegate;
                    }

                    @Override
                    public Action createContextAwareInstance(Lookup lkp) {
                        Action result = delegate.createContextAwareInstance(lkp);
                        if (result instanceof WrapperAction) {
                            ContextAwareAction aa = (ContextAwareAction) result;
                            return new WrapperAction(aa);
                        } else {
                            return new SimpleWrapper(result);
                        }
                    }

                    @Override
                    public Object getValue(String string) {
                        return delegate.getValue(string);
                    }

                    @Override
                    public void putValue(String string, Object o) {
                        delegate.putValue(string, o);
                    }

                    @Override
                    public void setEnabled(boolean bln) {
                        delegate.setEnabled(bln);
                    }

                    @Override
                    public boolean isEnabled() {
                        return delegate.isEnabled();
                    }

                    @Override
                    public void addPropertyChangeListener(PropertyChangeListener pl) {
                        delegate.addPropertyChangeListener(pl);
                    }

                    @Override
                    public void removePropertyChangeListener(PropertyChangeListener pl) {
                        delegate.removePropertyChangeListener(pl);
                    }

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        delegate.actionPerformed(ae);
                        updateFavorites(FN.this);
                    }
                }
            }
        }
    }
}
