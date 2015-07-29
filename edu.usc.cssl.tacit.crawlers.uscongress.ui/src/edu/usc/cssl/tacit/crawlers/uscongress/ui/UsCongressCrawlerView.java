package edu.usc.cssl.tacit.crawlers.uscongress.ui;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.ViewPart;

import edu.usc.cssl.tacit.common.ui.CommonUiActivator;
import edu.usc.cssl.tacit.common.ui.composite.from.TacitFormComposite;
import edu.usc.cssl.tacit.common.ui.outputdata.OutputLayoutData;
import edu.usc.cssl.tacit.common.ui.validation.OutputPathValidation;
import edu.usc.cssl.tacit.common.ui.views.ConsoleView;
import edu.usc.cssl.tacit.crawlers.uscongress.services.AvailableRecords;
import edu.usc.cssl.tacit.crawlers.uscongress.services.UsCongressCrawler;
import edu.usc.cssl.tacit.crawlers.uscongress.ui.internal.IUsCongressCrawlerViewConstants;
import edu.usc.cssl.tacit.crawlers.uscongress.ui.internal.UsCongressCrawlerViewImageRegistry;

public class UsCongressCrawlerView extends ViewPart implements IUsCongressCrawlerViewConstants {
	public static String ID = "edu.usc.cssl.tacit.crawlers.uscongress.ui.uscongresscrawlerview";
	private ScrolledForm form;
	private FormToolkit toolkit;
	
	private OutputLayoutData outputLayout;
	private Combo cmbCongress;
	
	private String[] allSenators;
	private String[] congresses;
	private String[] congressYears;
	
	private Date maxDate;
	private Date minDate;
	private SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
	private Button dateRange;
	
	private DateTime toDate;
	private DateTime fromDate;
	private Button limitRecords;
	private Text limitText;
	
	private int totalSenators;
	private int progressSize = 100;
	private Button sortByDateYes;
	private Button sortByDateNo;
	private Table senatorTable;
	private Button removeSenatorButton;
	private SenatorListDialog senatorListDialog;
	private LinkedHashSet<String> senatorList;
	private ArrayList<String> selectedSenators;
	private Button addSenatorBtn;
	
	String previousSelectedCongress = "";
	String[] availabileSenators;
	
	@Override
	public void createPartControl(Composite parent) {
		// Creates toolkit and form
		toolkit = createFormBodySection(parent, "US Congress Crawler");
		Section section = toolkit.createSection(form.getBody(), Section.TITLE_BAR | Section.EXPANDED);
		GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(section);
		section.setExpanded(true);
		form.setImage(UsCongressCrawlerViewImageRegistry.getImageIconFactory().getImage(IUsCongressCrawlerViewConstants.IMAGE_US_CONGRESS_OBJ));

		// Create a composite to hold the other widgets
		ScrolledComposite sc = new ScrolledComposite(section, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(sc);
	
		// Creates an empty to create a empty space
		TacitFormComposite.createEmptyRow(toolkit, sc);

		// Create a composite that can hold the other widgets
		Composite client = toolkit.createComposite(form.getBody());
		GridLayoutFactory.fillDefaults().equalWidth(true).numColumns(1).applyTo(client); // Align the composite section to one column
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1).applyTo(client);		
		
		createSenateInputParameters(client);
		TacitFormComposite.createEmptyRow(toolkit, client);
		outputLayout = TacitFormComposite.createOutputSection(toolkit, client, form.getMessageManager());
		// Add run and help button on the toolbar
		addButtonsToToolBar();	
	}
	

	private void createSenateInputParameters(Composite client) {
		Section inputParamsSection = toolkit.createSection(client, Section.TITLE_BAR | Section.EXPANDED | Section.DESCRIPTION);
		GridDataFactory.fillDefaults().grab(true, false).span(1, 1).applyTo(inputParamsSection);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(inputParamsSection);
		inputParamsSection.setText("Input Parameters");
		
		ScrolledComposite sc = new ScrolledComposite(inputParamsSection, SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);

		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(sc);
		
		Composite sectionClient = toolkit.createComposite(inputParamsSection);
		sc.setContent(sectionClient);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sc);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(sectionClient);
		inputParamsSection.setClient(sectionClient);
		
		String[] loading = {"Loading..."};

