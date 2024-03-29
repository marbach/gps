/*
Copyright (c) 2013-2015 Daniel Marbach

We release this software open source under an MIT license (see below). If this
software was useful for your scientific work, please cite our paper available at:
http://regulatorycircuits.org

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

import java.io.File;
import java.util.ArrayList;

import ch.unil.gps.App;
import ch.unil.gps.net.control.JobMagnum;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller for a single "launch job" dialog managing multiple threads
 */
public class JobController extends ViewController {

	/** Static flag indicates if jobs are interrupted => there can be only one ThreadController instance */
	volatile static public boolean interrupted = false;

	
	/** The thread -- this will become an array! */
	private ArrayList<JobMagnum> jobs;
	/** Number of cores to be used for jobs */
	private int numCores;
	/** The output directory for the jobs */
	private File outputDir;

	/** The next job in line */
	private int nextJob;
	/** No more jobs are running (they finished with success, error or interrupt) */
	private boolean allDone;
	
	/** Jobs in queue */
	private IntegerProperty numJobsQueued = new SimpleIntegerProperty();
	/** Jobs running */
	private IntegerProperty numJobsRunning = new SimpleIntegerProperty();
	/** Jobs finished successfully */
	private IntegerProperty numJobsFinished = new SimpleIntegerProperty();
	/** Jobs aborted (interrupt or error) */
	private IntegerProperty numJobsAborted = new SimpleIntegerProperty();
	
	/** The javafx alert / dialog */
	private Alert alert;
	/** The ok button */
	private Button okButton;
	/** The cancel button */
	private Button stopButton;
	/** The progress indicator */
	private ProgressIndicator progressIndicator;
	/** The console text area */
	private TextArea console; 
	
	
	// ============================================================================
	// FXML -- thread status

	@FXML
	private VBox threadStatusVBox;
	@FXML
	private Label statusLabel;
	@FXML
	private GridPane statusGridPane;

	@FXML
	private Rectangle numQueuedRectangle;
	@FXML
	private Rectangle numRunningRectangle;
	@FXML
	private Rectangle numFinishedRectangle;
	@FXML
	private Rectangle numAbortedRectangle;
	
	@FXML
	private Label numQueuedLabel;
	@FXML
	private Label numRunningLabel;
	@FXML
	private Label numFinishedLabel;
	@FXML
	private Label numAbortedLabel;
	
	
	// ============================================================================
	// PUBLIC METHODS

	/** Open the dialog, start the thread */
	public void start(ArrayList<JobMagnum> jobs, int numCores) {
		
		// Initialize jobs
		if (numCores > jobs.size())
			this.numCores = jobs.size();
		else
			this.numCores = numCores;
		initializeJobs(jobs);
		
		// Header text
    	String networkS = ((jobs.size() == 1) ? "" : "s");
    	String coreS = ((numCores == 1) ? "" : "s");
    	String headerText = "Running: " + jobs.size() + " network" + networkS;
    	if (numCores > this.numCores)
    		headerText += " (" + this.numCores + " out of " + numCores + " core" + coreS + " used)";
    	else
    		headerText += " (" + this.numCores + " core" + coreS + " used)";

		// Create the javafx dialog
		initializeDialog(headerText);
		// Copy stdout to the console
		App.log.setConsole(console);
		App.log.println(headerText);
		// TODO arrgh
		App.log.println("- Output directory: " + App.app.getRootLayoutController().getNetworkAnalysisController().getEnrichmentController().getOutputDir().getPath() + "\n");
		if (this.numCores > 1)
			App.log.println("==> NOTE: Using multiple cores, console output of individual jobs turned OFF!\n"
					      + "==> See the log files in the output directory instead: <job_name>.log.txt\n");

		// Start the first jobs (jobFinished() callback will start the subsequent jobs)
		for (int i=0; i<this.numCores; i++)
			startNextJob();
		// Show dialog and wait
    	alert.showAndWait();
		
    	// Remove console from logger
    	App.log.setConsole(null);
	}
	

	// ----------------------------------------------------------------------------

