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

import jd.PluginWrapper;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDMediaConvert;

public class MyVideo extends PluginForHost {
    static private final String AGB = "http://www.myvideo.de/news.php?rubrik=jjghf&p=hm8";

    public MyVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return AGB;
    }

    // @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {
        /*
         * warum sollte ein video das der decrypter sagte es sei online, offline
         * sein ;)
         */
        return true;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FATAL); }
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
        if (dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                ConversionMode convertto = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                ConversionMode InType = ConversionMode.VIDEOFLV;

                if (!JDMediaConvert.ConvertFile(downloadLink, InType, convertto)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

}