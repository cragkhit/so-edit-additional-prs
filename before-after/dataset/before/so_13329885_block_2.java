	public static void main(String[] args) {
	    final Display display = new Display();
	    final Shell shell = new Shell(display);
	    shell.setLayout(new GridLayout(2, false));
	
	    final TableViewer viewer = new TableViewer(shell, SWT.READ_ONLY);
	
	    // First column is for the name
	    TableViewerColumn col = createTableViewerColumn("Name", 100, 0, viewer);
	    col.setLabelProvider(new ColumnLabelProvider() {
	        @Override
	        public String getText(Object element) {
	            if(element instanceof Person)
	            {
	                return ((Person)element).getName();
	            }
	            return "";
	        }
	    });
	
	    // First column is for the location
	    TableViewerColumn col2 = createTableViewerColumn("Location", 100, 1, viewer);
	    col2.setLabelProvider(new ColumnLabelProvider() {
	        @Override
	        public String getText(Object element) {
	            if(element instanceof Person)
	            {
	                return ((Person)element).getLocation();
	            }
	            return "";
	        }
	    });
	
	    final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
	    data.horizontalSpan = 2;
	    table.setLayoutData(data);
	
	    /* Add listener to listen for selection change */
	    final Text name = new Text(shell, SWT.BORDER);
	    name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	    final Text location = new Text(shell, SWT.BORDER);
	    location.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	    
	    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				Person person = (Person) selection.getFirstElement();
				
				name.setText(person.getName());
				location.setText(person.getLocation());
			}
		});
	
	    viewer.setContentProvider(ArrayContentProvider.getInstance());
	
	    final Person[] persons = new Person[] { new Person("Baz", "Loc"),
	            new Person("BazBaz", "LocLoc"), new Person("BazBazBaz", "LocLocLoc") };
	
	    viewer.setInput(persons);
	
	    shell.pack();
	    shell.open();
	    while (!shell.isDisposed()) {
	        if (!display.readAndDispatch()) {
	            display.sleep();
	        }
	    }
	    display.dispose();
	}
	
	private static TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber, TableViewer viewer) {
	    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
	    final TableColumn column = viewerColumn.getColumn();
	    column.setText(title);
	    column.setWidth(bound);
	    column.setResizable(true);
	    column.setMoveable(false);
	
	    return viewerColumn;
	}
	
	public static class Person {
	    private String name;
	    private String location;
	
	    public Person(String name, String location) {
	        this.name = name;
	        this.location = location;
	    }
	
	    public String getName() {
	        return name;
	    }
	
	    public void setName(String name) {
	        this.name = name;
	    }
	
	    public String getLocation() {
	        return location;
	    }
	
	    public void setLocation(String location) {
	        this.location = location;
	    }
	
	    public String toString()
	    {
	        return name + " " + location;
	    }
	}