	/** JavaFX thread: scheduled using Platform.runLater() by the launched jobs upon completion */
	public void jobFinished(JobMagnum job, Throwable e) {
		
		reduce(numJobsRunning);

		if (interrupted) {
	    	App.log.println("Job interrupted: " + job.getJobName());
			increment(numJobsAborted);
			if (numJobsRunning.get() == 0)
				allJobsDone();
			return;
		} 

		// Normal finish
		if (e == null) {
			App.log.println("Job finished:\t" + job.getJobName() + "\n" +
					"- Runtime = " + App.mag.utils.chronometer(job.getRuntime()));
			increment(numJobsFinished);

		// Exception
		} else if (e instanceof Exception){
			App.log.println("\nJOB ABORTED:\t" + job.getJobName());
			App.log.printStackTrace(e);
			increment(numJobsAborted);
			updateStatusLabel("Status: ONGOING, ENCOUNTERED ERRORS! (See console and log files for details)", "status-warning-label");

		// Out of memory error
		} else {
			increment(numJobsAborted);
			interrupted = true;

			App.log.println("\nOUT OF MEMORY ERROR:\t" + job.getJobName());
			App.log.printStackTrace(e);
			App.log.println("=== OUT OF MEMORY ERROR ===\n\n" +
					"Solutions:\n" +
					"- Reduce the number of cores (parallel jobs) or\n" +
					"- Export settings and run jobs with the command-line tool\n" +
					"  (increase memory using -Xmx, e.g. \"-Xmx8g\" for 8GB)\n\n" +
					"See the user guide for further instructions.\n");
			
			// Interestingly, this does not end up at App.main()...
			//throw new RuntimeException(e);
			
			// Show an alert and quit
			Alert alert = new Alert(AlertType.ERROR);
			alert.getDialogPane().setPrefWidth(420);
			alert.setTitle("Error");
			alert.setHeaderText("Out of memory error!");
			alert.setContentText("It is not save to recover from an out of memory error, Magnum will now EXIT!\n\n" +
					"Solutions:\n" +
					"- Reduce the number of cores (parallel jobs) or\n" +
					"- Export settings and run jobs with the command-line tool\n" +
					"  (increase memory using -Xmx, e.g. \"-Xmx8g\" for 8GB)\n\n" +
					"See the user guide for further instructions.\n");
			alert.getButtonTypes().clear();
			alert.getButtonTypes().add(new ButtonType("Quit", ButtonData.OK_DONE));
			alert.showAndWait();
			System.exit(-1);
		}

		if (numJobsQueued.get() != 0)
			startNextJob();
		else if (numJobsRunning.get() == 0)
			allJobsDone();
	}
	
	
	// ----------------------------------------------------------------------------

	/** Finished, release the ok button etc. */
	public void allJobsDone() {

		// Update controls
		okButton.setDisable(false);
		stopButton.setDisable(true);
		progressIndicator.setVisible(false);
		allDone = true;
		App.log.println("\nDone!");
		
		if (interrupted) {
			updateStatusLabel("Status: JOBS STOPPED!", "status-error-label");
			statusGridPane.setDisable(true);
			interrupted = false;
		
		} else if (numJobsAborted.get() > 0) {
			updateStatusLabel("Status: FINISHED WITH ERRORS! (See console and log files for details)", "status-error-label");
		
		} else {
			updateStatusLabel("Status: DONE!", "status-success-label");
		}
	}

	
	// ----------------------------------------------------------------------------

	/** Print to console of this thread */
	public void print(String msg) {
		
		Platform.runLater(() -> {
			console.appendText(msg);
		});
	}

	
	// ============================================================================
	// PRIVATE

