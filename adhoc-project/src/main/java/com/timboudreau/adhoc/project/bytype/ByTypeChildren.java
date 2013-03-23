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
package com.timboudreau.adhoc.project.bytype;

import com.timboudreau.adhoc.project.FavoritesTrackingNodeFactory;
import com.timboudreau.adhoc.project.bytype.ByTypeChildren.MimeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

/**
 * Builds a subtree of mime types with nodes for each type under them
 *
 * @author Tim Boudreau
 */
public class ByTypeChildren extends Children.Keys<MimeType> {

    private volatile boolean attached;
    private static final RequestProcessor rp = new RequestProcessor(ByTypeChildren.class.getSimpleName(), 2);
    private final R r = new R();
    private final RequestProcessor.Task task = rp.create(r);
    private FileObject root;
    private final Set<MimeType> types = Collections.synchronizedSet(new HashSet<MimeType>());
    public static final int MAX_DEPTH = 12;
    private final FavoritesTrackingNodeFactory factory;

    public ByTypeChildren(FileObject root, FavoritesTrackingNodeFactory factory) {
        this.root = root;
        this.factory = factory;
    }

    public synchronized FileObject getRoot() {
        return root;
    }

    public synchronized void setRoot(FileObject root) {
        this.root = root;
    }

    public void addNotify() {
        attached = true;
        task.schedule(120);
    }

    public void removeNotify() {
        attached = false;
        task.cancel();
    }

    @Override
    protected Node[] createNodes(MimeType t) {
        Children kids = Children.create(new FileFinder(t), true);
        AbstractNode result = new AbstractNode(kids, Lookups.fixed(t));
        result.setName(t.type);
        result.setDisplayName(t.toString());
        result.setShortDescription(t.type);
        result.setIconBaseWithExtension("com/timboudreau/adhoc/project/type.png");
        return new Node[]{ factory.createNode(result)};
    }

    private final class R implements Runnable, FileObjectVisitor<Set<MimeType>> {

        public void run() {
            visit(getRoot(), 0, MAX_DEPTH, this, types);
            List<MimeType> all = new ArrayList<>(types);
            Collections.sort(all);
            setKeys(all);
        }

        @Override
        public boolean visitFileObject(FileObject ob, Set<MimeType> r) {
            r.add(new MimeType(ob.getMIMEType()));
            return attached;
        }
    }

    private final class FileFinder extends ChildFactory.Detachable<FileObject> implements Comparator<FileObject>, FileObjectVisitor<List<FileObject>> {

        private final MimeType mt;
        private volatile boolean finderAttached;

        FileFinder(MimeType mt) {
            this.mt = mt;
        }

        @Override
        protected void addNotify() {
            finderAttached = true;
        }

        @Override
        protected void removeNotify() {
            finderAttached = false;
        }

        @Override
        protected Node createNodeForKey(FileObject key) {
            try {
                DataObject ob = DataObject.find(key);
                return new RelativePathNode(ob.getNodeDelegate());
            } catch (DataObjectNotFoundException ex) {
                Logger.getLogger(FileFinder.class.getName()).log(Level.INFO,
                        "File disappeared: {0}", key.getPath());
                return null;
            }
        }
        
        @Override
        protected boolean createKeys(List<FileObject> list) {
            FileObject root = getRoot();
            visit(root, 0, MAX_DEPTH, this, list);
            Collections.sort(list, this);
            return true;
        }

        @Override
        public boolean visitFileObject(FileObject ob, List<FileObject> r) {
            if (new MimeType(ob.getMIMEType()).equals(mt)) {
                r.add(ob);
            }
            return attached && finderAttached;
        }

        @Override
        public int compare(FileObject t, FileObject t1) {
            return t.getName().compareToIgnoreCase(t1.getName());
        }
    }

    private <R> boolean visit(FileObject root, int depth, int maxDepth, FileObjectVisitor<R> v, R arg) {
        if (depth == maxDepth || !root.isValid()) {
            return false;
        }
        if (root.isData() && root.canRead() && root.isValid()) {
            boolean result = v.visitFileObject(root, arg);
            if (!result) {
                return result;
            }
        }
        if (depth != maxDepth - 1 && root.isFolder() && root.canRead() && root.isValid()) {
            for (FileObject fo : root.getChildren()) {
                boolean result = visit(fo, depth + 1, maxDepth, v, arg);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }
    
    private class RelativePathNode extends FilterNode {
        RelativePathNode(Node orig) {
            super (orig);
        }

        @Override
        public String getHtmlDisplayName() {
            FileObject fo = getLookup().lookup(DataObject.class).getPrimaryFile().getParent();
            if (fo.equals(root)) {
                return super.getHtmlDisplayName();
            }
            return getDisplayName() + "<font color=\"!controlShadow\"> (" + fo.getNameExt() + ')';
        }
    }

    interface FileObjectVisitor<R> {

        boolean visitFileObject(FileObject ob, R r);
    }

    static class MimeType implements Comparable<MimeType> {

        public final String type;

        public MimeType(String type) {
            Parameters.notNull("type", type);
            this.type = type;
        }

        public String type() {
            return type;
        }

        public String toString() {
            switch (type) {
                case "text/x-java":
                    return "Java";
                case "application/javascript":
                case "text/x-javascript":
                case "text/javascript":
                    return "Javascript";
                case "text/html":
                    return "HTML";
                case "text/xml":
                    return "XML";
                case "text/plain":
                    return "Text";
                case "application/unknown":
                case "content/unknown":
                    return "Unknown";
                case "application/pdf":
                    return "PDFs";
                case "image/gif":
                case "image/png":
                case "image/jpeg":
                case "image/svg":
                case "image/svg+xml":
                case "image/tiff":
                    return "Images";
            }
            if (type.startsWith("text/xml")) {
                return "XML";
            }
            if ((type.startsWith("text/") && type.length() > 5) || (type.startsWith("application/") && type.length() > "application/".length())) {
                String sub = type.substring(type.indexOf("/") + 1);
                if (sub.startsWith("x-") && sub.length() > 2) {
                    sub = sub.substring(2);
                }
                StringBuilder sb = new StringBuilder(sub);
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                return sb.toString();
            }
            return type;
        }

        public boolean equals(Object o) {
            return o instanceof MimeType && o.toString().equals(toString());
        }

        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public int compareTo(MimeType t) {
            String a = toString();
            String b = t.toString();
            boolean aKnown = a.indexOf('/') > 0;
            boolean bKnown = b.indexOf('/') > 0;
            if (aKnown == bKnown) {
                return a.compareToIgnoreCase(b);
            } else {
                return aKnown ? 1 : -1;
            }
        }
    }
}
