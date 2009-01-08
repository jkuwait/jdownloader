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

package jd.plugins.host;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.PostRequest;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.update.HTMLEntities;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.SnifferException;
import jd.utils.Sniffy;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidshare extends PluginForHost {

    private static long LAST_FILE_CHECK = 0;

    private static final Pattern PATTERM_MATCHER_ALREADY_LOADING = Pattern.compile("(Warten Sie bitte, bis der Download abgeschlossen ist)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_DOWNLOAD_POST_URL = Pattern.compile("<form name=\"dl[f]?\" action=\"(.*?)\" method=\"post\"");

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?download the following file:.*?<p>(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_1 = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?<p.*?>(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_2 = Pattern.compile("<!-- E#[\\d]{1,2} -->(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_3 = Pattern.compile("<!-- E#[\\d]{1,2} --><p>(.*?)<\\/p>", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_MIRROR_URL = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    private static final Pattern PATTERN_FIND_MIRROR_URLS = Pattern.compile("<input.*?type=\"radio\" name=\"mirror\" onclick=\"document\\.dlf?\\.action=[\\\\]?'(.*?)[\\\\]?';\" /> (.*?)<br />", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_PRESELECTED_SERVER = Pattern.compile("<form name=\"dlf?\" action=\"(.*?)\" method=\"post\">");

    private static final Pattern PATTERN_FIND_TICKET_WAITTIME = Pattern.compile("var c=([\\d]*?);");

    private static final Pattern PATTERN_MATCHER_TOO_MANY_USERS = Pattern.compile("(2 minute)");

    private static final String PROPERTY_INCREASE_TICKET = "INCREASE_TICKET";

    private static final String PROPERTY_SELECTED_SERVER = "SELECTED_SERVER";

    private static final String PROPERTY_SELECTED_SERVER2 = "SELECTED_SERVER#2";

    private static final String PROPERTY_SELECTED_SERVER3 = "SELECTED_SERVER#3";

    private static final String PROPERTY_USE_PRESELECTED = "USE_PRESELECTED";

    private static final String PROPERTY_USE_TELEKOMSERVER = "USE_TELEKOMSERVER";

    private static String[] serverList1;

    private static String[] serverList2;

    private static String[] serverList3;

    private static HashMap<String, String> serverMap = new HashMap<String, String>();

    public static void correctURL(DownloadLink downloadLink) {
        downloadLink.setUrlDownload(Rapidshare.getCorrectedURL(downloadLink.getDownloadURL()));
    }

    /**
     * Korrigiert die URL und befreit von subdomains etc.
     * 
     * @param link
     * @return
     */
    private static String getCorrectedURL(String link) {
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
        }
        String fileid = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/([\\d]{3,9})/?.*").getMatch(0);
        String filename = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/?(.*)").getMatch(0);
        return "http://rapidshare.com/files/" + fileid + "/" + filename;
    }

    public Rapidshare(PluginWrapper wrapper) {
        super(wrapper);

        serverMap.put("Cogent", "cg");
        serverMap.put("Cogent #2", "cg2");
        serverMap.put("Deutsche Telekom", "dt");
        serverMap.put("GlobalCrossing", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Level(3)", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("Tata Com.", "tg");
        serverMap.put("Tata Com. #2", "tg2");
        serverMap.put("Teleglobe", "tg");
        serverMap.put("Teleglobe #2", "tg2");
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("TeliaSonera #3", "tl3");

        serverList1 = new String[] { "cg", "cg2", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tl", "tl2" };
        serverList2 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "tg", "tg2", "tl", "tl2", "tl3" };
        serverList3 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tg2", "tl", "tl2" };

        setConfigElements();
        enablePremium("http://rapidshare.com/premium.html");
        this.setMaxConnections(30);

    }

    public int getTimegapBetweenConnections() {
        return 100;
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden.
     */
    public boolean[] checkLinks(DownloadLink[] urls) {
        try {
            if (urls == null) { return null; }
            boolean[] ret = new boolean[urls.length];
            int c = 0;
            ArrayList<Integer> sjlinks = new ArrayList<Integer>();
            while (true) {
                String post = "";
                int i = 0;
                boolean isRSCom = false;
                for (i = c; i < urls.length; i++) {
                    if (urls[i].getDownloadURL().matches("sjdp://rapidshare\\.com.*")) {
                        sjlinks.add(i);
                        ret[i] = false;
                    } else {
                        isRSCom = true;
                        if (!canHandle(urls[i].getDownloadURL())) { return null; }

                        urls[i].setUrlDownload(getCorrectedURL(urls[i].getDownloadURL()));

                        if ((post + urls[i].getDownloadURL() + "%0a").length() > 10000) {
                            break;
                        }
                        post += urls[i].getDownloadURL() + "%0a";
                    }

                }
                if (!isRSCom) return ret;
                PostRequest r = new PostRequest("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi");
                r.setPostVariable("urls", post);
                post = null;
                r.setPostVariable("toolmode", "1");
                String page = r.load();
                r = null;
                String[] lines = Regex.getLines(page);
                page = null;
                if (lines.length != i - c) {
                    lines = null;
                    System.gc();
                    return null;
                }

                for (String line : lines) {

                    String[] erg = line.split(",");
                    /*
                     * 1: Normal online -1: date nicht gefunden 3: Drect
                     * download
                     */
                    while (sjlinks.contains(c)) {
                        c++;
                    }
                    ret[c] = true;
                    if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) {
                        ret[c] = false;
                    } else {
                        urls[c].setDownloadSize(Integer.parseInt(erg[4]));
                        urls[c].setName(erg[5].trim());
                    }
                    c++;

                }
                if (c >= urls.length) {
                    lines = null;
                    System.gc();
                    return ret;
                }
                Thread.sleep(400);
            }

        } catch (Exception e) {
            System.gc();
            e.printStackTrace();
            return null;
        }

    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (Sniffy.hasSniffer()) throw new SnifferException();
        }
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (ddl)this.doPremium(downloadLink);
        Rapidshare.correctURL(downloadLink);

        // if (getRemainingWaittime() > 0) { return
        // handleDownloadLimit(downloadLink); }
        String freeOrPremiumSelectPostURL = null;
        Browser br = new Browser();

        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.setFollowRedirects(false);

        String link = downloadLink.getDownloadURL();

        // RS URL wird aufgerufen
        // req = new GetRequest(link);
        // req.load();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            logger.info("Direct Download");
            this.handlePremium(downloadLink, new Account("dummy", "dummy"));
            return;
        }
        // posturl für auswahl free7premium wird gesucht
        freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
        // Fehlerbehandlung auf der ersten Seite
        if (freeOrPremiumSelectPostURL == null) {
            String error = null;
            if ((error = findError(br + "")) != null) { throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error)); }
            reportUnknownError(br, 1);
            logger.warning("could not get newURL");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }

        // Post um freedownload auszuwählen
        Form[] forms = br.getForms();

        br.submitForm(forms[0]);
        // PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
        // pReq.setPostVariable("dl.start", "free");
        // pReq.load();
        String error = null;

        if ((error = findError(br + "")) != null) {
            // für java 1.5
            if (new Regex(error, "(kostenlose Nutzung erreicht)|(.*download.{0,3}limit.{1,50}free.{0,3}users.*)").matches()) {

                String waitfor = new Regex(br, "es in ca\\.(.*?)Minuten wieder").getMatch(0);
                if (waitfor == null) {
                    waitfor = new Regex(br, "Or try again in about(.*?)minutes").getMatch(0);

                }
                long waitTime = 60 * 60 * 1000l;
                try {
                    waitTime = new Long(waitfor.trim()) * 60 * 1000l;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
        }

        // Fehlersuche
        if (Regex.matches(br, PATTERN_MATCHER_TOO_MANY_USERS)) {
            logger.warning("Too many users are currently downloading this file. Wait 2 Minutes and try again");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 120 * 1000l);
        } else if (new Regex(br, PATTERM_MATCHER_ALREADY_LOADING).matches()) {
            logger.severe("Already downloading. Wait 2 min. or reconnect");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 120 * 1000l);
        } else if ((error = findError(br + "")) != null) {
            reportUnknownError(br, 2);
            throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
        }
        // Ticketwartezeit wird gesucht
        String ticketTime = new Regex(br, PATTERN_FIND_TICKET_WAITTIME).getMatch(0);
        if (ticketTime != null && ticketTime.equals("0")) {
            ticketTime = null;
        }

        String ticketCode = br + "";

        String tt = new Regex(ticketCode, "var tt =(.*?)document\\.getElementById\\(\"dl\"\\)\\.innerHTML").getMatch(0);

        String fun = "function f(){ return " + tt + "} f()";
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();

        // Collect the arguments into a single string.

        // Now evaluate the string we've colected.
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

        // Convert the result to a string and print it.
        String code = Context.toString(result);
        if (tt != null) ticketCode = code;
        Context.exit();
        if (ticketCode.contains("Leider sind derzeit keine freien Slots ")) {
            downloadLink.getLinkStatus().setStatusText("All free slots in use: try to download again after 2 minutes");
            logger.warning("All free slots in use: try to download again after 2 minutes");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 120000);
        }
        if (new Regex(ticketCode, ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches()) {
            String waitfor = new Regex(ticketCode, "Or try again in about(.*?)minutes").getMatch(0);
            long waitTime = 60 * 60 * 1000l;
            try {
                waitTime = new Long(waitfor.trim()) * 60 * 1000l;
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);

        }
        long pendingTime = 0;
        if (ticketTime != null) {
            pendingTime = Long.parseLong(ticketTime);

            if (getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                logger.warning("Waittime increased by JD: " + pendingTime + " --> " + (pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100));
                pendingTime = pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100;
            }
            pendingTime *= 1000;
        }

        waitTicketTime(downloadLink, pendingTime);

        String postTarget = getDownloadTarget(downloadLink, ticketCode);

        // Falls Serverauswahl fehlerhaft war
        if (linkStatus.isFailed()) return;

        Request request = br.createPostRequest(postTarget, "mirror=on&x=" + Math.random() * 40 + "&y=" + Math.random() * 40);

        // Download
        dl = new RAFDownload(this, downloadLink, request);

        HTTPConnection con = dl.connect();
        if (!con.isContentDisposition() && con.getHeaderField("Cache-Control") != null) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    /**
     * premiumdownload Methode
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (Sniffy.hasSniffer()) throw new SnifferException();
        }
        String freeOrPremiumSelectPostURL = null;
        Rapidshare.correctURL(downloadLink);
        br = login(account, true);
        br.setFollowRedirects(false);
        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.getPage(downloadLink.getDownloadURL());
        if (account.getStringProperty("premcookie", null) == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        String error;
        if ((error = findError(br.toString())) != null) {
            logger.warning(error);
            if (Regex.matches(error, Pattern.compile("(Betrugserkennung)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."), LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
            if (Regex.matches(error, Pattern.compile("(expired|abgelaufen)"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            } else if (Regex.matches(error, Pattern.compile("(You have exceeded the download limit|Sie haben heute das Limit überschritten)"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.limitexeeded", "You have exceeded the download limit."), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (Regex.matches(error, Pattern.compile("Passwort ist falsch"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            } else if (Regex.matches(error, Pattern.compile("IP"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (Regex.matches(error, Pattern.compile("(Account wurde nicht gefunden|Your Premium Account has not been found)"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.accountnotfound", "Your Premium Account has not been found."), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
            }
        }

        // posturl für auswahl wird gesucht
        freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
        // Fehlerbehandlung auf der ersten Seite
        if (freeOrPremiumSelectPostURL == null) {
            if ((error = findError(br + "")) != null) { throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error)); }
            reportUnknownError(br, 1);
            logger.warning("could not get newURL");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        // Post um Premium auszuwählen
        Form[] forms = br.getForms();
        br.submitForm(forms[1]);
        String postTarget = getDownloadTarget(downloadLink, br.toString());
        if (postTarget == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        Request request = br.createGetRequest(postTarget);

        // Download
        dl = new RAFDownload(this, downloadLink, request);
        // Premiumdownloads sind resumefähig
        dl.setResume(true);
        // Premiumdownloads erlauben chunkload
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        HTTPConnection urlConnection;
        try {
            urlConnection = dl.connect(br);
        } catch (Exception e) {
            br.setRequest(request);
            request = br.createGetRequest(null);
            logger.info("Load from " + request.getUrl().toString().substring(0, 35));
            // Download
            dl = new RAFDownload(this, downloadLink, request);
            // Premiumdownloads sind resumefähig
            dl.setResume(true);
            // Premiumdownloads erlauben chunkload
            dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
            urlConnection = dl.connect(br);
        }
        // Download starten
        // prüft ob ein content disposition header geschickt wurde. Falls nicht,
        // ist es eintweder eine Bilddatei oder eine Fehlerseite. BIldfiles
        // haben keinen Cache-Control Header
        if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
            // Lädt die zuletzt aufgebaute vernindung
            br.setRequest(request);
            br.followConnection();

            // Fehlerbehanldung
            /*
             * Achtung! keine Parsing arbeiten an diesem String!!!
             */
            if ((error = findError(br.toString())) != null) {
                logger.warning(error);
                if (Regex.matches(error, Pattern.compile("(Betrugserkennung)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."), LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
                if (Regex.matches(error, Pattern.compile("(expired|abgelaufen)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("(You have exceeded the download limit|Sie haben heute das Limit überschritten)"))) {

                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.limitexeeded", "You have exceeded the download limit."), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("Passwort ist falsch"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);

                } else if (Regex.matches(error, Pattern.compile("IP"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("(Account wurde nicht gefunden|Your Premium Account has not been found)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.accountnotfound", "Your Premium Account has not been found."), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
                }
            } else {
                reportUnknownError(br.toString(), 6);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

        }

        dl.startDownload();

    }

    private String findError(String string) {
        String error = null;
        error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE).getMatch(0);

        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_3).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_2).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_1).getMatch(0);
        }

        error = Encoding.htmlDecode(error);
        String[] er = Regex.getLines(error);

        if (er == null || er.length == 0) { return null; }
        er[0] = HTMLEntities.unhtmlentities(er[0]);
        if (er[0] == null || er[0].length() == 0) { return null; }
        return er[0];

    }

    private String dynTranslate(String error) {
        String error2 = JDLocale.L("plugins.host.rapidshare.errors." + JDHash.getMD5(error) + "", error);
        if (error.equals(error2)) {
            logger.warning("NO TRANSLATIONKEY FOUND FOR: " + error + "(" + JDHash.getMD5(error) + ")");
        }
        return error2;
    }

    public String getAGBLink() {
        return "http://rapidshare.com/faq.html";
    }

    /**
     * Sucht im ticketcode nach der entgültigen DownloadURL Diese Downlaodurl
     * beinhaltet in ihrer Subdomain den zielserver. Durch Anpassung dieses
     * Zielservers kann also die Serverauswahl vorgenommen werden.
     * 
     * @param step
     * @param downloadLink
     * @param ticketCode
     * @return
     */
    private String getDownloadTarget(DownloadLink downloadLink, String ticketCode) {

        String postTarget = new Regex(ticketCode, PATTERN_FIND_DOWNLOAD_POST_URL).getMatch(0);

        String server1 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
        String server3 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER3, "TeliaSonera");

        String serverAbb = serverMap.get(server1);
        String server2Abb = serverMap.get(server2);
        String server3Abb = serverMap.get(server3);
        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer("Use Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Use Random #2 server " + server2Abb);
        }
        if (server3Abb == null) {
            server3Abb = serverList3[(int) (Math.random() * (serverList3.length - 1))];
            logger.finer("Use Random #3 server " + server3Abb);
        }
        // String endServerAbb = "";
        boolean telekom = getPluginConfig().getBooleanProperty(PROPERTY_USE_TELEKOMSERVER, false);
        boolean preselected = getPluginConfig().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

        if (postTarget == null) {
            logger.severe("postTarget not found:");
            reportUnknownError(ticketCode, 4);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_RETRY);
            return null;
        }
        String[] serverstrings = new Regex(ticketCode, PATTERN_FIND_MIRROR_URLS).getColumn(0);
        logger.info("wished Mirror #1 Server " + serverAbb);
        logger.info("wished Mirror #2 Server " + server2Abb);
        logger.info("wished Mirror #3 Server " + server3Abb);
        String selected = new Regex(ticketCode, PATTERN_FIND_PRESELECTED_SERVER).getMatch(0);
        logger.info("Preselected Server: " + selected.substring(0, 30));
        if (preselected) {
            logger.info("RS.com Use preselected : " + selected.substring(0, 30));
            postTarget = selected;
        } else if (telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
            logger.info("RS.com Use Telekom Server");
            postTarget = getURL(serverstrings, "Deutsche Telekom", postTarget);
        } else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #1 Server: " + getServerName(serverAbb));
            postTarget = getURL(serverstrings, getServerName(serverAbb), postTarget);
        } else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #2 Server: " + getServerName(server2Abb));
            postTarget = getURL(serverstrings, getServerName(server2Abb), postTarget);
        } else if (ticketCode.indexOf(server3Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #3 Server: " + getServerName(server3Abb));
            postTarget = getURL(serverstrings, getServerName(server3Abb), postTarget);
        } else if (serverstrings.length > 0) {
            logger.severe("Kein Server gefunden 1");
        } else {
            logger.severe("Kein Server gefunden 2");
        }

        return postTarget;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return false;
        if (System.currentTimeMillis() - LAST_FILE_CHECK < 250) {
            try {
                Thread.sleep(System.currentTimeMillis() - LAST_FILE_CHECK);
            } catch (InterruptedException e) {
            }
        }
        Rapidshare.correctURL(downloadLink);
        LAST_FILE_CHECK = System.currentTimeMillis();

        String[] erg = br.getPage("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi?urls=" + downloadLink.getDownloadURL() + "&toolmode=1").trim().split(",");
        /*
         * 1: Normal online -1: date nicht gefunden 3: Drect download
         */
        if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) { return false; }

        downloadLink.setName(erg[5]);
        downloadLink.setDownloadSize(Integer.parseInt(erg[4]));
        downloadLink.setDupecheckAllowed(true);

        return true;
    }

    private String getServerName(String id) {
        Iterator<Entry<String, String>> it = serverMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            if (next.getValue().equalsIgnoreCase(id)) { return next.getKey(); }
        }
        return null;
    }

    private String getURL(String[] serverstrings, String selected, String postTarget) {
        if (!serverMap.containsKey(selected.trim())) {
            logger.severe("Unknown Servername: " + selected);
            return postTarget;
        }
        String abb = serverMap.get(selected.trim());

        for (String url : serverstrings) {
            if (url.contains(abb + ".rapidshare.com")) {
                logger.info("Load from " + selected + "(" + abb + ")");
                return url;
            }
        }

        logger.warning("No Serverstring found for " + abb + "(" + selected + ")");
        return postTarget;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void reportUnknownError(Object req, int id) {
        logger.severe("Unknown error(" + id + "). please add this htmlcode to your bugreport:\r\n" + req);
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    public void reset() {
    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {

        Vector<String> m1 = new Vector<String>();
        Vector<String> m2 = new Vector<String>();
        Vector<String> m3 = new Vector<String>();
        for (String element : serverList1) {
            m1.add(getServerName(element));
        }
        for (String element : serverList2) {
            m2.add(getServerName(element));
        }
        for (String element : serverList3) {
            m3.add(getServerName(element));
        }
        m1.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m2.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m3.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1").setDefaultValue("Level(3)"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2").setDefaultValue("TeliaSonera"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER3, m3.toArray(new String[] {}), "#3").setDefaultValue("TeliaSonera"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_TELEKOMSERVER, JDLocale.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_PRESELECTED, JDLocale.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_INCREASE_TICKET, JDLocale.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setStep(1));
    }

    public Browser login(Account account, boolean usesavedcookie) throws IOException, PluginException {
        Browser br = new Browser();
        String cookie = account.getStringProperty("premcookie", null);
        if (usesavedcookie && cookie != null) {
            br.setCookie("http://rapidshare.com", "user", cookie);
            return br;
        }
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        cookie = br.getCookie("http://rapidshare.com", "user");
        account.setProperty("premcookie", cookie);
        return br;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        br = login(account, false);
        if (account.getStringProperty("premcookie", null) == null || account.getUser().equals("") || account.getPass().equals("") || br.getRegex("(wurde nicht gefunden|Your Premium Account has not been found)").matches() || br.getRegex("but the password is incorrect").matches() || br.getRegex("Fraud detected, Account").matches()) {

            String error = findError("" + br);
            if (error != null) {
                if (error.contains("Fraud")) {
                    ai.setStatus(JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."));
                } else {
                    ai.setStatus(this.dynTranslate(error));
                }
            }
            ai.setValid(false);
            account.setProperty("premcookie", null);
            return ai;
        }

        String validUntil = br.getRegex("<td>(Expiration date|G\\&uuml\\;ltig bis)\\:</td><td style=.*?><b>(.*?)</b></td>").getMatch(1).trim();

        String trafficLeft = br.getRegex("<td>(Traffic left:|Traffic &uuml;brig:)</td><td align=right><b><script>document\\.write\\(setzeTT\\(\"\"\\+Math\\.ceil\\(([\\d]*?)\\/1000\\)\\)\\)\\;<\\/script> MB<\\/b><\\/td>").getMatch(1);
        String files = br.getRegex("<td>(Files:|Dateien:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        String rapidPoints = br.getRegex("<td>RapidPoints:</td><td.*?><b>(.*?)</b></td>").getMatch(0).trim();
        String newRapidPoints = br.getRegex(">RapidPoints PU</a>:</td><td.*?><b>(.*?)</b></td>").getMatch(0).trim();
        String usedSpace = br.getRegex("<td>(Used storage:|Belegter Speicher:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        String trafficShareLeft = br.getRegex("<td>(TrafficShare left:|TrafficShare &uuml;brig:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        ai.setTrafficLeft(Regex.getSize(trafficLeft + " Mb") / 1000);
        ai.setTrafficMax(10 * 1024 * 1024 * 1024l);
        ai.setFilesNum(Integer.parseInt(files));
        ai.setPremiumPoints(Integer.parseInt(rapidPoints));
        ai.setNewPremiumPoints(Integer.parseInt(newRapidPoints));
        ai.setUsedSpace(Regex.getSize(usedSpace));
        ai.setTrafficShareLeft(Regex.getSize(trafficShareLeft));
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy", Locale.UK);

        try {
            Date date = dateFormat.parse(validUntil);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            try {
                dateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy");
                Date date = dateFormat.parse(validUntil);
                ai.setValidUntil(date.getTime());
                e.printStackTrace();
            } catch (ParseException e2) {
                return null;
            }

        }

        if (br.containsHTML("expired") && br.containsHTML("if (1)")) {
            ai.setExpired(true);
            account.setProperty("premcookie", null);
        }

        return ai;
    }

    /**
     * Wartet die angegebene Ticketzeit ab
     * 
     * @param step
     * @param downloadLink
     * @param pendingTime
     * @throws InterruptedException
     */
    private void waitTicketTime(DownloadLink downloadLink, long pendingTime) throws InterruptedException {

        while (pendingTime > 0 && !downloadLink.isAborted()) {
            downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("plugin.rapidshare.tickettime", "Wait %s for ticket"), JDUtilities.formatSeconds((int) (pendingTime / 1000))));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);
            pendingTime -= 1000;
        }
    }
}
