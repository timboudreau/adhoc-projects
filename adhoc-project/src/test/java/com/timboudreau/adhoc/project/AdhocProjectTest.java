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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.netbeans.spi.project.MoveOperationImplementation;
import org.netbeans.spi.project.ProjectConfiguration;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.ProjectActionPerformer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.NbPreferences;

/**
 *
 * @author tim
 */
public class AdhocProjectTest {

    private FileObject root;
    private AdhocProject project;
    private String name;
    private static int count = 1;
    private FileObject[] files = new FileObject[10];

    @Before
    public void setup() throws IOException, BackingStoreException {
        // clean up after any failed runs
        Preferences p1 = NbPreferences.forModule(AdhocProject.class);
        p1.removeNode();
        // Use a memory fs
        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject a = fs.getRoot().createFolder("a");
        FileObject b = a.createFolder("b");
        // Use timestamp in the name to make sure that repeated runs don't
        // pollute each others preferences
        name = "theProject_" + Long.toString(System.currentTimeMillis(), 36) + "_" + count++;
        root = b.createFolder(name);
        for (int i = 0; i < 10; i++) {
            FileObject someFile = root.createData("foo-" + i + ".js");
            files[i] = someFile;
        }
        project = new AdhocProject(root, new ProjectState() {
            @Override
            public void markModified() {
                //do nothing
            }

            @Override
            public void notifyDeleted() throws IllegalStateException {
                // do nothing
            }
        });
    }

    @After
    public void tearDown() throws BackingStoreException {
        if (project != null) {
            Preferences p = project.preferences(false);
            if (p != null) {
                p.removeNode();
            }
            Preferences p1 = NbPreferences.forModule(AdhocProject.class);
            p1.removeNode();
        }
    }

    @Test
    public void testGetProjectDirectory() {
        assertTrue(true);
    }

    @Test
    public void testGetLookup() {
        assertNotNull(project.getLookup().lookup(Project.class));
        assertNotNull(project.getLookup().lookup(AdhocProject.class));
        assertNotNull(project.getLookup().lookup(ProjectInformation.class));
        assertNotNull(project.getLookup().lookup(FileOwnerQueryImplementation.class));
        assertNotNull(project.getLookup().lookup(ProjectConfiguration.class));
        assertNotNull(project.getLookup().lookup(ProjectActionPerformer.class));
        assertNotNull(project.getLookup().lookup(ActionProvider.class));
        assertNotNull(project.getLookup().lookup(LogicalViewProvider.class));
        assertNotNull(project.getLookup().lookup(CustomizerProvider.class));
        assertNotNull(project.getLookup().lookup(CopyOperationImplementation.class));
        assertNotNull(project.getLookup().lookup(DeleteOperationImplementation.class));
        assertNotNull(project.getLookup().lookup(MoveOperationImplementation.class));
    }

    @Test
    public void testGetName() {
        assertEquals(name, project.getLookup().lookup(ProjectInformation.class).getName());
    }

    @Test
    public void testGetDisplayName() {
        assertEquals(name, project.getDisplayName());
    }

    @Test
    public void testGetProject() {
        assertSame(project, project.getLookup().lookup(ProjectInformation.class).getProject());
    }

    @Test
    public void testGetEncoding() {
        Charset ascii = Charset.forName("US-ASCII");
        Charset utf8 = Charset.forName("UTF-8");
        assertEquals(utf8, project.getEncoding());
        project.setEncoding(ascii);
        assertEquals(ascii, project.getEncoding());
    }

    @Test
    public void testFavorites() {
        java.util.List<Favorite> favs = new ArrayList<>();
        char[] chars = new char[]{'d', 'c', 'b', 'a'};
        int ix = 1;
        for (char c : chars) {
            favs.add(new Favorite(ix *= 10, "foo/" + c));
        }
        project.saveFavorites(favs);
        java.util.List<Favorite> nue = project.favorites();
        assertEquals(new HashSet<>(favs), new HashSet<>(nue));
    }

    @Test
    public void testConfiguration() {
        AuxiliaryProperties p = project.getLookup().lookup(AuxiliaryProperties.class);
        assertNotNull(p);
        String a = p.get("a", false);
        assertNull(a);
        a = p.get("a", true);
        assertNull(a);

        p.put("a", "shared", true);
        p.put("a", "unshared", false);

        assertEquals("shared", p.get("a", true));
        assertEquals("unshared", p.get("a", false));
    }

    @Test
    public void testFindPath() {
        AdhocProjectNode pn = (AdhocProjectNode) project.createLogicalView();
        assertNotNull(pn);
        Node[] nn = pn.getChildren().getNodes(true);
        assertNotNull(nn);
        assertEquals(3, nn.length);
        assertEquals("Favorites", nn[0].getName());
        assertEquals("byType", nn[1].getName());
        assertEquals("Sources", nn[2].getName());
        Node[] sources = nn[2].getChildren().getNodes(true);
        assertNotNull(sources);
        assertEquals(10, sources.length);

        Node found = pn.findPath(pn, files[3]);
        assertNotNull(found);
        assertEquals(files[3], found.getLookup().lookup(DataObject.class).getPrimaryFile());
        assertTrue(isAncestor(pn, found));

        FilterNode fn = new FilterNode(pn);
        found = pn.findPath(fn, files[3]);
        assertNotNull(found);
        assertEquals(files[3], found.getLookup().lookup(DataObject.class).getPrimaryFile());

        assertTrue(isAncestor(pn, found));
    }
    
    @Test
    public void testMinMax() {
        project.setMaxFavorites(52);
        assertEquals(52, project.getMaxFavorites());
        project.setFavoriteUsageCount(14);
        assertEquals(14, project.getFavoriteUsageCount());
        String old = project.getLookup().lookup(ProjectInformation.class).getDisplayName();
        assertNotSame("figpucker".intern(), old.intern());
        project.setDisplayName("figpucker");
        String nue = project.getLookup().lookup(ProjectInformation.class).getDisplayName();
        assertEquals("figpucker", nue);
    }

    private static boolean isAncestor(Node root, Node n) {
        if (n == null) {
            return false;
        }
        if (n == root) {
            return true;
        }
        return isAncestor(root, n.getParentNode());
    }
}