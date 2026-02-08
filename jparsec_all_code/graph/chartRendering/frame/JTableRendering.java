package jparsec.graph.chartRendering.frame;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Comparator;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.io.FileIO;
import jparsec.io.ReadFile;
import jparsec.io.image.Picture;
import jparsec.time.TimeElement;
import jparsec.util.JPARSECException;

/**
 * A class to create and manage tables. Implementation and support is just for
 * basic tables, but anyway useful to avoid code duplication.
 * @author T. Alonso Albi - OAN (Spain)
 */
public class JTableRendering implements PropertyChangeListener, MouseListener {

	/** The table. */
	private JTable table;
	private boolean ascending[], editable[];
	private String lineTable[][] = null, lineTableOriginal[][];
	private boolean valueChanging = false;
    private JTableHeader tableHeader;
	private final String SEPARATOR = "@<>@"; // A strange separator that is never used in the fields
	private String selectedRow;
	private int colColumn = -1;
	private String[] colVal, columnToolTips;
	private Color[] colCol;
	private String columnNames[];
	private Class<?> columnClasses[];
	private Class<?> columnClassesForSort[];
	private int alignment[];
	private String separator = ",";
	private boolean allowHighlightRow = true;
	private int iconSize = 50;
	
	/**
	 * Constructor for a table.
	 * @param columns The names of the columns.
	 * @param classes The classes in each column. Valid values are numbers (Integer, ...), Boolean
	 * and String classes. Set the entire array to null to use String for all columns, or any of the elements
	 * to null to use String but letting the value to be parsed to an angle formatted by the methods in
	 * {@linkplain Functions} or a date formatted in {@linkplain TimeElement}.
	 * @param editable True or false to allow or not to edit columns. Set to
	 * null to set all to false for String and true for Boolean.
	 * @param table The table, ordered as [rows][columns].
	 * @throws JPARSECException If an error occurs.
	 */
	public JTableRendering(String columns[], Class<?> classes[], boolean editable[], String table[][]) throws JPARSECException  {
		columnNames = columns;
		columnClasses = classes;
		this.editable = editable;
		updateData(table);
		if (columnClasses == null) {
			columnClasses = new Class<?>[columns.length];
			for (int i=0; i<columns.length; i++) {
				columnClasses[i] = String.class;
			}
		}
		createTable();
		lineTableOriginal = lineTable.clone();
	}

	/**
	 * Constructor for a table.
	 * @param columns The names of the columns.
	 * @param classes The classes in each column. Valid values are numbers (Integer, ...), Boolean
	 * and String classes. Set the entire array to null to use String for all columns, or any of the elements
	 * to null to use String but letting the value to be parsed to an angle formatted by the methods in
	 * {@linkplain Functions} or a date formatted in {@linkplain TimeElement}.
	 * @param editable True or false to allow or not to edit columns. Set to
	 * null to set all to false for String and true for Boolean.
	 * @param table The table, ordered as [rows][columns].
	 * @param sortClasses The column classes for sorting. Only columns of type Double for set and 
	 * type String for visualization will be considered, creating an specific sorter for them.
	 * @throws JPARSECException If an error occurs.
	 */
	public JTableRendering(String columns[], Class<?> classes[], boolean editable[], String table[][], 
			Class<?> sortClasses[]) throws JPARSECException  {
		columnNames = columns;
		columnClasses = classes;
		this.editable = editable;
		updateData(table);
		if (columnClasses == null) {
			columnClasses = new Class<?>[columns.length];
			for (int i=0; i<columns.length; i++) {
				columnClasses[i] = String.class;
			}
		}
		columnClassesForSort = sortClasses;
		createTable();
		lineTableOriginal = lineTable.clone();
	}

	private void updateData(String table[][]) {
		if (table == null || table.length == 0) {
			lineTable = new String[0][0];
			return;
		}
		lineTable = new String[table.length][table[0].length+1];
		for (int i=0; i<table.length; i++) {
			for (int j=0; j<table[i].length; j++) {
				lineTable[i][j] = table[i][j];
			}
			lineTable[i][lineTable[0].length-1] = ""+i;
		}
	}

