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
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
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

    @Override
    public boolean isProject(FileObject fo) {
        return check(fo);
    }

    static boolean check(FileObject fo) {
        if (fo == null) {
            return false;
        }
        return fo.isFolder() && "adhoc".equals(fo.getAttribute("adhocProject"));
    }

    static void mark(FileObject fo) throws IOException {
        fo.setAttribute("adhocProject", "adhoc");
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
//        for (AdhocProject p : cache) {
//            if (p != null) {
//                if (p.getProjectDirectory().equals(fo)) {
//                    return p;
//                }
//            }
//        }
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
        if ("adhoc".equals(fo.getAttribute("adhocProject"))) {
            return new ProjectManager.Result(ImageUtilities.loadImageIcon("com/timboudreau/adhoc/project/adhoc.png", true));
        }
        return null;
    }
}