		Label congressLabel = toolkit.createLabel(sectionClient, "Congress:", SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(congressLabel);
		cmbCongress = new Combo(sectionClient, SWT.FLAT | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0).applyTo(cmbCongress);
		toolkit.adapt(cmbCongress);
		cmbCongress.setItems(loading);
		cmbCongress.select(0);
		
		Label dummy1 = new Label(sectionClient, SWT.NONE);
		dummy1.setText("Senator:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0)
				.applyTo(dummy1);

		senatorTable = new Table(sectionClient, SWT.BORDER
				| SWT.MULTI);
		GridDataFactory.fillDefaults().grab(true, true).span(1, 3)
				.hint(90, 50).applyTo(senatorTable);

		Composite buttonComp = new Composite(sectionClient, SWT.NONE);
		GridLayout btnLayout = new GridLayout();
		btnLayout.marginWidth = btnLayout.marginHeight = 0;
		btnLayout.makeColumnsEqualWidth = false;
		buttonComp.setLayout(btnLayout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		addSenatorBtn = new Button(buttonComp, SWT.PUSH); //$NON-NLS-1$
		addSenatorBtn.setText("Add...");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 1)
				.applyTo(addSenatorBtn);

		addSenatorBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAdd(addSenatorBtn.getShell());
			}
		});
		addSenatorBtn.setEnabled(false);

		removeSenatorButton = new Button(buttonComp,
				SWT.PUSH);
		removeSenatorButton.setText("Remove...");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 1)
				.applyTo(removeSenatorButton);
		removeSenatorButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				for (TableItem item : senatorTable.getSelection()) {
					selectedSenators.remove(item.getText());
					item.dispose();
				}
				if(selectedSenators.size() == 0) {
					removeSenatorButton.setEnabled(false);
				}
			}
		});
		removeSenatorButton.setEnabled(false);

		TacitFormComposite.createEmptyRow(toolkit, sectionClient);
		Group limitGroup = new Group(client, SWT.SHADOW_IN);
		limitGroup.setText("Limit Records");
		//limitGroup.setBackground(client.getBackground());
		limitGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout limitLayout = new GridLayout();
		limitLayout.numColumns = 1;
		limitGroup.setLayout(limitLayout);	
		
		final Composite limitRecordsClient = new Composite(limitGroup, SWT.None);
		GridDataFactory.fillDefaults().grab(true, false).span(1,1).applyTo(limitRecordsClient);
		GridLayoutFactory.fillDefaults().numColumns(3).equalWidth(false).applyTo(limitRecordsClient);
	
		limitRecords = new Button(limitRecordsClient, SWT.CHECK);
		limitRecords.setText("Limit Records per Senator");	
		GridDataFactory.fillDefaults().grab(false, false).span(3, 0).applyTo(limitRecords);
		limitRecords.addSelectionListener(new SelectionListener() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(!limitRecords.getSelection()){
					form.getMessageManager().removeMessage("limitText");
				}				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub			
			}
		});
		
		final Label sortLabel = new Label(limitRecordsClient, SWT.NONE);
		sortLabel.setText("Sort Records by Date:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(sortLabel);
		sortLabel.setEnabled(false);
		
		sortByDateYes = new Button(limitRecordsClient, SWT.RADIO);
		sortByDateYes.setText("Yes");
		sortByDateYes.setEnabled(false);
		sortByDateYes.setSelection(true);

		sortByDateNo = new Button(limitRecordsClient, SWT.RADIO);
		sortByDateNo.setText("No");
		sortByDateNo.setEnabled(false);

		final Label limitLabel = new Label(limitRecordsClient, SWT.NONE);
		limitLabel.setText("No.of.Records per Senator:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(limitLabel);
		limitLabel.setEnabled(false);
		limitText = new Text(limitRecordsClient, SWT.BORDER);
		limitText.setText("1");
		GridDataFactory.fillDefaults().grab(true, false).span(2, 0).applyTo(limitText);
		limitText.setEnabled(false);
		
		limitText.addKeyListener(new KeyListener() {			
			@Override
			public void keyReleased(KeyEvent e) {
	             if(!(e.character>='0' && e.character<='9')) {
	            	 form.getMessageManager() .addMessage( "limitText", "Provide valid no.of.records per senator", null, IMessageProvider.ERROR);
	            	 limitText.setText(""); 
	             } else {
	            	 form.getMessageManager().removeMessage("limitText");
	             }			
			}			
			@Override
			public void keyPressed(KeyEvent e) {
				// TODO Auto-generated method stub			
			}
		});
		
		TacitFormComposite.createEmptyRow(toolkit, client);
		
		Group dateGroup = new Group(client, SWT.SHADOW_IN);
		dateGroup.setText("Date");
		dateGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		dateGroup.setLayout(layout);

		dateRange = new Button(dateGroup, SWT.CHECK);
		dateRange.setText("Specify Date Range");
		
		//TacitFormComposite.createEmptyRow(toolkit, group);
		final Composite dateRangeClient = new Composite(dateGroup, SWT.None);
		GridDataFactory.fillDefaults().grab(true, false).span(1,1).applyTo(dateRangeClient);
		GridLayoutFactory.fillDefaults().numColumns(4).equalWidth(false).applyTo(dateRangeClient);
		dateRangeClient.setEnabled(false);
		dateRangeClient.pack();
		
		final Label fromLabel = new Label(dateRangeClient, SWT.NONE);
		fromLabel.setText("From:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(fromLabel);
		fromDate = new DateTime(dateRangeClient, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(fromDate);
		fromLabel.setEnabled(false);
		fromDate.setEnabled(false);
		
		fromDate.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event event) {
	            int day = fromDate.getDay();
	            int month = fromDate.getMonth() + 1;
	            int year = fromDate.getYear();
	            Date newDate = null;
	            try {
	                newDate = format.parse(day + "/" + month + "/" + year);
	            }
	            catch (ParseException e) {
	                e.printStackTrace();
	            }
	            
	            if(newDate.before(minDate) || newDate.after(maxDate))
	            {
	                Calendar cal = Calendar.getInstance();
	                cal.setTime(minDate);
	                fromDate.setMonth(cal.get(Calendar.MONTH));
	                fromDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
	                fromDate.setYear(cal.get(Calendar.YEAR));
	            }	            
			}
		});
		
		final Label toLabel = new Label(dateRangeClient, SWT.NONE);
		toLabel.setText("To:");
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(toLabel);
		toDate = new DateTime(dateRangeClient, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(false, false).span(1, 0).applyTo(toDate);
		toLabel.setEnabled(false);
		toDate.setEnabled(false);
		
		toDate.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event event) {
	            int day = toDate.getDay();
	            int month = toDate.getMonth() + 1;
	            int year = toDate.getYear();
	            Date newDate = null;
	            try {
	                newDate = format.parse(day + "/" + month + "/" + year);
	            }
	            catch (ParseException e) {
	                e.printStackTrace();
	            }
	            
	            if(newDate.after(maxDate) || newDate.before(minDate))
	            {
	                Calendar cal = Calendar.getInstance();
	                cal.setTime(maxDate);
	                toDate.setMonth(cal.get(Calendar.MONTH));
	                toDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
	                toDate.setYear(cal.get(Calendar.YEAR));
	            }
			}
		});

		
		Job loadFieldValuesJob = new Job("Loading form field values") {			
			HashMap<String, String> congressDetails = null;
			final ArrayList<String> tempCongress = new ArrayList<String>();
			final ArrayList<String> tempCongressYears = new ArrayList<String>();
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					congressDetails = AvailableRecords.getAllCongresses();
				} catch (IOException e) {
					e.printStackTrace();
				}
				Display.getDefault().syncExec(new Runnable() {
				      @Override
				      public void run() {
				    	  cmbCongress.removeAll();
				    	  for(String key : congressDetails.keySet()) {
				    		  tempCongress.add(key);
				    		  String value = congressDetails.get(key);
				    		  tempCongressYears.add(value);
				    		 
				    		  cmbCongress.add(key+" ("+ value+ ")");
				    		  
				    		  if(key.equalsIgnoreCase("All")) {
				    			  String[] tempYears = value.split("-");
				    			  Calendar cal = Calendar.getInstance();
				    			  cal.set(Integer.parseInt(tempYears[0]), 0, 1);
				    			  minDate = cal.getTime();
				    			  fromDate.setMonth(cal.get(Calendar.MONTH));
				    			  fromDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
				    			  fromDate.setYear(cal.get(Calendar.YEAR));
					                
				    			  cal.set(Integer.parseInt(tempYears[1]), 11, 31);
				    			  toDate.setMonth(cal.get(Calendar.MONTH));
				    			  toDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
				    			  toDate.setYear(cal.get(Calendar.YEAR));
				    			  maxDate = cal.getTime();
				    		  }
				    	  }
				    	  //cmbCongress.setItems(congresses);
				    	  cmbCongress.select(0);
				      }});		
				congresses = tempCongress.toArray(new String[0]);
				congressYears = tempCongressYears.toArray(new String[0]);
				try {
					allSenators = AvailableRecords.getAllSenators(congresses);
					totalSenators = allSenators.length + 5;
					Display.getDefault().syncExec(new Runnable() {						
						@Override
						public void run() {
							addSenatorBtn.setEnabled(true);
							
						}
					});
				} catch (IOException e2) {
					e2.printStackTrace();
				}
				return Status.OK_STATUS;
			}
		};
		//loadFieldValuesJob.setUser(true);
		loadFieldValuesJob.schedule();
		
		cmbCongress.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// set dates
				String tempYears[] = congressYears[cmbCongress.getSelectionIndex()].split("-");
				Calendar cal = Calendar.getInstance();
				cal.set(Integer.parseInt(tempYears[0]), 0, 1);
				minDate = cal.getTime();
				fromDate.setMonth(cal.get(Calendar.MONTH));
				fromDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
				fromDate.setYear(cal.get(Calendar.YEAR));
				    
				cal.set(Integer.parseInt(tempYears[1]), 11, 31);
				toDate.setMonth(cal.get(Calendar.MONTH));
				toDate.setDay(cal.get(Calendar.DAY_OF_MONTH));
				toDate.setYear(cal.get(Calendar.YEAR));
				maxDate = cal.getTime();
				//cmbSenator.select(0);
				
				//Empty the senatorTable
				senatorTable.removeAll();
				selectedSenators = new ArrayList<String>();
			}
		});	
		
		dateRange.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dateRange.getSelection()) {					
					dateRangeClient.setEnabled(true);
					fromLabel.setEnabled(true);
					fromDate.setEnabled(true);
					toLabel.setEnabled(true);
					toDate.setEnabled(true);
				} else {					
					dateRangeClient.setEnabled(false);
					fromLabel.setEnabled(false);
					fromDate.setEnabled(false);
					toLabel.setEnabled(false);
					toDate.setEnabled(false);
				}
			}
		});	
		
		limitRecords.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (limitRecords.getSelection()) {	
					sortByDateYes.setEnabled(true);
					sortByDateNo.setEnabled(true);
					sortLabel.setEnabled(true);
					limitLabel.setEnabled(true);
					limitText.setEnabled(true);
				} else {
					sortByDateYes.setEnabled(false);
					sortByDateNo.setEnabled(false);
					sortLabel.setEnabled(false);
					limitLabel.setEnabled(false);
					limitText.setEnabled(false);
				}
			}
		});
	}

	static class ArrayLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			return (String) element;
		}
	}
	
	public void processElementSelectionDialog(Shell shell) {
		ILabelProvider lp = new ArrayLabelProvider();
		senatorListDialog = new SenatorListDialog(shell, lp);
		senatorListDialog.setTitle("Select the Authors from the list");
		senatorListDialog.setMessage("Enter Author name to search");
	}
	
	
	private void handleAdd(Shell shell) {
		
		processElementSelectionDialog(shell);

		senatorList = new LinkedHashSet<String>();
		Job listSenators = new Job("Retrieving senator list ...") {

			
			String selectedCongress = "";
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				senatorList.clear();
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						selectedCongress = congresses[cmbCongress.getSelectionIndex()];
					}
				});
				
				try { 
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(0, "All Senators");
			    	temp.add(1, "All Democrats");
			    	temp.add(2, "All Republicans");
			    	temp.add(3, "All Independents");
					if(selectedCongress.equals("All")) {
						for(String s : allSenators) 
							temp.add(s);					
					} else {
						if(previousSelectedCongress.isEmpty() || !previousSelectedCongress.equals(selectedCongress)) {
							availabileSenators = AvailableRecords.getSenators(selectedCongress);
						}
						for(String s : availabileSenators) 
							temp.add(s);	
						
					}
					senatorList.addAll(temp);
					if (selectedSenators != null)
						senatorList.removeAll(selectedSenators);

					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							senatorListDialog.refresh(senatorList.toArray());
						}
					});
					previousSelectedCongress = selectedCongress;
				} catch (final IOException exception) {
					ConsoleView.printlInConsole(exception.toString());
					Display.getDefault().syncExec(new Runnable() {

						@Override
						public void run() {
							ErrorDialog.openError(Display.getDefault()
									.getActiveShell(), "Problem Occurred",
									"Please Check your connectivity to server",
									new Status(IStatus.ERROR,
											CommonUiActivator.PLUGIN_ID,
											"Network is not reachable"));

						}
					});
				}
				return Status.OK_STATUS;
			}
		};

		listSenators.schedule();
		senatorList.add("Loading...");
		senatorListDialog.setElements(senatorList.toArray());
		senatorListDialog.setMultipleSelection(true);
		if (senatorListDialog.open() == Window.OK) {
			updateSenatorTable(senatorListDialog.getResult());
		}

	}

	private void updateSenatorTable(Object[] result) {
		if (selectedSenators == null) {
			selectedSenators = new ArrayList<String>();
		}

		for (Object object : result) {
			selectedSenators.add((String) object);
		}
		//Collections.sort(selectedSenators);
		senatorTable.removeAll();
		for (String itemName : selectedSenators) {
			TableItem item = new TableItem(senatorTable, 0);
			item.setText(itemName);
			if(!removeSenatorButton.isEnabled()) {
				removeSenatorButton.setEnabled(true);
			}
		}

	}
	
	
	/**
	 * Adds "Classify" and "Help" buttons on the Naive Bayes Classifier form
	 */
	private void addButtonsToToolBar() {
		IToolBarManager mgr = form.getToolBarManager();
		mgr.add(new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (UsCongressCrawlerViewImageRegistry.getImageIconFactory().getImageDescriptor(IMAGE_LRUN_OBJ));
			}

			@Override
			public String getToolTipText() {
				return "Crawl";
			}

			String dateFrom = "";
			String dateTo = "";
			int maxDocs = -1;
			String sortType = "Default";
			String congressNum = "-1";
			ArrayList<String> senatorDetails = new ArrayList<String>();
			String outputDir = "";
			private boolean canProceed;
			@Override
			public void run() {
				final UsCongressCrawler sc = new UsCongressCrawler();

				final Job job = new Job("US Congress Crawler") {					
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						TacitFormComposite.setConsoleViewInFocus();
						TacitFormComposite.updateStatusMessage(getViewSite(), null,null, form);						
						Display.getDefault().syncExec(new Runnable() {
							
							@Override
							public void run() {
								if(congresses[cmbCongress.getSelectionIndex()].indexOf("All")!=-1) {
									congressNum = "-1";
								} else {
									congressNum = congresses[cmbCongress.getSelectionIndex()];	
								}
								senatorDetails = selectedSenators;
								if (dateRange.getSelection()) {
									dateFrom = (fromDate.getMonth()+1)+"/"+fromDate.getDay()+"/"+fromDate.getYear();
									dateTo = (toDate.getMonth()+1)+"/"+toDate.getDay()+"/"+toDate.getYear();
								} else {
									dateFrom = "";
									dateTo = "";
								}
								if(limitRecords.getSelection()) {
									//sort by date : begining
									sortType = sortByDateNo.getSelection() ? "Default" : "Date"; 
									maxDocs = Integer.parseInt(limitText.getText());
								} else {
									maxDocs = -1;
									sortType = "Date";
								}
								outputDir = outputLayout.getOutputLabel().getText();	
							}
						});
						
						if(senatorDetails.contains("All Senators") && congressNum.equals("-1")) { // all senators and all congresses
							progressSize = (totalSenators * congresses.length) + 50;
						} else {
							int count = 1;
							if(congressNum.equals("-1")) {
								if(senatorDetails.contains("All Democrats")) {
									progressSize = (20 * congresses.length) + 50; // on an average of 20 democrats
									count++;
								}
								if(senatorDetails.contains("All Republicans")) {
									progressSize+= (20 * congresses.length) + 50;
									count++;
								}
								if(senatorDetails.contains("All Independents")) {
									progressSize+= (20 * congresses.length) + 50;
									count++;
								}
								progressSize+= ((senatorDetails.size() - count)+1 * congresses.length) + 50; // considering none of "All" selected
							} else {
								if(senatorDetails.contains("All Democrats")) {
									progressSize = 100 + 50; // on an average of 20 democrats
									count++;
								}
								if(senatorDetails.contains("All Republicans")) {
									progressSize+= 100 + 50;
									count++;
								}
								if(senatorDetails.contains("All Independents")) {
									progressSize+=  100 + 50;
									count++;
								}
								progressSize+= ((senatorDetails.size() - count)+1 * 10) + 50; // considering none of "All" selected								
							}
						}
						monitor.beginTask("Running US Congress Crawler..." , progressSize);
						TacitFormComposite.writeConsoleHeaderBegining("US Congress Crawler started ");						
						
						final ArrayList<Integer> allCongresses = new ArrayList<Integer>();
						for(String s: congresses) {
							if(!s.contains("All"))
								allCongresses.add(Integer.parseInt(s));
						}
							
						if(monitor.isCanceled()) {
							TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
							return handledCancelRequest("Cancelled");
						}
						try {
							monitor.subTask("Initializing...");
							monitor.worked(10);
							if(monitor.isCanceled()) {
								TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
								return handledCancelRequest("Cancelled");
							}
							sc.initialize(sortType, maxDocs, Integer.parseInt(congressNum), senatorDetails, dateFrom, dateTo, outputDir, allCongresses, monitor, progressSize - 30);
							if(monitor.isCanceled()) {
								TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
								return handledCancelRequest("Cancelled");
							}
							monitor.worked(10);
														
							monitor.subTask("Crawling...");
							if(monitor.isCanceled()) {
								TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
								return handledCancelRequest("Cancelled");
							}
							sc.crawl();
							if(monitor.isCanceled()) {
								TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
								return handledCancelRequest("Cancelled");
							}
							monitor.worked(10);
						} catch (NumberFormatException e) {						
							return handleException(monitor, e, "Crawling failed. Provide valid data");
						} catch (IOException e) {							
							return handleException(monitor, e, "Crawling failed. Provide valid data");
						} catch(Exception e) {
							return handleException(monitor, e, "Crawling failed. Provide valid data");
						}
						monitor.worked(100);
						monitor.done();
						ConsoleView.printlInConsoleln("US Congress crawler completed successfully.");
						ConsoleView.printlInConsoleln("Total no.of.files downloaded : " + sc.totalFilesDownloaded);
						ConsoleView.printlInConsoleln("Done");
						TacitFormComposite.updateStatusMessage(getViewSite(), "US Congress crawler completed successfully.", IStatus.OK, form);
						TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
						return Status.OK_STATUS;
					}					
				};	
				job.setUser(true);
				canProceed = canItProceed();
				if(canProceed) {
					job.schedule(); // schedule the job
				}
			}
		});

		Action helpAction = new Action() {
			@Override
			public ImageDescriptor getImageDescriptor() {
				return (UsCongressCrawlerViewImageRegistry.getImageIconFactory().getImageDescriptor(IMAGE_HELP_CO));
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
								"edu.usc.cssl.tacit.crawlers.uscongress.ui.uscongress");
			};
		};
		mgr.add(helpAction);
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(helpAction,
						"edu.usc.cssl.tacit.crawlers.uscongress.ui.uscongress");
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(form,
						"edu.usc.cssl.tacit.crawlers.uscongress.ui.uscongress");
		form.getToolBarManager().update(true);
	}
	
	private IStatus handleException(IProgressMonitor monitor, Exception e, String message) {
		monitor.done();
		System.out.println(message);
		e.printStackTrace();
		TacitFormComposite.updateStatusMessage(getViewSite(), message, IStatus.ERROR, form);
		TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
		return Status.CANCEL_STATUS;
	}
	
	private IStatus handledCancelRequest(String message) {
		TacitFormComposite.updateStatusMessage(getViewSite(), message, IStatus.ERROR, form);
		ConsoleView.printlInConsoleln("US Congress crawler cancelled.");
		TacitFormComposite.writeConsoleHeaderBegining("<terminated> US Congress Crawler");
		return Status.CANCEL_STATUS;
		
	}

	private boolean canItProceed() {
		if(limitRecords.getSelection()) {
			if(limitText.getText().isEmpty()) {
				form.getMessageManager() .addMessage( "limitText", "Provide valid no.of.records per senator", null, IMessageProvider.ERROR);
				return false;
			}
		}
		
		String message = OutputPathValidation.getInstance().validateOutputDirectory(outputLayout.getOutputLabel().getText(), "Output");
		if (message != null) {
			message = outputLayout.getOutputLabel().getText() + " " + message;
			form.getMessageManager().addMessage("output", message, null,IMessageProvider.ERROR);
			return false;
		} else {
			form.getMessageManager().removeMessage("output");
		}
		
		return true;
	};
	
	@Override
	public void setFocus() {
		form.setFocus();
	}
	
	/**
	 * 
	 * @param parent
	 * @param title
	 * @return - Creates a form body section for Naive Bayes Classifier
	 */
	private FormToolkit createFormBodySection(Composite parent, String title) {
		// Every interface requires a toolkit(Display) and form to store the components
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		toolkit.decorateFormHeading(form.getForm());
		form.setText(title);
		GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(true)
				.applyTo(form.getBody());
		return toolkit;
	}

}