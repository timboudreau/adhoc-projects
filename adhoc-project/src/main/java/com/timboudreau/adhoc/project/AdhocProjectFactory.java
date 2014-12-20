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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
import org.openide.util.WeakSet;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProviders(value = {
    @ServiceProvider(service = ProjectFactory.class),
    @ServiceProvider(service = ProjectFactory2.class)})
public class AdhocProjectFactory implements ProjectFactory, ProjectFactory2 {

    private static Set<AdhocProject> cache = new WeakSet<>();
    private static long lastFetch = 0;
    private static final Set<String> all = Collections.synchronizedSet(new HashSet<String>());

    private static Preferences projectsList(Flusher f) {
        Preferences pp = NbPreferences.forModule(AdhocProjectFactory.class);
        Preferences all = pp.node("__projects");
        f.add(pp);
        f.add(all);
        return all;
    }

    private static Preferences forProject(Flusher f, FileObject fo) {
        Preferences res = projectsList(f).node(toNodeName(fo));
        f.add(res);
        return res;
    }

    private static String toNodeName(FileObject fo) {
        return fo.getPath().replace('/', '~').replace('\\', '!').replace(':', '~');
    }

    private static class Flusher {

        private final List<Preferences> all = new ArrayList<>();

        void add(Preferences prefs) {
            all.add(prefs);
        }

        void flush() {
            int max = all.size() - 1;
            for (int i = max; i >= 0; i--) {
                try {
                    all.get(i).flush();
                } catch (BackingStoreException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private static synchronized Set<String> knownProjects() {
        if (System.currentTimeMillis() - lastFetch > 60000) {
            lastFetch = System.currentTimeMillis();
            Set<String> result = new HashSet<>();
            try {
                Preferences allNodes = projectsList(new Flusher());
                String[] kids = allNodes.childrenNames();
                for (String s : kids) {
                    Preferences n = allNodes.node(s);
                    if (n.getBoolean("alive", false)) {
                        result.add(s);
                    }
                }
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
                return Collections.emptySet();
            }
//            all.clear();
            all.addAll(result);
        }
        return all;
    }

    @Override
    public boolean isProject(FileObject fo) {
        return check(fo);
    }

    static boolean check(FileObject fo) {
        boolean result = fo == null ? false : knownProjects().contains(toNodeName(fo)) && fo.isFolder();
        return result;
    }

    static void mark(FileObject fo) throws IOException {
        Flusher f = new Flusher();
        Preferences mine = forProject(f, fo);
        mine.putBoolean("alive", true);
        try {
            synchronized (AdhocProjectFactory.class) {
                all.add(toNodeName(fo));
            }
        } finally {
            f.flush();
        }
        ProjectManager.getDefault().clearNonProjectCache();
    }

    static AdhocProject findLiveOwner(FileObject fo) {
        do {
            if (check(fo)) {
                try {
                    for (AdhocProject a : cache) {
                        if (a != null) {
                            if (a.getProjectDirectory().equals(fo)) {
                                return a;
                            }
                        }
                    }
                    Project pp = ProjectManager.getDefault().findProject(fo);
                    if (pp != null) {
                        AdhocProject result = (AdhocProject) pp.getLookup().lookup(AdhocProject.class);
                        if (result != null) {
                            return result;
                        }
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                }

            }
            fo = fo.getParent();
        } while (fo != null);
        return null;
    }

    @Override
    public Project loadProject(FileObject fo, ProjectState ps) throws IOException {
        if (fo == null) {
            return null;
        }
        for (AdhocProject p : cache) {
            if (p != null) {
                if (p.getProjectDirectory().equals(fo)) {
                    return p;
                }
            }
        }
        if (isProject(fo)) {
            AdhocProject result = new AdhocProject(fo, ps);
            cache.add(result);
            return result;
        }
        return null;
    }

    @Override
    public void saveProject(Project prjct) throws IOException, ClassCastException {
        //do nothing
    }

    @Override
    public ProjectManager.Result isProject2(FileObject fo) {
        if (fo == null) {
            return null;
        }
        if (check(fo)) {
            return new ProjectManager.Result(ImageUtilities.loadImageIcon("com/timboudreau/adhoc/project/adhoc.png", true));
        }
        return null;
    }
}
