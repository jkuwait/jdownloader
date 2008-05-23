//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelDownload extends ConfigPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 4145243293360008779L;

    private SubConfiguration  config;

    private ConfigContainer container;

    private ConfigEntriesPanel cep;

    public ConfigPanelDownload(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);

        initPanel();

        load();

    }

    public void save() {
        logger.info("save");
        cep.save();
        //this.saveConfigEntries();
        config.save();
    }
    public void  setupContainer(){
        this.container= new ConfigContainer(this);
        config= JDUtilities.getSubConfig("DOWNLOAD");
        ConfigContainer network = new ConfigContainer(this, JDLocale.L("gui.config.download.network.tab", "Internet & Netzwerkverbindung"));
        ConfigContainer download = new ConfigContainer(this, JDLocale.L("gui.config.download.download.tab", "Downloadsteuerung"));
        ConfigContainer extended = new ConfigContainer(this, JDLocale.L("gui.config.download.network.extended", "Erweiterte Einstellungen"));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, network));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, download));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));
        ConfigEntry ce;
        ConfigEntry conditionEntry;
        ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, JDUtilities.getConfiguration(), Configuration.PARAM_DOWNLOAD_DIRECTORY, JDLocale.L("gui.config.general.downloadDirectory", "Downloadverzeichnis")).setDefaultValue(JDUtilities.getResourceFile("downloads").getAbsolutePath());
        container.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, JDLocale.L("gui.config.general.createSubFolders", "Wenn möglich Unterordner mit Paketname erstellen"));
        ce.setDefaultValue(false);        
        container.addEntry(ce);
        ce =new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, JDUtilities.getConfiguration(), Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, new String[]{Configuration.FINISHED_DOWNLOADS_REMOVE, Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START, Configuration.FINISHED_DOWNLOADS_NO_REMOVE}, JDLocale.L("gui.config.general.toDoWithDownloads", "Fertig gestellte Downloads ...")).setDefaultValue(Configuration.FINISHED_DOWNLOADS_REMOVE_AT_START).setExpertEntry(true);
        container.addEntry(ce); 
       
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, JDLocale.L("gui.config.download.timeout.read", "Timeout beim Lesen [ms]"), 0, 60000);
        ce.setDefaultValue(10000);
        ce.setStep(500);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, JDLocale.L("gui.config.download.timeout.connect", "Timeout beim Verbinden(Request) [ms]"), 0, 60000);
        ce.setDefaultValue(10000);
        ce.setStep(500);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, JDLocale.L("gui.config.download.simultan_downloads", "Maximale gleichzeitige Downloads"), 1, 20);
        ce.setDefaultValue(3);
        ce.setStep(1);
        download.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, JDLocale.L("gui.config.download.chunks", "Anzahl der Verbindungen/Datei(Chunkload)"), 1, 20);
        ce.setDefaultValue(3);
        ce.setStep(1);
        conditionEntry = ce;
        download.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, "PARAM_DOWNLOAD_AUTO_CORRECTCHUNKS", JDLocale.L("gui.config.download.autochunks", "Chunks an Dateigröße anpassen."));
        ce.setDefaultValue(true);
        ce.setEnabledCondidtion(conditionEntry, ">", 1);
        ce.setExpertEntry(true);
        download.addEntry(ce);
        
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, PluginForHost.PARAM_MAX_RETRIES, JDLocale.L("gui.config.download.retries", "Max. Neuversuche bei vorrübergehenden Hosterproblemen"), 0, 20);
        ce.setDefaultValue(3);
        ce.setStep(1);
     
        download.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, PluginForHost.PARAM_MAX_ERROR_RETRIES, JDLocale.L("gui.config.download.errorretries", "Max. Neuversuche bei einem Fehler"), 0, 20);
        ce.setDefaultValue(0);
        ce.setStep(1);
     
        download.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, JDLocale.L("gui.config.download.ipcheck.disable", "IP Überprüfung deaktivieren"));
        ce.setDefaultValue(false);
        ce.setExpertEntry(true);
        conditionEntry=ce;
        extended.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDLocale.L("gui.config.download.ipcheck.website", "IP prüfen über (Website)"));
        ce.setDefaultValue("http://checkip.dyndns.org");
        ce.setExpertEntry(true);
        ce.setEnabledCondidtion(conditionEntry, "==", false);
        extended.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDLocale.L("gui.config.download.ipcheck.regex", "RegEx zum filtern der IP"));
        ce.setDefaultValue("Address\\: ([0-9.]*)\\<\\/body\\>");
        ce.setEnabledCondidtion(conditionEntry, "==", false);
        ce.setExpertEntry(true);
        extended.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME), SimpleGUI.PARAM_START_DOWNLOADS_AFTER_START, JDLocale.L("gui.config.download.startDownloadsOnStartUp", "Download beim Programmstart beginnen"));
        ce.setDefaultValue(false);
        container.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, config, Configuration.PARAM_FILE_EXISTS, new String[] { JDLocale.L("system.download.triggerfileexists.overwrite", "Datei überschreiben"), JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen") }, JDLocale.L("system.download.triggerfileexists", "Wenn eine Datei schon vorhanden ist:"));
        ce.setDefaultValue(JDLocale.L("system.download.triggerfileexists.skip", "Link überspringen"));
        ce.setExpertEntry(false);
        container.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, JDLocale.L("gui.config.download.crc", "SFV/CRC Check wenn möglich durchführen"));
        ce.setDefaultValue(false);
        ce.setExpertEntry(true);
        extended.addEntry(ce);
        
        ce= new ConfigEntry(ConfigContainer.TYPE_SEPARATOR);
        network.addEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getConfiguration(), Configuration.USE_PROXY, JDLocale.L("gui.config.download.use_proxy", "Proxy Verwenden"));
        ce.setDefaultValue(false);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        conditionEntry=ce;
        
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PROXY_HOST, JDLocale.L("gui.config.download.proxy.host", "Host/IP"));
        
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        
        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PROXY_PORT, JDLocale.L("gui.config.download.proxy.port", "Port"),1,65000);
        ce.setDefaultValue(1080);
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        
        
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PROXY_USER, JDLocale.L("gui.config.download.proxy.user", "Benutzername (optional)"));
      
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        ce.setExpertEntry(true);
        network.addEntry(ce);
        
        
        ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PROXY_PASS, JDLocale.L("gui.config.download.proxy.pass", "Passwort (optional)"));
    
        ce.setEnabledCondidtion(conditionEntry, "==", true);
        ce.setExpertEntry(true);
        network.addEntry(ce);
     

        

  
    }
    public void initPanel() {
        setupContainer();      
        this.setLayout(new GridBagLayout());
       
        JDUtilities.addToGridBag(this, cep=new ConfigEntriesPanel(this.container,"Download"), 0, 0, 1, 1, 1, 1,null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
    
    }

    @Override
    public void load() {
        this.loadConfigEntries();

    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.download.name", "Netzwerk/Download");
    }

}
