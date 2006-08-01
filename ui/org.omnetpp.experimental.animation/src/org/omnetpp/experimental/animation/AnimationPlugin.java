package org.omnetpp.experimental.animation;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.omnetpp.experimental.animation.editors.LiveAnimationEditor;
import org.omnetpp.experimental.simkernel.SimkernelPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AnimationPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.omnetpp.experimental.animation";

	// The shared instance
	private static AnimationPlugin plugin;
	
	// There can be only one "live" simulation executing -- this 
	// points to the instance (or null)
	private LiveAnimationEditor currentLiveAnimation;
	
	/**
	 * The constructor
	 */
	public AnimationPlugin() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		//SimkernelPlugin.getDefault(); //XXX test
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AnimationPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public LiveAnimationEditor getCurrentLiveAnimation() {
		return currentLiveAnimation;
	}

	public void setCurrentLiveAnimation(LiveAnimationEditor currentLiveAnimation) {
		this.currentLiveAnimation = currentLiveAnimation;
	}
}
