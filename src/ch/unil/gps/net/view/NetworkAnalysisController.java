/*
Copyright (c) 2013 Daniel Marbach

We release this software open source under an MIT license (see below). If this
software was useful for your scientific work, please cite our paper available at:
http://networkinference.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package ch.unil.gps.net.view;

import ch.unil.gps.net.model.NetworkCollection;
import ch.unil.gps.view.AnalysisController;
import ch.unil.gps.view.RootLayoutController;
import ch.unil.gps.view.ViewController;
import edu.mit.magnum.MagnumSettings;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller of the main layout of the networks analysis pane (has no fxml file)
 */
public class NetworkAnalysisController extends AnalysisController {

	/** "Other networks" controller */
    private NetworkCollectionController networksController;
    /** "Connectivity enrichment" controller */
    private EnrichmentController enrichmentController;

    /** The collection of networks */
    private NetworkCollection networkCollection = null;
    
	
	// ============================================================================
	// PUBLIC METHODS
	
	/** Constructor */
    public NetworkAnalysisController(RootLayoutController rootLayoutController) {

    	super(rootLayoutController);
	}


	// ----------------------------------------------------------------------------

    /** Load preferences */
    public void loadPreferences() {
    	networksController.loadPreferences();
		enrichmentController.loadPreferences();
    }

    
	// ----------------------------------------------------------------------------

    /** Save preferences */
    public void savePreferences() {
    	networksController.savePreferences();
    	enrichmentController.savePreferences();
    }

    
	// ----------------------------------------------------------------------------
	
    /** Apply settings from the given magnum settings instance */
    public void applySettings(MagnumSettings set) {
    	enrichmentController.applySettings(set);
    }



    
	// ----------------------------------------------------------------------------

    /** Network analysis pane */
	@Override
    protected void initContent() {
    	
    	// Add css style
    	//root.getStyleClass().add("networks-tab-pane");

    	// Show panes
        showNetworkCollection();
        showConnetivityEnrichmentPane();
        //rootLayoutController.consoleController.getConsoleTextArea().setText("Wickedy oiseau");
        
        // Console and credits
        rightSide.getChildren().add(rootLayoutController.getConsolePane());
        rightSide.getChildren().add(rootLayoutController.getCreditsHBox());
    }

	
	// ----------------------------------------------------------------------------

    /** "My networks" pane */
    private void showNetworkCollection() {

    	// Initialize network collection, needs to be done before loading controller
		networkCollection = new NetworkCollection();
    	// Initialize user networks pane
    	networksController = (NetworkCollectionController) ViewController.loadFxml("net/view/NetworkCollection.fxml");
    	networksController.initialize(this);
    	
    	// Add to root layout
    	Node child = networksController.getRoot();
    	VBox.setVgrow(child, Priority.ALWAYS);
    	leftSide.getChildren().add(child);  
    }

    
	// ----------------------------------------------------------------------------

    /** "Connectivity enrichment" pane */
    private void showConnetivityEnrichmentPane() {

    	// Initialize user networks pane
    	enrichmentController = (EnrichmentController) ViewController.loadFxml("net/view/Enrichment.fxml");
    	enrichmentController.initialize(this);
    	// Add to root layout
    	rightSide.getChildren().add(enrichmentController.getRoot());  
    }


	// ============================================================================
	// PRIVATE METHODS

	
	// ============================================================================
	// SETTERS AND GETTERS

	public EnrichmentController getEnrichmentController() {	return enrichmentController; }
	public NetworkCollectionController getOtherNetworksController() { return networksController; }
    public NetworkCollection getNetworkCollection() { return networkCollection; }

}
