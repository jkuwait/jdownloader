package jd;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDUtilities;

/**
 * Der Installer erscheint nur beim ersten mal STarten der Webstartversion und
 * beim neuinstallieren der webstartversion der User kann Basiceinstellungen
 * festlegen
 * 
 * @author Coalado
 */
public class Installer extends JDialog implements ActionListener, WindowListener {

    /**
     * 8764525546298642601L
     */
    private static final long serialVersionUID = 8764525546298642601L;

    private Logger            logger           = JDUtilities.getLogger();

    private JPanel            panel;

    protected Insets          insets           = new Insets(0, 0, 0, 0);

    private BrowseFile        homeDir;

    private BrowseFile        downloadDir;

    private JButton           btnOK;

    private  boolean    aborted          = false;

    /**
     * 
     */
    public Installer(File installPath, File downloadPath) {
        super();
        setModal(true);
        setLayout(new BorderLayout());

        this.setTitle("JDownloader Installation");
        this.setAlwaysOnTop(true);

        setLocation(20, 20);
        panel = new JPanel(new GridBagLayout());

        homeDir = new BrowseFile();
        homeDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        homeDir.setEditable(true);
        homeDir.setText(installPath.getAbsolutePath());

        downloadDir = new BrowseFile();
        downloadDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        downloadDir.setEditable(true);
        downloadDir.setText(downloadPath.getAbsolutePath());
        addWindowListener(this);
        JDUtilities.addToGridBag(panel, new JLabel("Install Directory"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, homeDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel("Default Download Directory"), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, downloadDir, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        btnOK = new JButton("Continue...");
        btnOK.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnOK, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.add(panel, BorderLayout.CENTER);
        this.pack();

        this.setVisible(true);
    }

    public void windowClosing(WindowEvent e) {
        this.aborted=true;
        JOptionPane.showMessageDialog(this, "Installation aborted!");
        homeDir.setText("");
        downloadDir.setText("");
  
        
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}

    public String getHomeDir() {
      
        new File(homeDir.getText()).mkdirs();
        if (!new File(homeDir.getText()).exists() || !new File(homeDir.getText()).canRead()) {
            this.aborted=true;
            JOptionPane.showMessageDialog(this, "Installation aborted!\r\nInstallation Directory is not valid: " + homeDir.getText());
            homeDir.setText("");
            downloadDir.setText("");
            this.dispose();
            System.exit(1);
            return null;
        }
        return homeDir.getText();
    }

    public String getDownloadDir() {
        new File(downloadDir.getText()).mkdirs();
        if (!new File(downloadDir.getText()).exists() || !new File(downloadDir.getText()).canWrite()) {
            this.aborted=true;
            JOptionPane.showMessageDialog(this, "Installation aborted!\r\nDownload Directory is not valid: " + homeDir.getText());
            homeDir.setText("");
            downloadDir.setText("");
            this.dispose();
            System.exit(1);
            return null;
        }

        return downloadDir.getText();
    }

    public void actionPerformed(ActionEvent e) {
  this.setVisible(false);

    }

    public  boolean isAborted() {
        return aborted;
    }

}
