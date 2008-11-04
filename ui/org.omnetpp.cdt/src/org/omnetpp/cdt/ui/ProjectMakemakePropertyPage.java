package org.omnetpp.cdt.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICBuildSetting;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.cdtvariables.CdtVariableManager;
import org.eclipse.cdt.ui.newui.CDTPropertyManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.internal.dialogs.PropertyDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.omnetpp.cdt.Activator;
import org.omnetpp.cdt.CDTUtils;
import org.omnetpp.cdt.makefile.BuildSpecification;
import org.omnetpp.cdt.makefile.MakefileTools;
import org.omnetpp.cdt.makefile.MakemakeOptions;
import org.omnetpp.cdt.makefile.MetaMakemake;
import org.omnetpp.cdt.makefile.MakemakeOptions.Type;
import org.omnetpp.common.color.ColorFactory;
import org.omnetpp.common.ui.HoverSupport;
import org.omnetpp.common.ui.IHoverTextProvider;
import org.omnetpp.common.ui.SizeConstraint;
import org.omnetpp.common.util.StringUtils;

/**
 * This property page is shown for an OMNeT++ CDT Project, and lets the user 
 * manage the C++ configuration.
 *
 * @author Andras
 */
//XXX LIBRARY PATHS: ne out/configname legyen, hanem a source folder!
//FIXME warning if there's no makefile in a "custom makefile" folder (yellow "/!\")
//FIXME warning ha egy source foldert egy makefile sem fed be
@SuppressWarnings("restriction")
public class ProjectMakemakePropertyPage extends PropertyPage {
    private static final String SOURCE_FOLDER_IMG = "icons/full/obj16/folder_srcfolder.gif";
    private static final String SOURCE_SUBFOLDER_IMG = "icons/full/obj16/folder_srcsubfolder.gif";
    private static final String NONSRC_FOLDER_IMG = "icons/full/obj16/folder_nonsrc.gif";
    
    protected static final String OVR_MAKEMAKE_IMG = "icons/full/obj16/ovr_makemake.png";
    protected static final String OVR_CUSTOMMAKE_IMG = "icons/full/obj16/ovr_custommake.png";
    
    // state
    protected BuildSpecification buildSpec;

    protected static boolean suppressExcludeProjectRootQuestion = false; // per-session variable

    // controls
    protected Label errorMessageLabel;
    protected TreeViewer treeViewer;
    protected Button markAsRootButton;
    protected Button optionsButton;
    protected Button sourceLocationButton;
    protected Button excludeButton;
    protected Button includeButton;
    protected Button makemakeButton;
    protected Button customMakeButton;
    protected Button noMakeButton;

