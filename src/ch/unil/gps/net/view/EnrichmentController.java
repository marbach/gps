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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

import ch.unil.gps.App;
import ch.unil.gps.AppSettings;
import ch.unil.gps.net.control.JobEnrichment;
import ch.unil.gps.net.control.JobMagnum;
import ch.unil.gps.net.model.NetworkModel;
import ch.unil.gps.view.*;
import ch.unil.gpsutils.FileExport;
import edu.mit.magnum.MagnumSettings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.Alert.AlertType;
import javafx.util.converter.NumberStringConverter;


/**
 * Controller for "Connectivity Enrichment" pane 
 */
public class EnrichmentController extends ViewController {

	/** Reference to the selected networks */
	private LinkedHashSet<TreeItem<NetworkModel>> selectedNetworks; 

	/** Bound to geneScoreTextField */
	private ObjectProperty<File> geneScoreFileProperty = new SimpleObjectProperty<>();
	/** Bound to outputDirTextField */
	private ObjectProperty<File> outputDirProperty = new SimpleObjectProperty<>();
	/** Bound to numPermutationsTextField */
	private IntegerProperty numPermutationsProperty = new SimpleIntegerProperty();
	
	/** The enrichment score / p-value file */
	private ObjectProperty<File> pvalFileProperty = new SimpleObjectProperty<>();
	
	/** Writes the enrichment scores for each job */
	private FileExport scoreWriter;
	
	/** Flag to disable the warning for multiple cores */
	private boolean disableNumCoresWarning = false;
	
	/** The text used for the example gene score file */
	private final String exampleGeneScoreText = "Example: advanced macular degeneration, neovascular";
	/** The text used for the example pval file */
	private final String examplePvalText = "Example: psychiatric, cross-disorder (Fig. 6a, Marbach et al.)";
	

	// ============================================================================
	// FXML

    /** Input */
    @FXML
    private TextField networksTextField;
    @FXML
    private TextField geneScoreTextField; // bound
    @FXML
    private Button geneScoreBrowseButton;
    @FXML
    private CheckBox usePrecomputedKernelsCheckBox;
    @FXML
    private Hyperlink geneScoreDownloadLink;
    @FXML
    private Hyperlink geneScoreExampleLink;
    @FXML
    private Hyperlink pascalDownloadLink;
    
    /** Output */
    @FXML
    private TextField outputDirTextField; // bound
    @FXML
    private Button outputDirBrowseButton;
    @FXML
    private CheckBox exportKernelsCheckBox;
    
    /** Parameters */
    @FXML
    private TextField numPermutationsTextField; // bound
    @FXML
    private Label numPermutationsLabel;
    @FXML
    private CheckBox excludeHlaGenesCheckBox;
    @FXML
    private Label excludeHlaGenesLabel;
    @FXML
    private CheckBox excludeXYChromosomesCheckBox;
    @FXML
    private Label excludeXYChromosomesLabel;
    @FXML
    private ChoiceBox<Integer> numCoresChoiceBox;
    @FXML
    private Label numCoresLabel;
    @FXML
    private Button exportSettingsButton;
    @FXML
    private Button runButton;
    
    /** Results */
    @FXML
    private TextField pvalFileTextField;
    @FXML
    private Button pvalFileBrowseButton;
    @FXML
	private Button plotButton;
    @FXML
    private CheckBox bonferroniCheckBox;
    @FXML
    private Hyperlink downloadRScriptsLink;
    @FXML
    private Hyperlink exampleFileLink;
    
    
	// ============================================================================
	// PUBLIC METHODS
    
    /** Initialize */
    public void initialize(NetworkAnalysisController networkAnalysisController) {

    	selectedNetworks = 
    			networkAnalysisController.getOtherNetworksController().getSelectedNetworks();
    	
    	// Number of permutations
    	numPermutationsTextField.textProperty().addListener(new ChangeListener<String>() {
    	    @Override 
    	    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
    	        if (!newValue.matches("\\d*")) {
    	        	numPermutationsTextField.setText(oldValue);
    	        	numPermutationsTextField.positionCaret(numPermutationsTextField.getLength());
    	        }
    	    }
    	});
    	
