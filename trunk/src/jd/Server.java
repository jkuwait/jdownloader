package jd;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.controlling.DistributeData;
import jd.controlling.JDController;
import jd.controlling.interaction.Unrar;
import jd.event.ControlEvent;
import jd.gui.UIInterface;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;
import jd.utils.Reconnecter;

public class Server extends UnicastRemoteObject implements ServerInterface {

    private static final long serialVersionUID = 1L;
    public static Logger logger = JDUtilities.getLogger();

    Server() throws RemoteException {
        super();
    }

    public void go() {

        try {
            LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (RemoteException ex) {
            System.out.println(ex.getMessage());
        }

        try {
            Properties p = System.getProperties();
            p.setProperty("sun.rmi.transport.tcp.handshakeTimeout", "100");
            Naming.rebind("jDownloader", new Server());
        } catch (MalformedURLException ex) {
            System.out.println(ex.getMessage());
        } catch (RemoteException ex) {
            System.out.println(ex.getMessage());
        }

    }

    public void processParameters(String[] input) throws RemoteException {

        boolean addLinksSwitch = false;
        boolean addContainersSwitch = false;
        boolean addPasswordsSwitch = false;
        boolean extractSwitch = false;

        Vector<String> linksToAdd = new Vector<String>();
        Vector<String> containersToAdd = new Vector<String>();

        Vector<String> paths = new Vector<String>();
        long extractTime = 0;
        boolean doExtract = false;
        boolean hideGrabber = false;
        boolean startDownload = false;
        
        JDController controller = JDUtilities.getController();

        for (String currentArg : input) {

            if (currentArg.equals("--help") || currentArg.equals("-h")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;

                showCmdHelp();

            } else if (currentArg.equals("--add-links") || currentArg.equals("--add-link") || currentArg.equals("-a")) {

                addLinksSwitch = true;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-containers") || currentArg.equals("--add-container") || currentArg.equals("-c")) {

                addContainersSwitch = true;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-passwords") || currentArg.equals("--add-password") || currentArg.equals("-p")) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = true;
                extractSwitch = false;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--extract") || currentArg.equals("-e")) {

                doExtract = true;
                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = true;
                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--start-download") || currentArg.equals("-d")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                
                logger.info(currentArg + " parameter");
                startDownload = true;
                

            } else if (currentArg.equals("--stop-download") || currentArg.equals("-D")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;

                logger.info(currentArg + " parameter");
                if ( controller.getDownloadStatus() == JDController.DOWNLOAD_RUNNING) {
            		// only in this way the button state is correctly set
            		// stopDownloads() is called by button itself so it cannot handle this
            		controller.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_STARTSTOP_DOWNLOAD, this));
            	}

            } else if (currentArg.equals("--show") || currentArg.equals("-s")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;

                JACController.showDialog(false);

            } else if (currentArg.equals("--train") || currentArg.equals("-t")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;

                JACController.showDialog(true);

            } else if (currentArg.equals("--minimize") || currentArg.equals("-m")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                JDUtilities.getGUI().setGUIStatus(UIInterface.WINDOW_STATUS_MINIMIZED);

                logger.info(currentArg + " parameter");

            } else if (currentArg.equals("--focus") || currentArg.equals("-f")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                logger.info(currentArg + " parameter");
                JDUtilities.getGUI().setGUIStatus(UIInterface.WINDOW_STATUS_FOREGROUND);
                
            } else if (currentArg.equals("--hide") || currentArg.equals("-H")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                logger.info(currentArg + " parameter");
                hideGrabber = true;
                
            } else if (currentArg.equals("--reconnect") || currentArg.equals("-r")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                logger.info(currentArg + " parameter");
                Reconnecter.waitForNewIP(1);
                
            } else if (addLinksSwitch && currentArg.charAt(0) != '-') {

                linksToAdd.add(currentArg);

            } else if (addContainersSwitch && currentArg.charAt(0) != '-') {

                if (new File(currentArg).exists()) {
                    containersToAdd.add(currentArg);
                } else {
                    logger.warning("Container does not exist");
                }

            } else if (addPasswordsSwitch && !(currentArg.charAt(0) == '-')) {

                JUnrar unrar = new JUnrar(true);
                unrar.addToPasswordlist(currentArg);
                logger.info("Add password: " + currentArg);

            } else if (extractSwitch && !(currentArg.charAt(0) == '-')) {

                if (currentArg.equals("--rotate") || currentArg.equals("-r")) {

                    logger.info(currentArg + " parameter");
                    extractTime = -1;

                } else if (extractTime == -1) {

                    if (currentArg.matches("[\\d]+")) {
                        extractTime = (int) Integer.parseInt(currentArg);
                    } else
                        extractTime = 0;

                } else if (!currentArg.matches("[\\s]*")) {

                    paths.add(currentArg);

                }

            } else if (currentArg.contains("http://") && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                linksToAdd.add(currentArg);

            } else if (new File(currentArg).exists() && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;
                containersToAdd.add(currentArg);

            } else {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                extractSwitch = false;

            }

        }