    /**
     * Constructor.
     */
    public ProjectMakemakePropertyPage() {
        super();
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        composite.setLayout(new GridLayout(2,false));
        ((GridLayout)composite.getLayout()).marginWidth = 0;
        ((GridLayout)composite.getLayout()).marginHeight = 0;

        String text = 
            "On this page you can configure source folders and makefile generation; " +
            "they are independent of each other. Source folder changes on this page " +
            "apply to all configurations. Source folders can also be managed on the " +
            "<A>Paths and symbols</A> page.";
        final Link pathsAndSymbolsLink = createLink(composite, text);
        pathsAndSymbolsLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
        ((GridData)pathsAndSymbolsLink.getLayoutData()).widthHint = 300;

        errorMessageLabel = new Label(composite, SWT.WRAP);
        errorMessageLabel.setForeground(ColorFactory.RED2);
        errorMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
        ((GridData)errorMessageLabel.getLayoutData()).widthHint = 300;

        treeViewer = new TreeViewer(composite, SWT.BORDER);
        treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ((GridData)treeViewer.getTree().getLayoutData()).widthHint = 300;

        Composite buttons = new Composite(composite, SWT.NONE);
        buttons.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
        buttons.setLayout(new GridLayout(1,false));

        makemakeButton = createButton(buttons, SWT.PUSH, "&Makemake", "Enable makefile generation in this folder");
        customMakeButton = createButton(buttons, SWT.PUSH, "&Custom Make", "Declare that folder contains custom makefile");
        noMakeButton = createButton(buttons, SWT.PUSH, "&No Make", "No makefile in this folder");
        createLabel(buttons, "", 1);
        optionsButton = createButton(buttons, SWT.PUSH, "&Options...", "Edit makefile generation options");
        createLabel(buttons, "", 1);
        markAsRootButton = createButton(buttons, SWT.PUSH, "Mark as &Root", "Set root makefile to invoke (see Build Location on the C/C++ Build page)");
        createLabel(buttons, "", 1);
        Label sep = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
        sep.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        createLabel(buttons, "", 1);
        sourceLocationButton = createButton(buttons, SWT.PUSH, "&Source Location", "Mark folder as source location");
        excludeButton = createButton(buttons, SWT.PUSH, "&Exclude", "Exclude from build");
        includeButton = createButton(buttons, SWT.PUSH, "&Include", "Include into build");

        pathsAndSymbolsLink.addSelectionListener(new SelectionListener(){
            public void widgetSelected(SelectionEvent e) {
                gotoPathsAndSymbolsPage();
            }
            public void widgetDefaultSelected(SelectionEvent e) {
                gotoPathsAndSymbolsPage();
            }
        });

        makemakeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setFolderMakeType(getTreeSelection(), BuildSpecification.MAKEMAKE);
            }
        });
        customMakeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setFolderMakeType(getTreeSelection(), BuildSpecification.CUSTOM);
            }
        });
        noMakeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setFolderMakeType(getTreeSelection(), BuildSpecification.NONE);
            }
        });
        markAsRootButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                markAsRoot(getTreeSelection());
            }
        });
        optionsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editFolderOptions(getTreeSelection());
            }
        });
        sourceLocationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                markAsSourceLocation(getTreeSelection());
            }
        });
        excludeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                excludeFolder(getTreeSelection());
            }
        });
        includeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                includeFolder(getTreeSelection());
            }
        });

        // register this page in the CDT property manager (note: if we skip this,
        // "Mark as source folder" won't work on the page until we visit a CDT property page)
        CDTPropertyManager.getProjectDescription(this, getProject());
        
        // configure the tree
        treeViewer.setContentProvider(new WorkbenchContentProvider() {
            @Override
            public Object[] getChildren(Object element) {
                if (element instanceof IWorkspaceRoot)
                    return new Object[] {getProject()};
                List<Object> result = new ArrayList<Object>();
                for (Object object : super.getChildren(element))
                    if (object instanceof IContainer && MakefileTools.isGoodFolder((IContainer)object))
                        result.add(object);
                return result.toArray();
            }
        });
        
        treeViewer.setLabelProvider(new LabelProvider() {
            public Image getImage(Object element) {
                if (!(element instanceof IContainer))
                    return null;

                IContainer folder = (IContainer)element;
                ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(getProject());
                ICConfigurationDescription configuration = projectDescription.getActiveConfiguration();
                boolean isSrcFolder = CDTUtils.getSourceEntryFor(folder, configuration.getSourceEntries())!=null;
                boolean isExcluded = CDTUtils.isExcluded(folder, configuration.getSourceEntries());
                String imagePath = isSrcFolder ? SOURCE_FOLDER_IMG : !isExcluded ? SOURCE_SUBFOLDER_IMG : NONSRC_FOLDER_IMG;

                int makeType = buildSpec.getFolderMakeType(folder);
                String overlayImagePath = null; 
                switch (makeType) {
                    case BuildSpecification.MAKEMAKE: overlayImagePath = OVR_MAKEMAKE_IMG; break;
                    case BuildSpecification.CUSTOM: overlayImagePath = OVR_CUSTOMMAKE_IMG; break;
                    case BuildSpecification.NONE: overlayImagePath = null; break;
                }
                return Activator.getCachedDecoratedImage(imagePath, overlayImagePath, SWT.END, SWT.END);
            }

            public String getText(Object element) {
                if (element instanceof IContainer)
                    return getLabelFor((IContainer) element);
                return element.toString();
            }
        });

        treeViewer.getTree().addSelectionListener(new SelectionAdapter() {
            public void widgetDefaultSelected(SelectionEvent e) {
                editFolderOptions(getTreeSelection());
            }
        });
        treeViewer.getTree().addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == '\r' && e.stateMask == SWT.ALT)  // Alt+Enter
                    editFolderOptions(getTreeSelection());
            }
            public void keyReleased(KeyEvent e) {
            }
        });

        new HoverSupport().adapt(treeViewer.getTree(), new IHoverTextProvider() {
            public String getHoverTextFor(Control control, int x, int y, SizeConstraint outPreferredSize) {
                Item item = treeViewer.getTree().getItem(new Point(x,y));
                Object element = item==null ? null : item.getData();
                if (element instanceof IContainer)
                    return getFolderHoverText((IContainer)element);
                return null;
            }
        });

        // make the error text label wrap properly; see https://bugs.eclipse.org/bugs/show_bug.cgi?id=9866
        composite.addControlListener(new ControlAdapter(){
            public void controlResized(ControlEvent e){
                GridLayout layout = (GridLayout)composite.getLayout();
                GridData data = (GridData)errorMessageLabel.getLayoutData();
                data.widthHint = composite.getClientArea().width - 2*layout.marginWidth;
                GridData data2 = (GridData)pathsAndSymbolsLink.getLayoutData();
                data2.widthHint = composite.getClientArea().width - 2*layout.marginWidth;
                composite.layout(true);
            }
        });
        
        loadBuildSpecFile();

        treeViewer.setInput(getProject().getParent());
        treeViewer.expandToLevel(2);
        
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updatePageState();
            }
        });
        
        updatePageState();
        return composite;
    }

    protected String getLabelFor(IContainer folder) {
        String result = folder.getName();

        if (buildSpec.getFolderMakeType(folder)==BuildSpecification.MAKEMAKE) {
            MakemakeOptions options = buildSpec.getMakemakeOptions(folder);
            
            String target = options.target == null ? folder.getProject().getName() : options.target;
            switch (options.type) {
                case NOLINK: target = null; break;
                case EXE: target += " (executable)"; break;
                case SHAREDLIB: target += " (dynamic lib)"; break;
                case STATICLIB: target += " (static lib)"; break;
            }

            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(folder.getProject());
            ICConfigurationDescription configuration = projectDescription.getActiveConfiguration();
            boolean isExcluded = CDTUtils.isExcluded(folder, configuration.getSourceEntries());

            result += 
                ": makemake (" + 
                (isExcluded ? "no src" : options.isDeep ? "deep" : "shallow") + ", "+ 
                (options.metaRecurse ? "recurse" : "no-recurse") + ")" +
                (target==null ? "" : " --> " + target);
        }
        if (buildSpec.getFolderMakeType(folder)==BuildSpecification.CUSTOM) {
            result += ": custom makefile";
        }
        return result;
    }

    protected String getFolderHoverText(IContainer folder) {
        IProject project = folder.getProject();
        ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
        ICConfigurationDescription configuration = projectDescription.getActiveConfiguration();
        ICSourceEntry[] sourceEntries = configuration.getSourceEntries();

        boolean isSourceLocation = CDTUtils.getSourceLocations(project, sourceEntries).contains(folder);
        boolean isExcluded = CDTUtils.isExcluded(folder, sourceEntries);
        boolean isUnderSourceLocation = CDTUtils.getSourceEntryThatCovers(folder, sourceEntries) != null;
        int makeType = buildSpec.getFolderMakeType(folder);

        String folderTypeText = "";
        if (isSourceLocation)
            folderTypeText = "source location";
        else if (isExcluded)
            folderTypeText = "excluded from build";
        else
            folderTypeText = "source folder";

        String what = null;
        String comments = "";

        // find which makefile covers this folder (ignoring makefile type and scope for now)
        IContainer parentMakefileFolder = null;
        for (IContainer f = folder.getParent(); !(f instanceof IWorkspaceRoot) && !CDTUtils.isExcluded(f, sourceEntries); f = f.getParent())
            if (buildSpec.getMakeFolders().contains(f)) {
                parentMakefileFolder = f; break;}
        int parentMakefileType = buildSpec.getFolderMakeType(parentMakefileFolder);
        MakemakeOptions parentMakeOptions = buildSpec.getMakemakeOptions(parentMakefileFolder);

        //TODO Tooltip irja ki, hogy:
        //   - source folder, "source location" vagy excluded, ill. make= none/makemake/custom
        //   - ha makefile:
        //        - milyen submake-eket hiv meg (kell meghivnia)
        //        - illetve milyen foldereket buildel meg (kell megbuildelnie)
        //           ^^^ ezt a kettot a MetaMakemake kell hogy eloallitsa, es itt ezt kell felhasznalnunk!!!
        //
        //TODO give special hints if the folder is project root

       
        if (makeType==BuildSpecification.MAKEMAKE) {
            what = folderTypeText + "; makefile generation enabled";
            
            //TODO INVOKED BY:!!!!

            MakemakeOptions options = buildSpec.getMakemakeOptions(folder);
            MakemakeOptions translatedOptions = null;
            try {
                //TODO this may take a long time (if dependency-cache is empty)
                translatedOptions = MetaMakemake.translateOptions(folder, options, buildSpec.getMakeFolders(), configuration);
            }
            catch (CoreException e) {
                Activator.logError(e);
            }
            
            if (translatedOptions != null) {
                comments += "<p>Invokes make in: " + StringUtils.defaultIfEmpty(StringUtils.join(translatedOptions.submakeDirs, ", "), "-");
                if (translatedOptions.submakeDirs.size() > 2)
                    comments += "<p>HINT: To control invocation order, add rules to the Makefrag file (on the Custom page of the Makemake Options dialog)";
                comments += "<p>Covers the following sub-folders: TBD";  //FIXME 
            }
            if (folder instanceof IProject && !isExcluded && buildSpec.getMakeFolders().size()>1)  
                comments = 
                    "<p>HINT: You may want to exclude this (project root) folder from build, " +
                    "and leave compilation to subdirectory makefiles.";
        }
        else if (makeType==BuildSpecification.CUSTOM) {
            what = folderTypeText + "; custom makefile";
            
            IFile makefile = folder.getFile(new Path("Makefile")); //XXX Makefile.vc?
            if (!makefile.exists())
                comments = "<p>WARNING: Custom makefile \"Makefile\" missing";
            else if (isExcluded)
                comments = 
                    "<p>Note: The makefile is not supposed to compile any (potentially existing) source files " +
                    "from this folder, because the folder is excluded from build. However, it may " +
                    "invoke other makefiles, may create executables or libraries by invoking the linker, " +
                    "or may contain other kinds of targets.";
        }
        else if (makeType==BuildSpecification.NONE) {
            what = folderTypeText; // + "; no makefile";

            if (isExcluded)
                /*NOP*/;
            else if (parentMakefileFolder == null) 
                comments = "<p>WARNING: This is a source folder, but it is not covered by any makefile. Is this intentional?";
            else if (buildSpec.getFolderMakeType(parentMakefileFolder)==BuildSpecification.CUSTOM) 
                comments = "<p>This source folder is supposed to be covered by the custom makefile in " + parentMakefileFolder.getFullPath();
            else if (buildSpec.getFolderMakeType(parentMakefileFolder)==BuildSpecification.MAKEMAKE && !buildSpec.getMakemakeOptions(parentMakefileFolder).isDeep) 
                comments = "<p>WARNING: This is a source folder but it is not covered by any makefile, because the generated makefile in " + parentMakefileFolder.getFullPath() + " is not deep. Is this intentional?";
            else if (buildSpec.getFolderMakeType(parentMakefileFolder)==BuildSpecification.MAKEMAKE) 
                comments = "<p>This source folder is covered by the generated makefile in " + parentMakefileFolder.getFullPath(); 
            else 
                Activator.logError(new AssertionFailedException("Tooltip logic error"));
        }

        String result = "<b>" + folder.getFullPath() + "</b> (" + what + ")";
        if (!StringUtils.isEmpty(comments))
            result += comments;
        return HoverSupport.addHTMLStyleSheet(result);
    }

    protected void setFolderMakeType(IContainer folder, int makeType) {
        buildSpec.setFolderMakeType(folder, makeType);
        if (makeType == BuildSpecification.MAKEMAKE) {
            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(folder.getProject());
            ICConfigurationDescription configuration = projectDescription.getActiveConfiguration();
            boolean isExcluded = CDTUtils.isExcluded(folder, configuration.getSourceEntries());
            if (isExcluded)
                buildSpec.getMakemakeOptions(folder).type = Type.NOLINK;
        }
        updatePageState();

        if (makeType!=BuildSpecification.NONE)
            maybeOfferToExcludeProjectRoot(folder);
    }

    protected void markAsRoot(IContainer folder) {
        try {
            IProject project = getProject();
            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
            for (ICConfigurationDescription configuration : projectDescription.getConfigurations())
                configuration.getBuildSetting().setBuilderCWD(new Path("${workspace_loc:"+folder.getFullPath().toString()+"}"));
        }
        catch (Exception e) {
            errorDialog(e.getMessage(), e);
        }
        updatePageState();
    }

    protected void editFolderOptions(IContainer folder) {
        if (buildSpec.getFolderMakeType(folder) != BuildSpecification.MAKEMAKE)
            return;
        MakemakeOptionsDialog dialog = new MakemakeOptionsDialog(getShell(), folder, buildSpec.getMakemakeOptions(folder), buildSpec.getMakeFolders());
        if (dialog.open() == Dialog.OK) {
            buildSpec.setMakemakeOptions(folder, dialog.getResult());
            updatePageState();
        }
    }

    protected void markAsSourceLocation(IContainer folder) {
        try {
            IProject project = getProject();
            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
            for (ICConfigurationDescription configuration : projectDescription.getConfigurations()) {
                ICSourceEntry[] entries = configuration.getSourceEntries();
                ICSourceEntry entry = (ICSourceEntry)CDataUtil.createEntry(ICSettingEntry.SOURCE_PATH, folder.getProjectRelativePath().toString(), folder.getProjectRelativePath().toString(), new IPath[0], ICSettingEntry.VALUE_WORKSPACE_PATH);
                entries = (ICSourceEntry[]) ArrayUtils.add(entries, entry);
                configuration.setSourceEntries(entries);
            }
        }
        catch (Exception e) {
            errorDialog(e.getMessage(), e);
        }
        updatePageState();
     
        maybeOfferToExcludeProjectRoot(folder);
    }

    protected void maybeOfferToExcludeProjectRoot(IContainer selectedFolder) {
        if (!suppressExcludeProjectRootQuestion && !selectedFolder.equals(getProject())) {
            IProject project = getProject();
            ICSourceEntry[] sourceEntries = CDTPropertyManager.getProjectDescription(project).getActiveConfiguration().getSourceEntries();
            boolean isRootExcluded = CDTUtils.isExcluded(project, sourceEntries);
            if (!isRootExcluded) {
                MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(this.getShell(), 
                        "Exclude Root", 
                        "Do you want to exclude the project root folder from build? (recommended for projects that have all source files in subdirectories)", 
                        "Do not ask this question again in this session", false, null, null);
                if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
                    // exclude project root
                    excludeFolder(getProject());
                    // but: put back current folder if it became excluded
                    sourceEntries = CDTPropertyManager.getProjectDescription(project).getActiveConfiguration().getSourceEntries();
                    if (CDTUtils.isExcluded(selectedFolder, sourceEntries))
                        includeFolder(selectedFolder);
                    updatePageState();
                }
                if (dialog.getToggleState()==true) 
                    suppressExcludeProjectRootQuestion = true;
            }
        }
    }

    //note: this method is unused -- the "Exclude" button seems to work just fine
    protected void removeSourceLocation(IContainer folder) {
        try {
            IProject project = getProject();
            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
            for (ICConfigurationDescription configuration : projectDescription.getConfigurations()) {
                ICSourceEntry[] entries = configuration.getSourceEntries();
                ICSourceEntry entry = CDTUtils.getSourceEntryFor(folder, entries);
                entries = (ICSourceEntry[]) ArrayUtils.removeElement(entries, entry); // works for null too
                configuration.setSourceEntries(entries);
            }
        }
        catch (Exception e) {
            errorDialog(e.getMessage(), e);
        }
        updatePageState();
    }

    protected void excludeFolder(IContainer folder) {
        setExcluded(folder, true);
    }

    protected void includeFolder(IContainer folder) {
        setExcluded(folder, false);
    }

    protected void setExcluded(IContainer folder, boolean exclude) {
        try {
            IProject project = getProject();
            ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
            for (ICConfigurationDescription configuration : projectDescription.getConfigurations()) {
                ICSourceEntry[] newEntries = CDTUtils.setExcluded(folder, exclude, configuration.getSourceEntries());
                configuration.setSourceEntries(newEntries);
            }
            CoreModel.getDefault().setProjectDescription(project, projectDescription);
        }
        catch (CoreException e) {
            errorDialog(e.getMessage(), e);
        }
        catch (Exception e) {
            errorDialog(e.getMessage(), e);
        }
        updatePageState();
    }

    protected void gotoPathsAndSymbolsPage() {
        IPreferencePageContainer container = getContainer();
        if (container instanceof PropertyDialog)
            ((PropertyDialog)container).setCurrentPageId(MakemakeOptionsPanel.PROPERTYPAGE_PATH_AND_SYMBOLS);
    }

    protected Group createGroup(Composite composite, String text, int numColumns) {
        Group group = new Group(composite, SWT.NONE);
        group.setText(text);
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        group.setLayout(new GridLayout(numColumns,false));
        return group;
    }

    protected Label createLabel(Composite composite, String text, int hspan) {
        Label label = new Label(composite, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData());
        ((GridData)label.getLayoutData()).horizontalSpan = hspan;
        return label;
    }

    protected Button createButton(Composite parent, int style, String text, String tooltip) {
        Button button = new Button(parent, style);
        button.setText(text);
        if (tooltip != null)
            button.setToolTipText(tooltip);
        button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return button;
    }

    protected Link createLink(Composite composite, String text) {
        Link link = new Link(composite, SWT.NONE);
        link.setText(text);
        link.setLayoutData(new GridData());
        return link;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible == true)
            updatePageState();
    }

    protected void updatePageState() {
        // display warnings about CDT misconfiguration, etc 
        String message = getDiagnosticMessage(getProject(), buildSpec);
        errorMessageLabel.setText(message==null ? "" : message);
        ((GridData)errorMessageLabel.getLayoutData()).exclude = (message==null); 
        errorMessageLabel.setVisible(message!=null);
        errorMessageLabel.getParent().layout(true);
        
        treeViewer.refresh();
        
        // if there's no tree selection, disable all buttons
        IContainer folder = getTreeSelection();
        if (folder == null) {
            makemakeButton.setEnabled(false);
            customMakeButton.setEnabled(false);
            noMakeButton.setEnabled(false);
            markAsRootButton.setEnabled(false);
            optionsButton.setEnabled(false);
            sourceLocationButton.setEnabled(false);
            excludeButton.setEnabled(false);
            includeButton.setEnabled(false);
            return;
        }
        
        // enable/disable buttons
        IProject project = getProject();
        int makeType = buildSpec.getFolderMakeType(folder);
        makemakeButton.setEnabled(makeType!=BuildSpecification.MAKEMAKE);
        customMakeButton.setEnabled(makeType!=BuildSpecification.CUSTOM);
        noMakeButton.setEnabled(makeType!=BuildSpecification.NONE);
        
        markAsRootButton.setEnabled(makeType!=BuildSpecification.NONE);
        optionsButton.setEnabled(makeType==BuildSpecification.MAKEMAKE);
        
        ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
        ICConfigurationDescription configuration = projectDescription.getActiveConfiguration();
        ICSourceEntry[] sourceEntries = configuration.getSourceEntries();
        boolean isExcluded = CDTUtils.isExcluded(folder, sourceEntries);
        boolean isSourceLocation = CDTUtils.getSourceLocations(project, sourceEntries).contains(folder);
        boolean isUnderSourceLocation = CDTUtils.getSourceEntryThatCovers(folder, sourceEntries) != null;
        
        sourceLocationButton.setEnabled(!isSourceLocation);
        excludeButton.setEnabled(!isExcluded && !(folder instanceof IProject && sourceEntries.length==1));
        includeButton.setEnabled(isUnderSourceLocation && isExcluded);
    }

    protected IContainer getTreeSelection() {
        IContainer folder = (IContainer)((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
        return folder;
    }

    protected boolean isDirty() {
        return false;
    }

    // note: this is shared with MakemakeFolderPropertyPage (not very pretty)
    public static String getDiagnosticMessage(IContainer folder, BuildSpecification buildSpec) {
        // Check CDT settings
        IProject project = folder.getProject();
        ICProjectDescription projectDescription = CDTPropertyManager.getProjectDescription(project);
        if (projectDescription == null)
            return "Cannot access CDT build information for this project. Is this a C/C++ project?";
        ICConfigurationDescription activeConfiguration = projectDescription.getActiveConfiguration();
        if (activeConfiguration == null)
            return "No active build configuration -- please create one.";
        boolean isOmnetppConfig = false;
        for (ICConfigurationDescription c = activeConfiguration; c != null; c = (ICConfigurationDescription)c.getParent())
            if (c.getId().startsWith("org.omnetpp.cdt.")) {
                isOmnetppConfig = true; break;}
        if (!isOmnetppConfig)
            return "The active build configuration \""+ activeConfiguration.getName() + "\" is " +
            		"not an OMNeT++ configuration. Please re-create the project it with the " +
            		"New OMNeT++ Project wizard, overwriting the existing project settings.";
        ICBuildSetting buildSetting = activeConfiguration.getBuildSetting();
        if (buildSetting == null)
            return "No CDT Project Builder. Activate one on the C/C++ Build / Tool Chain Editor page."; //???
        String builderId = buildSetting.getId();
        if (builderId==null || !builderId.startsWith("org.omnetpp.cdt."))
            return "C/C++ Builder \"" + buildSetting.getName()+ "\" set in the active build configuration " +
                   "is not suitable for OMNeT++. Please re-create the project with the " +
                   "New OMNeT++ Project wizard, overwriting the existing project settings.";

        // warn if there're differences in source entries across configurations.
        // first, collect which entries occur in which configurations
        Map<String,List<String>> sourceEntryMap = new HashMap<String, List<String>>(); // srcEntry -> config*
        for (ICConfigurationDescription cfg : projectDescription.getConfigurations()) {
            for (ICSourceEntry e : cfg.getSourceEntries()) {
                String entryText = StringUtils.defaultIfEmpty(CDataUtil.makeAbsolute(project, e).getFullPath().toString(), ".");
                if (e.getExclusionPatterns().length > 0) 
                    entryText += " (excl: " + StringUtils.join(e.getExclusionPatterns(), ", ") + ")";
                if (!sourceEntryMap.containsKey(entryText))
                    sourceEntryMap.put(entryText, new ArrayList<String>());
                sourceEntryMap.get(entryText).add(cfg.getName());
            }
        }
        List<String> wrongSourceLocations = new ArrayList<String>(); 
        int numConfigs = projectDescription.getConfigurations().length;
        for (String e : sourceEntryMap.keySet())
            if (sourceEntryMap.get(e).size() != numConfigs)
                wrongSourceLocations.add(e + " in " + StringUtils.join(sourceEntryMap.get(e), ","));
        Collections.sort(wrongSourceLocations);
        if (!wrongSourceLocations.isEmpty())
            return "Note: Source locations are set up differently across configurations: " + StringUtils.join(wrongSourceLocations, "; ");
            
        // warn if there's no makefile generated at all
        if (buildSpec.getMakeFolders().isEmpty())
            return "No makefile has been specified for this project.";

        // check if build directory exists, and there's a makefile in it
        String buildLocation = activeConfiguration.getBuildSetting().getBuilderCWD().toString();
        ICConfigurationDescription configurationDescription = projectDescription.getActiveConfiguration();
        try {
            buildLocation = CdtVariableManager.getDefault().resolveValue(buildLocation, "[unknown-macro]", ",", configurationDescription);
        }
        catch (CdtVariableException e) {
            Activator.logError("Cannot resolve macros in build directory spec "+buildLocation, e);
        }
        IContainer[] buildFolderPossibilities = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocation(new Path(buildLocation));
        if (buildFolderPossibilities.length == 0)
            return "Build location \""+ buildLocation + "\" does not map to any folder in the workspace. Check the C/C++ Build page.";
        IContainer buildFolder = null;
        for (IContainer f : buildFolderPossibilities)
            if (buildSpec.getMakeFolders().contains(f))
                buildFolder = f;
        if (buildFolder == null) {
            List<String> tmp = new ArrayList<String>();
            for (IContainer f : buildFolderPossibilities)
                tmp.add(f.getFullPath().toString());
            return "No makefile specified for root build folder " + StringUtils.join(tmp, " or ")+ " (see C/C++ Build page).";
        }
        
        //XXX "Out" dir should not overlap with source folders (check!!!)

        return null;
    }

    /**
     * The resource for which the Properties dialog was brought up.
     */
    protected IProject getProject() {
        return (IProject) getElement().getAdapter(IProject.class);
    }

    @Override
    public boolean performOk() {
        // note: performApply() delegates here too
        saveBuildSpecFile();
        CDTPropertyManager.performOk(this);
        return true;
    }

    protected void loadBuildSpecFile() {
        IProject project = getProject().getProject();
        try {
            buildSpec = BuildSpecification.readBuildSpecFile(project);
        } 
        catch (CoreException e) {
            errorDialog("Cannot read build specification, reverting page content to the default settings.", e);
        }

        if (buildSpec == null)
            buildSpec = BuildSpecification.createBlank(project);
    }

    protected void saveBuildSpecFile() {
        try {
            buildSpec.save();
        } 
        catch (CoreException e) {
            errorDialog("Cannot store build specification", e);
        }
    } 

    protected void errorDialog(String message, Throwable e) {
        Activator.logError(message, e);
        IStatus status = new Status(IMarker.SEVERITY_ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
        ErrorDialog.openError(Display.getCurrent().getActiveShell(), "Error", message, status);
    }
}

