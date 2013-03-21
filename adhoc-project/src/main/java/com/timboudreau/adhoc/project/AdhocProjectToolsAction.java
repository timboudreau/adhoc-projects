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

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.AsyncGUIJob;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
@NbBundle.Messages(value = {"ACT_Open=Open As Project"})
public class AdhocProjectToolsAction extends AbstractAction implements ContextAwareAction {

    @ActionID(id = "com.timboudreau.adhoc.project.AdhocProjectToolsAction", category = "Window")
    @ActionRegistration(displayName = "#ACT_Open", lazy = false)
    @ActionReference(position = 300, path = "UI/ToolActions/Files")
    public static ContextAwareAction instance() {
        return new AdhocProjectToolsAction();
    }
    private final Lookup lkp;

    AdhocProjectToolsAction() {
        this(Utilities.actionsGlobalContext());
    }

    AdhocProjectToolsAction(Lookup lkp) {
        this.lkp = lkp;
        putValue(NAME, NbBundle.getMessage(AdhocProjectToolsAction.class, "ACT_Open"));
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new AdhocProjectToolsAction(actionContext);
    }

    @Override
    public boolean isEnabled() {
        FileObject fo = getFileObject();
        return fo != null && !fo.isVirtual() && fo.isFolder();
    }

    private FileObject getFileObject() {
        DataObject ob = lkp.lookup(DataObject.class);
        if (ob != null) {
            return ob.getPrimaryFile();
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final FileObject fo = getFileObject();
        if (fo != null) {
            RequestProcessor.getDefault().post(new Runnable() {
                boolean reallyOpen = false;
                AdhocProject ap;

                @Override
                public void run() {
                    if (!ProjectManager.mutex().isReadAccess() && !reallyOpen) {
                        ProjectManager.mutex().readAccess(this);
                    } else {
                        try {
                            if (!reallyOpen) {
                                Project p = ProjectManager.getDefault().findProject(fo);
                                if (p != null) {
                                    // try to hold a reference to it
                                    ap = p.getLookup().lookup(AdhocProject.class);
                                } else {
                                    if (!AdhocProjectFactory.check(fo)) {
                                        AdhocProjectFactory.mark(fo);
                                    }
                                    p = ProjectManager.getDefault().findProject(fo);
                                    if (p != null) {
                                        ap = p.getLookup().lookup(AdhocProject.class);
                                    }
                                }
                                reallyOpen = true;
                                // Some kind of race condition with either the
                                // open projects list or ProjectManager's wrapper for
                                // the real project
                                Task task = RequestProcessor.getDefault().create(this);
                                task.schedule(150);
                            } else {
                                Project p = ProjectManager.getDefault().findProject(fo);
                                if (p != null) {
                                    AdhocProject ap = p.getLookup().lookup(AdhocProject.class);
                                    OpenProjects.getDefault().open(new Project[]{ap == null ? p : ap}, false);
                                } else {
                                    p = new AdhocProject(fo, null);
                                    OpenProjects.getDefault().open(new Project[]{p}, false);
                                    OpenProjects.getDefault().openProjects();
                                }
                            }
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            });
        }
    }
}
