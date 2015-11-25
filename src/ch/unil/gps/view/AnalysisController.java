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

import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller of the main layout of the networks analysis pane (has no fxml file)
 */
public abstract class AnalysisController {

	/** The root layout controller */
	protected RootLayoutController rootLayoutController;
	
	/** The root */
	protected HBox root;
	/** Left side */
	protected VBox leftSide;
	/** Rigth side */
	protected VBox rightSide;

	
	// ============================================================================
	// PUBLIC METHODS
	    
	/** Constructor */
	public AnalysisController(RootLayoutController rootLayoutController) {

		this.rootLayoutController = rootLayoutController;
		
		// Initialize the layout
		initLayout();
		// Initialize the content
		initContent();
	}
	
	
	// ============================================================================
	// PRIVATE METHODS

	/** Initialize layout (hbox with two vboxes inside) */
	protected void initLayout() {
		
    	// The hbox
    	root = new HBox();
    	root.setFillHeight(true);
    	
    	// The two vboxes
    	leftSide = new VBox();
    	leftSide.setMaxWidth(Double.MAX_VALUE);
    	leftSide.setMaxHeight(Double.MAX_VALUE);
    	leftSide.setSpacing(10);
    	
    	rightSide = new VBox();
    	rightSide.setMaxWidth(Double.MAX_VALUE);
    	rightSide.setMaxHeight(Double.MAX_VALUE);
    	rightSide.setSpacing(10);

    	root.getChildren().add(leftSide);
    	root.getChildren().add(rightSide);
    	
    	HBox.setHgrow(leftSide, Priority.ALWAYS);
    	HBox.setHgrow(rightSide, Priority.NEVER);
    	
    	// Margins: top right bottom left
    	HBox.setMargin(leftSide, new Insets(0, 5, 10, 10));
    	HBox.setMargin(rightSide, new Insets(0, 10, 5, 5));
	}


	// ----------------------------------------------------------------------------

	/** Abstract method adding the content to the layout */
	abstract protected void initContent();
	

	
	// ============================================================================
	// SETTERS AND GETTERS

	public HBox getRoot() {
		return root;
	}
	
}