	/**
	 * Sets the separator used to show in one cell multiple lines of text ({@linkplain JList}).
	 * In the table data the list of items will be an unique string, that should be separated 
	 * in a list with the separator provided here. Default is a comma. 
	 * @param s The separator for lists of items in one cell.
	 */
	public void setSeparatorForLists(String s) {
		separator = s;
	}
	
	/**
	 * Returns the separator used to show text in several lines within a cell.
	 * @return Separator string.
	 */
	public String getSeparator() {
		return separator;
	}
	
	/**
	 * Returns the icon size.
	 * @return Icon size.
	 */
	public int getIconSize() { return iconSize; }
	/**
	 * Sets the icon size.
	 * @param s Icon size.
	 */
	public void setIconSize(int s) { iconSize = s; }
	
	/**
	 * Returns the table component.
	 * @return The table.
	 */
	public JTable getComponent() {
		return table;
	}

	/**
	 * Returns a copy of the original data in the table.
	 * @return The data, ordered as [rows][columns].
	 */
	public String[][] getOriginalTableData() {
		return lineTableOriginal.clone();
	}

	/**
	 * Returns a copy of the current data in the table.
	 * @return The data, ordered as [rows][columns].
	 * @throws JPARSECException If an error occurs.
	 */
	public String[][] getTableData() throws JPARSECException {
		String lt[][] = new String[lineTable.length][lineTable[0].length-1];
		double index[] = new double[lineTable.length];
		for (int i=0; i<lineTable.length; i++) {
			for (int j=0; j<lineTable[i].length-1; j++) {
				lt[i][j] = lineTable[i][j];
				lt[i][j] = lineTable[i][j];
			}
			index[i] = Integer.parseInt(lineTable[i][lineTable[0].length-1]);
		}

		lt = (String[][]) DataSet.sortInCrescent(lt, index);
		return lt;
	}

	/**
	 * Converts the row index from view to model. The underlying methods
	 * of the table component should not be used in this class to convert
	 * row indexes, use the methods for this instance.
	 * @param viewRowIndex Row index in the visible table.
	 * @return Row index in the table model.
	 */
	public int convertRowIndexToModel(int viewRowIndex) {
		return Integer.parseInt(lineTable[viewRowIndex][lineTable[0].length-1]);
	}

	/**
	 * Converts the row index from model to view. The underlying methods
	 * of the table component should not be used in this class to convert
	 * row indexes, use the methods for this instance.
	 * @param modelRowIndex Row index in the table model.
	 * @return Row index in the visible table.
	 */
	public int convertRowIndexToView(int modelRowIndex) {
		for (int i=0; i<lineTable.length; i++) {
			if (convertRowIndexToModel(i) == modelRowIndex) return i;
		}
		return modelRowIndex;
	}

	/**
	 * Sets the colors for the rows. Color can be changed according to the value of 
	 * the cell or to the row index. In the second case, set column as a negative value 
	 * (-2 to alternate colors, -3 to change the color or one row per 3, etc), the 
	 * values array to null, and the col array to just one color.
	 * @param column The column to use to select the color based on its value.
	 * @param values A set of possible values to select a different color from default.
	 * @param col The set of colors so that in row 'row' col[i] is used when table[row][column] = values[i].
	 */
	public void setRowColor(int column, String values[], Color col[]) {
		this.colColumn = column;
		this.colVal = values;
		this.colCol = col;
	}

	/**
	 * Sets the alignment of the data in each columns
	 * @param ca An array of integers (length equals to the number of columns) 
	 * with constants selecting the alignment. Constant defined in {@linkplain SwingConstants}
	 * for left, right, and center alignment.
	 */
	public void setColumnAlignments(int ca[]) {
		alignment = ca;
	}

	/**
	 * Sets the width in pixels for a given column at startup.
	 * @param width The set of widths for each column.
	 */
	public void setColumnWidth(int width[]) {
		for (int i=0; i<width.length; i++) {
			if (table.getColumnModel().getColumnCount() <= i) break;
			table.getColumnModel().getColumn(i).setPreferredWidth(width[i]);
		}
	}
	
	/**
	 * Set text tooltips for each columns in the header of the table.
	 * @param tt Tooltips, or null to hide all.
	 */
	public void setColumnToolTips(String tt[]) {
		columnToolTips = tt;
	}

