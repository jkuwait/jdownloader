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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class rapidsharknet extends PluginForDecrypt {

    static private final String host = "Rapidshark.net";
    private String version = "1.0.0.0";

    private static final Pattern patternLink_direct = Pattern.compile("http://[\\w\\.]*?rapidshark\\.net/(?!safe\\.php\\?id=)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_safephp = Pattern.compile("http://[\\w\\.]*?rapidshark\\.net/safe\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternLink_direct.pattern() + "|" + patternLink_safephp.pattern(), Pattern.CASE_INSENSITIVE);

    public rapidsharknet() {
        super();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo;

            if (cryptedLink.matches(patternLink_direct.pattern())) {
                String downloadid = url.getFile().substring(1);
                /* weiterleiten zur safephp Seite */
                decryptedLinks.add(this.createDownloadlink("http://rapidshark.net/safe.php?id=" + downloadid));
            } else if (cryptedLink.matches(patternLink_safephp.pattern())) {
                String downloadid = url.getFile().substring(13);
                requestInfo = HTTP.getRequest(url, null, "http://rapidshark.net/" + downloadid, false);
                downloadid = new Regex(requestInfo, "src=\"(.*)\"></iframe>").getFirstMatch();
                decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(downloadid)));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
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

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