    	// Number of cores
    	int numCoresSyst = Runtime.getRuntime().availableProcessors();
    	for (int i=1; i<=numCoresSyst; i++)
    		numCoresChoiceBox.getItems().add(i);
    	numCoresChoiceBox.getSelectionModel().selectFirst();
    	numCoresChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
    		if (!disableNumCoresWarning && newValue > 1) {
    			Alert alert = new Alert(AlertType.WARNING);
    			alert.setTitle("Warning");
    			alert.setHeaderText("Are you sure your computer can handle " + newValue + " parallel jobs?");
    			alert.setContentText("The analysis is both COMPUTE and MEMORY intensive. Rule of thumb:\n\n"
    					+ "\t1 job = 1 core and <8 GB memory (depends on network size).\n\n"
    					+ "Example: on a high-end laptop with 4 cores and 16 GB memory, you can run 2 parallel jobs. "
    					+ "If you launch more jobs, performance will degrade. If you have access to a cluster,"
    					+ "we recommend that you use the magnum command-line tool to run jobs in parallel."); 
    			alert.showAndWait();
    		}
    	});
    	
    	// Bindings
        Bindings.bindBidirectional(outputDirTextField.textProperty(), outputDirProperty, new FileStringConverter());
        Bindings.bindBidirectional(numPermutationsTextField.textProperty(), numPermutationsProperty, new NumberStringConverter("###"));
        //numPermutationsTextField.textProperty().bindBidirectional(numPermutationsProperty, new NumberStringConverter());

        //Bindings.bindBidirectional(geneScoreTextField.textProperty(), geneScoreFileProperty, new FileStringConverter(true));
        geneScoreFileProperty.addListener((observable, oldValue, newValue) -> {
        	File file = (File) newValue;
    		if (file == null)
    			geneScoreTextField.setText(exampleGeneScoreText);
    		else
    			geneScoreTextField.setText(file.getName());
        });
        		
        //Bindings.bindBidirectional(pvalFileTextField.textProperty(), pvalFileProperty, new FileStringConverter());
        pvalFileProperty.addListener((observable, oldValue, newValue) -> {
        	File file = (File) newValue;
    		if (file == null)
    			pvalFileTextField.setText(examplePvalText);
    		else
    			pvalFileTextField.setText(file.getName());
        });
        
        // Example texts
        if (geneScoreFileProperty.get() == null)
        	geneScoreTextField.setText(exampleGeneScoreText);
        pvalFileTextField.setText(examplePvalText);
        
        // Tooltips
        initTooltips();
    }
    
    
    // ----------------------------------------------------------------------------

    /** Initialize with settings from AppSettings */
    @Override
    public void loadPreferences() {
   	    	
        geneScoreFileProperty.set(getFilePreference("geneScoreFile"));
        outputDirProperty.set(getFilePreference("outputDir"));
        
        usePrecomputedKernelsCheckBox.setSelected(prefs.getBoolean("usePrecomputedKernels", true));
        exportKernelsCheckBox.setSelected(prefs.getBoolean("exportKernels", false));
        excludeHlaGenesCheckBox.setSelected(prefs.getBoolean("excludeHlaGenes", true));
        excludeXYChromosomesCheckBox.setSelected(prefs.getBoolean("excludeXYChromosomes", true));
        bonferroniCheckBox.setSelected(prefs.getBoolean("bonferroni", true));
        numPermutationsProperty.set(prefs.getInt("numPermutations", 10000));
        
        // Initialize stuff that's not saved
    	pvalFileProperty.set(null);

        Platform.runLater(() -> {
        	disableNumCoresWarning = true;
            numCoresChoiceBox.getSelectionModel().select(prefs.getInt("numCores", 0));
            disableNumCoresWarning = false;
        }); 
    }

    
    // ----------------------------------------------------------------------------

    /** Initialize with settings from AppSettings */
    @Override
    public void savePreferences() {

    	saveFilePreference("geneScoreFile", geneScoreFileProperty.get());
    	saveFilePreference("outputDir", outputDirProperty.get());

    	prefs.putBoolean("usePrecomputedKernels", usePrecomputedKernelsCheckBox.isSelected());
    	prefs.putBoolean("exportKernels", exportKernelsCheckBox.isSelected());
    	prefs.putBoolean("excludeHlaGenes", excludeHlaGenesCheckBox.isSelected());
    	prefs.putBoolean("excludeXYChromosomes", excludeXYChromosomesCheckBox.isSelected());
    	prefs.putBoolean("bonferroni", bonferroniCheckBox.isSelected());

    	prefs.putInt("numPermutations", numPermutationsProperty.get());
    	prefs.putInt("numCores", numCoresChoiceBox.getSelectionModel().getSelectedIndex());    	
    }


    // ----------------------------------------------------------------------------

    /** Called when networks have been selected */
    public void networkSelectionUpdated() {

    	int numNetworks = selectedNetworks.size();
    	if (numNetworks == 0)
    		networksTextField.setText(null);
    	else if (numNetworks == 1)
			networksTextField.setText(selectedNetworks.iterator().next().getValue().getName());
		else
			networksTextField.setText(numNetworks + " networks selected");
    }
    
	
    // ----------------------------------------------------------------------------

		
    /** Get kernel dir based on current output dir */
    public File getKernelDir() {
    	
    	if (outputDirProperty.get() == null)
    		return null;
    	Path outputDirPath = outputDirProperty.get().toPath();
    	Path kernelDirPath = outputDirPath.resolve("network_kernels"); 
    	return kernelDirPath.toFile();
    }

    
    // ----------------------------------------------------------------------------

    /** Get the job name for this network (geneScoreName--networkName) */
    public void writeScore(String networkName, double score, String settingsFile) {
    	
    	if (scoreWriter == null)
    		initScoreWriter();
    	
    	scoreWriter.println(networkName + "\t" + App.mag.utils.toStringScientific10(score) + "\t" + settingsFile);
    	scoreWriter.flush();
    }

    
	// ----------------------------------------------------------------------------
	
    /** Apply settings from the given magnum settings instance */
    public void applySettings(MagnumSettings set) {
    	
    	usePrecomputedKernelsCheckBox.setSelected(set.usePrecomputedKernels);
    	geneScoreFileProperty.set(set.geneScoreFile_);
    	outputDirProperty.set(set.outputDirectory_);
    	exportKernelsCheckBox.setSelected(set.exportKernels);
    	numPermutationsProperty.set(set.numPermutations_);
    	excludeHlaGenesCheckBox.setSelected(set.excludeHlaGenes_);
    	excludeXYChromosomesCheckBox.setSelected(set.excludeXYChromosomes_);    	
    }


    
	// ============================================================================
	// HANDLES

    /** Gene score browse button */
    @FXML
    private void handleGeneScoreBrowseButton() {
    	chooseFile(geneScoreFileProperty, "Select a gene score file");
    }


    // ----------------------------------------------------------------------------

    /** Gene score download link */
    @FXML
    private void handleGeneScoreDownloadLink() {  	
    	openWebpage(AppSettings.geneScoresLink);
    }

    /** PASCAL download link */
    @FXML
    private void handlePascalDownloadLink() {
    	openWebpage(AppSettings.pascalLink);
    }

    /** Example gene scores link */
    @FXML
    private void handleExampleGeneScoresLink() {  	
        geneScoreFileProperty.set(null);
    }

    
    // ----------------------------------------------------------------------------

    /** Output directory browse button */
    @FXML
    private void handleOutputDirBrowseButton() {
    	chooseDir(outputDirProperty, "Choose output directory");
    }

    
    // ----------------------------------------------------------------------------

    /** Export settings button */
    @FXML
    private void handleExportSettingsButton() {
    	
    	if (!checkOptions())
    		return;
    			
		if (showExportSettingsConfirmation(selectedNetworks.size()) != ButtonType.OK)
			return;
    	
    	for (TreeItem<NetworkModel> item_i : selectedNetworks) {
    		JobEnrichment job = new JobEnrichment(null, getJobName(item_i.getValue()), this, item_i.getValue());
    		job.writeSettingsFile(App.log);
    	}
    }

    
    // ----------------------------------------------------------------------------

    /** Run button */
    @FXML
    private void handleRunButton() {

    	// Check that required options are set
    	if (!checkOptions())
    		return;
    	    	
    	if (selectedNetworks.size() > 1) {
    		if (showMultipleNetworksWarning(selectedNetworks.size()) != ButtonType.OK)
    			return;
    	}
    	
    	// Disable the main window
    	app.getRootLayout().setDisable(true);

		// Export example gene scores
    	boolean exportGeneScores = geneScoreFileProperty.get() == null;
		if (exportGeneScores) {
			// Create the example_data directory
			File exampleDir = new File(outputDirProperty.get(), "example_data");
			exampleDir.mkdirs();
			App.log.println("Exporting example gene scores to: " + exampleDir.getPath());

			// Copy gene scores from jar to example_data
			geneScoreFileProperty.set(
					AppSettings.exportResource("ch/unil/gps/net/resources/macular_degeneration_neovascular.txt", exampleDir));			
		}
		
    	// Create the thread controller / dialog
    	JobController jobManager = (JobController) ViewController.loadFxml("view/ThreadStatus.fxml");
    	jobManager.setOutputDir(outputDirProperty.get()); // Has to be done before creating the jobs

    	// Create a job for each network
    	ArrayList<JobMagnum> jobs = new ArrayList<>();
    	for (TreeItem<NetworkModel> item_i : selectedNetworks) {
    		JobEnrichment job_i = new JobEnrichment(jobManager, getJobName(item_i.getValue()), this, item_i.getValue());
    		jobs.add(job_i);
    	}
    	
    	// A new score writer will be created when the first result is ready
    	scoreWriter = null;

    	// Start the jobs
    	int numCores = numCoresChoiceBox.getSelectionModel().getSelectedItem();
		jobManager.start(jobs, numCores); 
		
		// Cleanup
		if (scoreWriter != null) {
			scoreWriter.close();
			pvalFileProperty.set(scoreWriter.getFile());
		}
		if (exportGeneScores)
			geneScoreFileProperty.set(null);
    	app.getRootLayout().setDisable(false);
    	plotButton.setDisable(false);
    	System.gc();
    }

    
    // ----------------------------------------------------------------------------

    /** Run button */
    @FXML
    private void handlePvalFileBrowseButton() {

    	chooseFile(pvalFileProperty, "Select an enrichment p-value file");
    	// Enable plot button
    	plotButton.setDisable(false);
    }
    
    
    // ----------------------------------------------------------------------------

    /** Run button */
    @FXML
    private void handlePlotButton() {

    	EnrichmentPlotController controller = new EnrichmentPlotController(pvalFileProperty.get(), bonferroniCheckBox.isSelected());
    	controller.show();
    }
    

    // ----------------------------------------------------------------------------

    /** Plot example results */
    @FXML
    private void handleExamplePvalsLink() {  	
    	// Null now sets the example promt text and will load the resource from the jar
        pvalFileProperty.set(null);
    }

    
    // ----------------------------------------------------------------------------

    /** PASCAL download link */
    @FXML
    private void handleDownloadRScriptsLink() {
    	openWebpage(AppSettings.downloadRScriptsLink);
    }

    
	// ============================================================================
	// PRIVATE METHODS

    /** Construct commands for command-line tool based on specified options */
    private boolean checkOptions() {
    	
    	String errors = "";
    	if (selectedNetworks.isEmpty())
    		errors += "- No networks selected\n";
    	//if (geneScoreFileProperty.get() == null)
    		//errors += "- No GWAS gene score file selected\n";
    	if (outputDirProperty.get() == null)
    		errors += "- No output directory selected\n";
    	
    	if (!errors.equals("")) {
    		Alert alert = new Alert(AlertType.ERROR);
    		alert.setTitle("Error");
    		alert.setHeaderText("Not all required options are set!");
    		alert.setContentText("Errors:\n" + errors); 
    		alert.showAndWait();
			//App.log.closeLogFile(); // Why the ... would we do that here?
    		return false;
    	} else
    		return true;
    }

    
    // ----------------------------------------------------------------------------

    /** Show a warning before launching job with multiple networks */
    private ButtonType showExportSettingsConfirmation(int numNetworks) {

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().setPrefWidth(540);
		alert.setTitle("Export settings");
		String s = (numNetworks > 1) ? "s" : "";
		alert.setHeaderText("Writing settings file" + s + " for " + numNetworks + " network" + s);
		alert.setContentText(
				"Output directory:\n" +
				outputDirProperty.get().getPath() + "\n\n" +
				"Settings files can be used to:\n\n" + 
				"(1) Run jobs from the command line (typically on a computing cluster)\n" +
				"(2) Reload the settings in the App (click the \"Settings\" button)\n\n" +
				"TIP: Settings files are also saved when launching a run, keeping them together with your results ensures reproducibility!\n\n" +
				"See the exported settings file and user guide for further instructions.");

		Optional<ButtonType> result = alert.showAndWait();
		return result.get();
    }
    
    
    // ----------------------------------------------------------------------------

    /** Show a warning before launching job with multiple networks */
    private ButtonType showMultipleNetworksWarning(int numNetworks) {
    	
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().setPrefWidth(540);
		alert.setTitle("Start job");
		alert.setHeaderText("Compute connectivity enrichment for " + numNetworks + " networks?");
		alert.setContentText(
				"Runtime is ~15min for 1 network on high-end laptop using default settings.\n\n" +
				"Multiple networks can be run in parallel (using >1 cores)\n\n" +
				"With the command-line tool you can run these jobs on your compting cluster:\n\n" + 
				"1. Use the \"Export settings\" button to save a text file with your settings\n" +
				"2. Download the Magnum command-line tool from regulatorycircuits.org\n" +
				"3. Run Magnum from the command line with the option \"--set <settings_file>\"\n\n" +
				"See the exported settings file and user guide for further instructions.");

		Optional<ButtonType> result = alert.showAndWait();
		return result.get();
    }
    

    // ----------------------------------------------------------------------------

    /** Get the job name for this network (geneScoreName--networkName) */
    private String getJobName(NetworkModel network) {
    	String gwasName = App.mag.utils.extractBasicFilename(geneScoreFileProperty.get().getName(), false);
    	String jobName = gwasName	+ "--" + App.mag.utils.extractBasicFilename(network.getFile().getName(), false);
    	return jobName;
    }

    
    // ----------------------------------------------------------------------------

    /** Get the job name for this network (geneScoreName--networkName) */
    private void initScoreWriter() {
    	
    	App.log.println("Creating gene score result file ...");
    	// The output file
    	String gwasName = App.mag.utils.extractBasicFilename(geneScoreFileProperty.get().getName(), false);
    	String fileprefix = gwasName + ".pvals";
    	File file = new File(outputDirProperty.get(), fileprefix + ".txt");
    	
    	// If it already exists, don't overwrite - add index
    	if (file.exists()) {
    		for (int i=1; i<Integer.MAX_VALUE; i++) {
    			file = new File(outputDirProperty.get(), fileprefix + "." + i + ".txt");
    			if (!file.exists())
    				break;
    		}
    	}
    	scoreWriter = new FileExport(App.log, file);
    	
    	// Write header
    	scoreWriter.println("# GWAS = " + gwasName);
    	scoreWriter.println("Network\tPvalue\tSettings");
    	scoreWriter.flush();
    }

    
    // ----------------------------------------------------------------------------

    /** Add tooltips */
    private void initTooltips() {
    	
    	networksTextField.setTooltip(new Tooltip(
    			"Select at least one network\n" +
    			"in the table on the left"));

    	Tooltip tip = new Tooltip("Choose your GWAS results file (gene-level\n" +
    			"p-values, e.g., computed using PASCAL)");
    	geneScoreTextField.setTooltip(tip);
    	geneScoreBrowseButton.setTooltip(tip);
    	geneScoreDownloadLink.setTooltip(new Tooltip(
    			"Download a collection of GWAS gene scores\n"
    			+ "(37 traits used by Marbach et al.)"));
    	geneScoreExampleLink.setTooltip(new Tooltip(
    			"Use included example GWAS\n" +
    			"(see tutorial for explanations)"));

    	pascalDownloadLink.setTooltip(new Tooltip(
    			"Download the PASCAL tool to compute gene scores\n"
    			+ "from GWAS summary statistics (SNP p-values)"));
    	
    	usePrecomputedKernelsCheckBox.setTooltip(new Tooltip(
    			"Check if precomputed network kernels\nare available at:\n" +
    			"<output_directory>/network_kernels"));

    	exportKernelsCheckBox.setTooltip(new Tooltip(
    			"Export network kernels to compressed text files.\n" +
    			"WARNING: File sizes can be >1GB! Only useful for\n" +
    			"running many GWASs on the same network."));
    	
    	tip = new Tooltip(
    			"Select directory for\n" +
    			"result and log files");
    	outputDirTextField.setTooltip(tip);
    	outputDirBrowseButton.setTooltip(tip);
    	
    	tip = new Tooltip(
    			"Number of permutations N to estimate p-values:\n" +
    			"- Lower bound for p-values is 1/N\n" + 
    			"- Runtime scales linearly with N");
    	numPermutationsTextField.setTooltip(tip);
    	numPermutationsLabel.setTooltip(tip);
    	
    	tip = new Tooltip(
    			"Exclude all genes in the HLA region (exceptionally\n" +
    			"strong LD and associations for some immune traits)");
    	excludeHlaGenesCheckBox.setTooltip(tip);
    	excludeHlaGenesLabel.setTooltip(tip);
    	
    	tip = new Tooltip("Exclude the sex chromosomes");
    	excludeXYChromosomesCheckBox.setTooltip(tip);
    	excludeXYChromosomesLabel.setTooltip(tip);
    	
    	tip = new Tooltip(
    			"Number of jobs launched in parallel. Rule of thumb:\n" +
    			"1 job = 1 core and <8 GB memory (depends on network size)");
    	numCoresChoiceBox.setTooltip(tip);
    	numCoresLabel.setTooltip(tip);
    	
    	exportSettingsButton.setTooltip(new Tooltip(
    			"Export files with the current settings, can be used to:\n" +
    			"(1) Run jobs from the command line (typically on a computing cluster)\n" +
    			"(2) Reload the settings in the App (click the \"Settings\" button)"));
    	
    	runButton.setTooltip(new Tooltip("Run enrichment analysis\nfor all selected networks"));
    	
    	tip = new Tooltip(
    			"Result file with the enrichment p-value for each network.\n" +
    			"The file can be found in the output directory of your run:\n" +
    			"\t<gwas_name>.pvals.txt");
    	pvalFileTextField.setTooltip(tip);
    	pvalFileBrowseButton.setTooltip(tip);
    	
    	bonferroniCheckBox.setTooltip(new Tooltip(
    			"Adjust the p-values of the N networks\n" +
    			"using Bonferroni correction"));
    	
    	plotButton.setTooltip(new Tooltip(
    			"Generate enrichment score plot\n" +
    			"(similar to Fig. 6, Marbach et al.)"));
    	
    	downloadRScriptsLink.setTooltip(new Tooltip(
    			"Download R-scripts\n" +
    			"to plot results"));
    	exampleFileLink.setTooltip(new Tooltip(
    			"Plot results for the psychiatric, cross-\n" +
    			"disorder study (Fig. 6a, Marbach et al.)"));
    }

    
	// ============================================================================
	// SETTERS AND GETTERS

    public File getGeneScoreFile() { return geneScoreFileProperty.get(); }
    public File getOutputDir() { return outputDirProperty.get(); }
    
    public int getNumPermutations() { return numPermutationsProperty.get(); }
    
    public boolean getExcludeHlaGenes() { return excludeHlaGenesCheckBox.isSelected(); }
    public boolean getExcludeXYChromosomes() { return excludeXYChromosomesCheckBox.isSelected(); }
    
    public boolean getUsePrecomputedKernels() { return usePrecomputedKernelsCheckBox.isSelected(); }
    public boolean getExportKernels() { return exportKernelsCheckBox.isSelected(); }
}