	/**
	 * Sets the name of the columns. No check is done.
	 * @param names The new names.
	 */
	public void setColumnNames(String names[]) {
		this.columnNames = names.clone();
	}

	/**
	 * Returns the names of the columns.
	 * @return The column names.
	 */
	public String[] getColumnNames() {
		return columnNames.clone();
	}

	/**
	 * Sets if all rows should be highlighted when a cell is selected.
	 * Default is true;
	 * @param h True or false.
	 */
	public void setAllowHighLightRow(boolean h) {
		allowHighlightRow = h;
	}

	/**
	 * Returns the selected row after a mouse click event.
	 * @param separator Separator to use for the items in the row.
	 * @return Selected row.
	 */
	public String getSelectedRow(String separator) {
		return DataSet.replaceAll(selectedRow, SEPARATOR, separator, true);
	}

	private void createTable() throws JPARSECException {
		final int columns = columnNames.length;
		ascending = new boolean[columns];
		for (int i=0; i<ascending.length; i++)
		{
			ascending[i] = true;
		}

	    TableModel dataModel = new AbstractTableModel() {
	    	static final long serialVersionUID = 1L;
	         public int getColumnCount() { return columns; }
	         public int getRowCount() { return lineTable.length;}
	         public String getColumnName(int i) {
	        	 return columnNames[i];
	         }
	         public Class<?> getColumnClass(int i)
	         {
	        	 if (columnClasses[i] == null) return String.class;
	        	 return columnClasses[i];
	         }
	         public Object getValueAt(int row, int col) {
	        	 if (lineTable.length == 0) return null;
	        	 if (row >= lineTable.length || col >= lineTable[0].length || lineTable[row][col] == null)
	        		 return null;
	        	 if (!lineTable[row][col].equals("")) {
		        	 if (columnClasses[col] == Boolean.class) return new Boolean(Boolean.parseBoolean(lineTable[row][col]));
		        	 if (columnClasses[col] == Integer.class) return new Integer(Integer.parseInt(lineTable[row][col]));
		        	 if (columnClasses[col] == Double.class) return Double.parseDouble(lineTable[row][col]);
		        	 if (columnClasses[col] == Float.class) return Float.parseFloat(lineTable[row][col]);
		        	 if (columnClasses[col] == Long.class) return Long.parseLong(lineTable[row][col]);
		        	 if (columnClasses[col] == ImageIcon.class) {
		        		 try {
		        			 if (lineTable[row][col] == null || lineTable[row][col].equals("") || 
		        					 lineTable[row][col].equals("null")) return null;
		        			 if (lineTable[row][col].startsWith("data:")) {
		        				 String imgData = lineTable[row][col].substring(5);
		        				 int is = Integer.parseInt(imgData.substring(0, imgData.indexOf(",")));
	        					 imgData = imgData.substring(imgData.indexOf(",")+1);
		        				 if (is == iconSize) {
		        					 imgData = imgData.substring(imgData.indexOf(",")+1);
					        		 if (imgData != null && !imgData.equals("") && imgData.length() % 4 == 0) {
				        				 ImageIcon icon = new ImageIcon(Picture.stringToImage(imgData));
						        		 icon.setDescription(lineTable[row][col]); //(lineTable[row][col]);
						        		 return icon;
					        		 }
		        				 }
	        					 imgData = imgData.substring(0, imgData.indexOf(","));
		        				 lineTable[row][col] = imgData;
		        			 }
		        			 Picture pic = null;
		        			 if (ReadFile.resourceAvailable(lineTable[row][col])) {
		        				 pic = new Picture(ReadFile.readImageResource(lineTable[row][col]));
		        			 } else {
			        			 URL imageURL = ClassLoader.getSystemResource(lineTable[row][col]);
			        			 if (imageURL != null) pic = new Picture(Picture.toBufferedImage(Toolkit.getDefaultToolkit().getImage(imageURL)));
		        			 }
		        			 if (pic == null) return null;
		        			 pic.setImage(Picture.removeAlpha(pic.getImage(), Color.BLACK));
		        			 pic.getScaledInstance(iconSize, iconSize, true);
			        		 ImageIcon icon = new ImageIcon(pic.getImage());
			        		 String img = pic.imageToString(false);
			        		 if (img != null && !img.equals("") && img.length() % 4 == 0) {
				        		 lineTable[row][col] = "data:"+iconSize+","+lineTable[row][col]+","+img;
				        		 icon.setDescription(lineTable[row][col]); //(lineTable[row][col]);
			        		 }
			        		 return icon;
		        		 } catch (Exception exc) {exc.printStackTrace(); }
		        	 }
		        	 //if (columnClasses[col] == JList.class) return new JList(DataSet.toStringArray(lineTable[row][col], separator)); //Long.parseLong(lineTable[row][col]);
	        	 }
	        	 if (columnClasses[col] == Boolean.class) return null;
	        	 if (columnClasses[col] == Integer.class) return null;
	        	 if (columnClasses[col] == Double.class) return null;
	        	 if (columnClasses[col] == Float.class) return null;
	        	 if (columnClasses[col] == Long.class) return null;
	        	 //if (columnClasses[col] == JList.class) return null;
	        	 
	        	 String out = lineTable[row][col];
	        	 out = DataSet.replaceAll(out, "@BOLD", "", true);
	        	 /*
	        	 if (out.indexOf(separator) >= 0) {
	        		 String lines[] = DataSet.toStringArray(out, separator, false);
	        		 JTextArea ta = new JTextArea(1, lines.length);
	        		 ta.setText(DataSet.toString(lines, FileIO.getLineSeparator()));
	        		 ta.setFont(table.getFont());
	        		 return ta;
	        	 }
	        	 */
	        	 return out;
	         }
	         public void setValueAt(Object b, int row, int col) {
	        	 if (!isCellEditable(row, col) || valueChanging) return;
	        	 if (columnClasses[col] == Boolean.class) {
	        		 boolean a = (Boolean) b;
        			 String lt = lineTable[row][col];
	        		 if (a) {
	        			 if (lt.equals("false")) {
	        				 lineTable[row][col] = "true";
	    	        		 valueChanging = true;
	        			 }
	        		 } else {
	        			 if (lt.equals("true")) {
	        				 lineTable[row][col] = "false";
	    	        		 valueChanging = true;
	        			 }
	        		 }
	        		 return;
	        	 }
	        	 if (columnClasses[col] == ImageIcon.class) {
	        		 if (b != null) {
		        		 String name = ((ImageIcon) b).getDescription();
		        		 lineTable[row][col] = name;
	        		 }
	        	 }
	        	 if (columnClasses[col] == JList.class) {
	        		 String cell = "";
	        		 ListModel lm = ((JList) b).getModel();
	        		 for (int i=0; i<lm.getSize();i++) {
	        			 cell += ((String) lm.getElementAt(i));
	        			 if (i < lm.getSize()-1) cell += separator;
	        		 }
	        		 lineTable[row][col] = cell;
	        	 } else {
	        		 lineTable[row][col] = (String) b;
	        	 }
        		 valueChanging = true;
	         }
	         public boolean isCellEditable(int row, int column)
	         {
	        	boolean edit = false;
	        	if (columnClasses[column] == Boolean.class) edit = true;
	        	if (editable != null) edit = editable[column];
	        	return edit;
	         }
	    };

		table = new JTable(dataModel)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				TableCellRenderer tcr = super.getCellRenderer(row, column);
				if (tcr instanceof DefaultTableCellRenderer && alignment != null) {
					((DefaultTableCellRenderer) tcr).setHorizontalAlignment(alignment[column]);
					((DefaultTableCellRenderer) tcr).setHorizontalTextPosition(alignment[column]);
					((DefaultTableCellRenderer) tcr).setVerticalAlignment(SwingConstants.CENTER);
				}
				return tcr;
			}
			public Component prepareRenderer(TableCellRenderer renderer,
                    int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				Font font = table.getFont();
				int row = this.convertRowIndexToModel(rowIndex);
				
				String s2 = lineTable[row][vColIndex];
				if (s2 != null && s2.indexOf("@BOLD") >= 0) {
					font = font.deriveFont(Font.BOLD);
				}
				c.setFont(font);
				
				if (columnClasses[vColIndex] == ImageIcon.class && s2 != null) {
					JPanel cc = new JPanel();
					((JPanel) cc).setBackground(null);
					cc.setBackground(null);
					cc.setForeground(null);
					cc.setOpaque(true);
					cc.setName(s2);
					cc.setSize(c.getSize());
					((FlowLayout) ((JPanel) cc).getLayout()).setVgap(0);
					((FlowLayout) ((JPanel) cc).getLayout()).setHgap(0);
					((JPanel) cc).add(c);
					return cc;
				}
				

				if (columnClasses[vColIndex] == JList.class) {
					String s = s2; //((DefaultTableCellRenderer) c).getText();
					if (s == null) s = "";
					s = DataSet.replaceAll(s, "@BOLD", "", true);
					c = new JPanel();
					((JPanel) c).setBackground(null);
					String ss[] = DataSet.toStringArray(s, separator, false);
					JList list = new JList(ss);
					list.setFont(font);
					list.setBackground(null);
					list.setForeground(null);

					int w = -1;
					for (int i=0; i<ss.length; i++) {
						int d = list.getFontMetrics(getFont()).stringWidth(ss[i]);
						if (d > w || w == -1) w = d;
					}
					
					c.setSize(new Dimension(w + list.getFont().getSize()/3, 
							(list.getFont().getSize() + list.getFont().getSize() / 3) * 2));
					((FlowLayout) ((JPanel) c).getLayout()).setVgap(0);
					((FlowLayout) ((JPanel) c).getLayout()).setHgap(0);
					((JPanel) c).add(list);
				}

				c.setForeground(null);
				if (isCellSelected(rowIndex, vColIndex) || 
						isRowSelected(rowIndex) && allowHighlightRow) {
					c.setBackground(getSelectionBackground());
					return c;
				} else {
					// If not shaded, match the table's background
					c.setBackground(getBackground());
				}
				if (colColumn >= 0 && colCol != null && colVal != null) {
					String f = lineTable[row][colColumn];
					int index = DataSet.getIndex(colVal, f);
					if (index >= 0) {
						c.setBackground(colCol[index]);
						return c;
					}
				}
				if (colColumn < 0 && colVal == null && colCol != null && colCol.length > 0) {
					int n = rowIndex % Math.abs(colColumn);
					if (n < colCol.length) {
						c.setBackground(colCol[n]);
						return c;
					}
				}
				
				return c;
			}
			
