//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.storage.simplejson.JSonUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "data.hu" }, urls = { "http://[\\w\\.]*?data.hu/get/\\d+/[^<>\"/%]+" })
public class DataHu extends antiDDoSForHost {

    private static final String nice_host         = "data.hu";
    private static final String nice_hostproperty = nice_host.replaceAll("(\\.|\\-)", "");

    private int                 statuscode        = 0;

    public DataHu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://data.hu/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://data.hu/adatvedelem.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace(".html", ""));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    /*
     * Max 4 connections per downloadserver, if we try more this will end up in 503 responses. At the moment we allow 3 simultan DLs * 2
     * Chunks each.
     */
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    private void prepBR() {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    /** Using API: http://data.hu/api.php */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBR();
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            String checkurl = null;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once (limit = 50) */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    checkurl = "http://data.hu/get/" + getFID(dl) + "/";
                    sb.append(checkurl);
                    sb.append("%2C");
                }
                this.getAPISafe("http://data.hu/api.php?act=check_download_links&links=" + sb.toString());
                br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
                for (final DownloadLink dllink : links) {
                    final String added_url = dllink.getDownloadURL();
                    final String fid = getFID(dllink);
                    checkurl = "http://data.hu/get/" + fid + "/";
                    final String thisjson = br.getRegex("\"" + checkurl + "\":\\{(.*?)\\}").getMatch(0);
                    if (thisjson == null || !"online".equals(PluginJSonUtils.getJsonValue(thisjson, "status"))) {
                        dllink.setAvailable(false);
                    } else {
                        final String name = PluginJSonUtils.getJsonValue(thisjson, "filename");
                        final String size = PluginJSonUtils.getJsonValue(thisjson, "filesize");
                        final String md5 = PluginJSonUtils.getJsonValue(thisjson, "md5");
                        final String sha1 = PluginJSonUtils.getJsonValue(thisjson, "sha1");
                        /* Correct urls so when users copy them they can actually use them. */
                        if (!added_url.contains("name")) {
                            final String correctedurl = "http://data.hu/get/" + fid + "/" + name;
                            dllink.setUrlDownload(correctedurl);
                            try {
                                dllink.setContentUrl(correctedurl);
                            } catch (final Throwable e) {
                                /* Not available in old 0.9.581 Stable */
                            }
                        }
                        /* Names via API are good --> Use as final filenames */
                        dllink.setFinalFileName(name);
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
                        if (sha1 != null) {
                            dllink.setSha1Hash(sha1);
                        } else if (md5 != null) {
                            dllink.setMD5Hash(md5);
                        }
                        dllink.setAvailable(true);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!"premium".equals(PluginJSonUtils.getJsonValue(br, "type"))) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final String expiredate = PluginJSonUtils.getJsonValue(br, "expiration_date");
        if (expiredate != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expiredate, "yyyy-MM-dd mm:HH:ss", Locale.ENGLISH));
        }
        ai.setStatus("Premium account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        getPage(downloadLink.getDownloadURL());
        handleSiteErrors();
        if (br.containsHTML("class=\\'slow_dl_error_text\\'")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        final String link = br.getRegex(Pattern.compile("(?:\"|\\')(http://ddl\\d+\\.data\\.hu/get/\\d+/\\d+/.*?)(?:\"|\\')", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            /*
             * Wait a minute for respons 503 because JD tried to start too many downloads in a short time
             */
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.datahu.toomanysimultandownloads", "Too many simultan downloads, please wait some time!"), 60 * 1000l);
            }
            br.followConnection();
            handleSiteErrors();
            if (br.getURL().contains("data.hu/only_premium.php")) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) {
                        throw (PluginException) e;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();

    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        getAPISafe("http://data.hu/api.php?act=get_direct_link&link=" + PluginJSonUtils.escape(downloadLink.getDownloadURL()) + "&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
        final String link = PluginJSonUtils.getJsonValue(br, "direct_link");
        if (link == null) {
            /* Should never happen */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The finallink doesn't seem to be a file...");
            /*
             * Wait a minute for respons 503 because JD tried to start too many downloads in a short time
             */
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.datahu.toomanysimultandownloads", "Too many simultan downloads, please wait some time!"), 60 * 1000l);
            }
            br.followConnection();
            handleSiteErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void handleSiteErrors() throws PluginException {
        if (br.containsHTML("Az adott fájl nem létezik\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Az adott fájl már nem elérhető\\.")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBR();
        getAPISafe("http://data.hu/api.php?act=check_login_data&username=" + JSonUtils.escape(account.getUser()) + "&password=" + JSonUtils.escape(account.getPass()));
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "data.hu/get/(\\d+)").getMatch(0);
    }

    private void getAPISafe(final String accesslink) throws Exception {
        getPage(accesslink);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    private void postAPISafe(final String accesslink, final String postdata) throws Exception {
        postPage(accesslink, postdata);
        updatestatuscode();
        handleAPIErrors(this.br);
    }

    /**
     * 0 = everything ok, 1-99 = "error"-errors, 100-199 = "not_available"-errors, 200-299 = Other (html) [download] errors, sometimes mixed
     * with the API errors.
     */
    private void updatestatuscode() {
        String error = PluginJSonUtils.getJsonValue(br, "error");
        final String msg = PluginJSonUtils.getJsonValue(br, "msg");
        if (error != null && msg != null) {
            if (msg.equals("wrong username or password")) {
                statuscode = 1;
            } else if (msg.equals("no premium")) {
                statuscode = 2;
            } else if (msg.equals("wrong link")) {
                statuscode = 3;
            } else if (msg.equals("max 50 link can check")) {
                statuscode = 4;
            } else if (msg.equals("no act param defined")) {
                statuscode = 5;
            } else {
                statuscode = 666;
            }
        } else {
            /* No way to tell that something unpredictable happened here --> status should be fine. */
            statuscode = 0;
        }
    }

    private void handleAPIErrors(final Browser br) throws PluginException {
        String statusMessage = null;
        try {
            switch (statuscode) {
            case 0:
                /* Everything ok */
                break;
            case 1:
                /* Wrong username or password -> permanently disable account */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.";
                } else {
                    statusMessage = "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 2:
                /* Account = Free account tried a download in premium mode --> (Should never happen anyways) Permanently disable */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nNicht unterstützter Accounttyp!\r\nFalls du denkst diese Meldung sei falsch die Unterstützung dieses Account-Typs sich\r\ndeiner Meinung nach aus irgendeinem Grund lohnt,\r\nkontaktiere uns über das support Forum.";
                } else {
                    statusMessage = "\r\nUnsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum.";
                }
                throw new PluginException(LinkStatus.ERROR_PREMIUM, statusMessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 3:
                /* User tried to download an invalid link via account --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUngültiger Downloadlink";
                } else {
                    statusMessage = "\r\nInvalid downloadlink";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 4:
                /* We tried to check more than 50 links at the same time --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nKann maximal 50 Links gleichzeitig überprüfen";
                } else {
                    statusMessage = "\r\nMax simultaneous checkable links number equals 50";
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, statusMessage);
            case 5:
                /* 'no act param defined' --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nFATALER API Fehler";
                } else {
                    statusMessage = "\r\nFATAL API error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            case 666:
                /* SHTF --> Should never happen */
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    statusMessage = "\r\nUnbekannter Fehler";
                } else {
                    statusMessage = "\r\nUnknown error";
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, statusMessage);
            default:
                /* Completely Unknown error */
                statusMessage = "Unknown error";
                logger.info(nice_host + ": Unknown API error");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e) {
            logger.info(nice_host + ": Exception: statusCode: " + statuscode + " statusMessage: " + statusMessage);
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}