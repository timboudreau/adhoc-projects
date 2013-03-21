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

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import javax.swing.JComponent;
import org.netbeans.modules.editor.indent.project.api.Customizers;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class AdhocProjectCustomizerProvider implements CustomizerProvider {

    private final AdhocProject project;

    AdhocProjectCustomizerProvider(AdhocProject project) {
        this.project = project;
    }

    @Override
    public void showCustomizer() {
        String path = "Projects/" + AdhocProject.TYPE_NAME + "/Customizer";
        Dialog dlg = ProjectCustomizer.createCustomizerDialog(path, project.getLookup(), "Basic", new AL(), HelpCtx.DEFAULT_HELP);
        dlg.setModal(false);
        dlg.setVisible(true);
    }

    private class AL implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            //do nothing
        }
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = AdhocProject.TYPE_NAME, position = 1000,
            category = "Basic", categoryLabel = "#LBL_CategoryBasic")
    @NbBundle.Messages("LBL_CategoryBasic=Basic")
    public static ProjectCustomizer.CompositeCategoryProvider basic() {
        return new ProjectCustomizer.CompositeCategoryProvider() {
            @Override
            public ProjectCustomizer.Category createCategory(Lookup lkp) {
                return ProjectCustomizer.Category.create("Basic", "Basic", ImageUtilities.loadImage("com/timboudreau/adhoc/project/adhoc.png"));
            }

            @Override
            public JComponent createComponent(ProjectCustomizer.Category ctgr, Lookup lkp) {
                AdhocProject prj = lkp.lookup(AdhocProject.class);
                return new BasicCustomizer(prj);
            }
        };
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = AdhocProject.TYPE_NAME, position = 1000,
            category = "Formatting", categoryLabel = "#LBL_CategoryFormatting")
    @NbBundle.Messages("LBL_CategoryFormatting=Formatting")
    public static ProjectCustomizer.CompositeCategoryProvider formatting() {
        return Customizers.createFormattingCategoryProvider(Collections.emptyMap());
    }

    public JComponent create(ProjectCustomizer.Category ctgr) {
        if (ctgr.getName().equals("Basic")) {
            return new BasicCustomizer(project);
        }
        return null;
    }
}