		    // Implement table header tool tips.
		    protected JTableHeader createDefaultTableHeader() {
		        return new JTableHeader(columnModel) {
		            public String getToolTipText(MouseEvent e) {
		            	if (columnToolTips == null) return null;
		                java.awt.Point p = e.getPoint();
		                int index = columnModel.getColumnIndexAtX(p.x);
		                if (index < 0) return null;
		                int realIndex = columnModel.getColumn(index).getModelIndex();
		                if (realIndex >= columnToolTips.length) return null;
		                return columnToolTips[realIndex];
		            }
		        };
		    }
		};
		table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		table.setAutoCreateColumnsFromModel(false);
		table.setColumnSelectionAllowed(true);
		table.setRowSelectionAllowed(true);
		//table.setAutoCreateRowSorter(true);
		table.setCellSelectionEnabled(true);
		table.addPropertyChangeListener(this);
		table.addMouseListener(this);
		tableHeader = table.getTableHeader();
		tableHeader.addMouseListener(this);
		Font h = tableHeader.getFont();
		tableHeader.setFont(new Font(h.getFontName(), Font.BOLD, h.getSize()));

		TableRowSorter<TableModel> sorter = new TableRowSorter(table.getModel());
		if (columnClassesForSort != null) {
			for (int i=0; i<columnClassesForSort.length; i++) {
				if (columnClassesForSort[i] == ImageIcon.class) {
					sorter.setSortable(i, false); 
					continue;
				}
				if (columnClassesForSort[i] == Double.class && 
						(columnClasses[i] == String.class || 
						columnClasses[i] == JList.class)) {
					sorter.setComparator(i, new Comparator<String>() {
					    @Override
					    public int compare(String name1, String name2) {
					    	String sep = " ";
					    	if (name1.indexOf(separator) >= 0) sep = separator;
					    	String s1 = FileIO.getField(1, name1, sep, false);
					    	String s2 = FileIO.getField(1, name2, sep, false);
					    	Double d1 = DataSet.isDoubleStrictCheck(s1) ? Double.parseDouble(s1) : 0;
					    	Double d2 = DataSet.isDoubleStrictCheck(s2) ? Double.parseDouble(s2) : 0;
					    	return d1.compareTo(d2);
					    }
					});
				} else {
					if (columnClasses[i] == JList.class) {
						sorter.setComparator(i, new Comparator<String>() {
						    @Override
						    public int compare(String n1, String n2) {
						    	return n1.compareTo(n2);
						    }
						});
					}
				}
			}
		}
		table.setRowSorter(sorter);

		updateTable(lineTable, true);
	}
	
	/**
	 * Updates the contents of the table.
	 * @param stable The new table.
	 * @param show True to update the view.
	 */
	public void updateTable(String stable[][], boolean show) {
		int row = table.getSelectedRow();
		String rowS = null;
		if (row >= 0 && lineTable.length > row) rowS = DataSet.toString(lineTable[row], SEPARATOR);
		
		updateData(stable);
		if (table.getRowSorter() != null) {
			try {
				table.getRowSorter().allRowsChanged();
			} catch (Exception exc) {}
		}
		
		//int index = -1;
        //if (tableSorted >= 0) sortColumn(table.getModel(), tableSorted, tableSortAscending);
        if (show) {
	    	//table.revalidate();
	    	/*
			if (selectedRow != null) {
		    	  for (int i=0; i<lineTable.length; i++) {
		    		  String li = DataSet.toString(lineTable[i], SEPARATOR);
		    		  if (li.equals(selectedRow)) {
		    			  index = i;
		    			  break;
		    		  }
		    	  }
			}
    		if (index >= 0) {
    			table.setRowSelectionInterval(index, index);
    		} else {
    			table.clearSelection();
    		}
    		*/
	    	for (int i=0; i<columnNames.length; i++) {
	    		if (tableHeader.getColumnModel().getColumnCount() <= i) break;
	    		tableHeader.getColumnModel().getColumn(i).setHeaderValue(columnNames[i]);
	    	}
	    	table.repaint();
	    	tableHeader.repaint();
        }
        
        if (rowS != null && row >= 0 && lineTable != null && lineTable.length > 0) {
        	row = -1;
        	for (int i=0; i<lineTable.length; i++) {
        		if (!rowS.startsWith(lineTable[i][0]+SEPARATOR)) continue;
        		String rs = DataSet.toString(lineTable[i], SEPARATOR);
        		if (rs.equals(rowS)) {
        			row = i;
        			break;
        		}
        	}
        	if (row >= 0 && lineTable.length > row) {
	        	this.selectedRow = DataSet.toString(lineTable[row], SEPARATOR);
	        	row = table.convertColumnIndexToView(row);
	        	if (row >= 0 && row < table.getRowCount()) table.setRowSelectionInterval(row, row);
        	}
        }
		table.validate();
	}

    //  Regardless of sort order (ascending or descending), null values always appear last.
    // colIndex specifies a column in model.
	/*
	private int tableSorted = -1;
	private boolean tableSortAscending = false;
    private void sortColumn(TableModel model, int colIndex, boolean ascending) {
    	// Sort the array of column data
    	String table[] = new String[lineTable.length];
    	for (int i=0; i<lineTable.length; i++) {
    		table[i] = DataSet.toString(lineTable[i], SEPARATOR);
    	}
        Arrays.sort(table, new ColumnSorter(ascending, colIndex));
    	for (int i=0; i<lineTable.length; i++) {
    		lineTable[i] = DataSet.toStringArray(table[i], SEPARATOR, false);
    	}
        tableSorted = colIndex;
        tableSortAscending = ascending;
    }

    private void sortByColumn(int column) {
    	boolean asc = true;
    	if (this.ascending != null) {
    		asc = this.ascending[column];
    		if (asc) {
    			asc = false;
    		} else {
    			asc = true;
    		}
    		this.ascending[column] = asc;
    	}

    	this.sortColumn(table.getModel(), column, this.ascending[column]);
    	table.revalidate();
    	table.repaint();
    }
	*/
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
			valueChanging = false;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