	/** Creates the alert dialog */
	private void initializeDialog(String headerText) {
		
		// The alert
    	alert = new Alert(AlertType.CONFIRMATION);
    	DialogPane dialog = alert.getDialogPane();
    	dialog.getStylesheets().add(
    			   getClass().getResource("GpsStyle.css").toExternalForm());

    	// Header text
    	alert.setTitle("Connectivity enrichment");
    	alert.setHeaderText(headerText);
    	
    	// The content
    	dialog.setContent(threadStatusVBox);
    	updateStatusLabel("Status: ONGOING", "status-ongoing-label");
    	
    	// Set the icon
    	progressIndicator = new ProgressIndicator();
        progressIndicator.setProgress(-1);
    	alert.setGraphic(progressIndicator);

    	// Disable the ok button
    	ButtonType stopButtonType = new ButtonType("Stop", ButtonData.CANCEL_CLOSE);
    	//ButtonType okButtonType = new ButtonType("OK", ButtonData.OK_DONE);
    	alert.getButtonTypes().setAll(stopButtonType, ButtonType.OK);
    	
        okButton = (Button) dialog.lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        stopButton = (Button) dialog.lookupButton(stopButtonType);
        stopButton.setOnAction((event) -> {
        	App.log.println();
        	if (!interrupted)
        		App.log.println("INTERRUPT: Waiting for threads ...");
        	else
        		App.log.println("Still waiting for threads to exit gracefully, this can take a minute ...");
            updateStatusLabel("Status: STOPPING JOBS ...", "status-error-label");
            interrupted = true;
        });

        // Allow the dialog to be closed only if the thread stopped (success, error, or interrupt)
        alert.setOnCloseRequest(event -> {
        	if (!allDone)
        		event.consume();
        });
        
    	// The console
    	console = new TextArea();
    	console.setEditable(false);
    	console.setWrapText(false);
    	console.getStyleClass().add("console-text");

    	console.setMaxWidth(Double.MAX_VALUE);
    	console.setMaxHeight(Double.MAX_VALUE);
    	console.setPrefWidth(650);
    	console.setPrefHeight(400);
    	GridPane.setVgrow(console, Priority.ALWAYS);
    	GridPane.setHgrow(console, Priority.ALWAYS);

    	// This could be a pane with tabs for multiple threads
    	GridPane expContent = new GridPane();
    	expContent.setMaxWidth(Double.MAX_VALUE);
    	Label label = new Label("Console");
    	expContent.add(label, 0, 0);
    	expContent.add(console, 0, 1);

    	// Set expandable Exception into the dialog pane.
    	dialog.setExpandableContent(expContent);
    	dialog.setExpanded(true);
    	
    	// Bindings
    	numQueuedLabel.textProperty().bind(Bindings.convert(numJobsQueued));
    	numRunningLabel.textProperty().bind(Bindings.convert(numJobsRunning));
    	numFinishedLabel.textProperty().bind(Bindings.convert(numJobsFinished));
    	numAbortedLabel.textProperty().bind(Bindings.convert(numJobsAborted));
    	
    	double maxWidth = 175;
    	numQueuedRectangle.widthProperty().bind(numJobsQueued.multiply(maxWidth/jobs.size()).add(1));
    	numRunningRectangle.widthProperty().bind(numJobsRunning.multiply(maxWidth/jobs.size()).add(1));
    	numFinishedRectangle.widthProperty().bind(numJobsFinished.multiply(maxWidth/jobs.size()).add(1));
    	numAbortedRectangle.widthProperty().bind(numJobsAborted.multiply(maxWidth/jobs.size()).add(1));
    	
    	// Bind the console to the jobs
		if (numCores == 1)
			for (JobMagnum job_i : jobs)
				job_i.setConsole(console);
	}
	
	
	// ----------------------------------------------------------------------------

	/** Initialize with a list of jobs */
	private void initializeJobs(ArrayList<JobMagnum> jobs) {

		if (jobs == null || jobs.isEmpty())
			throw new RuntimeException("Null or empty job list");
		
		this.jobs = jobs;
		allDone = false;
				
		numJobsQueued.set(jobs.size());
		numJobsRunning.set(0);
		numJobsFinished.set(0);
		numJobsAborted.set(0);
		nextJob = 0;

		for (JobMagnum job_i : jobs)
			job_i.setController(this);
	}

	
	// ----------------------------------------------------------------------------

	/** Update text and style of status label */
	private void updateStatusLabel(String msg, String styleClass) {
        statusLabel.setText(msg);
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add(styleClass);
	}
	
	
	// ----------------------------------------------------------------------------

	/** Called by jobFinished(), runs on the FX thread */
	private void startNextJob() {

		// Update counts before start for good measure
		int tbdJob = nextJob;
    	nextJob++;
    	reduce(numJobsQueued);
    	increment(numJobsRunning);
    	assert assertJobCountsConsistency();
    	
    	// Start the job
    	if (numCores == 1)
    		App.log.println("\n=========================================================================");
    	App.log.println("Running job:\t" + jobs.get(tbdJob).getJobName());
    	if (numCores == 1)
    		App.log.println("=========================================================================\n");
    	jobs.get(tbdJob).start();
	}
	
	
	// ----------------------------------------------------------------------------

	private void increment(IntegerProperty prop) {
		prop.set(prop.get() + 1);
	}
	
	private void reduce(IntegerProperty prop) {
		prop.set(prop.get() - 1);
	}


	// ----------------------------------------------------------------------------
	
	/** Always returns true, asserts that job counts are consistent */
	private boolean assertJobCountsConsistency() {
		
		int total = numJobsQueued.get() + numJobsRunning.get() + numJobsFinished.get() + numJobsAborted.get();
		assert total == jobs.size();
		assert nextJob == total - numJobsQueued.get();
		return true;
	}

	

	
	// ============================================================================
	// SETTERS AND GETTERS

	public boolean getInterrupted() { return interrupted; }
	public File getOutputDir() { return outputDir; }
	public void setOutputDir(File outputDir) { this.outputDir = outputDir; }

}