        if (linksToAdd.size() > 0) logger.info("Links to add: " + linksToAdd.toString());
        if (containersToAdd.size() > 0) logger.info("Containers to add: " + containersToAdd.toString());

        for (int i = 0; i < containersToAdd.size(); i++) {
            JDUtilities.getController().loadContainerFile(new File(containersToAdd.get(i)));
        }

        String linksToAddString = "";

        for (int i = 0; i < linksToAdd.size(); i++) {
            linksToAddString += linksToAdd.get(i) + "\n";
        }
        
        if (!linksToAddString.equals("")) {
        	
        	DistributeData distributeData = new DistributeData(linksToAddString, hideGrabber, startDownload);
            distributeData.addControlListener(JDUtilities.getController());
            distributeData.start();
            
        } else if (startDownload) {
        	
        	if ( controller.getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING ) {
        		// only in this way the button state is correctly set
        		// startDownloads() is called by button itself so it cannot handle this
        		controller.fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_STARTSTOP_DOWNLOAD, this));
        	}
        	
        }

        if (doExtract) {
            logger.info("Extract: [" + paths.toString() + " | " + extractTime + "]");
            Server.extract(paths, extractTime, false);
        }
        
    }

    public static void showCmdHelp() {

        String[][] help = new String[][] {
        		{ JDUtilities.getJDTitle(), "Coalado|Astaldo|DwD|Botzi|eXecuTe GPLv3" },
        		{ "http://jdownloader.ath.cx/\t\t", "http://www.the-lounge.org/viewforum.php?f=217" + System.getProperty("line.separator") },
        		{ "-h/--help\t", "Show this help message" },
        		{ "-a/--add-link(s)", "Add links" },
        		{ "-c/--add-container(s)", "Add containers" },
        		{ "-p/--add-password(s)", "Add passwords" },
        		{ "-d/--start-download", "Start download" },
        		{ "-D/--stop-download", "Stop download" },
        		{ "-H/--hide\t", "Don't open Linkgrabber when adding Links" },
        		{ "-m/--minimize\t", "Minimize download window" },
        		{ "-f/--focus\t", "Get jD to foreground/focus" },
        		{ "-s/--show\t", "Show JAC prepared captchas" },
        		{ "-t/--train\t", "Train a JAC method" },
        		{ "-r/--reconnect\t", "Perform a Reconnect" },
        		{ "-C/--captcha <filepath or url> <method>", "Get code from image using JAntiCaptcha" },
        		{ "-e/--extract (<sourcePath1> (<sourcePath2...n> <targetPath>)) (-r/--rotate <seconds>)", "" },
                { "\t\t\tExtract (optional from given directory and optional to given directory) with jD settings (optional periodly)", "" },
                { "\t\t", "Example: java -jar JDownloader.jar -e /source/folder -r 60 [extract every minute from /source/folder to /source/folder" },
                { "-n --new-instance", "Force new instance if another jD is running" } };

        for (String helpLine[] : help) {
            System.out.println(helpLine[0] + "\t" + helpLine[1]);
        }

    }

    public static void extract(final Vector<String> paths, final long rtime, final boolean isServer) {

        new Thread(new Runnable() {

            public void run() {

                if (isServer) {

                    JDInit init = new JDInit();
                    init.loadConfiguration();

                }

                while (true) {

                    JUnrar unrar = new JUnrar();
                    unrar.progressInTerminal = true;
                    String downloadFolder = JDUtilities.getConfiguration().getDefaultDownloadDirectory();
                    LinkedList<String> folders = new LinkedList<String>();

                    if (paths != null) {

                        // leave out last element if there are more than one
                        // folders
                        short max = (short) paths.size();
                        if (max > 1) max -= 1;

                        for (int i = 0; i < max; i++) {

                            folders.add(paths.get(i));

                        }

                    } else {
                        folders.add(downloadFolder);
                    }

                    logger.info("Unrar input folders: " + folders.toString());
                    unrar.setFolders(folders);
                    unrar.useToextractlist = false;

                    boolean useExtractFolder = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_ENABLE_EXTRACTFOLDER);

                    if (paths.size() > 1) {
                        unrar.extractFolder = new File(paths.get(1));
                    } else if (useExtractFolder) {
                        unrar.extractFolder = new File(JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_EXTRACTFOLDER));
                    } else {

                        if (paths.size() == 1) {
                            unrar.extractFolder = new File(paths.get(0));
                        } else {
                            unrar.extractFolder = new File(downloadFolder);
                        }

                    }

                    logger.info("Unrar output folder: " + unrar.extractFolder);

                    unrar.overwriteFiles = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_OVERWRITE_FILES, false);
                    unrar.autoDelete = JDUtilities.getConfiguration().getBooleanProperty(Unrar.PROPERTY_AUTODELETE, true);
                    unrar.unrar = JDUtilities.getConfiguration().getStringProperty(Unrar.PROPERTY_UNRARCOMMAND);
                    unrar.unrar();

                    if (rtime >= 1) {

                        try {
                            Thread.sleep(rtime * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } else {

                        if (isServer) System.exit(0);
                        break;

                    }

                }

            }

        }).start();

    }

}
