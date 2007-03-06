package org.omnetpp.scave.jobs;

import static org.omnetpp.scave.engineext.IndexFile.isIndexFileUpToDate;
import static org.omnetpp.scave.engineext.IndexFile.isVectorFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.omnetpp.scave.ScavePlugin;
import org.omnetpp.scave.engineext.IndexFile;

/**
 * This job generates index files for vector files in the workspace.
 *
 * @author tomi
 */
public class VectorFileIndexerJob extends WorkspaceJob {
	
	private static final boolean debug = false;
	
	private List<IFile> filesToBeIndexed;

	public VectorFileIndexerJob(String name, IFile[] filesToBeIndexed) {
		super(name);
		setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		
		this.filesToBeIndexed = new ArrayList<IFile>();
		for (IFile file : filesToBeIndexed)
			if (toBeIndexed(file))
				this.filesToBeIndexed.add(file);
	}

	/**
	 * Generate indeces for files in the queue.
	 */
	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {

		if (!filesToBeIndexed.isEmpty()) {
			try {
				monitor.beginTask(getName(), filesToBeIndexed.size());

				for (IFile file : filesToBeIndexed) {
					if (monitor.isCanceled())
						return Status.CANCEL_STATUS;

					monitor.subTask("Indexing "+file.getName());
					if (file.exists() && !isIndexFileUpToDate(file)) {
						IndexFile.performIndexing(file);
					}
					monitor.worked(1);
				}
			}
			finally {
				monitor.done();
			}
		}
		return Status.OK_STATUS;
	}

	private boolean toBeIndexed(IFile file) {
		if (isVectorFile(file)) {
			return !isIndexFileUpToDate(file);
		}
		else
			return false;
	}
}
