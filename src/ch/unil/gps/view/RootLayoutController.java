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
package ch.unil.gps.view;

import ch.unil.gps.App;
import ch.unil.gps.net.view.NetworkAnalysisController;
import ch.unil.gps.pathway.view.PathwayAnalysisController;
import edu.mit.magnum.MagnumSettings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller for the Overview 
 */
public class RootLayoutController extends ViewController {
    
	/** Master controller for genes and pathways view */
	private PathwayAnalysisController pathwayAnalysisController;
	/** Master controller for networks view */
	private NetworkAnalysisController networkAnalysisController;
    /** "Console" controller */
    public ConsoleController consoleController;
    /** "Credits" controller */
    private CreditsController creditsController;

	/** The root */
	@FXML
	private BorderPane rootBorderPane;
	
    /** The "menu" buttons */
	@FXML
    private Button genesPathwaysButton;
	@FXML
    private Button networksButton;
    @FXML
    private Button helpButton;
    @FXML
    private Button preferencesButton;
    @FXML
    private Button aboutButton;

    /** The console pane (within the analysis views) */
    private TitledPane consolePane;
    /** The credits pane (within the analysis views) */
    private HBox creditsHBox;
    
    
	// ============================================================================
	// METHODS

    /** Initialize (does not work if called initialize() or init(), for the former I don't know why, mystery) */
    public void show() {

    	// Initialize console and credits
    	initConsolePane();
    	initCreditsHBox();
    	
    	pathwayAnalysisController = new PathwayAnalysisController(this);
    	networkAnalysisController = new NetworkAnalysisController(this);
    	
    	// First show the genes & pathways view
    	rootBorderPane.setCenter(pathwayAnalysisController.getRoot());
    }
    
    
	// ----------------------------------------------------------------------------

    /** Load preferences */
    @Override
    public void loadPreferences() {
    	networkAnalysisController.loadPreferences();
    }

    
	// ----------------------------------------------------------------------------

    /** Save preferences */
    @Override
    public void savePreferences() {
    	networkAnalysisController.savePreferences();
    }

    	
	// ----------------------------------------------------------------------------
	
    /** Apply settings from the given magnum settings instance */
    public void applySettings(MagnumSettings set) {
    	networkAnalysisController.applySettings(set);
    }


	// ----------------------------------------------------------------------------

    /** "Console" pane */
    private void initConsolePane() {

    	consoleController = (ConsoleController) ViewController.loadFxml("view/Console.fxml");
    	// Add to root layout
    	consolePane = (TitledPane) consoleController.getRoot();
    	consolePane.setExpanded(true);
    	VBox.setVgrow(consolePane, Priority.ALWAYS);

    	// Link to logger
    	App.log.setConsole(consoleController.getConsoleTextArea());
    	// Copy previous output to console
    	consoleController.getConsoleTextArea().setText(App.log.getLogCopy());
    	App.log.disableLogCopy();
    }
    
    
	// ----------------------------------------------------------------------------

    /** "Credits" pane */
    private void initCreditsHBox() {

    	creditsController = (CreditsController) ViewController.loadFxml("view/Credits.fxml");
    	// Add to root layout
    	creditsHBox = (HBox) creditsController.getRoot();
    	VBox.setVgrow(creditsHBox, Priority.NEVER);
    }

    
	// ----------------------------------------------------------------------------

    /** Set styles for active and inactive buttons */
    private void activeInactiveMenuButton(Button active, Button inactive) {
    	
    	ObservableList<String> styleActive = active.getStyleClass();
    	ObservableList<String> styleInactive = inactive.getStyleClass();
    	
    	styleActive.remove("lion");
    	styleActive.add("lion-default");
    	
    	styleInactive.remove("lion-default");
    	styleInactive.add("lion");
    }
    

	// ============================================================================
	// HANDLES
    
    /** Networks button handle */
    @FXML
    private void handleNetworksButton() {
    	rootBorderPane.setCenter(networkAnalysisController.getRoot());
    	activeInactiveMenuButton(networksButton, genesPathwaysButton);
    }
	    

    // ----------------------------------------------------------------------------

    /** Pathways button handle */
    @FXML
    private void handlePathwaysButton() {
    	rootBorderPane.setCenter(pathwayAnalysisController.getRoot());
    	activeInactiveMenuButton(genesPathwaysButton, networksButton);
   }

    
	// ----------------------------------------------------------------------------

    /** Help button handle */
    @FXML
    private void handleHelpButton() {
    	SimpleInfoController.show("view/Help.fxml", "Help", "Getting help");
    }
	    
    
	// ----------------------------------------------------------------------------

    /** Preferences button handle */
    @FXML
    private void handlePreferencesButton() {
    	app.getPreferencesController().show();
    }


	// ----------------------------------------------------------------------------

    /** About button handle */
    @FXML
    private void handleAboutButton() {
    	SimpleInfoController.show("view/About.fxml", "About", "Magnum v1.0");
    }

	
    
	// ============================================================================
	// SETTERS AND GETTERS

    public BorderPane getRoot() { return (BorderPane) root; }
    
    public NetworkAnalysisController getNetworkAnalysisController() { return networkAnalysisController; }

	public TitledPane getConsolePane() {
		return consolePane;
	}

	public HBox getCreditsHBox() {
		return creditsHBox;
	}
	  
}
