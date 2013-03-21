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

import java.awt.Component;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.templates.TemplateRegistration;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@TemplateRegistration(folder = "Project/Misc", displayName = "#AdhocProject_displayName", description = "AdhocProjectDescription.html", iconBase = "com/timboudreau/adhoc/project/AdhocProject.png")
@NbBundle.Messages("AdhocProject_displayName=Ad-Hoc Project")
public class AdhocProjectRegistration implements WizardDescriptor./*Progress*/InstantiatingIterator {
    
    public AdhocProjectRegistration() {
        pnl.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent ce) {
                supp.fireChange();
            }
        });
    }

    @Override
    public Set instantiate() throws IOException {
        FileObject fo = pnl.pnl.getFileObject();
        AdhocProjectFactory.mark(fo);
        return Collections.singleton(fo);
    }

    @Override
    public void initialize(WizardDescriptor wd) {
        FileObject fo = (FileObject) wd.getProperty("dir");
        if (fo != null) {
            pnl.getComponent();
            pnl.pnl.setFileObject(fo);
        }
    }

    @Override
    public void uninitialize(WizardDescriptor wd) {
        FileObject fo = pnl.pnl.getFileObject();
        if (fo != null) {
            wd.putProperty("dir", fo);
        }
    }
    private final Pnl pnl = new Pnl();

    @Override
    public WizardDescriptor.Panel current() {
        return pnl;
    }

    @Override
    public String name() {
        return "Choose Folder";
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void nextPanel() {
    }

    @Override
    public void previousPanel() {
    }
    private final ChangeSupport supp = new ChangeSupport(this);

    @Override
    public void addChangeListener(ChangeListener cl) {
        supp.addChangeListener(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        supp.removeChangeListener(cl);
    }

    private static class Pnl implements WizardDescriptor.Panel<Object> {

        private AdhocProjectWizardPanel pnl;

        @Override
        public Component getComponent() {
            boolean wasNull = pnl == null;
            AdhocProjectWizardPanel result = pnl == null ? pnl = new AdhocProjectWizardPanel() : pnl;
            if (wasNull) {
                pnl.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent ce) {
                        supp.fireChange();
                    }
                    
                });
            }
            return result;
        }

        @Override
        public HelpCtx getHelp() {
            return HelpCtx.DEFAULT_HELP;
        }

        @Override
        public void readSettings(Object data) {
        }

        @Override
        public void storeSettings(Object data) {
        }

        @Override
        public boolean isValid() {
            getComponent();
            return pnl.hasValidFile();
        }
        private final ChangeSupport supp = new ChangeSupport(this);

        @Override
        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }
    }
}
