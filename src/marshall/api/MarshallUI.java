package marshall.api;


import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;

import javax.swing.*;
import javax.swing.border.LineBorder;

	
public class MarshallUI {
	public final static String PROUCT_1 = "cocacola";
	public final static String PROUCT_2 = "fuzetea";
	public final static String PROUCT_3 = "pepsi";
	public final static String PROUCT_4 = "colazero";
	
	public final static String SELECT = "Select Product";
	public final static String TAKE = "Take Product";
	//public final static String STOP = "Stop Session";
	public final static String CANCEL = "Cancel Vend";
	public final static String SELECT_PORT = "Select Port";
	
    MarshallMain marshall;
    JLabel picture;
    JFrame frame;
    
    public static JComboBox comboBox;
    public static JTextArea consoleText;
    public static JTextArea logText;
    public static String selectedPort = null;
    
    public static JButton button1;
    public static JButton button2;
    public static JButton button3;
   
    
    JRadioButton cocacola;
    JRadioButton fuzeTea;
    JRadioButton pepsi;
    JRadioButton colaZero;
    
	public static String itemSelected = PROUCT_1;
	    
		public MarshallUI(JFrame frame_, MarshallMain marshall_) {
						
			this.frame = frame_;	
			this.marshall = marshall_;
			
			
			logText = new JTextArea();
			logText.setFont(new Font("Courier New", Font.PLAIN, 12));
			logText.setBounds(270, 18, 860, 200);
			logText.setBorder(new LineBorder(new Color(0, 0, 0)));
			frame.getContentPane().add(logText);
			
			consoleText = new JTextArea();
			consoleText.setFont(new Font("Courier New", Font.PLAIN, 12));
			consoleText.setBounds(270, 250, 860, 280);
			consoleText.setBorder(new LineBorder(new Color(0, 0, 0)));
			consoleText.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
			//consoleText.scrollRectToVisible(null);
			consoleText.setAutoscrolls(true);
			consoleText.setLineWrap(true);
			
		
			frame.getContentPane().add(consoleText);			
			Action action = new SwingAction(logText);
			
			//Commands button Panel
			JPanel commandsPanel = new JPanel();
			commandsPanel.setBounds(22, 300, 161, 180);			
			frame.getContentPane().add(commandsPanel);
			commandsPanel.setLayout(null);
			
			
			 //Create the buttons.
			button1 = new JButton(SELECT);
	        button1.setSelected(true);
	        button1.setBounds(0, 0, 144, 50);
	        button1.setSize(new Dimension(160,50));
	        button1.setAction(action);
	        button1.setText("SELECT PRODUCT");
	        button1.setActionCommand("select");
	        button1.setEnabled(false);
	        commandsPanel.add(button1);
	        
	     	button2 = new JButton(TAKE);
	     	button2.setBounds(0, 63, 160, 50);
	     	button2.setSize(new Dimension(160,50));
			button2.setAction(action);
			button2.setText("TAKE PRODUCT");
			button2.setActionCommand("take");
			button2.setEnabled(false);
			commandsPanel.add(button2);
			
			button3 = new JButton(CANCEL);
			button3.setBounds(0, 125, 160, 50);
			button3.setSize(new Dimension(160,50));
			button3.setAction(action);
			button3.setText("CANCEL VEND");
			button3.setActionCommand("cancel");
			button3.setEnabled(true);
			commandsPanel.add(button3);
			
			
	        
			//Radio button Panel
			JPanel radioPanel = new JPanel();
			radioPanel.setBounds(22, 18, 118, 140);
			radioPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			frame.getContentPane().add(radioPanel);
			radioPanel.setLayout(null);
			
			cocacola = new JRadioButton(PROUCT_1);
			cocacola.setBounds(6, 10, 109, 23);
			cocacola.setAction(action);
			cocacola.setText("COCA-COLA");
			cocacola.setActionCommand(PROUCT_1);
	        cocacola.setSelected(true);
			radioPanel.add(cocacola);
			
			fuzeTea = new JRadioButton(PROUCT_2);
			fuzeTea.setBounds(6, 42, 109, 23);
			fuzeTea.setAction(action);
			fuzeTea.setText("FUZE TEA");
		    fuzeTea.setActionCommand(PROUCT_2);
			radioPanel.add(fuzeTea);
			
			pepsi = new JRadioButton(PROUCT_3);
			pepsi.setBounds(6, 74, 109, 23);
			pepsi.setAction(action);
			pepsi.setText("PEPSI");
			pepsi.setActionCommand(PROUCT_3);
			radioPanel.add(pepsi);
			
			colaZero = new JRadioButton(PROUCT_4);
			colaZero.setBounds(6, 106, 109, 23);
			colaZero.setAction(action);
			colaZero.setText("COLA ZERO");
			colaZero.setActionCommand(PROUCT_4);
			radioPanel.add(colaZero);
			
				        			
	        //Set up the picture label.
	        picture = new JLabel(createImageIcon("/images/" + "cocacola"
	                                             + ".png"));
	     		        
	        picture.setBounds(120, 20, 170, 150);
	        frame.getContentPane().add(picture);
	         
	        //Openup the combo box with available ports
	        MarshallHal.getAvailableSerialPorts();
			comboBox = new JComboBox(MarshallHal.portNames);
			frame.getContentPane().add(comboBox);
			comboBox.setBounds(22, 206, 116, 23);			
			comboBox.setAction(action);
			comboBox.setActionCommand("comSelect");				
		        
		}
	
