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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class PicourlDe extends PluginForDecrypt {

    final static String host = "picourl.de";

    private String version = "1.0.0.0";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?picourl\\.de/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public PicourlDe() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }



    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String link = new Regex(reqinfo.getHtmlCode(), Pattern.compile("src=\"(.*?)\">", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (link != null) {
                decryptedLinks.add(this.createDownloadlink(link));
            } else
                return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}