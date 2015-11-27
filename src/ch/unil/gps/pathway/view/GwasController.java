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
package ch.unil.gps.pathway.view;

import java.io.File;

import ch.unil.gps.AppSettings;
import ch.unil.gps.view.FileStringConverter;
import ch.unil.gps.view.ViewController;
import edu.mit.magnum.MagnumSettings;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

/**
 * Controller of the main layout of the networks analysis pane (has no fxml file)
 */
public class GwasController extends ViewController {
    
	/** Bound to gwasTextField */
	private ObjectProperty<File> gwasFileProperty = new SimpleObjectProperty<>();
	/** Bound to referencePopulationTextField */
	private ObjectProperty<File> refPopDirProperty = new SimpleObjectProperty<>();

	/** The text used for the example gene score file */
	private final String exampleGwasText = "Example: TBD";

	
	// ============================================================================
	// FXML
	
	@FXML
	private TextField gwasTextField;
	@FXML
	private TextField refPopTextField;
    @FXML
    private Button gwasBrowseButton;
    @FXML
    private Button refPopBrowseButton;

	
	// ============================================================================
	// PUBLIC METHODS
	
    /** Initialize */
    public void initialize() {
    	
    	// GWAS file
    	gwasFileProperty.addListener((observable, oldValue, newValue) -> {
        	File file = (File) newValue;
    		if (file == null)
    			gwasTextField.setText(exampleGwasText);
    		else
    			gwasTextField.setText(file.getName());
        });

    	// Reference pop directory
        Bindings.bindBidirectional(refPopTextField.textProperty(), refPopDirProperty, new FileStringConverter());

        // Example texts
        if (gwasFileProperty.get() == null)
        	gwasTextField.setText(exampleGwasText);
        
        // Tooltips
        initTooltips();
    }


	// ============================================================================
	// SETTINGS

    /** Load preferences */
    public void loadPreferences() {
    }

    /** Save preferences */
    public void savePreferences() {
    }

    /** Apply settings from the given magnum settings instance */
    public void applySettings(MagnumSettings set) {
    }

    
	// ============================================================================
	// HANDLES

    /** Gwas browse button */
    @FXML
    private void handleGwasBrowseButton() {
    	chooseFile(gwasFileProperty, "Select a GWAS summary statistics file");
    }

    /** Example GWAS link */
    @FXML
    private void handleExampleGwasLink() {  	
        gwasFileProperty.set(null);
    }

    /** Reference population directory browse button */
    @FXML
    private void handleRefPopBrowseButton() {
    	chooseDir(refPopDirProperty, "Locate the reference population directory");
    }

    /** 1KG download link */
    @FXML
    private void handleRefPopDownloadLink() {  	
    	openWebpage(AppSettings.refPopLink);
    }

    
	// ============================================================================
	// PRIVATE METHODS

    /** Add tooltips */
    private void initTooltips() {
    }
    
	
	// ----------------------------------------------------------------------------

	
	
	// ============================================================================
	// SETTERS AND GETTERS


}