	 /** Returns an ImageIcon, or null if the path was invalid. */
	 private static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = MarshallUI.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
	        
	class SwingAction extends AbstractAction {
		
		private static final long serialVersionUID = 1L;
		JTextArea textArea;
				
		public SwingAction(JTextArea textArea_) {
			
			putValue(NAME, "Marshall Action");
			putValue(SHORT_DESCRIPTION, "Pick a Drink");
		
			textArea = textArea_;			
		}
		
		public void actionPerformed(ActionEvent e) {
			
			String action = e.getActionCommand();
			String imagePath = ("/images/" + e.getActionCommand()+ ".png");
						
	        switch (action){
	        	case "select":	        
	        		marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.PRODUCT_SELECTED);
					if (MarshallMessage.beginSession == true){
						button1.setEnabled(false);
					}
	        		break;
	        	case "cancel":	
	        		marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_CANCELLED);	
	        		break;
	        	case "take":	        
	        		marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.PRODUCT_TAKEN);	
	        		button2.setEnabled(false);
	        		break;
	        	case "cocacola": 
	        		picture.setIcon(createImageIcon("/images/" + e.getActionCommand()+ ".png"));
	        		itemSelected = PROUCT_1;
	        		cocacola.setSelected(true);
	        		fuzeTea.setSelected(false);
	        		pepsi.setSelected(false);
	        		colaZero.setSelected(false);
	        		break;
	        	case "fuzetea":       		
	        		picture.setIcon(createImageIcon(imagePath));
	        		itemSelected = PROUCT_2;
	        		fuzeTea.setSelected(true);
	        		cocacola.setSelected(false);
	        		pepsi.setSelected(false);
	        		colaZero.setSelected(false);
	        		break;
	        	case "pepsi":
	        		picture.setIcon(createImageIcon("/images/" + e.getActionCommand()+ ".png"));
	        		itemSelected = PROUCT_3;
	        		pepsi.setSelected(true);
	        		fuzeTea.setSelected(false);
	        		cocacola.setSelected(false);
	        		colaZero.setSelected(false);
	        		break;
	        	case "colazero":
	        		picture.setIcon(createImageIcon("/images/" + e.getActionCommand()+ ".png"));
	        		itemSelected = PROUCT_4;
	        		colaZero.setSelected(true);
	        		fuzeTea.setSelected(false);
	        		cocacola.setSelected(false);
	        		pepsi.setSelected(false);
	        		break;
	        	case "comSelect":      	
	        		JComboBox cb = (JComboBox)e.getSource();
	                selectedPort = (String)cb.getSelectedItem();
	                //int index = (int)cb.getSelectedIndex();
	            	System.out.printf("**COM SELECTED: " + selectedPort + "\n");	        		
	        		break;
	        	default:
	        		textArea.insert( "\nUn supported Action " + action, 20);	
			}
		}
	}
			
			
			
	public static void createAndShowGUI(MarshallMain marshall){
		
		JFrame frame;
			
		frame = new JFrame();
		frame.setTitle("Marshall Simulator (Java/Linux)");
		frame.setBounds(100,100, 1200, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		MarshallUI window = new MarshallUI(frame, marshall);
		frame.setVisible(true);
		
	}
	 	
	 
}