/*       	if (e.getSource() == this.tableHeader) {
    		TableColumnModel columnModel = table.getColumnModel();
    		int viewColumn = columnModel.getColumnIndexAtX(e.getX());
    		if (viewColumn != -1) {
    			
	    		int column = table.convertColumnIndexToModel(viewColumn);
	    		if (e.getClickCount() == 1 && column != -1) {
	    		      this.sortByColumn(column);
	    		      if (selectedRow != null) {
	    		    	  for (int i=0; i<lineTable.length; i++) {
	    		    		  String li = DataSet.toString(lineTable[i], SEPARATOR);
	    		    		  if (selectedRow.equals(li)) {
	    		    			  if (i >= 0 && i < table.getRowCount()) 
	    		    				  table.setRowSelectionInterval(i, i);
	    		    			  return;
	    		    		  }
	    		    	  }
	    		      }
	    		}
    		}
    		return;
    	}
*/
  		if (e.getSource() == table) {
			int row = table.getSelectedRow();
			if (row < 0) return;
       		if (row < lineTable.length) this.selectedRow = DataSet.toString(lineTable[row], SEPARATOR);
       		return;
  		}
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	/*
	class ColumnSorter implements Comparator<Object> {
	    boolean ascending;
	    int column;
	    ColumnSorter(boolean ascending, int column) {
	        this.ascending = ascending;
	        this.column = column;
	    }
	    public int compare(Object a, Object b) {
	        // Treat empty strains like nulls
	        if (a instanceof String && ((String)a).length() == 0) {
	            a = null;
	        }
	        if (b instanceof String && ((String)b).length() == 0) {
	            b = null;
	        }

	        // Sort nulls so they appear last, regardless
	        // of sort order
	        if (a == null && b == null) {
	            return 0;
	        } else if (a == null) {
	            return 1;
	        } else if (b == null) {
	            return -1;
	        } else {
	        	String fa = FileIO.getField(column + 1, (String) a, SEPARATOR, false);
	        	String fb = FileIO.getField(column + 1, (String) b, SEPARATOR, false);
	        	if (columnClasses[column] == Integer.class || columnClasses[column] == Double.class
	        			|| columnClasses[column] == Long.class || columnClasses[column] == Float.class) {
	        		Double va = Double.parseDouble(fa);
	        		Double vb = Double.parseDouble(fb);
		            if (ascending) {
		                return va.compareTo(vb);
		            } else {
		                return vb.compareTo(va);
		       		}
	        	} else {
	        		if (columnClasses[column] == null) {
	        			try {
	        				Double ffa = Functions.parseRightAscension(fa);
	        				Double ffb = Functions.parseRightAscension(fb);
	    		            if (ascending) {
	    		                return ffa.compareTo(ffb);
	    		            } else {
	    		                return ffb.compareTo(ffa);
	    		       		}
	        			} catch (Exception exc) {
	        				try {
		        				Double ffa = Functions.parseDeclination(fa);
		        				Double ffb = Functions.parseDeclination(fb);
		    		            if (ascending) {
		    		                return ffa.compareTo(ffb);
		    		            } else {
		    		                return ffb.compareTo(ffa);
		    		       		}
	        				} catch (Exception exc2) {
	        					try {
	        						if (!DataSet.isDoubleOrMathOperationFastCheck(fa) ||
	        								!DataSet.isDoubleOrMathOperationFastCheck(fb)) throw new Exception("is not an operation");
	        		        		Double va = DataSet.getDoubleValueWithoutLimit(fa);
	        		        		Double vb = DataSet.getDoubleValueWithoutLimit(fb);
	        			            if (ascending) {
	        			                return va.compareTo(vb);
	        			            } else {
	        			                return vb.compareTo(va);
	        			       		}
	        					} catch (Exception exc3) {
	        						try {
		    	        				Double ffa = (new TimeElement(fa)).astroDate.jd();
		    	        				Double ffb = (new TimeElement(fb)).astroDate.jd();
				    		            if (ascending) {
				    		                return ffa.compareTo(ffb);
				    		            } else {
				    		                return ffb.compareTo(ffa);
				    		       		}
	        						} catch (Exception exc4) {}
	        					}
	        				}
	        			}
	        		}
		            if (ascending) {
		                return fa.compareTo(fb);
		            } else {
		                return fb.compareTo(fa);
		       		}
	        	}
	        }
	    }
	}
	*/
}
