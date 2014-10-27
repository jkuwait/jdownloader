//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "copy.com" }, urls = { "https?://copydecrypted\\.com/(\\d+|[a-zA-Z0-9]+/[^\\s]+)" }, flags = { 0 })
public class CopyCom extends PluginForHost {

    public CopyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.copy.com/about/tos";
    }

    private static final String NOCHUNKS = "NOCHUNKS";
    private String              ddlink   = null;

    /**
     * Corrects downloadLink.urlDownload().<br/>
     * <br/>
     * The following code respect the hoster supported protocols via plugin boolean settings and users config preference
     *
     * @author raztoki
     * */
    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replace("copydecrypted.com", "copy.com"));
    }

    /** They got an API: https://www.copy.com/developer/documentation */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("ddlink", false)) {
            ddlink = link.getDownloadURL();
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(ddlink);
                if (con.isContentDisposition() && con.isOK()) {
                    // ddlink!
                    if (link.getFinalFileName() == null) {
                        link.setFinalFileName(getFileNameFromHeader(con));
                    }
                    link.setVerifiedFileSize(con.getContentLength());
                    link.setProperty("ddlink", true);
                    return AvailableStatus.TRUE;
                } else {
                    link.setProperty("ddlink", Property.NULL);
                    ddlink = null;
                    br.followConnection();
                }
            } finally {
                try {
                    /* make sure we close connection */
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getStringProperty("mainlink", null));

        if (br.containsHTML(">You&rsquo;ve found a page that doesn&rsquo;t exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_name", null);
        final String filesize = link.getStringProperty("plain_size", null);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(filename);
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (ddlink == null) {
            ddlink = downloadLink.getStringProperty("specified_link", null) + "?download=1";
        }

        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, ddlink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Cannot find requested object id")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Link abused */
            if (br.containsHTML("\"error_code\":\"1048\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(CopyCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(CopyCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(CopyCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(CopyCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}