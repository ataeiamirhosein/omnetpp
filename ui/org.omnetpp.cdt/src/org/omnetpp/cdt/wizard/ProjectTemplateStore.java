package org.omnetpp.cdt.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.omnetpp.cdt.Activator;

/**
 * Stores project templates
 * @author Andras
 */
public class ProjectTemplateStore {
    private List<IProjectTemplate> cppTemplates = new ArrayList<IProjectTemplate>();
    private List<IProjectTemplate> noncppTemplates = new ArrayList<IProjectTemplate>();

    public static final Image ICON_TEMPLATE = Activator.getCachedImage("icons/full/obj16/template.png");
    
    public ProjectTemplateStore() {
        createTemplates();
    }

    public List<IProjectTemplate> getCppTemplates() {
        return cppTemplates;
    }
    
    public List<IProjectTemplate> getNoncppTemplates() {
        return noncppTemplates;
    }

    protected void createTemplates() {
        final String DEFAULT_SRCFOLDER_OPTIONS = "--deep --meta:recurse --meta:auto-include-path --meta:export-library --meta:use-exported-libs";
        final String DEFAULT_ROOTFOLDER_OPTIONS = "--nolink --meta:recurse";
        
        // non-C++ projects
        final String SINGLE_DIR0 = "Single-directory project";
        noncppTemplates.add(new ProjectTemplate("Empty project", SINGLE_DIR0, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                createBuildSpec(new String[] {".", DEFAULT_SRCFOLDER_OPTIONS});                
            }
        });
        
        // C++ projects
        final String SINGLE_DIR = "Single-directory project, for small simulations";
        cppTemplates.add(new ProjectTemplate("Empty project", SINGLE_DIR, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                createBuildSpec(new String[] {".", DEFAULT_SRCFOLDER_OPTIONS});                
            }
        });
        cppTemplates.add(new ProjectTemplate("Tictoc example", SINGLE_DIR, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                setVariable("namespace", "");
                setVariable("rootpackage", "");
                setVariable("simulationspackage", "");
                createBuildSpec(new String[] {".", DEFAULT_SRCFOLDER_OPTIONS});                
                createFileFromResource("Txc.ned", "templates/Txc.ned");
                createFileFromResource("Txc.h", "templates/Txc.h");
                createFileFromResource("Txc.cc", "templates/Txc.cc");
                createFileFromResource("Tictoc.ned", "templates/Tictoc.ned");
                createFileFromResource("omnetpp.ini", "templates/Tictoc.ini");
            }
        }) ;
        cppTemplates.add(new ProjectTemplate("Source-sink example", SINGLE_DIR, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                setVariable("namespace", "");
                setVariable("rootpackage", "");
                setVariable("simulationspackage", "");
                createBuildSpec(new String[] {".", DEFAULT_SRCFOLDER_OPTIONS});                
                createFileFromResource("Source.ned", "templates/Source.ned");
                createFileFromResource("Source.cc", "templates/Source.cc");
                createFileFromResource("Source.h", "templates/Source.h");
                createFileFromResource("Sink.ned", "templates/Sink.ned");
                createFileFromResource("Sink.cc", "templates/Sink.cc");
                createFileFromResource("Sink.h", "templates/Sink.h");
                createFileFromResource("Network.ned", "templates/SourceSink.ned");
                createFileFromResource("omnetpp.ini", "templates/SourceSink.ini");
            }
        }) ;

        final String SRC_AND_SIMULATIONS = "Project with \"src\" and \"simulations\" folders";
        cppTemplates.add(new ProjectTemplate("Empty project", SRC_AND_SIMULATIONS, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                createAndSetSourceFolders(new String[]{"src"});
                createAndSetNedSourceFolders(new String[] {"src", "simulations"});
                createBuildSpec(new String[] {".", DEFAULT_ROOTFOLDER_OPTIONS, "src", DEFAULT_SRCFOLDER_OPTIONS});                
            }
        });
        cppTemplates.add(new ProjectTemplate("Tictoc example", SRC_AND_SIMULATIONS, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                setVariable("namespace", "");
                setVariable("rootpackage", "");
                setVariable("simulationspackage", "");
                createBuildSpec(new String[] {".", DEFAULT_ROOTFOLDER_OPTIONS, "src", DEFAULT_SRCFOLDER_OPTIONS});                
                createAndSetSourceFolders(new String[]{"src"});
                createAndSetNedSourceFolders(new String[] {"src", "simulations"});
                createFileFromResource("src/Txc.ned", "templates/Txc.ned");
                createFileFromResource("src/Txc.h", "templates/Txc.h");
                createFileFromResource("src/Txc.cc", "templates/Txc.cc");
                createFileFromResource("simulations/Tictoc.ned", "templates/Tictoc.ned");
                createFileFromResource("simulations/omnetpp.ini", "templates/Tictoc.ini");
            }
        }) ;
        cppTemplates.add(new ProjectTemplate("Source-sink example (with NED packages and C++ namespace)", SRC_AND_SIMULATIONS, null, ICON_TEMPLATE) {
            @Override
            public void doConfigure() throws CoreException {
                setVariable("namespace", "{{projectname}}");
                setVariable("rootpackage", "org.example.{{projectname}}");
                setVariable("simulationspackage", "{{rootpackage}}.simulations");
                createAndSetSourceFolders(new String[]{"src"});
                createAndSetNedSourceFolders(new String[] {"src", "simulations"});
                createBuildSpec(new String[] {".", DEFAULT_ROOTFOLDER_OPTIONS, "src", DEFAULT_SRCFOLDER_OPTIONS});                
                createFileFromResource("src/package.ned", "templates/package.ned");
                createFileFromResource("src/Source.ned", "templates/Source.ned");
                createFileFromResource("src/Source.cc", "templates/Source.cc");
                createFileFromResource("src/Source.h", "templates/Source.h");
                createFileFromResource("src/Sink.ned", "templates/Sink.ned");
                createFileFromResource("src/Sink.cc", "templates/Sink.cc");
                createFileFromResource("src/Sink.h", "templates/Sink.h");
                createFileFromResource("simulations/package.ned", "templates/simulationsPackage.ned");
                createFileFromResource("simulations/Network.ned", "templates/SourceSink.ned");
                createFileFromResource("simulations/omnetpp.ini", "templates/SourceSink.ini");
            }
        }) ;

        //final String SRC_AND_EXAMPLES = "Project with \"src\" and \"examples\" folders";
        //TODO like the above!
    }
    
}
