//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.interfaces;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import jd.Installer;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.PasswordListController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.JDFlags;
import jd.nutils.encoding.Encoding;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.OptionalPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "externinterface", interfaceversion = 5)
public class JDExternInterface extends PluginOptional {

    private RequestHandler handler;
    private HttpServer server = null;
    private static String jdpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/JDownloader.jar";

    public JDExternInterface(PluginWrapper wrapper) {
        super(wrapper);
        handler = new RequestHandler();
    }

    @Override
    public String getIconKey() {
        return "gui.images.flashgot";
    }

    private void initConfigEntries() {
        config.setGroup(new ConfigGroup(JDL.L("jd.plugins.optional.interfaces.JDExternInterface.flashgot.configgroup", "Install FlashGot Firefox Addon"), JDTheme.II("gui.images.flashgot", 16, 16)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Installer.installFirefoxaddon();
            }

        }, JDL.L("jd.plugins.optional.interfaces.JDExternInterface.flashgot", "Install"), JDL.L("jd.plugins.optional.interfaces.JDExternInterface.flashgot.long", "Install Firefox integration"), null));
    }

    @Override
    public boolean initAddon() {
        logger.info("Extern Interface API initialized on port 9666");
        initConfigEntries();

        if (!SubConfiguration.getConfig("FLASHGOT").getBooleanProperty("ASKED_TO_INSTALL_FLASHGOT", false)) {
            Installer.askInstallFlashgot();
        }
        try {
            server = new HttpServer(this.getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666), handler);
            server.start();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public void onExit() {
        try {
            if (server != null) server.sstop();
        } catch (Exception e) {
        }
        server = null;
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    class RequestHandler implements Handler {
        private String namespace;
        private String[] splitPath;

        public void handle(Request request, Response response) {
            splitPath = request.getRequestUrl().substring(1).split("[/|\\\\]");
            namespace = splitPath[0];
            try {
                if (namespace.equalsIgnoreCase("flash")) {
                    if (splitPath.length > 1 && splitPath[1].equalsIgnoreCase("add")) {
                        askPermission(request);
                        /* parse the post data */
                        String urls[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("urls")));
                        String passwords[] = Regex.getLines(Encoding.htmlDecode(request.getParameters().get("passwords")));
                        PasswordListController.getInstance().addPasswords(passwords);
                        if (urls.length != 0) {
                            ArrayList<DownloadLink> links = new DistributeData(Encoding.htmlDecode(request.getParameters().get("urls"))).findLinks();
                            LinkGrabberController.getInstance().addLinks(links, false, false);
                            response.addContent("success\r\n");
                        } else {
                            response.addContent("failed\r\n");
                        }
                    } else if (splitPath.length > 1 && splitPath[1].equalsIgnoreCase("addcrypted")) {
                        askPermission(request);
                        /* parse the post data */
                        String dlc = Encoding.htmlDecode(request.getParameters().get("crypted")).trim().replace(" ", "+");
                        File tmp;
                        try {
                            JDUtilities.getResourceFile("tmp").mkdirs();
                            tmp = File.createTempFile("jd_", ".dlc", JDUtilities.getResourceFile("tmp"));

                            JDIO.saveToFile(tmp, dlc.getBytes());
                            ArrayList<DownloadLink> links = JDUtilities.getController().getContainerLinks(tmp);

                            LinkGrabberController.getInstance().addLinks(links, false, false);
                            response.addContent("success\r\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        response.addContent(JDUtilities.getJDTitle() + "\r\n");
                    }
                } else if (request.getRequestUrl().equalsIgnoreCase("/crossdomain.xml")) {
                    response.addContent("<?xml version=\"1.0\"?>\r\n");
                    response.addContent("<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">\r\n");
                    response.addContent("<cross-domain-policy>\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"jdownloader.net:8081\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net:8081\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.org\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.jdownloader.net\" />\r\n");
                    response.addContent("<allow-access-from domain=\"linksave.in\" />\r\n");
                    response.addContent("<allow-access-from domain=\"relink.us\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.linksave.in\" />\r\n");
                    response.addContent("<allow-access-from domain=\"*.relink.us\" />\r\n");
                    response.addContent("</cross-domain-policy>\r\n");
                } else if (namespace.equalsIgnoreCase("flashgot")) {
                    /*
                     * path and commandline to JD, so FlashGot can check
                     * existence and start jd if needed
                     */
                    response.addContent(jdpath + "\r\n");
                    response.addContent("java -Xmx512m -jar " + jdpath + "\r\n");

                    if (request.getHeader("referer") == null || !request.getHeader("referer").endsWith(getPluginConfig().getIntegerProperty("INTERFACE_PORT", 9666) + "/flashgot")) {
                        /*
                         * security check for flashgot referer, skip asking if
                         * we find valid flashgot referer
                         */
                        askPermission(request);
                    } else {
                        JDLogger.getLogger().info("Valid FlashGot Referer found, skipping AskPermission");
                    }
                    String urls[] = Regex.getLines(Encoding.urlDecode(request.getParameters().get("urls"), false));
                    String desc[] = Regex.getLines(Encoding.urlDecode(request.getParameters().get("descriptions"), false));
                    String dir = null;
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName("FlashGot");
                    fp.setProperty(LinkGrabberController.DONTFORCEPACKAGENAME, "yes");
                    if (request.getParameters().get("dir") != null) {
                        dir = Encoding.urlDecode(request.getParameters().get("dir"), false).trim();
                        fp.setDownloadDirectory(dir);
                    }
                    String cookies = null;
                    if (request.getParameters().get("cookies") != null) cookies = Encoding.urlDecode(request.getParameters().get("cookies"), false);
                    String post = null;
                    if (request.getParameters().get("postData") != null) post = Encoding.urlDecode(request.getParameters().get("postData"), false);
                    boolean autostart = false;
                    if (request.getParameters().get("autostart") != null && request.getParameter("autostart").startsWith("1")) autostart = true;
                    String referer = Encoding.urlDecode(request.getParameter("referer"), false);
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                    if (urls.length != 0) {
                        for (int i = 0; i < urls.length; i++) {
                            String url = urls[i];
                            DistributeData dt = new DistributeData(url);
                            dt.setFilterNormalHTTP(true);
                            ArrayList<DownloadLink> foundlinks = dt.findLinks();
                            if (foundlinks.size() > 0) {
                                for (DownloadLink dl : foundlinks) {
                                    if (!dl.gotBrowserUrl()) dl.setBrowserUrl(referer);
                                    if (i < desc.length) dl.setSourcePluginComment(desc[i]);
                                }
                                links.addAll(foundlinks);
                            } else {
                                /* directlinks here */
                                PluginForHost plg = JDUtilities.getNewPluginForHostInstance("DirectHTTP");
                                String name = Plugin.getFileNameFormURL(new URL(url));
                                DownloadLink direct = new DownloadLink(plg, name, "DirectHTTP", url, true);
                                direct.setBrowserUrl(referer);
                                if (i < desc.length) direct.setSourcePluginComment(desc[i]);
                                direct.setProperty("cookies", cookies);
                                direct.setProperty("post", post);
                                direct.setProperty("referer", referer);
                                plg.correctDownloadLink(direct);
                                links.add(direct);
                            }
                        }
                        fp.addLinks(links);
                        LinkGrabberController.getInstance().addLinks(links, autostart, autostart);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }

        private void askPermission(Request request) throws Exception {
            String app = "unknown application";
            if (request.getHeader("user-agent") != null) {
                app = request.getHeader("user-agent").replaceAll("\\(.*\\)", "");
            }
            JDLogger.getLogger().warning("\r\n\r\n-----------------------External request---------------------");
            JDLogger.getLogger().warning("An external tool adds links to JDownloader. Request details:");
            JDLogger.getLogger().warning(request.toString());
            JDLogger.getLogger().warning(request.getParameters().toString());
            JDLogger.getLogger().warning("\r\n-----------------------External request---------------------");
            if (!JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, JDL.LF("jd.plugins.optional.interfaces.jdflashgot.security.title", "External request from %s to %s interface!", app, namespace), JDL.LF("jd.plugins.optional.interfaces.jdflashgot.security.message", "An external application tries to add links. See Log for details."), UserIO.getInstance().getIcon(UserIO.ICON_WARNING), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_allow", "Allow it!"), JDL.L("jd.plugins.optional.interfaces.jdflashgot.security.btn_deny", "Deny access!")), UserIO.RETURN_OK)) {
                JDLogger.getLogger().warning("Denied access.");
                throw new Exception("User denied access");
            }

        }

    }

}
