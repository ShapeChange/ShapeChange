/**
 * ShapeChange - processing application schemas for geographic information
 *
 * This file is part of ShapeChange. ShapeChange takes a ISO 19109 
 * Application Schema from a UML model and translates it into a 
 * GML Application Schema or other implementation representations.
 *
 * Additional information about the software can be found at
 * http://shapechange.net/
 *
 * (c) 2002-2012 interactive instruments GmbH, Bonn, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * interactive instruments GmbH
 * Trierer Strasse 70-72
 * 53115 Bonn
 * Germany
 */

package de.interactive_instruments.ShapeChange.UI;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang3.SystemUtils;

import de.interactive_instruments.ShapeChange.Converter;
import de.interactive_instruments.ShapeChange.Options;
import de.interactive_instruments.ShapeChange.ShapeChangeAbortException;
import de.interactive_instruments.ShapeChange.ShapeChangeResult;

public class DefaultDialog extends JFrame
                           implements ActionListener, Dialog {

	private static final long serialVersionUID = 3197452835574541123L;

	private JTextField mdlField;
	private JTextField cfgField;
	private JTextField outField;
	private JTextField asField;

	private JButton mdlButton;
	private JButton cfgButton;
	private JButton outButton;
	private JButton startButton;
	private JButton logButton;
	private JButton exitButton;
	
	private ButtonGroup reportGroup;
	private ButtonGroup ruleGroup;
	
	private JCheckBox docCB;
	private JCheckBox visCB;

	private JFileChooser fc = new JFileChooser();
	
 	protected Converter converter = null;
	protected ShapeChangeResult result = null;
	protected Options options = null;
	protected String mdl = null;
	
	private File logfile = null; 
     
     public DefaultDialog(Converter c, Options o, ShapeChangeResult r, String m) {
         super("ShapeChange");
         initialise(c, o, r, m);
     };
     
 	public void initialise(Converter c, Options o, ShapeChangeResult r, String m) {
        converter = c;
        options = o;
        result = r;
        mdl = m;

        // frame
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JComponent newContentPane = new JPanel(new BorderLayout());
		newContentPane.setOpaque(true); 
		setContentPane(newContentPane);
		
		// pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Main options", createTab1());
        tabbedPane.addTab("Secondary options", createTab2());
        newContentPane.add(tabbedPane);
        
		// frame size
        int height = 480;
        int width = 720;

        pack();

        Insets fI = getInsets();
        setSize(width + fI.right + fI.left, height + fI.top + fI.bottom);
        Dimension sD = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((sD.width - width)/2, (sD.height - height)/2);
	}
 	
	public void setVisible(boolean vis){
		super.setVisible(vis);
	}
	
 	private Component createTab1() {

         final JPanel mdlPanel = new JPanel();
         mdlField = new JTextField(40);
         String s = options.parameter("inputFile");
         if (s==null)
        	 s = "";
         mdlField.setText(s);
         mdlPanel.add(mdlField);
    	 mdlPanel.add(mdlButton = new JButton("Select File"));
    	 mdlButton.setActionCommand("MDL");
    	 mdlButton.addActionListener(this);
    	 mdlPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Model file", TitledBorder.LEFT, TitledBorder.TOP));
         
         final JPanel outPanel = new JPanel();
         outField = new JTextField(40);
         s = options.parameter("outputDirectory");
         if (s==null)
        	 s = ".";
         outField.setText(s);
    	 outPanel.add(outField);
         outPanel.add(outButton = new JButton("Select File"));
         outButton.setActionCommand("OUT");
         outButton.addActionListener(this);
         outPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Output directory", TitledBorder.LEFT, TitledBorder.TOP));

         final JPanel asPanel = new JPanel();
         asField = new JTextField(49);
         s = options.parameter("appSchemaName");
         if (s==null)
        	 s = "";
         asField.setText(s);
    	 asPanel.add(asField);
         asPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Application schema name (optional)", TitledBorder.LEFT, TitledBorder.TOP));
         
         final JPanel startPanel = new JPanel();
         startButton = new JButton("Process Model");
         startButton.setActionCommand("START");
         startButton.addActionListener(this);
         startPanel.add(startButton);
         logButton = new JButton("View Log");
         logButton.setActionCommand("LOG");
         logButton.addActionListener(this);
         logButton.setEnabled(false);
         startPanel.add(logButton);
         exitButton = new JButton("Exit");
         exitButton.setActionCommand("EXIT");
         exitButton.addActionListener(this);
         exitButton.setEnabled(true);
         startPanel.add(exitButton);
         
         Box fileBox = Box.createVerticalBox();
         fileBox.add(mdlPanel);
         fileBox.add(asPanel);
         fileBox.add(outPanel);
         fileBox.add(startPanel);

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(fileBox, BorderLayout.CENTER);
         
         return panel;
     };

     private void addRadioButton(JPanel panel, ButtonGroup group, String label, String value, String parameter) {
         JRadioButton radioButton;
         panel.add(radioButton = new JRadioButton(label, parameter.equalsIgnoreCase(value)));
         radioButton.setActionCommand(value);
         group.add(radioButton);    	 
     }
     
     private JPanel createTab2()
     {
         final JPanel reportPanel = new JPanel(new GridLayout(3, 1));
         reportGroup = new ButtonGroup();
         String param = options.parameter("reportLevel");
         addRadioButton(reportPanel, reportGroup, "Error", "ERROR", param);
         addRadioButton(reportPanel, reportGroup, "Warning", "WARNING", param);
         addRadioButton(reportPanel, reportGroup, "Info", "INFO", param);
         reportPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Report options", TitledBorder.LEFT, TitledBorder.TOP));

         final JPanel rulePanel = new JPanel(new GridLayout(3, 1));
         ruleGroup = new ButtonGroup();
         param = options.parameter(Options.TargetXmlSchemaClass,"defaultEncodingRule");
         addRadioButton(rulePanel, ruleGroup, "GML 3.2", "iso19136_2007", param);
         addRadioButton(rulePanel, ruleGroup, "GML 3.3", "gml33", param);
         addRadioButton(rulePanel, ruleGroup, "ISO/TS 19139", "iso19139_2007", param);
         addRadioButton(rulePanel, ruleGroup, "GML 3.2 (ShapeChange extensions)", "iso19136_2007_ShapeChange_1.0_extensions", param);
         addRadioButton(rulePanel, ruleGroup, "GML 3.3 (INSPIRE extensions)", "iso19136_2007_INSPIRE_Extensions", param);
         rulePanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Default encoding rule", TitledBorder.LEFT, TitledBorder.TOP));
         
         final JPanel otherPanel = new JPanel(new GridLayout(2, 1));
         docCB = new JCheckBox("Include documentation",true);
         boolean b = true;
         String s = options.parameter(Options.TargetXmlSchemaClass, "includeDocumentation");
         if (s!=null && s.equals("false"))
        	 b = false;				
         docCB.setSelected(b);
         otherPanel.add(docCB);
         visCB = new JCheckBox("Ignore visibility");
         b = true;
         s = options.parameter("publicOnly");
         if (s!=null && s.equals("false"))
        	 b = false;				
         visCB.setSelected(!b);
         otherPanel.add(visCB);
         otherPanel.setBorder(new TitledBorder(new LineBorder(Color.black), 
             "Other options", TitledBorder.LEFT, TitledBorder.TOP));

         Box innerBox = Box.createHorizontalBox();
         innerBox.add(reportPanel);
         innerBox.add(otherPanel);

         Box mainBox = Box.createVerticalBox();
         mainBox.add(innerBox);
         mainBox.add(rulePanel);

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(mainBox, BorderLayout.CENTER);

         return panel;
     };

     public void actionPerformed(ActionEvent e) {
    	 if(startButton == e.getSource()) {
			 mdl = mdlField.getText().trim();
			 startButton.setEnabled(false);
	         exitButton.setEnabled(false);
			 try {
				options.setParameter("inputFile", mdl);
				if (mdl.toLowerCase().endsWith(".xmi")||mdl.toLowerCase().endsWith(".xml"))
					options.setParameter("inputModelType", "XMI10");
				else if (mdl.toLowerCase().endsWith(".eap"))
					options.setParameter("inputModelType", "EA7");
				else if (mdl.toLowerCase().endsWith(".mdb"))
					options.setParameter("inputModelType", "GSIP");
				
				options.setParameter("outputDirectory", outField.getText());
				options.setParameter("logFile", outField.getText()+"/log.xml");
				options.setParameter("appSchemaName", asField.getText());
				options.setParameter("reportLevel", reportGroup.getSelection().getActionCommand()); 
				options.setParameter(Options.TargetXmlSchemaClass,"defaultEncodingRule", ruleGroup.getSelection().getActionCommand());
				if (docCB.isSelected())
					 options.setParameter(Options.TargetXmlSchemaClass,"includeDocumentation", "true");
				else
					 options.setParameter(Options.TargetXmlSchemaClass,"includeDocumentation", "false");
				if (!visCB.isSelected())
					 options.setParameter("publicOnly", "true");
				else
					 options.setParameter("publicOnly", "false");
				
				converter.convert();
			 } catch (ShapeChangeAbortException ex) {
				 Toolkit.getDefaultToolkit().beep();
			 }
			 logfile = new File(options.parameter("logFile").replace(".xml", ".html"));
			 if (logfile!=null && logfile.canRead())
				 logButton.setEnabled(true);
			 else {
				 logfile = new File(options.parameter("logFile"));
				 if (logfile!=null && logfile.canRead())
					 logButton.setEnabled(true);
			 }
	         exitButton.setEnabled(true);
    	 } else if (e.getSource() == logButton) {
    		 try {
    			 if (Desktop.isDesktopSupported())
        			 Desktop.getDesktop().open(logfile);
    			 else if (SystemUtils.IS_OS_WINDOWS)
    				 Runtime.getRuntime().exec("cmd /c start "+logfile.getPath());
    			 else 
    				 Runtime.getRuntime().exec("open "+logfile.getPath());
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
    	 } else if (e.getSource() == exitButton) {
    		 System.exit(0);
    	 } else if (e.getSource() == mdlButton) {
    	        int returnVal = fc.showOpenDialog(DefaultDialog.this);
    	        if (returnVal == JFileChooser.APPROVE_OPTION) {
    	            File file = fc.getSelectedFile();
    	            mdlField.setText(file.getAbsolutePath());
    	        }
    	 } else if (e.getSource() == cfgButton) {
 	        int returnVal = fc.showOpenDialog(DefaultDialog.this);
 	        if (returnVal == JFileChooser.APPROVE_OPTION) {
 	            File file = fc.getSelectedFile();
 	            cfgField.setText(file.getAbsolutePath());
 	        }
    	 } else if (e.getSource() == outButton) {
  			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
  	        int returnVal = fc.showOpenDialog(DefaultDialog.this);
  	        if (returnVal == JFileChooser.APPROVE_OPTION) {
  	            File file = fc.getSelectedFile();
  	            outField.setText(file.getAbsolutePath());
  	        }
    	}
     }
}

