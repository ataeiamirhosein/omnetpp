package org.omnetpp.ned.model.interfaces;

/**
 * Can provide a NED Model. Allows that we access the model from any selection
 * without depending on the GEF plugin. GEF EditParts should implement this, as an
 * IStructuredSelection contains GEF EditParts in the graphical NED editor.
 * @author rhornig
 */
public interface IModelProvider {
    public Object getModel();
}
