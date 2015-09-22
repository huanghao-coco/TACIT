package edu.usc.cssl.tacit.topicmodel.lda.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

import edu.usc.cssl.tacit.common.Preprocess;
import edu.usc.cssl.tacit.common.ui.composite.from.TacitFormComposite;
import edu.usc.cssl.tacit.common.ui.composite.from.TwitterReadJsonData;
import edu.usc.cssl.tacit.common.ui.corpusmanagement.internal.ICorpus;
import edu.usc.cssl.tacit.common.ui.corpusmanagement.internal.ICorpusClass;
import edu.usc.cssl.tacit.common.ui.corpusmanagement.services.DataType;
import edu.usc.cssl.tacit.common.ui.corpusmanagement.services.ManageCorpora;
import edu.usc.cssl.tacit.common.ui.outputdata.OutputLayoutData;
import edu.usc.cssl.tacit.common.ui.validation.OutputPathValidation;
import edu.usc.cssl.tacit.common.ui.views.ConsoleView;
import edu.usc.cssl.tacit.topicmodel.lda.services.LdaAnalysis;
import edu.usc.cssl.tacit.topicmodel.lda.ui.internal.ILdaTopicModelClusterViewConstants;
import edu.usc.cssl.tacit.topicmodel.lda.ui.internal.LdaTopicModelViewImageRegistry;

