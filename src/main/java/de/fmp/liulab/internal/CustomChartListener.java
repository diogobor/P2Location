package de.fmp.liulab.internal;

import java.util.Map;

import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;

/**
 * Class responsible for customizing linear gradient color as chart color
 * @author diogobor
 *
 */
public class CustomChartListener {
	 private static final String FACTORY_ID = "org.cytoscape.LinearGradient";
     private CyCustomGraphics2Factory<?> factory;
     
     /**
      * Method responsible for adding the customized GraphicsFactory based on FACTORY_ID
      * @param factory graphic instance
      * @param serviceProps services properties
      */
     public void addCustomGraphicsFactory(CyCustomGraphics2Factory<?> factory, Map<Object,Object> serviceProps) {
             if(FACTORY_ID.equals(factory.getId())) {
                     this.factory = factory;
             }
     }
     
     /**
      * Method responsible for removing the customized GraphicsFactory
      * @param factory graphic instance
      * @param serviceProps services properties
      */
     public void removeCustomGraphicsFactory(CyCustomGraphics2Factory<?> factory, Map<Object,Object> serviceProps) {
             this.factory = null;
     }
     
     /**
      * Get current Factory
      * @return current Factory
      */
     public CyCustomGraphics2Factory<?> getFactory() {
             return factory;
     }
}
