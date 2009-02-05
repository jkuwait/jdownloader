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

package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class Uploadedto extends PluginForHost {

    static private final String AGB_LINK = "http://uploaded.to/agb";

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploaded.to/ref?id=70683&r");
        setMaxConnections(20);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "PREMIUMCHUNKS", JDLocale.L("plugins.hoster.uploadedto.chunks", "Premium connections # (>1 causes higher traffic)"), 1, 20).setDefaultValue(1).setStep(1));
    }

    /**
     * Korrigiert den Downloadlink in ein einheitliches Format
     * 
     * @param parameter
     */
    private void correctURL(DownloadLink parameter) {
        String link = parameter.getDownloadURL();
        link = link.replace("/?id=", "/file/");
        link = link.replace("?id=", "file/");
        link = link.replaceFirst("/\\?.*?&id=", "/file/");
        String[] parts = link.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }

        parameter.setUrlDownload(newLink);

    }

    public int getTimegapBetweenConnections() {
        return 800;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));

        br.submitForm(login);
        if (br.containsHTML("Login failed!")) {
            ai.setValid(false);
            ai.setStatus("User ID or password wrong");
            return ai;

        }
        String accounttype = br.getMatch("Accounttype:</span> <span class=.*?>(.*?)</span>");
        if (accounttype != null && accounttype.equalsIgnoreCase("free")) {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            ai.setAccountBalance((int) (Double.parseDouble(balance) * 100));
            ai.setPremiumPoints(Integer.parseInt(points));
            ai.setValidUntil(System.currentTimeMillis() + (356 * 24 * 60 * 60 * 1000l));
            ai.setStatus("Accounttyp: Collectorsaccount");
        } else {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            String traffic = br.getMatch("Traffic left: </span><span class=.*?>(.*?)</span> ");
            String expire = br.getMatch("Valid until: </span> <span class=.*?>(.*?)</span>");

            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd-MM-yyyy hh:mm", null));
            ai.setAccountBalance((int) (Double.parseDouble(balance) * 100));
            ai.setTrafficLeft(Regex.getSize(traffic));
            ai.setPremiumPoints(Integer.parseInt(points));
        }

        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        getFileInformation(downloadLink);
        br.setDebug(true);
        br.setCookie("http://uploaded.to/", "lang", "de");

        String user = account.getUser();

        logger.info("login");
        br.setFollowRedirects(true);
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));

        br.submitForm(login);
        if (br.containsHTML("Login failed!")) {
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Login Error: " + user);
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            return;

        }
        String accounttype = br.getMatch("Accounttyp:</span> <span class=.*?>(.*?)</span>");

        if (accounttype != null && accounttype.equalsIgnoreCase("free")) {
            logger.severe("Entered a Free-account");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Account " + user + " is a freeaccount");
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            return;

        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        checkPasswort(downloadLink);
        String error = new Regex(br.getRedirectLocation(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            if (error.equalsIgnoreCase("error_traffic")) {
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.uploadedto.premiumtrafficreached", "Traffic limit reached"));
                linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                return;

            }
            String message = JDLocale.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage("ServerError: " + message);
            return;

        }

        if (br.getRedirectLocation() == null) {
            logger.info("InDirect Downloads active");
            Form form = br.getFormbyValue("Download");
            HTTPConnection con = br.openFormConnection(form);
            if (br.getRedirectLocation() == null) {
                con.disconnect();
                logger.severe("Endlink not found");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage("Indirect Link Error");
                return;
            }
        } else {
            logger.info("Direct Downloads active");
        }

        br.setDebug(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, this.getPluginConfig().getIntegerProperty("PREMIUMCHUNKS", 1));

        dl.setFileSizeVerified(true);
        if (dl.getConnection().getContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(5 * 60 * 1000l);
            return;
        }
        dl.startDownload();
    }

    public String getAGBLink() {
        return AGB_LINK;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return false;

        this.setBrowserExclusive();
        correctURL(downloadLink);

        br.setFollowRedirects(true);
        String id = new Regex(downloadLink.getDownloadURL(), "uploaded.to/file/(.*?)/").getMatch(0);

        br.getPage("http://uploaded.to/api/file?id=" + id);
        String[] lines = Regex.getLines(br + "");
        try {
            String fileName = lines[0].trim();

            long fileSize = Long.parseLong(lines[1].trim());
            downloadLink.setFinalFileName(fileName);
            downloadLink.setDupecheckAllowed(true);
            downloadLink.setDownloadSize(fileSize);
            downloadLink.setSha1Hash(lines[2].trim());
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("error_fileremoved")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return true;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void checkPasswort(DownloadLink downloadLink) throws PluginException, InterruptedException, IOException {
        Form form = br.getForm(0);
        String passCode = null;
        if (form != null && form.getVars().containsKey("file_key")) {
            logger.info("pw protected link");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("file_key", passCode);
            br.setFollowRedirects(false);
            br.submitForm(form);
            form = br.getForm(0);
            if (form != null && form.getVars().containsKey("file_key")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        getFileInformation(downloadLink);
        br.setCookie("http://uploaded.to/", "lang", "de");
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        checkPasswort(downloadLink);
        if (br.containsHTML("ist aufgebraucht")) {
            long wait = Regex.getMilliSeconds(br.getRegex("\\(Oder warten Sie (.*?)\\!\\)").getMatch(0));
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            logger.info("Traffic Limit reached....");
            linkStatus.setValue(wait);
            return;
        }
        String error = new Regex(br.getURL(), "http://uploaded.to/\\?view=(.*?)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getURL(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            String message = JDLocale.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(message);
            return;

        }

        br.setFollowRedirects(false);

        Form form = br.getFormbyValue("Download");
        if (form != null) {
            form.put("download_submit", "Download");
            sleep(10000l, downloadLink);
            dl = br.openDownload(downloadLink, form, false, 1);
        } else {
            String dlLink = br.getRedirectLocation();
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_FATAL);
            dl = br.openDownload(downloadLink, dlLink, false, 1);
        }
        dl.fakeContentRangeHeader(false);
        dl.setFileSizeVerified(true);
        if (dl.getConnection().getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(5 * 60 * 1000l);
            return;
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

}