public class LdaTopicModelView extends ViewPart implements
		ILdaTopicModelClusterViewConstants {
	public static final String ID = "edu.usc.cssl.tacit.topicmodel.lda.ui.view1";
	private ScrolledForm form;
	private FormToolkit toolkit;
	private Button preprocessEnabled;

	private OutputLayoutData layoutData;
	private OutputLayoutData inputLayoutData;
	private Text numberOfTopics;
	private Text prefixTxt;
	private LdaAnalysis lda = new LdaAnalysis();
	protected Job job;
	private Button wordWeights;
	private List<String> inputList;
	private String[] corpuraList;
	private ManageCorpora manageCorpora;
	

	@Override
	public void createPartControl(Composite parent) {
		toolkit = createFormBodySection(parent);
		Section section = toolkit.createSection(form.getBody(),
				Section.TITLE_BAR | Section.EXPANDED);

		GridDataFactory.fillDefaults().grab(true, false).span(3, 1)
				.applyTo(section);
		section.setExpanded(true);
		ScrolledComposite sc = new ScrolledComposite(section, SWT.H_SCROLL
				| SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false)
				.applyTo(sc);
		TacitFormComposite.addErrorPopup(form.getForm(), toolkit);
		TacitFormComposite.createEmptyRow(toolkit, sc);
		Composite client = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(true).numColumns(1)
				.applyTo(client);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(client);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		// create input data
		inputLayoutData = TacitFormComposite.createInputSection(toolkit,
				client, form.getMessageManager());
		Composite compInput;
		// Create pre process link
		compInput = inputLayoutData.getSectionClient();

		numberOfTopics = createAdditionalOptions(compInput, "No. of Topics :",
				"1");

		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(compInput);
		createPreprocessLink(compInput);

		Composite client1 = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(true).numColumns(1)
				.applyTo(client1);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(client1);
		
		/*TacitFormComposite.createEmptyRow(toolkit, sc);
		createCorpusSection(client1);
		TacitFormComposite.createEmptyRow(toolkit, sc);*/
		
		layoutData = TacitFormComposite.createOutputSection(toolkit,
				client1, form.getMessageManager());

		// we dont need stop word's as it will be taken from the preprocessor
		// settings

		Composite output = layoutData.getSectionClient();

		prefixTxt = createAdditionalOptions(output, "Output Prefix", "Lda");

		wordWeights = toolkit.createButton(output, "Create Word Weight File",
				SWT.CHECK);

		form.getForm().addMessageHyperlinkListener(new HyperlinkAdapter());
		// form.setMessage("Invalid path", IMessageProvider.ERROR);
		this.setPartName("LDA Topic Model");
		addButtonsToToolBar();
		toolkit.paintBordersFor(form.getBody());

	}

	private void createPreprocessLink(Composite client) {

		Composite clientLink = toolkit.createComposite(client);
		GridLayoutFactory.fillDefaults().equalWidth(false).numColumns(2)
				.applyTo(clientLink);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 1)
				.applyTo(clientLink);

		preprocessEnabled = toolkit.createButton(clientLink, "", SWT.CHECK);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 1)
				.applyTo(preprocessEnabled);
		final Hyperlink link = toolkit.createHyperlink(clientLink,
				"Preprocess", SWT.NONE);
		link.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		link.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String id = "edu.usc.cssl.tacit.common.ui.prepocessorsettings";
				PreferencesUtil.createPreferenceDialogOn(link.getShell(), id,
						new String[] { id }, null).open();
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(link);

	}

	private Text createAdditionalOptions(Composite sectionClient,
			String lblText, String defaultText) {
		Label simpleTxtLbl = toolkit.createLabel(sectionClient, lblText,
				SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0)
				.applyTo(simpleTxtLbl);
		Text simpleTxt = toolkit.createText(sectionClient, "", SWT.BORDER);
		simpleTxt.setText(defaultText);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0)
				.applyTo(simpleTxt);
		return simpleTxt;
	}

	private FormToolkit createFormBodySection(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);

		toolkit.decorateFormHeading(form.getForm());
		form.setText("LDA Topic Model"); //$NON-NLS-1$
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(true)
				.applyTo(form.getBody());
		return toolkit;
	}

	private void addButtonsToToolBar() {
		IToolBarManager mgr = form.getToolBarManager();
		mgr.add(new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (LdaTopicModelViewImageRegistry.getImageIconFactory()
						.getImageDescriptor(IMAGE_LRUN_OBJ));
			}

			@Override
			public String getToolTipText() {
				return "Analyze";
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jface.action.Action#run()
			 */
			@Override
			public void run() {
				final int noOfTopics = Integer
						.valueOf(numberOfTopics.getText()).intValue();
				final boolean isPreprocess = preprocessEnabled.getSelection();
				final String inputPath = inputLayoutData.getOutputLabel()
						.getText();
				final String outputPath = layoutData.getOutputLabel().getText();
				final String preFix = prefixTxt.getText();
				final boolean wordWeightFile = wordWeights.getSelection();
				TacitFormComposite.writeConsoleHeaderBegining("Topic Modelling started  ");
				job = new Job("Analyzing...") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						TacitFormComposite.setConsoleViewInFocus();
						TacitFormComposite.updateStatusMessage(
								getViewSite(), null, null, form);
						monitor.beginTask("TACIT started analyzing...", 100);
						List<String> inputFiles = new ArrayList<String>();
						String topicModelDirPath = inputPath;
						Preprocess preprocessTask = new Preprocess("ZLabel");
						if (isPreprocess) {
							monitor.subTask("Preprocessing...");
							//preprocessTask = new Preprocess("ZLabel");
							try {
								File[] inputFile = new File(inputPath)
										.listFiles();
								for (File iFile : inputFile) {
									if (iFile.getAbsolutePath().contains(
											"DS_Store"))
										continue;
									inputFiles.add(iFile.toString());

								}
								topicModelDirPath = preprocessTask
										.doPreprocessing(inputFiles, "");

							} catch (IOException e) {
								e.printStackTrace();
							}
							monitor.worked(10);
						}

						lda.initialize(topicModelDirPath, noOfTopics,
								outputPath, preFix, wordWeightFile);

						// lda processsing
						long startTime = System.currentTimeMillis();
						Date dateObj = new Date();
						monitor.subTask("Topic Modelling...");
						try {
							lda.doLDA(monitor,dateObj);
						} catch (FileNotFoundException e) {
							TacitFormComposite.writeConsoleHeaderBegining("<terminated> Topic Modelling   ");
							e.printStackTrace();
							return Status.CANCEL_STATUS;
						} catch (IOException e) {
							monitor.done();
							TacitFormComposite.writeConsoleHeaderBegining("<terminated> Topic Modelling   ");
							return Status.CANCEL_STATUS;
						}
						monitor.worked(20);
						ConsoleView.printlInConsoleln("LDA Topic Modelling completed successfully in "
										+ (System.currentTimeMillis() - startTime)
										+ " milliseconds.");

						if (monitor.isCanceled()) {
							TacitFormComposite.writeConsoleHeaderBegining("<terminated> Topic Modelling   ");
							return Status.CANCEL_STATUS;
						}
						if (isPreprocess) preprocessTask.clean();
						monitor.worked(10);
						monitor.done();
						TacitFormComposite.writeConsoleHeaderBegining("<terminated> Topic Modelling   ");
						TacitFormComposite.updateStatusMessage(
								getViewSite(), "LDA analysis completed",
								IStatus.OK, form);

						return Status.OK_STATUS;
					}
				};
				job.setUser(true);
				if (canProceedCluster()) {
					job.schedule();
				} else {
					TacitFormComposite
							.updateStatusMessage(
									getViewSite(),
									"LDA Topic Modelling cannot be started. Please check the Form status to correct the errors",
									IStatus.ERROR, form);
				}

			}

		});
		Action helpAction = new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (LdaTopicModelViewImageRegistry.getImageIconFactory()
						.getImageDescriptor(IMAGE_HELP_CO));
			}

			@Override
			public String getToolTipText() {
				return "Help";
			}

			@Override
			public void run() {
				PlatformUI
						.getWorkbench()
						.getHelpSystem()
						.displayHelp(
								"edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
			};
		};
		mgr.add(helpAction);
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(helpAction,
						"edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(form,
						"edu.usc.cssl.tacit.topicmodel.lda.ui.lda");
		form.getToolBarManager().update(true);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	private boolean canProceedCluster() {
		TacitFormComposite.updateStatusMessage(getViewSite(), null, null, form);
		boolean canProceed = true;
		form.getMessageManager().removeMessage("location");
		form.getMessageManager().removeMessage("inputlocation");
		form.getMessageManager().removeMessage("prefix");
		form.getMessageManager().removeMessage("topic");
		String message = OutputPathValidation.getInstance()
				.validateOutputDirectory(layoutData.getOutputLabel().getText(),
						"Output");
		if (message != null) {

			message = layoutData.getOutputLabel().getText() + " " + message;
			form.getMessageManager().addMessage("location", message, null,
					IMessageProvider.ERROR);
			canProceed = false;
		}

		String inputMessage = OutputPathValidation.getInstance()
				.validateOutputDirectory(
						inputLayoutData.getOutputLabel().getText(), "Input");
		if (inputMessage != null) {

			inputMessage = inputLayoutData.getOutputLabel().getText() + " "
					+ inputMessage;
			form.getMessageManager().addMessage("inputlocation", inputMessage,
					null, IMessageProvider.ERROR);
			canProceed = false;
		}

		if (prefixTxt.getText().length() < 1) {
			form.getMessageManager().addMessage("prefix",
					"Prefix Cannout be empty", null, IMessageProvider.ERROR);
			canProceed = false;
		}

		if (Integer.parseInt(numberOfTopics.getText()) < 1) {
			form.getMessageManager().addMessage("topic",
					"Number of topics cannot be less than 1", null,
					IMessageProvider.ERROR);
			canProceed = false;
		}
		return canProceed;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == Job.class) {
			return job;
		}
		return super.getAdapter(adapter);
	}
	
	/*private void createCorpusSection(Composite client) {

		Group group = new Group(client, SWT.SHADOW_IN);
		group.setText("Input Type");

		// group.setBackground(client.getBackground());
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		group.setLayout(layout);

		final Button corpusEnabled = new Button(group, SWT.CHECK);
		corpusEnabled.setText("Use Corpus");
		corpusEnabled.setBounds(10, 10, 10, 10);
		corpusEnabled.pack();

		// TacitFormComposite.createEmptyRow(toolkit, group);

		final Composite sectionClient = new Composite(group, SWT.None);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1)
				.applyTo(sectionClient);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false)
				.applyTo(sectionClient);
		sectionClient.pack();

		// Create a row that holds the textbox and browse button
		final Label inputPathLabel = new Label(sectionClient, SWT.NONE);
		inputPathLabel.setText("Select Corpus:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0)
				.applyTo(inputPathLabel);

		final Combo cmbSortType = new Combo(sectionClient, SWT.FLAT
				| SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0)
				.applyTo(cmbSortType);
		manageCorpora = new ManageCorpora();
		corpuraList = manageCorpora.getNames();
		cmbSortType.setItems(corpuraList);
		cmbSortType.setEnabled(false);

		corpusEnabled.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (corpusEnabled.getSelection()) {
					cmbSortType.setEnabled(true);

				} else {
					cmbSortType.setEnabled(false);
				}
			}
		});

		cmbSortType.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				ICorpus selectedCorpus = manageCorpora
						.readCorpusById(corpuraList[cmbSortType
								.getSelectionIndex()]);
				if (inputList == null) {
					inputList = new ArrayList<String>();
				}
				if (selectedCorpus.getDatatype().equals(DataType.TWITTER_JSON)) {
					TwitterReadJsonData twitterReadJsonData = new TwitterReadJsonData();
					for (ICorpusClass cls : selectedCorpus.getClasses()) {
						inputList.add(twitterReadJsonData
								.retrieveTwitterData(cls.getClassPath()));
					}
				} else if (selectedCorpus.getDatatype().equals(
						DataType.REDDIT_JSON)) {
					// TO-Do
				} else if (selectedCorpus.getDatatype().equals(
						(DataType.PLAIN_TEXT))) {
					// TO-Do
				} else if (selectedCorpus.getDatatype().equals(
						(DataType.MICROSOFT_WORD))) {
					// TO-Do
				} else if (selectedCorpus.getDatatype().equals((DataType.XML))) {
					// TO-Do
				}
				//inputLayoutData.refreshInternalTree(inputList);
			}
		});
		TacitFormComposite.createEmptyRow(null, sectionClient);
	}*/

}
